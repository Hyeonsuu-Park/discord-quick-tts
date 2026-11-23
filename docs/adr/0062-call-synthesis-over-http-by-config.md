# 봇 서버는 설정으로 synthesis 를 같은 프로세스나 HTTP 로 부른다

- 상태: 대체됨
  - 같은 프로세스 모드와 `QUICKTTS_SYNTHESIS_MODE` 를 없애고 언제나 HTTP 로 부름(시간 제한, 오류 코드, 다시 시도 안 함은 그대로) → ADR 0065
- 날짜: 2026-11-23

synthesis 서비스가 생겼습니다(ADR 0061). 쿠버네티스에서는 봇 서버가 이 서비스를 불러야 하지만, exe 안의 서버와 개발 PC 의 `bootRun` 은 지금처럼 서버 하나로 돌아야 합니다. 서비스를 하나 더 띄우라고 할 수 없기 때문입니다.

## 결정

1. **`quicktts.synthesis.mode` 로 고릅니다.** 기본은 `in-process`(이 서버 안의 synthesis 모듈), 쿠버네티스는 `http`(synthesis 서비스). 환경변수는 `QUICKTTS_SYNTHESIS_MODE` 입니다.
2. **바뀌는 것은 출력 어댑터뿐입니다.** speech 의 `TextToSpeechPort`, settings 의 `VoiceChoicesPort` 마다 어댑터가 둘입니다.
   - `...SynthesisAdapter`: synthesis 의 입력 포트를 바로 부릅니다(지금까지의 것).
   - `...SynthesisHttpAdapter`: `libs/synthesis-contract` 의 `SynthesisApi` 로 서비스를 부릅니다.
   - 서비스, 도메인, 포트는 어느 쪽인지 모릅니다. 헥사고날로 나눠 둔 덕에 바꿀 곳이 어댑터 둘과 조립 코드뿐입니다.
3. **`http` 면 synthesis 모듈의 빈을 올리지 않습니다.** 엔진, 캐시, TTS 설정 값이 봇 서버에 생기지 않아 같은 일을 두 곳에서 하지 않습니다.
4. **HTTP 클라이언트는 스프링의 HTTP 서비스 클라이언트(`@ImportHttpServices`)로 만듭니다.** 주소와 시간 제한은 `spring.http.serviceclient.synthesis.*` 입니다. 주소 기본값은 클러스터 안 이름 `http://synthesis-service:8080` 이고 `QUICKTTS_SYNTHESIS_URL` 로 바꿉니다. 연결은 2초, 응답은 45초까지 기다립니다. 서비스 안에서 Edge 가 실패하면 Google 번역으로 한 번 더 만들기 때문에, 같은 프로세스로 부를 때보다 먼저 끊지 않습니다.
5. **서비스에 닿지 못하거나 실패를 받으면 엔진 실패와 같은 오류 코드(`speech-synthesis-failed`)로 바꿉니다.** 데스크톱 앱과 디스코드 안내는 어느 모드인지 몰라도 됩니다.
6. **다시 시도하지 않고, 봇 서버의 준비됨(readiness)에 synthesis 서비스를 넣지 않습니다.** 엔진 대신 읽기가 이미 서비스 안에서 한 번 더 시도하고, 같은 문장을 두 번 만들면 그만큼 늦게 들립니다. 서비스가 잠깐 없어도 봇 서버는 `/연결`, `/목소리` 같은 명령을 계속 받아야 하므로 요청을 막지 않습니다. 죽은 서비스 파드는 쿠버네티스가 서비스 주소에서 뺍니다.

## 결과

- 봇 서버 `ArchitectureTest` 의 컨텍스트 지도 규칙에서, 따로 띄우는 서비스의 웹 API 약속(`<컨텍스트>.contract`)도 공개 입구로 봅니다. 부르는 자리는 지금처럼 `adapter/out/<상대>` 하나입니다.
- HTTP 어댑터는 가짜 HTTP 서버(`MockRestServiceServer`)로, 모드 고르기는 `http` 모드로 띄운 봇 서버로 테스트합니다.

## 고르지 않은 것

- 언제나 HTTP 로 부르기: exe 사용자가 서비스를 하나 더 띄워야 합니다.
- Resilience4j 차단기: 지금은 부르는 곳이 하나이고 시간 제한으로 충분합니다. 서비스가 자주 느려지는 게 지표에 보이면 그때 더합니다.
- 서비스 디스커버리(Eureka 등): 쿠버네티스 Service 이름이 그 일을 합니다.
