package io.github.phs314.quicktts.bot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 를 켠다. 슬래시 명령 다시 올리기, synthesis 목소리 목록 새로 받기가 쓴다(ADR 0071).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {
}
