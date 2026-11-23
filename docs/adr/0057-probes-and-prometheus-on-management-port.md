# 쿠버네티스 프로브와 Prometheus 지표는 따로 연 관리 포트에서 토큰 없이 본다

- 상태: 채택
- 날짜: 2026-11-23

봇 서버를 컨테이너로 쿠버네티스에 올리면(ADR 0055) 쿠버네티스가 파드가 살아 있는지(liveness), 요청을 받아도 되는지(readiness)를 주기적으로 묻고, Prometheus 가 지표를 긁어 갑니다. 지금은 두 가지가 맞지 않았습니다.

- 상태 확인은 `/actuator/health` 하나이고 디스코드 샤드까지 봅니다(ADR 0036, 0046). 이걸 liveness 로 쓰면 디스코드가 잠깐 끊겨 JDA 가 스스로 다시 붙는 동안 쿠버네티스가 파드를 죽이고 새로 띄웁니다. 새 파드는 샤드를 처음부터 다시 붙여야 해서 더 늦어집니다.
- 지표는 `/actuator/metrics` 에서 기기 토큰이 있어야 봅니다. Prometheus 는 기기 토큰이 없고, Prometheus 형식(`/actuator/prometheus`)도 없습니다.

## 결정

1. **스프링 부트의 쿠버네티스 프로브 그룹을 켭니다.** `/actuator/health/liveness` 는 프로세스가 살아 있는지만, `/actuator/health/readiness` 는 DB 와 디스코드 샤드(`discord`)까지 봅니다. `/actuator/health` 는 지금처럼 모두 봅니다. 데스크톱 앱이 서버가 떠 있는지 보는 주소라 바꾸지 않습니다.
2. **`micrometer-registry-prometheus` 를 넣고 `/actuator/prometheus` 를 엽니다.** 웹 API 포트에서는 `metrics` 와 같이 기기 토큰이 있어야 봅니다.
3. **관리 포트(`management.server.port`, 환경변수 `MANAGEMENT_SERVER_PORT`)를 따로 열면, 그 포트로 온 요청은 모두 토큰 없이 엽니다.** 쿠버네티스에서는 8081 을 열고 이 포트는 Service 로 바깥에 내보내지 않아 클러스터 안의 프로브와 Prometheus 만 닿습니다. 포트는 관리 서버가 뜰 때 받아 두고(`config/ManagementPortRequestMatcher`), 스프링 시큐리티가 그 포트로 온 요청을 가립니다.
4. **정하지 않으면(개발 PC, exe) 지금처럼 포트 하나**이고, 토큰 없이 열리는 건 상태 확인뿐입니다.
5. **로그를 JSON 으로 내려면 `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` 를 환경변수로 줍니다.** 스프링 부트 기능이라 설정 파일은 바꾸지 않고, 쿠버네티스 매니페스트에서 켭니다.

## 결과

- 디스코드가 끊겨도 liveness 는 `UP` 이라 파드가 죽지 않고, readiness 만 `DOWN` 이 되어 그동안 요청이 이 파드로 가지 않습니다.
- Prometheus 는 `http://<파드>:8081/actuator/prometheus` 를 토큰 없이 긁습니다. TTS, quick chat 빠르기(ADR 0053), 캐시 지표가 모두 들어 있습니다.
- `ManagementPortIntegrationTest` 가 관리 포트를 따로 연 서버를 띄워 위 동작을 봅니다.
- 이 ADR 은 ADR 0036 의 "웹으로는 health 와 metrics 만 연다" 에 `prometheus` 와 관리 포트를 더합니다.

## 고르지 않은 것

- 관리 포트 없이 `/actuator/prometheus` 만 토큰 없이 열기: 바깥에 지표가 그대로 보입니다. 경로로 막으려면 터널이나 Ingress 마다 따로 막아야 합니다.
- Prometheus 에 기기 토큰을 주기: 기기 토큰은 사람(디스코드 사용자)의 것이라 수집기에 줄 것이 아닙니다.
- 관리 포트를 늘 따로 열기: exe 사용자 PC 에서 포트가 하나 더 필요하고, 데스크톱 앱이 보는 상태 확인 주소가 바뀝니다.
