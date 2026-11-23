package io.github.phs314.quicktts.synthesis.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 엔진 어댑터가 쓰는 {@code speech} 캐시를 켠다. 캐시 이름과 크기는 application.yml 의 {@code spring.cache} 에 있다.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
}
