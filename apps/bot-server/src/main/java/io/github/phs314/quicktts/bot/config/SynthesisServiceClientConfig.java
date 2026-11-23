package io.github.phs314.quicktts.bot.config;

import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * synthesis 서비스를 부르는 HTTP 클라이언트({@link SynthesisApi})를 만든다 (ADR 0065).
 * 주소와 시간 제한은 application.yml 의 {@code spring.http.serviceclient.synthesis} 에 있다.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = SynthesisServiceClientConfig.GROUP, types = SynthesisApi.class)
public class SynthesisServiceClientConfig {

    static final String GROUP = "synthesis";
}
