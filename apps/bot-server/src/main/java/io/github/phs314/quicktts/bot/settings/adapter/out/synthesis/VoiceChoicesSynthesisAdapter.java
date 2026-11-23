package io.github.phs314.quicktts.bot.settings.adapter.out.synthesis;

import io.github.phs314.quicktts.bot.settings.application.port.out.VoiceChoicesPort;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoice;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoices;
import io.github.phs314.quicktts.synthesis.application.port.in.GetVoicesUseCase;
import io.github.phs314.quicktts.synthesis.application.port.in.dto.VoiceCatalogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * synthesis 에 엔진들이 낼 수 있는 목소리를 물어 고를 수 있는 목소리로 바꾼다 (ADR 0040).
 * 같은 프로세스 안의 synthesis 를 부른다. 기본값이고, {@code quicktts.synthesis.mode=http} 면 HTTP 어댑터가 대신한다 (ADR 0062).
 */
@Component
@ConditionalOnProperty(name = "quicktts.synthesis.mode", havingValue = "in-process", matchIfMissing = true)
@RequiredArgsConstructor
public class VoiceChoicesSynthesisAdapter implements VoiceChoicesPort {

    private final GetVoicesUseCase getVoicesUseCase;

    @Override
    public VoiceChoices getVoiceChoices() {
        VoiceCatalogDto catalog = getVoicesUseCase.getVoices();
        return new VoiceChoices(
                catalog.voices().stream().map(voice -> new VoiceChoice(voice.id(), voice.label())).toList(),
                catalog.defaultVoiceId());
    }
}
