package io.github.phs314.quicktts.bot.speech.adapter.out.synthesis;

import io.github.phs314.quicktts.bot.speech.application.port.out.TextToSpeechPort;
import io.github.phs314.quicktts.bot.speech.domain.vo.Audio;
import io.github.phs314.quicktts.bot.speech.domain.vo.SpokenText;
import io.github.phs314.quicktts.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.application.port.in.SynthesizeSpeechUseCase;
import io.github.phs314.quicktts.synthesis.application.port.in.command.SynthesizeSpeechCommand;
import io.github.phs314.quicktts.synthesis.application.port.in.dto.AudioDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * synthesis 에 다듬은 문장을 넘겨 음성을 받아 온다 (ADR 0040).
 * 같은 프로세스 안의 synthesis 를 부른다. 기본값이고, {@code quicktts.synthesis.mode=http} 면 HTTP 어댑터가 대신한다 (ADR 0062).
 */
@Component
@ConditionalOnProperty(name = "quicktts.synthesis.mode", havingValue = "in-process", matchIfMissing = true)
@RequiredArgsConstructor
public class TextToSpeechSynthesisAdapter implements TextToSpeechPort {

    private final SynthesizeSpeechUseCase synthesizeSpeechUseCase;

    @Override
    public Audio synthesize(SpokenText text, VoiceId voiceId) {
        AudioDto audio = synthesizeSpeechUseCase.synthesize(new SynthesizeSpeechCommand(text.text(), voiceId));
        return new Audio(audio.content(), audio.format());
    }
}
