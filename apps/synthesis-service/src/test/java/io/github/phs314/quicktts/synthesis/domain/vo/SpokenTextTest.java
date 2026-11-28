package io.github.phs314.quicktts.synthesis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.phs314.quicktts.synthesis.domain.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

class SpokenTextTest {

    @Test
    void 비었으면_만들_수_없다() {
        assertThatThrownBy(() -> new SpokenText(null)).isInstanceOf(InvalidDomainValueException.class);
        assertThatThrownBy(() -> new SpokenText(" ")).isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void 최대_글자_수까지만_받는다() {
        assertThat(new SpokenText("가".repeat(SpokenText.MAX_LENGTH)).value()).hasSize(SpokenText.MAX_LENGTH);
        assertThatThrownBy(() -> new SpokenText("가".repeat(SpokenText.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("너무 깁니다");
    }
}
