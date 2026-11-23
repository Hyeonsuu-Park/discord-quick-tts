package io.github.phs314.quicktts.bot.config;

import io.github.phs314.quicktts.synthesis.SynthesisConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 떼어 낸 synthesis 모듈(contexts/synthesis)의 빈을 이 앱에 올린다. 패키지가 봇 서버 밖이라 컴포넌트 스캔이 닿지 않는다 (ADR 0060).
 * 앱 클래스에 두지 않는 것은 컨트롤러 슬라이스 테스트(@WebMvcTest)까지 synthesis 빈을 올리지 않게 하려는 것이다.
 */
@Configuration(proxyBeanMethods = false)
@Import(SynthesisConfiguration.class)
public class SynthesisModuleConfig {
}
