package io.github.phs314.quicktts.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.phs314.quicktts.bot.config.DiscordListenerRegistrar;
import io.github.phs314.quicktts.bot.guild.application.port.out.BotOwnerPort;
import io.github.phs314.quicktts.bot.guild.application.port.out.JoinedGuildPort;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * 쿠버네티스처럼 관리 포트를 따로 열면, 프로브와 Prometheus 가 그 포트에서 토큰 없이 부르고
 * 웹 API 포트에서는 지금처럼 지표에 기기 토큰이 있어야 하는지 본다 (ADR 0057).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "quicktts.discord.token=test-token",
        "spring.datasource.url=jdbc:h2:mem:management-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "management.server.port=0"
})
@MockitoBean(types = {ShardManager.class, DiscordListenerRegistrar.class, BotOwnerPort.class, JoinedGuildPort.class})
class ManagementPortIntegrationTest {

    @LocalServerPort
    int port;

    @LocalManagementPort
    int managementPort;

    @Autowired
    ShardManager shardManager;

    RestTestClient api;
    RestTestClient management;

    @BeforeEach
    void setUp() {
        api = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        management = RestTestClient.bindToServer().baseUrl("http://localhost:" + managementPort).build();
    }

    @Test
    void 관리_포트에서는_Prometheus_지표를_토큰_없이_긁는다() {
        connectShard(JDA.Status.CONNECTED);

        management.get().uri("/actuator/prometheus").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("cache_gets_total{cache=\"speech\""));
    }

    @Test
    void 웹_API_포트로는_Prometheus_지표를_토큰_없이_못_본다() {
        api.get().uri("/actuator/prometheus").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 디스코드가_끊겨도_liveness_는_UP_이고_readiness_만_DOWN() {
        connectShard(JDA.Status.ATTEMPTING_TO_RECONNECT);

        management.get().uri("/actuator/health/liveness").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
        management.get().uri("/actuator/health/readiness").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo("DOWN");
    }

    private void connectShard(JDA.Status status) {
        JDA shard = mock(JDA.class, RETURNS_DEEP_STUBS);
        when(shard.getStatus()).thenReturn(status);
        when(shardManager.getShards()).thenReturn(List.of(shard));
    }
}
