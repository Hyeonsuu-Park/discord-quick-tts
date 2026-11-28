package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.bot.shared.domain.BusinessException;
import io.github.phs314.quicktts.bot.shared.domain.ErrorKind;

/** 사용량 제한을 넘은 요청. 웹 공통 예외 처리가 429 로 바꾼다. */
class TooManyRequestsException extends BusinessException {

    TooManyRequestsException(long retryAfterSeconds) {
        super(ErrorKind.RESOURCE_EXHAUSTED, ApiErrorCode.TOO_MANY_REQUESTS,
                "요청을 너무 많이 보냈습니다. " + retryAfterSeconds + "초 뒤에 다시 보내 주세요.");
    }
}
