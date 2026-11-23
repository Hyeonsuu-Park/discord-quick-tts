package io.github.phs314.quicktts.synthesis;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * synthesis 컨텍스트의 빈(서비스, TTS 엔진 어댑터, 설정 값)을 한 번에 올린다. 이 모듈을 쓰는 앱이 {@code @Import} 한다 (ADR 0060).
 * 앱이 줄 것: 캐시 켜기와 캐시 크기({@code spring.cache.*}, 엔진 어댑터는 이름 {@code speech} 캐시를 쓴다),
 * 시계({@link java.time.Clock} 빈), 지표({@code MeterRegistry} 빈, 액추에이터가 만든다).
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan
@ConfigurationPropertiesScan
public class SynthesisConfiguration {
}
