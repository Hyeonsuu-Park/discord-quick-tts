package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 웹 API 사용량 제한 ({@code quicktts.rate-limit}, ADR 0069). 이 패키지의 {@link RateLimitFilter} 만 쓴다.
 * 평소 쓰는 사람은 닿지 않는 값으로 두고, 스크립트로 몰아 보내는 요청에서 봇 서버와 TTS 엔진을 지킨다.
 *
 * @param requestsPerMinute      기기 토큰이 있는 요청을 디스코드 사용자마다 1분에 몇 번까지 받을지
 * @param registrationsPerMinute 기기 등록(토큰 없이 받는 요청)을 보낸 곳(IP)마다 1분에 몇 번까지 받을지
 */
@Validated
@ConfigurationProperties("quicktts.rate-limit")
public record RateLimitProperties(
        @DefaultValue("60") @Positive int requestsPerMinute,
        @DefaultValue("10") @Positive int registrationsPerMinute) {
}
