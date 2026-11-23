// 봇 서버와 synthesis 서비스 사이의 웹 API 약속(경로, 요청·응답 형식). 업무 규칙은 두지 않는다 (ADR 0061).
// 같은 HTTP 인터페이스를 서버는 컨트롤러로 구현하고, 봇 서버는 클라이언트로 쓴다.
plugins {
    id("quicktts.spring-library")
}

dependencies {
    // @HttpExchange 와 ResponseEntity. 이 약속을 쓰는 쪽도 함께 보아야 하므로 api 로 내보낸다.
    api("org.springframework:spring-web")
}
