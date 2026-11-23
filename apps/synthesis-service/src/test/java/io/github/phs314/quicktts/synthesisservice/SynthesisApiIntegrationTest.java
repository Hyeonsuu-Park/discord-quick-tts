package io.github.phs314.quicktts.synthesisservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.synthesis.application.port.out.FakeSpeechSynthesizerPort;
import io.github.phs314.quicktts.synthesis.application.port.out.SpeechSynthesizerPort;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import io.github.phs314.quicktts.synthesis.contract.SynthesizeRequest;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse.VoiceResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * 서비스를 통째로 띄우고 봇 서버와 같은 방법(약속 인터페이스로 만든 HTTP 클라이언트)으로 부른다.
 * 서버와 클라이언트가 같은 약속을 같은 뜻으로 읽는지 본다. TTS 엔진만 가짜다.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class SynthesisApiIntegrationTest {

    private static final FakeSpeechSynthesizerPort TTS = new FakeSpeechSynthesizerPort();

    @TestBean(methodName = "tts")
    SpeechSynthesizerPort speechSynthesizerPort;

    @LocalServerPort
    int port;

    SynthesisApi client;

    static SpeechSynthesizerPort tts() {
        return TTS;
    }

    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(SynthesisApi.class);
    }

    @AfterEach
    void tearDown() {
        TTS.reset();
    }

    @Test
    void 문장과_목소리를_보내면_mp3_음성을_받는다() {
        ResponseEntity<byte[]> response = client.synthesize(
                new SynthesizeRequest("안녕", FakeSpeechSynthesizerPort.INJOON.id().value()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString(SynthesisApi.AUDIO_MPEG);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("fake:injoon:안녕");
    }

    @Test
    void 목소리_목록과_기본_목소리를_받는다() {
        VoiceCatalogResponse catalog = client.getVoices();

        assertThat(catalog.voices()).containsExactly(
                new VoiceResponse("fake:sunhi", "선희"), new VoiceResponse("fake:injoon", "인준"));
        assertThat(catalog.defaultVoiceId()).isEqualTo("fake:sunhi");
    }

    @Test
    void 엔진이_실패하면_502_와_speech_synthesis_failed() {
        TTS.failing(true);

        assertThatThrownBy(() -> client.synthesize(new SynthesizeRequest("안녕", "fake:sunhi")))
                .isInstanceOfSatisfying(HttpServerErrorException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getResponseBodyAsString()).contains(ApiErrorCode.SPEECH_SYNTHESIS_FAILED);
                });
    }

    @Test
    void 목소리가_비었으면_400_과_invalid_value() {
        assertThatThrownBy(() -> client.synthesize(new SynthesizeRequest("안녕", " ")))
                .isInstanceOfSatisfying(HttpClientErrorException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getResponseBodyAsString()).contains(ApiErrorCode.INVALID_VALUE);
                });
    }
}
