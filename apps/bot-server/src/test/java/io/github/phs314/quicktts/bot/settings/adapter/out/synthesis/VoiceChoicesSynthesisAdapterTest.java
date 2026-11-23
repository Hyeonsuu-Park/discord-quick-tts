package io.github.phs314.quicktts.bot.settings.adapter.out.synthesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoice;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoices;
import io.github.phs314.quicktts.bot.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class VoiceChoicesSynthesisAdapterTest {

    MockRestServiceServer server;
    VoiceChoicesSynthesisAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://synthesis");
        server = MockRestServiceServer.bindTo(builder).build();
        SynthesisApi api = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build()
                .createClient(SynthesisApi.class);
        adapter = new VoiceChoicesSynthesisAdapter(api);
    }

    @Test
    void 서비스의_목소리_목록을_고를_수_있는_목소리로_바꾼다() {
        server.expect(requestTo("http://synthesis/internal/v1/voices"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"voices": [{"id": "edge:ko-KR-SunHiNeural", "label": "선희"}, {"id": "google:ko", "label": "구글 번역"}],
                         "defaultVoiceId": "edge:ko-KR-SunHiNeural"}
                        """, MediaType.APPLICATION_JSON));

        VoiceChoices choices = adapter.getVoiceChoices();

        assertThat(choices.choices()).containsExactly(
                new VoiceChoice(new VoiceId("edge:ko-KR-SunHiNeural"), "선희"),
                new VoiceChoice(new VoiceId("google:ko"), "구글 번역"));
        assertThat(choices.defaultVoiceId()).isEqualTo(new VoiceId("edge:ko-KR-SunHiNeural"));
    }

    @Test
    void 서비스에_닿지_못하면_음성_생성_실패로_바꾼다() {
        server.expect(requestTo("http://synthesis/internal/v1/voices"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.getVoiceChoices())
                .isInstanceOf(SynthesisServiceUnavailableException.class);
    }
}
