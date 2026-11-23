package io.github.phs314.quicktts.bot.speech.application.port.out;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.bot.shared.domain.BusinessException;
import io.github.phs314.quicktts.bot.shared.domain.ErrorKind;
import io.github.phs314.quicktts.bot.speech.domain.vo.Audio;
import io.github.phs314.quicktts.bot.speech.domain.vo.SpokenText;
import io.github.phs314.quicktts.bot.shared.domain.vo.VoiceId;
import java.nio.charset.StandardCharsets;

/** synthesis 서비스 대신 쓰는 대역. 바깥 서비스를 부르지 않고 "목소리:문장" 글을 음성 바이트로 돌려준다. */
public class FakeTextToSpeechPort implements TextToSpeechPort {

    private volatile boolean failing;

    /** 켜 두면 synthesis 서비스가 실패한 것처럼 음성을 만들지 못한다. */
    public void failing(boolean failing) {
        this.failing = failing;
    }

    public void reset() {
        failing = false;
    }

    @Override
    public Audio synthesize(SpokenText text, VoiceId voiceId) {
        if (failing) {
            throw new BusinessException(ErrorKind.UNAVAILABLE, ApiErrorCode.SPEECH_SYNTHESIS_FAILED,
                    "테스트용 synthesis 서비스가 실패했습니다.") {
            };
        }
        return new Audio((voiceId.value() + ":" + text.text()).getBytes(StandardCharsets.UTF_8), "mp3");
    }
}
