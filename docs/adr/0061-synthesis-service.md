# synthesis 를 클러스터 안 웹 API 서비스로도 띄운다

- 상태: 일부 대체됨
  - synthesis 코드는 `contexts/synthesis` 모듈 대신 이 서비스 안에(패키지 `io.github.phs314.quicktts.synthesis`) → ADR 0065
- 날짜: 2026-11-23

synthesis 를 Gradle 모듈로 뺐습니다(ADR 0060). 이제 그 모듈을 혼자 띄울 앱과, 봇 서버가 네트워크 너머로 부를 약속이 필요합니다. TTS 엔진(Edge, Google 번역)은 공식 API 가 아니어서 느려지거나 막히기 쉽고, 문장마다 수백 ms 를 씁니다. 봇 서버는 디스코드 연결 때문에 한 벌만 띄우지만(ADR 0059), TTS 는 상태가 없어 여러 벌로 나눌 수 있습니다.

## 결정

1. **`apps/synthesis-service` 는 synthesis 모듈에 웹 어댑터와 조립 코드만 더한 스프링 부트 앱입니다.** 패키지는 `io.github.phs314.quicktts.synthesisservice` 입니다. 디스코드와 DB 는 모릅니다.
2. **봇 서버와의 약속은 `libs/synthesis-contract` 의 HTTP 인터페이스(`SynthesisApi`) 하나입니다.** 스프링의 `@HttpExchange` 로 경로와 형식을 적고, 서비스의 컨트롤러가 이 인터페이스를 구현하며, 봇 서버는 같은 인터페이스로 HTTP 클라이언트를 만듭니다. 경로·형식이 한 곳에만 있어서 두 쪽이 어긋나지 않습니다. 패키지는 `io.github.phs314.quicktts.synthesis.contract` 입니다.
   - `POST /internal/v1/syntheses`: JSON `{text, voiceId}` 를 받아 음성 바이트를 돌려줍니다. 형식은 `Content-Type`(mp3 면 `audio/mpeg`)으로 알립니다. base64 로 JSON 에 싣지 않아 크기가 늘지 않습니다.
   - `GET /internal/v1/voices`: 목소리 목록과 기본 목소리.
   - 경로 앞의 `/internal` 은 클러스터 안에서만 부른다는 표시이고, `v1` 은 약속이 바뀌면 `v2` 를 나란히 두기 위한 것입니다.
3. **실패는 봇 서버 웹 API 와 같은 모양**(`application/problem+json`, `code` 에 `ApiErrorCode`)입니다. 상태 코드도 실패 종류(`ErrorKind`)로 같은 규칙을 씁니다.
4. **인증은 두지 않고, 클러스터 네트워크로 막습니다.** 서비스는 바깥에 내보내지 않고, 쿠버네티스 NetworkPolicy 로 봇 서버만 부르게 합니다. 서비스가 늘어 서로 부르는 곳이 많아지면 서비스 메시(mTLS)를 그때 정합니다.
5. **캐시는 벌마다 따로입니다.** 같은 문장을 다른 벌이 받으면 엔진을 다시 부릅니다. 적중률이 떨어지는 게 보이면 Redis 같은 공유 캐시를 그때 정합니다.
6. **이미지는 서비스마다 하나**(`ghcr.io/phs314/quicktts-synthesis-service`)이고, 이미지 워크플로의 서비스 목록에 더했습니다(ADR 0058).

## 결과

- 봇 서버가 이 서비스를 부르는 HTTP 어댑터와, 같은 프로세스로 부를지 HTTP 로 부를지 고르는 설정은 다음 단계에서 더합니다. exe 안의 서버는 계속 같은 프로세스로 부릅니다.
- 서비스 통합 테스트가 서비스를 통째로 띄우고 봇 서버와 같은 방법(약속 인터페이스로 만든 클라이언트)으로 불러, 두 쪽이 약속을 같은 뜻으로 읽는지 봅니다.

## 고르지 않은 것

- gRPC: 바이트를 주고받기에 알맞고 큰 회사에서 흔하지만, 두 쪽 모두 스프링 MVC 라 HTTP 인터페이스 하나로 충분합니다. 다른 언어 서비스가 생기면 그때 `proto/` 와 함께 다시 봅니다.
- OpenAPI 명세에서 코드 만들기: 두 쪽이 같은 자바 저장소에 있어 인터페이스를 바로 나눠 쓰는 편이 짧습니다.
- 메시지 큐로 비동기 요청: quick chat 은 음성을 바로 받아 재생 순서에 넣어야 해서 요청-응답이 맞습니다.
