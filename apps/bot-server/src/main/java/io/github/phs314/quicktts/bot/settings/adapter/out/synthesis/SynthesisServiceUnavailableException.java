package io.github.phs314.quicktts.bot.settings.adapter.out.synthesis;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.bot.shared.domain.BusinessException;
import io.github.phs314.quicktts.bot.shared.domain.ErrorKind;

/**
 * synthesis 서비스에 닿지 못했거나 서비스가 실패를 돌려줬을 때 던진다.
 */
public class SynthesisServiceUnavailableException extends BusinessException {

    public SynthesisServiceUnavailableException(String message, Throwable cause) {
        super(ErrorKind.UNAVAILABLE, ApiErrorCode.SPEECH_SYNTHESIS_FAILED, message, cause);
    }
}
