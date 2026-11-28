package io.github.phs314.quicktts.bot.identity.domain;

import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceId;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceName;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceToken;
import io.github.phs314.quicktts.bot.identity.domain.vo.DeviceTokenHash;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 디스코드 사용자에게 연결된 PC 한 대. 같은 기기인지는 {@link #id()} 로만 가린다.
 */
public class Device {

    /**
     * 마지막 사용 시각을 다시 적기까지의 간격. 요청마다 DB 에 쓰지 않도록 이 정도로만 정확하게 남긴다 (ADR 0078).
     */
    public static final Duration LAST_USED_PRECISION = Duration.ofHours(1);

    /** 사용자가 기기를 골라 해제할 때 쓰는 식별자 */
    private final DeviceId id;
    /** 기기 토큰의 해시 ({@link DeviceToken#hash()}) */
    private final DeviceTokenHash tokenHash;
    /** 이 기기로 quick chat 을 보내는 사용자 */
    private final DiscordUserId userId;
    /** 사용자에게 보여 줄 PC 이름 */
    private final DeviceName name;
    /** 등록 시각 */
    private final Instant registeredAt;
    /** 이 기기 토큰으로 마지막으로 인증한 시각. {@link #LAST_USED_PRECISION} 만큼 늦을 수 있다. 등록 뒤 아직 안 썼으면 null */
    private final @Nullable Instant lastUsedAt;

    private Device(DeviceId id, DeviceTokenHash tokenHash, DiscordUserId userId, DeviceName name, Instant registeredAt,
            @Nullable Instant lastUsedAt) {
        this.id = Objects.requireNonNull(id);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.userId = Objects.requireNonNull(userId);
        this.name = Objects.requireNonNull(name);
        this.registeredAt = Objects.requireNonNull(registeredAt);
        this.lastUsedAt = lastUsedAt;
    }

    public static Device register(DeviceToken token, DiscordUserId userId, DeviceName name, Instant now) {
        return new Device(DeviceId.generate(), token.hash(), userId, name, now, null);
    }

    /** 저장소에서 읽어 온 기기를 되살릴 때 쓴다. 새로 등록할 때는 {@link #register} 를 쓴다. */
    public static Device restore(DeviceId id, DeviceTokenHash tokenHash, DiscordUserId userId, DeviceName name,
            Instant registeredAt, @Nullable Instant lastUsedAt) {
        return new Device(id, tokenHash, userId, name, registeredAt, lastUsedAt);
    }

    /** 지금 쓴 것을 마지막 사용 시각으로 적어야 하는지. 아직 안 썼거나 적은 지 {@link #LAST_USED_PRECISION} 이 지났으면 적는다. */
    public boolean needsLastUsedUpdate(Instant now) {
        return lastUsedAt == null || !lastUsedAt.plus(LAST_USED_PRECISION).isAfter(now);
    }

    /** 그 사용자의 기기인지. 기기를 보거나 해제할 수 있는 사람은 그 사용자뿐이다. */
    public boolean belongsTo(DiscordUserId user) {
        return userId.equals(user);
    }

    public DeviceId id() {
        return id;
    }

    public DeviceTokenHash tokenHash() {
        return tokenHash;
    }

    public DiscordUserId userId() {
        return userId;
    }

    public DeviceName name() {
        return name;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public Optional<Instant> lastUsedAt() {
        return Optional.ofNullable(lastUsedAt);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Device other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Device[id=" + id.value() + ", userId=" + userId + ", name=" + name.value() + "]";
    }
}
