package io.github.phs314.quicktts.bot.settings.adapter.out.synthesis;

import io.github.phs314.quicktts.bot.settings.application.port.out.VoiceChoicesPort;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoice;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoices;
import io.github.phs314.quicktts.bot.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * synthesis 서비스에 엔진들이 낼 수 있는 목소리를 물어 고를 수 있는 목소리로 바꾼다 (ADR 0065).
 */
@Component
@RequiredArgsConstructor
public class VoiceChoicesSynthesisAdapter implements VoiceChoicesPort {

    private final SynthesisApi synthesisApi;

    @Override
    public VoiceChoices getVoiceChoices() {
        VoiceCatalogResponse catalog;
        try {
            catalog = synthesisApi.getVoices();
        } catch (RestClientException e) {
            throw new SynthesisServiceUnavailableException("synthesis 서비스에서 목소리 목록을 받지 못했습니다.", e);
        }
        return new VoiceChoices(
                catalog.voices().stream()
                        .map(voice -> new VoiceChoice(new VoiceId(voice.id()), voice.label()))
                        .toList(),
                new VoiceId(catalog.defaultVoiceId()));
    }
}
