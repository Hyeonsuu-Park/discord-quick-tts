package io.github.phs314.quicktts.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * 설정 파일에 기기 토큰을 적을 때 쓰는 보호 방식 (ADR 0077).
 * Windows 에서는 DPAPI 로 암호화해서 이 PC 의 이 Windows 사용자만 풀 수 있게 한다.
 * 파일을 다른 PC 나 다른 사용자에게 복사해도 토큰으로 쓸 수 없다.
 */
interface DeviceTokenProtection {

    /** 설정 파일에 적을 값. 빈 토큰은 빈 값 그대로다. */
    String protect(String deviceToken);

    /** 설정 파일의 값에서 토큰을 꺼낸다. 이 PC 에서 풀 수 없으면 빈 값이다. */
    Optional<String> unprotect(String stored);

    /** 예전 판이 평문으로 적은 토큰처럼, 다시 적어야 하는 값인지. */
    boolean needsProtecting(String stored);

    /** 이 PC 에 맞는 방식. Windows 면 DPAPI, 아니면(개발 중인 macOS·리눅스) 평문이다. */
    static DeviceTokenProtection forThisPc() {
        return WindowsDataProtection.isAvailable() ? WINDOWS_DPAPI : NONE;
    }

    /** 평문으로 적는다. Windows 가 아닌 개발 PC 와 테스트가 쓴다. DPAPI 로 적힌 값은 풀지 못한다. */
    DeviceTokenProtection NONE = new DeviceTokenProtection() {

        @Override
        public String protect(String deviceToken) {
            return deviceToken;
        }

        @Override
        public Optional<String> unprotect(String stored) {
            return stored.startsWith(Dpapi.PREFIX) ? Optional.empty() : Optional.of(stored);
        }

        @Override
        public boolean needsProtecting(String stored) {
            return false;
        }
    };

    /** {@code dpapi:<Base64>} 로 적는다. 예전 판의 평문 값도 읽고, 읽은 뒤 암호화해 다시 적는다. */
    DeviceTokenProtection WINDOWS_DPAPI = new Dpapi();

    final class Dpapi implements DeviceTokenProtection {

        static final String PREFIX = "dpapi:";

        @Override
        public String protect(String deviceToken) {
            if (deviceToken.isEmpty()) {
                return "";
            }
            byte[] encrypted = WindowsDataProtection.protect(deviceToken.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        }

        @Override
        public Optional<String> unprotect(String stored) {
            if (!stored.startsWith(PREFIX)) {
                return Optional.of(stored);
            }
            try {
                byte[] encrypted = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
                return Optional.of(new String(WindowsDataProtection.unprotect(encrypted), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException | WindowsDataProtection.DataProtectionException e) {
                return Optional.empty();
            }
        }

        @Override
        public boolean needsProtecting(String stored) {
            return !stored.isEmpty() && !stored.startsWith(PREFIX);
        }
    }
}
