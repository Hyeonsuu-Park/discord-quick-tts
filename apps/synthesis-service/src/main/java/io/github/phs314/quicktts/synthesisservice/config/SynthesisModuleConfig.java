package io.github.phs314.quicktts.synthesisservice.config;

import io.github.phs314.quicktts.synthesis.SynthesisConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * synthesis 모듈의 빈을 올리고, 엔진 어댑터가 쓰는 {@code speech} 캐시를 켠다 (ADR 0060).
 * 캐시 이름과 크기는 application.yml 의 {@code spring.cache} 에 있다.
 */
@Configuration(proxyBeanMethods = false)
@Import(SynthesisConfiguration.class)
@EnableCaching
public class SynthesisModuleConfig {
}
