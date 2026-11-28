package io.github.phs314.quicktts.bot.settings.adapter.out.synthesis;

import io.github.phs314.quicktts.bot.settings.application.port.out.VoiceChoicesPort;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoice;
import io.github.phs314.quicktts.bot.settings.domain.vo.VoiceChoices;
import io.github.phs314.quicktts.bot.shared.domain.vo.VoiceId;
import io.github.phs314.quicktts.synthesis.contract.SynthesisApi;
import io.github.phs314.quicktts.synthesis.contract.VoiceCatalogResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * synthesis 서비스에 엔진들이 낼 수 있는 목소리를 물어 고를 수 있는 목소리로 바꾼다 (ADR 0065).
 * 마지막으로 받은 목록을 들고 있다가 그것을 돌려주고, 1분마다 새로 받는다(ADR 0071).
 * 그래서 {@code /목소리} 가 디스코드 이벤트 스레드에서 HTTP 를 기다리지 않고, synthesis 가 잠깐 없어도 목록을 보여 준다.
 * 한 번도 받지 못했을 때만 그 자리에서 묻는다.
 */
@Component
@RequiredArgsConstructor
public class VoiceChoicesSynthesisAdapter implements VoiceChoicesPort {

    private static final Logger log = LoggerFactory.getLogger(VoiceChoicesSynthesisAdapter.class);

    private final SynthesisApi synthesisApi;
    private volatile VoiceChoices lastReceived;

    @Override
    public VoiceChoices getVoiceChoices() {
        VoiceChoices received = lastReceived;
        return received != null ? received : fetch();
    }

    /** 새 목록을 받아 둔다. 못 받으면 마지막으로 받은 목록을 그대로 쓴다. */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void refresh() {
        try {
            fetch();
        } catch (SynthesisServiceUnavailableException e) {
            log.warn("synthesis 서비스에서 목소리 목록을 새로 받지 못해 {} 목록을 씁니다.",
                    lastReceived == null ? "빈" : "마지막으로 받은", e);
        }
    }

    private VoiceChoices fetch() {
        VoiceCatalogResponse catalog;
        try {
            catalog = synthesisApi.getVoices();
        } catch (RestClientException e) {
            throw new SynthesisServiceUnavailableException("synthesis 서비스에서 목소리 목록을 받지 못했습니다.", e);
        }
        VoiceChoices choices = new VoiceChoices(
                catalog.voices().stream()
                        .map(voice -> new VoiceChoice(new VoiceId(voice.id()), voice.label()))
                        .toList(),
                new VoiceId(catalog.defaultVoiceId()));
        lastReceived = choices;
        return choices;
    }
}
