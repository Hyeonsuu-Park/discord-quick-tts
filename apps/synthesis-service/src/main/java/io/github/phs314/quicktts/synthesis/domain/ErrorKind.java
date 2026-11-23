package io.github.phs314.quicktts.synthesis.domain;

/**
 * 예상한 실패의 종류. 웹 어댑터는 예외 클래스가 아니라 이 종류로 HTTP 상태 코드를 정한다.
 * 이름은 봇 서버와 같이 gRPC 표준 상태 코드를 따른다. 이 서비스에서 생기는 종류만 둔다.
 */
public enum ErrorKind {

    /** 요청 값이 규칙에 맞지 않음. 다시 보내도 같다. */
    INVALID_ARGUMENT,

    /** 바깥 서비스(TTS 엔진)가 실패함. 우리 쪽에서 남길 로그가 있다. */
    UNAVAILABLE
}
