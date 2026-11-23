package io.github.phs314.quicktts.synthesis.adapter.in.web;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.synthesis.domain.BusinessException;
import io.github.phs314.quicktts.synthesis.domain.ErrorKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 실패를 봇 서버의 웹 API 와 같은 모양(오류 코드를 실은 ProblemDetail)으로 돌려준다.
 * 상태 코드는 실패 종류로 정한다. 엔진 실패는 502 다.
 */
@RestControllerAdvice
public class SynthesisExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SynthesisExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail businessFailure(BusinessException e) {
        if (e.kind() == ErrorKind.UNAVAILABLE) {
            log.warn("TTS 엔진이 실패했습니다.", e);
        }
        return problem(statusOf(e.kind()), e.code(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpectedFailure(Exception e) {
        log.error("요청을 처리하지 못했습니다.", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_FAILURE,
                "synthesis 서비스가 요청을 처리하지 못했습니다.");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty(ApiErrorCode.PROPERTY, statusCode.is4xxClientError()
                    ? ApiErrorCode.INVALID_REQUEST
                    : ApiErrorCode.UNEXPECTED_FAILURE);
        }
        return super.handleExceptionInternal(e, body, headers, statusCode, request);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(ApiErrorCode.PROPERTY, code);
        return problem;
    }

    static HttpStatus statusOf(ErrorKind kind) {
        return switch (kind) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
    }
}
