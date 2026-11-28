package io.github.phs314.quicktts.bot.shared.adapter.in;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * 누가 무엇을 했는지 남기는 감사 로그 (ADR 0075). 로거 이름은 {@value #LOGGER} 이고 한 줄은
 * {@code audit event=<사건> <키>=<값> ...} 모양이라 {@code grep "audit event="} 로 모아 본다.
 * 같은 키와 값을 SLF4J 키-값으로도 붙여서, 나중에 JSON 로그로 바꾸면 그대로 필드가 된다.
 * <p>
 * 행동을 받은 입력 어댑터(디스코드 명령, 웹 요청)에서 남긴다. 비밀(기기 토큰, 연결 코드)은 남기지 않는다.
 */
public final class AuditLog {

    public static final String LOGGER = "quicktts.audit";

    private static final Logger log = LoggerFactory.getLogger(LOGGER);

    private AuditLog() {
    }

    /**
     * @param event     사건 이름. {@code <대상>.<한 일>} 모양(예: {@code device.registered})
     * @param keyValues 키, 값, 키, 값 ... 순서. 값에 빈칸이 있으면 따옴표로 감싼다.
     */
    public static void record(String event, Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("키와 값은 짝이 맞아야 합니다: " + event);
        }
        LoggingEventBuilder builder = log.atInfo().addKeyValue("event", event);
        StringBuilder line = new StringBuilder("audit event=").append(event);
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            String value = Objects.toString(keyValues[i + 1]);
            builder = builder.addKeyValue(key, value);
            line.append(' ').append(key).append('=').append(quoted(value));
        }
        builder.log(line.toString());
    }

    private static String quoted(String value) {
        return value.isEmpty() || value.chars().anyMatch(Character::isWhitespace)
                ? '"' + value.replace("\"", "'") + '"'
                : value;
    }
}
