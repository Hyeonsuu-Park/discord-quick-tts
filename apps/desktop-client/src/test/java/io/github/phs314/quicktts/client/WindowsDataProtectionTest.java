package io.github.phs314.quicktts.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * 진짜 Windows DPAPI 로 기기 토큰을 암호화하고 푸는지 본다. Windows 에서만 돈다(CI 의 설치 파일 작업).
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsDataProtectionTest {

    @Test
    void 암호화한_값을_다시_풀면_원래_값이다() {
        byte[] plain = "secret-token".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = WindowsDataProtection.protect(plain);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(WindowsDataProtection.unprotect(encrypted)).isEqualTo(plain);
    }

    @Test
    void 망가진_값은_풀지_못한다() {
        assertThatThrownBy(() -> WindowsDataProtection.unprotect(new byte[] {1, 2, 3}))
                .isInstanceOf(WindowsDataProtection.DataProtectionException.class);
    }

    @Test
    void 설정_파일의_토큰은_dpapi_로_적고_다시_읽는다() {
        String stored = DeviceTokenProtection.WINDOWS_DPAPI.protect("secret-token");

        assertThat(stored).startsWith("dpapi:").doesNotContain("secret-token");
        assertThat(DeviceTokenProtection.WINDOWS_DPAPI.unprotect(stored)).contains("secret-token");
        assertThat(DeviceTokenProtection.WINDOWS_DPAPI.unprotect("dpapi:AAAA")).isEmpty();
    }
}
