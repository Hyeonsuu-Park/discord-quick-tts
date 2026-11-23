package io.github.phs314.quicktts.synthesisservice.adapter.in.web;

import io.github.phs314.quicktts.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.application.port.in.GetVoicesUseCase;
import io.github.phs314.quicktts.synthesis.application.port.in.SynthesizeSpeechUseCase;
import io.github.phs314.quicktts.synthesis.application.port.in.command.SynthesizeSpeechCommand;
import io.github.phs314.quicktts.synthesis.application.port.in.dto.AudioDto;
import io.github.phs314.quicktts.synthesis.application.port.in.dto.VoiceCatalogDto;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import io.github.phs314.quicktts.synthesis.contract.SynthesizeRequest;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse.VoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약속({@link SynthesisApi})의 경로와 형식을 그대로 구현한다. 유스케이스를 부르고 응답 모양만 바꾼다.
 */
@RestController
@RequiredArgsConstructor
public class SynthesisController implements SynthesisApi {

    private static final MediaType MP3 = MediaType.parseMediaType(AUDIO_MPEG);

    private final SynthesizeSpeechUseCase synthesizeSpeechUseCase;
    private final GetVoicesUseCase getVoicesUseCase;

    @Override
    public ResponseEntity<byte[]> synthesize(SynthesizeRequest request) {
        AudioDto audio = synthesizeSpeechUseCase.synthesize(
                new SynthesizeSpeechCommand(request.text(), new VoiceId(request.voiceId())));
        MediaType type = SynthesisApi.MP3.equals(audio.format()) ? MP3 : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(audio.content());
    }

    @Override
    public VoiceCatalogResponse getVoices() {
        VoiceCatalogDto catalog = getVoicesUseCase.getVoices();
        return new VoiceCatalogResponse(
                catalog.voices().stream().map(voice -> new VoiceResponse(voice.id().value(), voice.label())).toList(),
                catalog.defaultVoiceId().value());
    }
}
