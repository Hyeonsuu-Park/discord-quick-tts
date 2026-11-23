package io.github.phs314.quicktts.bot;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phs314.quicktts.bot.config.DiscordListenerRegistrar;
import io.github.phs314.quicktts.bot.guild.application.port.out.BotOwnerPort;
import io.github.phs314.quicktts.bot.guild.application.port.out.JoinedGuildPort;
import io.github.phs314.quicktts.bot.settings.adapter.out.synthesis.VoiceChoicesSynthesisHttpAdapter;
import io.github.phs314.quicktts.bot.settings.application.port.out.VoiceChoicesPort;
import io.github.phs314.quicktts.bot.speech.adapter.out.synthesis.TextToSpeechSynthesisHttpAdapter;
import io.github.phs314.quicktts.bot.speech.application.port.out.TextToSpeechPort;
import io.github.phs314.quicktts.synthesis.application.port.in.SynthesizeSpeechUseCase;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@code quicktts.synthesis.mode=http} 면 봇 서버가 synthesis 모듈을 올리지 않고 synthesis 서비스를 HTTP 로 부른다 (ADR 0062).
 * 같은 프로세스로 부르는 기본 모드는 API 통합 테스트가 본다.
 */
@SpringBootTest(properties = {
        "quicktts.discord.token=test-token",
        "quicktts.synthesis.mode=http",
        "spring.http.serviceclient.synthesis.base-url=http://synthesis.test",
        "spring.datasource.url=jdbc:h2:mem:synthesis-mode-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@MockitoBean(types = {ShardManager.class, DiscordListenerRegistrar.class, BotOwnerPort.class, JoinedGuildPort.class})
class SynthesisModeTest {

    @Autowired
    ApplicationContext context;

    @Test
    void http_모드면_synthesis_서비스를_부르는_어댑터를_쓴다() {
        assertThat(context.getBean(TextToSpeechPort.class)).isInstanceOf(TextToSpeechSynthesisHttpAdapter.class);
        assertThat(context.getBean(VoiceChoicesPort.class)).isInstanceOf(VoiceChoicesSynthesisHttpAdapter.class);
        assertThat(context.getBeanNamesForType(SynthesisApi.class)).hasSize(1);
    }

    @Test
    void http_모드면_같은_프로세스의_synthesis_는_올리지_않는다() {
        assertThat(context.getBeanNamesForType(SynthesizeSpeechUseCase.class)).isEmpty();
    }
}
