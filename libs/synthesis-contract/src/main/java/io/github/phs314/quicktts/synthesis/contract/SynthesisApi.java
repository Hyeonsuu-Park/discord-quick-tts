package io.github.phs314.quicktts.synthesis.contract;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * synthesis 서비스의 웹 API. 클러스터 안에서만 부른다(바깥에 내보내지 않는다) (ADR 0061).
 * synthesis 서비스의 컨트롤러가 이 인터페이스를 구현하고, 봇 서버는 같은 인터페이스로 HTTP 클라이언트를 만든다.
 * 실패하면 {@code application/problem+json} 으로 상태 코드와 {@code code}(오류 코드)를 돌려준다.
 * <p>두 서비스는 롤링 업데이트 중이나 하나만 되돌릴 때 다른 판끼리 붙는다. 그래서 요청·응답에는 필드를 더하기만 하고,
 * 빼거나 바꾸는 것은 두 서비스가 모두 새 판이 된 다음 판에서 한다.
 */
@HttpExchange(SynthesisApi.BASE_PATH)
public interface SynthesisApi {

    String BASE_PATH = "/internal/v1";
    String SYNTHESES_PATH = "/syntheses";
    String VOICES_PATH = "/voices";

    /** mp3 음성의 미디어 타입. 지금 엔진은 모두 mp3 를 만든다. */
    String AUDIO_MPEG = "audio/mpeg";
    String MP3 = "mp3";

    /**
     * 문장을 고른 목소리의 음성으로 만든다. 문장이 비었거나 1000자를 넘으면 400 과 {@code invalid-value} 다. 본문은 음성 바이트이고 형식은 {@code Content-Type} 으로 알린다(mp3 면 {@value #AUDIO_MPEG}).
     */
    @PostExchange(url = SYNTHESES_PATH, contentType = MediaType.APPLICATION_JSON_VALUE,
            accept = {AUDIO_MPEG, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    ResponseEntity<byte[]> synthesize(@RequestBody SynthesizeRequest request);

    /** 엔진들이 낼 수 있는 목소리 목록과 기본 목소리. */
    @GetExchange(url = VOICES_PATH, accept = MediaType.APPLICATION_JSON_VALUE)
    VoiceCatalogResponse getVoices();
}
