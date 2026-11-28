package io.github.phs314.quicktts.bot.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phs314.quicktts.bot.identity.application.port.in.query.AuthenticateDeviceQuery;
import io.github.phs314.quicktts.bot.identity.application.port.out.FakeDevicePort;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceName;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceToken;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuthenticateDeviceServiceTest {

    private static final DiscordUserId USER = new DiscordUserId(42L);
    private static final Instant NOW = Instant.parse("2026-10-05T00:00:00Z");

    private final FakeDevicePort devices = new FakeDevicePort();
    private final MutableClock clock = new MutableClock(NOW);
    private final AuthenticateDeviceService service = new AuthenticateDeviceService(devices, clock);

    @Test
    void 등록된_기기_토큰은_그_기기를_연결한_사용자로_인증된다() {
        DeviceToken token = devices.registered(USER, new DeviceName("현수-PC"), NOW);

        assertThat(service.authenticate(new AuthenticateDeviceQuery(token.value()))).contains(USER);
    }

    @Test
    void 등록되지_않은_토큰은_인증되지_않는다() {
        assertThat(service.authenticate(new AuthenticateDeviceQuery(DeviceToken.generate().value()))).isEmpty();
    }

    @Test
    void 빈_토큰은_인증되지_않는다() {
        assertThat(service.authenticate(new AuthenticateDeviceQuery(null))).isEmpty();
        assertThat(service.authenticate(new AuthenticateDeviceQuery(" "))).isEmpty();
    }

    @Test
    void 인증하면_마지막_사용_시각을_남긴다() {
        DeviceToken token = devices.registered(USER, new DeviceName("현수-PC"), NOW);
        clock.now = NOW.plus(Duration.ofDays(3));

        service.authenticate(new AuthenticateDeviceQuery(token.value()));

        assertThat(devices.findByTokenHash(token.hash()).orElseThrow().lastUsedAt()).contains(clock.now);
    }

    @Test
    void 마지막_사용_시각은_한_시간에_한_번만_고친다() {
        DeviceToken token = devices.registered(USER, new DeviceName("현수-PC"), NOW);
        service.authenticate(new AuthenticateDeviceQuery(token.value()));

        clock.now = NOW.plus(Duration.ofMinutes(59));
        service.authenticate(new AuthenticateDeviceQuery(token.value()));
        assertThat(devices.findByTokenHash(token.hash()).orElseThrow().lastUsedAt()).contains(NOW);

        clock.now = NOW.plus(Duration.ofHours(1));
        service.authenticate(new AuthenticateDeviceQuery(token.value()));
        assertThat(devices.findByTokenHash(token.hash()).orElseThrow().lastUsedAt()).contains(clock.now);
    }

    /** 테스트 안에서 시각을 앞으로 돌리는 시계. */
    private static final class MutableClock extends Clock {

        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
