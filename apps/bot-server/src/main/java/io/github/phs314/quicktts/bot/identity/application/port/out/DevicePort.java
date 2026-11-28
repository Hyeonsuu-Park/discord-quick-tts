package io.github.phs314.quicktts.bot.identity.application.port.out;

import io.github.phs314.quicktts.bot.identity.domain.Device;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceId;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceTokenHash;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DevicePort {

    void save(Device device);

    Optional<Device> findByTokenHash(DeviceTokenHash tokenHash);

    /** 등록한 순서대로 돌려준다. */
    List<Device> findByUserId(DiscordUserId userId);

    Optional<Device> findById(DeviceId id);

    /** 마지막 사용 시각만 고친다. 기기가 이미 없으면 아무것도 하지 않는다. */
    void updateLastUsedAt(DeviceId id, Instant usedAt);

    /** 지웠으면 true, 이미 없었으면 false. */
    boolean delete(DeviceId id);

    /** 지운 기기 수를 돌려준다. */
    int deleteAllByUserId(DiscordUserId userId);
}
