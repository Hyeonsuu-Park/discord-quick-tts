# 따로 띄울 synthesis 와 공유 커널은 contexts/ 아래 Gradle 모듈로 뺀다

- 상태: 채택
- 날짜: 2026-11-23

ADR 0045 는 컨텍스트를 봇 서버 안 패키지로 두고 "따로 배포하게 되면 그때 모듈로 뺀다"고 정했습니다. 쿠버네티스로 옮기면서 synthesis(TTS 엔진)를 봇 서버와 따로 띄우기로 했습니다. synthesis 는 상태가 없어 몇 벌이든 띄울 수 있고, Edge·Google 이 막히거나 느려져도 봇 서버의 디스코드 연결에 번지지 않게 하려는 것입니다. 한편 exe 안의 서버는 지금처럼 한 프로세스로 돌아야 합니다. 그래서 같은 synthesis 코드를 봇 서버와 새 서비스가 함께 쓸 수 있어야 합니다.

## 결정

1. **새 맨 위 폴더 `contexts/` 에 업무 모듈을 둡니다.** `libs/` 는 업무 규칙 없이 앱 사이 약속만 두는 곳이라(ADR 0045) 업무 코드를 넣지 않습니다.
   - `contexts/synthesis`: synthesis 컨텍스트 전체(도메인, 유스케이스, TTS 엔진 어댑터).
   - `contexts/shared`: 공유 커널의 도메인(`DiscordUserId`, `GuildId`, `VoiceId`, 도메인 예외의 공통 부모). synthesis 가 `VoiceId` 와 예외를 쓰므로 같이 뺍니다. 웹·디스코드 공통 예외 처리(`shared/adapter`)는 봇 서버 몫이라 봇 서버에 남깁니다.
2. **모듈로 빼는 것은 따로 띄울 컨텍스트만입니다.** speech, settings, identity, guild 는 봇 서버 안 패키지 그대로입니다. 디스코드 연결과 음성 상태를 같이 쓰기 때문에 봇 서버와 떨어질 수 없습니다.
3. **패키지는 앱 이름(`bot`)을 빼고 `io.github.phs314.quicktts.<모듈>` 입니다.** `libs/api-contract` 가 `io.github.phs314.quicktts.api` 인 것과 같은 규칙입니다. synthesis 는 봇 서버만의 코드가 아니게 되었기 때문입니다.
4. **모듈은 자기 빈을 올리는 설정 클래스 하나(`SynthesisConfiguration`)를 내놓고, 앱이 `@Import` 합니다.** 봇 서버는 `config/SynthesisModuleConfig` 에서 가져옵니다. 앱 클래스에 두면 컨트롤러 슬라이스 테스트까지 synthesis 빈을 올리게 됩니다. 캐시 켜기와 캐시 크기는 앱이 정합니다.
5. **봇 서버는 지금처럼 synthesis 의 공개 입구(`application.port.in`)만 부릅니다.** 봇 서버의 `ArchitectureTest` 는 synthesis 를 모듈 컨텍스트로 알고 같은 컨텍스트 지도 규칙을 겁니다. 모듈 안의 헥사고날 규칙과 "모듈은 앱을 모른다"는 규칙은 그 모듈의 `ArchitectureTest` 가 봅니다.
6. **다른 모듈의 테스트가 쓰는 가짜 엔진은 `src/testFixtures` 에 둡니다**(`FakeSpeechSynthesizerPort`). 봇 서버의 API 통합 테스트가 `testFixtures(project(":contexts:synthesis"))` 로 씁니다.
7. **스프링을 쓰는 모듈용 규칙 플러그인 `quicktts.spring-library` 를 더합니다.** 스프링 부트가 정한 라이브러리 버전을 따르되 실행 jar 는 만들지 않습니다. 스프링을 쓰지 않는 `contexts/shared` 는 `quicktts.java` 만 씁니다.

## 결과

- 동작은 바뀌지 않았습니다. exe 와 이미지 안의 봇 서버는 synthesis 를 같은 프로세스에서 부릅니다.
- synthesis 를 따로 띄우는 앱(`apps/synthesis-service`)은 이 모듈에 웹 어댑터만 더하면 됩니다.
- 패키지가 바뀌어 `import` 가 `io.github.phs314.quicktts.bot.synthesis` / `...bot.shared.domain` 에서 `io.github.phs314.quicktts.synthesis` / `...shared.domain` 으로 바뀌었습니다. IDE 가 옛 경로를 기억하면 Java 언어 서버 작업 공간을 한 번 지웁니다.
- Dockerfile, 이미지 워크플로, `.dockerignore` 가 `contexts/` 도 봅니다.

## 고르지 않은 것

- 모든 컨텍스트를 모듈로: 나머지 넷은 따로 띄울 계획이 없고, 디스코드 연결을 같이 써서 나눠도 한 프로세스입니다. ArchUnit 이 이미 경계를 지킵니다.
- 패키지 이름을 그대로(`...bot.synthesis`): 고칠 파일은 적지만 synthesis 서비스가 봇 서버 이름의 패키지를 쓰게 됩니다.
- 스프링 부트 자동 구성(`AutoConfiguration.imports`)으로 올리기: 남에게 나눠 주는 라이브러리에 흔한 방식입니다. 우리 앱 둘이 쓰는 모듈에는 `@Import` 한 줄이 어디서 무엇을 올리는지 더 잘 보입니다.
- 앱 클래스의 스캔 범위를 `io.github.phs314.quicktts` 로 넓히기: 어느 모듈의 빈이 올라오는지 코드에 드러나지 않습니다.
