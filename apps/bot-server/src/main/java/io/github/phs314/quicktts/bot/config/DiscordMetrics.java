package io.github.phs314.quicktts.bot.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;

/**
 * 디스코드 샤드 수와 게이트웨이에 붙어 있는 샤드 수를 지표로 낸다. 상태 확인({@link DiscordHealthIndicator})과 같은 기준이다.
 * 알림 규칙이 둘을 비교해 샤드가 오래 끊겨 있으면 알린다(ADR 0074).
 */
@Component
@RequiredArgsConstructor
public class DiscordMetrics implements MeterBinder {

    static final String SHARDS_METRIC = "quicktts.discord.shards";
    static final String CONNECTED_SHARDS_METRIC = "quicktts.discord.shards.connected";

    private final ShardManager shardManager;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(SHARDS_METRIC, shardManager, manager -> manager.getShards().size())
                .description("봇이 맡은 디스코드 샤드 수")
                .register(registry);
        Gauge.builder(CONNECTED_SHARDS_METRIC, shardManager, DiscordMetrics::connectedShards)
                .description("디스코드 게이트웨이에 붙어 있는 샤드 수")
                .register(registry);
    }

    private static double connectedShards(ShardManager manager) {
        return manager.getShards().stream().filter(shard -> shard.getStatus() == JDA.Status.CONNECTED).count();
    }
}
