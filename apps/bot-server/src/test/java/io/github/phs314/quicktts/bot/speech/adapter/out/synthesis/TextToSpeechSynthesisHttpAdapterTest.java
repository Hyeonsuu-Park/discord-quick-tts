package io.github.phs314.quicktts.bot.speech.adapter.out.synthesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.bot.speech.domain.vo.Audio;
import io.github.phs314.quicktts.bot.speech.domain.vo.SpokenText;
import io.github.phs314.quicktts.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class TextToSpeechSynthesisHttpAdapterTest {

    private static final VoiceId SUNHI = new VoiceId("edge:ko-KR-SunHiNeural");

    MockRestServiceServer server;
    TextToSpeechSynthesisHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://synthesis");
        server = MockRestServiceServer.bindTo(builder).build();
        SynthesisApi api = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build()
                .createClient(SynthesisApi.class);
        adapter = new TextToSpeechSynthesisHttpAdapter(api);
    }

    @Test
    void 문장과_목소리를_보내고_mp3_음성을_받는다() {
        byte[] mp3 = "mp3-bytes".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("http://synthesis/internal/v1/syntheses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"text\":\"안녕\",\"voiceId\":\"edge:ko-KR-SunHiNeural\"}"))
                .andRespond(withSuccess(mp3, MediaType.parseMediaType(SynthesisApi.AUDIO_MPEG)));

        Audio audio = adapter.synthesize(new SpokenText("안녕"), SUNHI);

        assertThat(audio.content()).isEqualTo(mp3);
        assertThat(audio.format()).isEqualTo("mp3");
        server.verify();
    }

    @Test
    void 서비스가_실패를_돌려주면_음성_생성_실패로_바꾼다() {
        server.expect(requestTo("http://synthesis/internal/v1/syntheses"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"code\":\"speech-synthesis-failed\"}"));

        assertThatThrownBy(() -> adapter.synthesize(new SpokenText("안녕"), SUNHI))
                .isInstanceOfSatisfying(SynthesisServiceUnavailableException.class,
                        e -> assertThat(e.code()).isEqualTo(ApiErrorCode.SPEECH_SYNTHESIS_FAILED));
    }

    @Test
    void mp3_가_아닌_응답은_받지_않는다() {
        server.expect(requestTo("http://synthesis/internal/v1/syntheses"))
                .andRespond(withSuccess("<html/>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> adapter.synthesize(new SpokenText("안녕"), SUNHI))
                .isInstanceOf(SynthesisServiceUnavailableException.class);
    }
}
