package io.github.phs314.quicktts.client;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 사용자 홈의 {@code .discord-quick-tts/client.properties} 에 저장하는 클라이언트 설정.
 * 기기 토큰은 디스코드 {@code /연결} 코드로 등록할 때 앱이 직접 채운다.
 * Windows 에서는 기기 토큰을 DPAPI 로 암호화해 적는다 ({@link DeviceTokenProtection}, ADR 0077).
 */
public record ClientConfig(String serverUrl, String deviceToken, Keybind keybind) {

    public static final Path FILE = Path.of(System.getProperty("user.home"), ".discord-quick-tts", "client.properties");

    /** 배포할 때는 {@code -Dquicktts.server-url=...} 로 기본 서버 주소를 바꾼다. */
    private static final String DEFAULT_SERVER_URL = System.getProperty("quicktts.server-url", "http://localhost:8080");

    /** 예전 판이 단축키를 적던 키. 읽으면 {@code keybind=} 로 옮겨 적는다. */
    private static final String LEGACY_KEYBIND_KEY = "hotkey";

    private static final String TEMPLATE = """
            # discord-quick-tts 데스크톱 클라이언트 설정
            # 봇 서버 주소
            server-url=%s
            # 이 PC 의 기기 토큰. 앱이 디스코드 /연결 코드로 등록할 때 채운다. 다른 사람에게 보여 주지 마세요.
            # Windows 에서는 이 Windows 사용자만 풀 수 있게 암호화해 둔다(dpapi:). 다른 PC 로 복사하면 /연결 을 다시 해야 한다.
            device-token=%s
            # 입력창을 여는 단축키. 트레이 메뉴의 Change Keybind... 에서 바꾼다. 예: Ctrl+Shift+Space, Ctrl+Alt+T, F9
            keybind=%s
            """;

    public boolean isRegistered() {
        return !deviceToken.isBlank();
    }

    public ClientConfig withDeviceToken(String newDeviceToken) {
        return new ClientConfig(serverUrl, newDeviceToken, keybind);
    }

    public ClientConfig withKeybind(Keybind newKeybind) {
        return new ClientConfig(serverUrl, deviceToken, newKeybind);
    }

    public static ClientConfig load() {
        return load(FILE, DeviceTokenProtection.forThisPc());
    }

    static ClientConfig load(Path file) {
        return load(file, DeviceTokenProtection.NONE);
    }

    /**
     * 단축키가 없거나 잘못 적혀 있으면 기본 단축키를 쓴다.
     * 예전 판의 {@code hotkey=} 만 있으면 그 값을 읽고 파일을 {@code keybind=} 로 다시 적는다.
     * 예전 판이 평문으로 적은 기기 토큰은 읽은 뒤 암호화해 다시 적는다.
     * 기기 토큰을 이 PC 에서 풀 수 없으면(다른 PC 에서 복사한 파일 등) 연결하지 않은 것으로 본다.
     */
    static ClientConfig load(Path file, DeviceTokenProtection protection) {
        if (Files.notExists(file)) {
            return new ClientConfig(DEFAULT_SERVER_URL, "", Keybind.DEFAULT);
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("설정 파일을 읽지 못했습니다: " + file, e);
        }
        String serverUrl = properties.getProperty("server-url", "").strip();
        String keybind = properties.getProperty("keybind");
        boolean legacy = keybind == null && properties.containsKey(LEGACY_KEYBIND_KEY);
        if (legacy) {
            keybind = properties.getProperty(LEGACY_KEYBIND_KEY);
        }
        String storedToken = properties.getProperty("device-token", "").strip();
        ClientConfig config = new ClientConfig(
                serverUrl.isEmpty() ? DEFAULT_SERVER_URL : serverUrl,
                protection.unprotect(storedToken).orElse(""),
                Keybind.parse(keybind == null ? "" : keybind).orElse(Keybind.DEFAULT));
        if (legacy || protection.needsProtecting(storedToken)) {
            config.save(file, protection);
        }
        return config;
    }

    public void save() {
        save(FILE, DeviceTokenProtection.forThisPc());
    }

    void save(Path file) {
        save(file, DeviceTokenProtection.NONE);
    }

    void save(Path file, DeviceTokenProtection protection) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, TEMPLATE.formatted(serverUrl, protection.protect(deviceToken), keybind),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("설정 파일을 저장하지 못했습니다: " + file, e);
        }
    }
}
