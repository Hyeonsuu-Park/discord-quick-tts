package io.github.phs314.quicktts.synthesis.adapter.out.tts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.phs314.quicktts.synthesis.application.exception.SpeechSynthesisException;
import io.github.phs314.quicktts.synthesis.config.ResilienceConfig;
import io.github.phs314.quicktts.synthesis.domain.vo.Audio;
import io.github.phs314.quicktts.synthesis.domain.vo.VoiceId;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** 엔진이 연달아 실패하면 차단기가 열려 엔진을 부르지 않고 바로 실패하는지 본다. 바깥 서버는 부르지 않는다. */
class TtsEngineCircuitBreakerTest {

    private final CircuitBreakerRegistry registry = new ResilienceConfig().circuitBreakerRegistry();

    @Test
    void 연달아_실패하면_더_부르지_않고_바로_실패한다() {
        CircuitBreaker breaker = registry.circuitBreaker("test");
        AtomicInteger calls = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> TtsEngine.throughCircuitBreaker(breaker, "가짜 TTS", () -> {
                calls.incrementAndGet();
                throw new SpeechSynthesisException("실패");
            })).isInstanceOf(SpeechSynthesisException.class);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> TtsEngine.throughCircuitBreaker(breaker, "가짜 TTS", () -> {
            calls.incrementAndGet();
            return new Audio(new byte[] {1}, "mp3");
        })).isInstanceOf(SpeechSynthesisException.class).hasMessageContaining("가짜 TTS 가 연달아 실패");
        assertThat(calls).hasValue(5);
    }

    @Test
    void 가끔_실패하면_열리지_않는다() {
        CircuitBreaker breaker = registry.circuitBreaker("test");

        for (int i = 0; i < 10; i++) {
            boolean fails = i % 3 == 0;
            try {
                TtsEngine.throughCircuitBreaker(breaker, "가짜 TTS", () -> {
                    if (fails) {
                        throw new SpeechSynthesisException("실패");
                    }
                    return new Audio(new byte[] {1}, "mp3");
                });
            } catch (SpeechSynthesisException ignored) {
                // 실패는 예상한 것이다.
            }
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void 차단기가_열린_엔진은_바깥_서버를_부르지_않고_바로_실패한다() {
        TtsProperties properties = new TtsProperties("edge:ko-KR-SunHiNeural", "143.0.3650.75",
                Duration.ofSeconds(4), Duration.ofSeconds(3));
        EdgeReadAloudEngine edge = new EdgeReadAloudEngine(properties, registry, Clock.systemUTC());
        GoogleTranslateEngine google = new GoogleTranslateEngine(properties, registry);
        registry.circuitBreaker(EdgeReadAloudEngine.CIRCUIT_BREAKER).transitionToOpenState();
        registry.circuitBreaker(GoogleTranslateEngine.CIRCUIT_BREAKER).transitionToOpenState();

        assertThatThrownBy(() -> edge.synthesize("안녕", new VoiceId("edge:ko-KR-SunHiNeural")))
                .isInstanceOf(SpeechSynthesisException.class).hasMessageContaining("Edge TTS 가 연달아 실패");
        assertThatThrownBy(() -> google.synthesize("안녕", GoogleTranslateEngine.VOICE_ID))
                .isInstanceOf(SpeechSynthesisException.class).hasMessageContaining("Google 번역 TTS 가 연달아 실패");
    }
}
