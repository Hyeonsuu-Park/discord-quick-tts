package io.github.phs314.quicktts.bot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;

class DiscordMetricsTest {

    @Test
    void 샤드_수와_붙어_있는_샤드_수를_낸다() {
        ShardManager shardManager = mock(ShardManager.class);
        JDA connected = mock(JDA.class);
        JDA reconnecting = mock(JDA.class);
        when(connected.getStatus()).thenReturn(JDA.Status.CONNECTED);
        when(reconnecting.getStatus()).thenReturn(JDA.Status.WAITING_TO_RECONNECT);
        when(shardManager.getShards()).thenReturn(List.of(connected, reconnecting));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new DiscordMetrics(shardManager).bindTo(registry);

        assertThat(registry.get(DiscordMetrics.SHARDS_METRIC).gauge().value()).isEqualTo(2);
        assertThat(registry.get(DiscordMetrics.CONNECTED_SHARDS_METRIC).gauge().value()).isEqualTo(1);
    }
}
