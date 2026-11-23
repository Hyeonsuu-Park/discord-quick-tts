package io.github.phs314.quicktts.bot.speech.adapter.out.synthesis;

import io.github.phs314.quicktts.bot.speech.application.port.out.TextToSpeechPort;
import io.github.phs314.quicktts.bot.speech.domain.vo.Audio;
import io.github.phs314.quicktts.bot.speech.domain.vo.SpokenText;
import io.github.phs314.quicktts.bot.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import io.github.phs314.quicktts.synthesis.contract.SynthesizeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * synthesis 서비스에 다듬은 문장을 보내 음성을 받아 온다 (ADR 0065).
 */
@Component
@RequiredArgsConstructor
public class TextToSpeechSynthesisAdapter implements TextToSpeechPort {

    private static final MediaType MP3 = MediaType.parseMediaType(SynthesisApi.AUDIO_MPEG);

    private final SynthesisApi synthesisApi;

    @Override
    public Audio synthesize(SpokenText text, VoiceId voiceId) {
        ResponseEntity<byte[]> response;
        try {
            response = synthesisApi.synthesize(new SynthesizeRequest(text.text(), voiceId.value()));
        } catch (RestClientException e) {
            throw new SynthesisServiceUnavailableException("synthesis 서비스에서 음성을 받지 못했습니다.", e);
        }
        MediaType type = response.getHeaders().getContentType();
        if (response.getBody() == null || type == null || !MP3.isCompatibleWith(type)) {
            throw new SynthesisServiceUnavailableException("synthesis 서비스가 모르는 형식의 음성을 보냈습니다: " + type, null);
        }
        return new Audio(response.getBody(), SynthesisApi.MP3);
    }
}
