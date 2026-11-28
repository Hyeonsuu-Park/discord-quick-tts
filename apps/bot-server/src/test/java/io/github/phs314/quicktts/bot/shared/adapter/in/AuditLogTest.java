package io.github.phs314.quicktts.bot.shared.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditLogTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AuditLog.LOGGER);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void 사건과_키_값을_한_줄로_남기고_키_값으로도_붙인다() {
        AuditLog.record("device.registered", "device-name", "현수 PC", "ip", "203.0.113.7");

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage())
                .isEqualTo("audit event=device.registered device-name=\"현수 PC\" ip=203.0.113.7");
        assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key + "=" + pair.value)
                .containsExactly("event=device.registered", "device-name=현수 PC", "ip=203.0.113.7");
    }

    @Test
    void 키와_값의_짝이_안_맞으면_남기지_않는다() {
        assertThatThrownBy(() -> AuditLog.record("x", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(appender.list).isEmpty();
    }
}
