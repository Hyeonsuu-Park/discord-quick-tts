package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 데스크톱 앱 판 ({@code quicktts.client}, ADR 0076). 이 패키지의 {@link ClientVersionFilter} 만 쓴다.
 *
 * @param minimumVersion 이 서버가 받쳐 주는 가장 낮은 앱 판(예 {@code 0.3.0}). 비어 있으면 알리지 않는다.
 *                       이보다 낮은 앱은 트레이 알림으로 업데이트를 안내한다.
 */
@Validated
@ConfigurationProperties("quicktts.client")
public record ClientVersionProperties(
        @DefaultValue("") @Pattern(regexp = "|\\d+\\.\\d+\\.\\d+", message = "1.2.3 처럼 숫자 세 개이거나 비어 있어야 합니다")
        String minimumVersion) {
}
