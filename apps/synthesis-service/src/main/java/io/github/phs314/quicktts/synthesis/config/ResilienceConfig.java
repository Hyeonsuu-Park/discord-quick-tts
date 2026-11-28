package io.github.phs314.quicktts.synthesis.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TTS 엔진마다 하나씩 쓰는 차단기의 공통 설정 (ADR 0070).
 * 최근 10번 중 5번 넘게 실패하면 30초 동안 그 엔진을 부르지 않고 바로 실패로 돌려서, 다음 엔진(Google)이 곧바로 읽게 한다.
 * 30초 뒤 두 번 시험 삼아 불러 보고 되면 다시 연다.
 * 상태는 {@code resilience4j.circuitbreaker.state} 지표로 보인다({@code name} 태그가 엔진 이름).
 */
@Configuration(proxyBeanMethods = false)
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }

    /** 스프링 부트가 {@code MeterBinder} 빈을 지표 저장소에 붙인다. */
    @Bean
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry registry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    }
}
