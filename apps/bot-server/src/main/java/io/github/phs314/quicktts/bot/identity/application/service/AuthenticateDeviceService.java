package io.github.phs314.quicktts.bot.identity.application.service;

import io.github.phs314.quicktts.bot.identity.application.port.in.AuthenticateDeviceUseCase;
import io.github.phs314.quicktts.bot.identity.application.port.in.query.AuthenticateDeviceQuery;
import io.github.phs314.quicktts.bot.identity.application.port.out.DevicePort;
import io.github.phs314.quicktts.bot.identity.domain.Device;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceToken;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceTokenHash;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateDeviceService implements AuthenticateDeviceUseCase {

    private final DevicePort devicePort;
    private final Clock clock;

    @Override
    public Optional<DiscordUserId> authenticate(AuthenticateDeviceQuery query) {
        String rawDeviceToken = query.rawDeviceToken();
        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            return Optional.empty();
        }
        DeviceTokenHash tokenHash = new DeviceToken(rawDeviceToken).hash();
        return devicePort.findByTokenHash(tokenHash).map(this::recordUse);
    }

    /**
     * 오래 안 쓴 PC 를 가려낼 수 있게 마지막 사용 시각을 남긴다 (ADR 0078).
     * 요청마다 쓰지 않고 {@link Device#LAST_USED_PRECISION} 에 한 번만 한 줄을 고친다.
     */
    private DiscordUserId recordUse(Device device) {
        Instant now = clock.instant();
        if (device.needsLastUsedUpdate(now)) {
            devicePort.updateLastUsedAt(device.id(), now);
        }
        return device.userId();
    }
}
