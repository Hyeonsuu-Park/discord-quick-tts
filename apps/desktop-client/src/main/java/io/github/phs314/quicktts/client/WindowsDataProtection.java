package io.github.phs314.quicktts.client;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

/**
 * Windows DPAPI({@code CryptProtectData}, {@code CryptUnprotectData})로 바이트를 암호화한다 (ADR 0077).
 * 열쇠는 Windows 가 로그인한 사용자 계정에 묶어 관리하므로, 같은 PC 의 같은 사용자만 풀 수 있다.
 * 따로 열쇠 파일을 두지 않고, 다른 라이브러리 없이 자바의 FFM 으로 부른다.
 */
final class WindowsDataProtection {

    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");

    /** 같은 사용자의 다른 프로그램이 DPAPI 로 감싼 값과 섞이지 않게 더하는 값. 바꾸면 이미 적은 토큰을 풀지 못한다. */
    private static final byte[] ENTROPY = "discord-quick-tts device-token".getBytes(StandardCharsets.UTF_8);

    /** 확인 창을 띄우지 않는다. 띄워야 하는 상황이면 실패한다. */
    private static final int CRYPTPROTECT_UI_FORBIDDEN = 0x1;

    private WindowsDataProtection() {
    }

    static boolean isAvailable() {
        return WINDOWS;
    }

    static byte[] protect(byte[] plain) {
        return call(Win32.CRYPT_PROTECT_DATA, plain, "암호화");
    }

    /** @throws DataProtectionException 다른 PC·다른 사용자가 암호화했거나 값이 망가졌을 때 */
    static byte[] unprotect(byte[] encrypted) {
        return call(Win32.CRYPT_UNPROTECT_DATA, encrypted, "복호화");
    }

    private static byte[] call(MethodHandle function, byte[] input, String action) {
        if (!WINDOWS) {
            throw new DataProtectionException("Windows 가 아니면 DPAPI 를 쓸 수 없습니다.");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment in = blob(arena, input);
            MemorySegment entropy = blob(arena, ENTROPY);
            MemorySegment out = arena.allocate(Win32.DATA_BLOB);
            int succeeded = (int) function.invokeExact(in, MemorySegment.NULL, entropy, MemorySegment.NULL,
                    MemorySegment.NULL, CRYPTPROTECT_UI_FORBIDDEN, out);
            if (succeeded == 0) {
                throw new DataProtectionException("DPAPI " + action + "에 실패했습니다.");
            }
            int size = (int) Win32.CB_DATA.get(out, 0L);
            MemorySegment data = ((MemorySegment) Win32.PB_DATA.get(out, 0L)).reinterpret(size);
            try {
                return data.toArray(JAVA_BYTE);
            } finally {
                // DPAPI 가 돌려준 메모리는 LocalFree 로 돌려준다.
                MemorySegment _ = (MemorySegment) Win32.LOCAL_FREE.invokeExact(data);
            }
        } catch (DataProtectionException e) {
            throw e;
        } catch (Throwable e) {
            throw new DataProtectionException("DPAPI " + action + "에 실패했습니다: " + e);
        }
    }

    private static MemorySegment blob(Arena arena, byte[] bytes) {
        MemorySegment data = arena.allocateFrom(JAVA_BYTE, bytes);
        MemorySegment blob = arena.allocate(Win32.DATA_BLOB);
        Win32.CB_DATA.set(blob, 0L, bytes.length);
        Win32.PB_DATA.set(blob, 0L, data);
        return blob;
    }

    static final class DataProtectionException extends RuntimeException {

        DataProtectionException(String message) {
            super(message);
        }
    }

    /** crypt32·kernel32 함수. 윈도우에서 처음 쓸 때만 불러온다. */
    private static final class Win32 {

        /** {@code DATA_BLOB { DWORD cbData; BYTE *pbData; }}. 64비트에서는 포인터 앞에 4바이트가 빈다. */
        static final StructLayout DATA_BLOB = MemoryLayout.structLayout(
                JAVA_INT.withName("cbData"), MemoryLayout.paddingLayout(4), ADDRESS.withName("pbData"));
        static final VarHandle CB_DATA = DATA_BLOB.varHandle(MemoryLayout.PathElement.groupElement("cbData"));
        static final VarHandle PB_DATA = DATA_BLOB.varHandle(MemoryLayout.PathElement.groupElement("pbData"));

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup CRYPT32 = SymbolLookup.libraryLookup("crypt32", Arena.global());
        private static final SymbolLookup KERNEL32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

        /** 두 함수 모두 (입력, 설명, 엔트로피, 예약, 확인 창, 플래그, 출력) -> BOOL 이다. */
        private static final FunctionDescriptor CRYPT_FUNCTION = FunctionDescriptor.of(JAVA_INT,
                ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

        static final MethodHandle CRYPT_PROTECT_DATA = downcall(CRYPT32, "CryptProtectData", CRYPT_FUNCTION);
        static final MethodHandle CRYPT_UNPROTECT_DATA = downcall(CRYPT32, "CryptUnprotectData", CRYPT_FUNCTION);
        static final MethodHandle LOCAL_FREE =
                downcall(KERNEL32, "LocalFree", FunctionDescriptor.of(ADDRESS, ADDRESS));

        private static MethodHandle downcall(SymbolLookup library, String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(library.findOrThrow(name), descriptor);
        }
    }
}
