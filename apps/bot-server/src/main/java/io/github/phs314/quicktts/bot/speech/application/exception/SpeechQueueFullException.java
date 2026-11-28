package io.github.phs314.quicktts.bot.speech.application.exception;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.bot.shared.domain.BusinessException;
import io.github.phs314.quicktts.bot.shared.domain.ErrorKind;

/**
 * 디스코드 서버 하나에 읽을 차례를 기다리는 문장이 상한만큼 쌓여서 더 받지 않을 때 던진다.
 * 문장이 끝없이 쌓여 메모리를 채우거나, 몇 분 뒤에야 읽히는 일을 막는다.
 */
public class SpeechQueueFullException extends BusinessException {

    public SpeechQueueFullException(int limit) {
        super(ErrorKind.RESOURCE_EXHAUSTED, ApiErrorCode.SPEECH_QUEUE_FULL,
                "읽을 차례를 기다리는 문장이 " + limit + "개라서 더 받지 않습니다. 앞 문장을 읽고 나면 다시 보내 주세요.");
    }
}
