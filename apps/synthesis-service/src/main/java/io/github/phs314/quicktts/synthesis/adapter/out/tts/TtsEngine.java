package io.github.phs314.quicktts.synthesis.adapter.out.tts;

import io.github.phs314.quicktts.synthesis.application.exception.SpeechSynthesisException;
import io.github.phs314.quicktts.synthesis.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.domain.vo.Audio;
import io.github.phs314.quicktts.synthesis.domain.vo.Voice;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.cache.annotation.Cacheable;

/**
 * TTS 엔진 하나. 엔진을 추가하려면 이 인터페이스를 구현한 빈을 만들면
 * {@link SpeechSynthesizerAdapter} 가 그 목소리들을 목록에 넣는다.
 */
interface TtsEngine {

    String SPEECH_CACHE = "speech";

    List<Voice> voices();

    /**
     * 같은 문장, 같은 목소리면 처음 만든 음성을 캐시에서 돌려준다(키는 두 인자). 실패는 기억하지 않으므로 다음에 다시 시도한다.
     * 캐시된 {@link Audio} 를 여러 곳이 함께 쓰므로 음성 바이트는 읽기만 한다.
     *
     * @throws io.github.phs314.quicktts.synthesis.application.exception.SpeechSynthesisException 음성 생성에 실패했을 때
     */
    @Cacheable(SPEECH_CACHE)
    Audio synthesize(String text, VoiceId voiceId);

    default boolean offers(VoiceId voiceId) {
        return voices().stream().anyMatch(offered -> offered.id().equals(voiceId));
    }

    /**
     * 엔진 호출을 그 엔진의 차단기로 감싼다(ADR 0070). 차단기가 열려 있으면 부르지 않고 바로 실패해서
     * {@link SpeechSynthesizerAdapter} 가 곧바로 다른 엔진으로 읽게 한다.
     * 캐시 프록시 안쪽에서 불리므로 캐시에서 꺼낸 음성은 차단기의 성공으로 세지 않는다.
     */
    static Audio throughCircuitBreaker(CircuitBreaker circuitBreaker, String engineName, Supplier<Audio> call) {
        try {
            return circuitBreaker.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            throw new SpeechSynthesisException(
                    engineName + " 가 연달아 실패해서 잠시 부르지 않습니다.", e);
        }
    }
}
