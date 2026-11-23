# 음성 만드는 코드는 synthesis 서비스 안에만 두고, 봇 서버는 언제나 HTTP 로 부른다

- 상태: 채택
- 날짜: 2026-11-23

ADR 0060 은 synthesis 코드를 `contexts/synthesis` 모듈로 빼서 봇 서버와 synthesis 서비스가 함께 쓰게 했고, ADR 0062 는 봇 서버가 설정(`QUICKTTS_SYNTHESIS_MODE`)으로 같은 프로세스와 HTTP 중 하나를 고르게 했습니다. exe 안의 서버를 한 프로세스로 두기 위해서였습니다. exe 에서 봇 서버를 빼기로 하면서(ADR 0066) 한 프로세스로 돌 곳이 없어졌습니다. 그러면 같은 코드를 두 앱이 나눠 쓰는 모듈, 두 가지 호출 방식, 공유 커널 모듈은 지킬 이유 없이 짐만 됩니다. 서비스마다 자기 코드와 모델을 갖고 서비스 사이에는 웹 API 약속만 두는 것이 MSA 에서 흔한 모양입니다.

## 결정

1. **synthesis 코드는 `apps/synthesis-service` 안에만 둡니다.** 패키지는 `io.github.phs314.quicktts.synthesis` 이고, 그 아래는 봇 서버의 컨텍스트와 같은 헥사고날 모양(`domain`, `application`, `adapter/in/web`, `adapter/out/tts`)입니다. 서비스 하나가 컨텍스트 하나입니다.
2. **봇 서버는 언제나 HTTP 로 부릅니다.** 같은 프로세스 어댑터와 `quicktts.synthesis.mode` 를 없애고, ADR 0062 의 HTTP 어댑터가 `...SynthesisAdapter` 라는 이름을 이어받습니다. 주소는 `QUICKTTS_SYNTHESIS_URL` 하나로 정합니다. 기본값은 개발 PC 에서 함께 띄운 synthesis 서비스(`http://localhost:8082`)이고, compose 와 쿠버네티스는 `http://synthesis-service:8080` 을 넣습니다. 시간 제한, 오류 코드, 다시 시도하지 않음(ADR 0062 결정 4~6)은 그대로입니다.
3. **공유 커널 모듈(`contexts/shared`)을 없앱니다.** `DiscordUserId`, `GuildId`, `VoiceId`, 도메인 예외의 공통 부모는 봇 서버의 `bot.shared.domain` 으로 돌아갑니다. synthesis 서비스는 자기가 쓰는 것(`VoiceId`, `BusinessException`, `ErrorKind`, `InvalidDomainValueException`)을 자기 `domain` 에 따로 둡니다. 서비스끼리 도메인 클래스를 나눠 쓰지 않고, 같은 뜻은 웹 API 약속의 문자열로 건넵니다.
4. **`contexts/` 폴더를 없앱니다.** 저장소 맨 위는 다시 `apps/`, `libs/`, `build-logic/`, `deploy/`, `docs/` 입니다. `libs/` 에는 지금처럼 업무 규칙 없는 약속만 둡니다(ADR 0045).
5. **음성 캐시와 TTS 설정(`QUICKTTS_DEFAULT_VOICE`, Edge 버전)은 synthesis 서비스에만 있습니다.** 봇 서버에서 캐시 의존성과 설정을 뺍니다.

## 결과

- 개발 PC 에서는 서버 둘을 띄웁니다: `./gradlew :apps:synthesis-service:bootRun`(8082)과 `./gradlew :apps:bot-server:bootRun`(8080). synthesis 서비스도 저장소 루트의 `.env` 를 읽습니다. Docker 가 있으면 `docker compose --profile app up --build` 한 줄로 둘과 MySQL 을 함께 띄웁니다.
- synthesis 서비스가 꺼져 있으면 quick chat 이 `speech-synthesis-failed`(502)로 실패하고 `/목소리` 목록을 못 받습니다. `/연결`, `/허용목록` 같은 다른 명령은 그대로 됩니다.
- 봇 서버 `ArchitectureTest` 의 컨텍스트 지도에서 synthesis 가 빠집니다. 대신 synthesis 서비스의 약속(`synthesis.contract`)은 `adapter/out/synthesis` 와 HTTP 클라이언트를 만드는 `config` 만 쓴다는 규칙을 둡니다. synthesis 서비스의 `ArchitectureTest` 는 헥사고날 방향과 함께, 약속을 웹 어댑터만 쓰는지 봅니다.
- 봇 서버 통합 테스트는 synthesis 대신 봇 서버의 출력 포트 대역(`FakeTextToSpeechPort`, `FakeVoiceChoicesPort`)을 씁니다. `java-test-fixtures` 로 모듈 사이에 대역을 건네던 일이 없어집니다.
- 같은 뜻의 클래스(`VoiceId`, `BusinessException` 등)가 두 서비스에 따로 있습니다. 한쪽을 고쳐도 다른 쪽은 그대로이고, 둘 사이의 약속은 `libs/synthesis-contract` 가 정합니다.

## 고르지 않은 것

- `contexts/synthesis` 모듈을 두고 synthesis 서비스만 쓰기: 쓰는 앱이 하나뿐인 모듈은 폴더와 빌드 파일만 늘립니다.
- 공유 커널을 `libs/` 로 옮겨 두 서비스가 함께 쓰기: 도메인 클래스를 서비스끼리 나눠 쓰면 한 서비스의 모델을 바꿀 때 다른 서비스도 같이 빌드하고 배포해야 합니다.
