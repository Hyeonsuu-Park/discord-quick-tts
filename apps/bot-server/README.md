# bot-server

디스코드 봇과 웹 API 를 함께 띄우는 서버입니다. `/연결` 명령, 기기 등록, quick chat 을 받아 음성 채널에서 읽습니다. 구조와 개발 방법은 [저장소 README](../../README.md) 를 봅니다.

| 항목 | 값 |
| --- | --- |
| 이미지 | `ghcr.io/phs314/quicktts-bot-server` (태그: `main`, `main-<커밋>`, 버전) |
| 웹 API 포트 | 8080 (`QUICKTTS_PORT`) |
| 관리 포트 | `MANAGEMENT_SERVER_PORT` 를 줄 때만 열림. 쿠버네티스는 8081 |
| 상태 확인 | `/actuator/health/liveness`, `/actuator/health/readiness` (관리 포트) |
| 지표 | `/actuator/prometheus` (관리 포트) |
| 띄우는 벌 수 | 1 (같은 봇 토큰으로 두 벌 금지) |
| 매니페스트 | [`deploy/k8s/base/bot-server`](../../deploy/k8s/base/bot-server) |

## 환경변수

| 이름 | 필수 | 내용 |
| --- | --- | --- |
| `DISCORD_TOKEN` | 예 | 디스코드 봇 토큰. 비밀값 |
| `QUICKTTS_DB_URL` | 아니오 | 없으면 `data/` 의 H2 파일. 배포는 MySQL |
| `QUICKTTS_DB_USERNAME`, `QUICKTTS_DB_PASSWORD` | MySQL 이면 예 | 비밀번호는 비밀값. 쿠버네티스는 `mysql-secrets` 에서 받음 |
| `MANAGEMENT_SERVER_PORT` | 아니오 | 프로브와 지표를 받을 포트 |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | 아니오 | `ecs` 면 로그를 한 줄에 JSON 하나로 |
| `QUICKTTS_DEFAULT_VOICE` | 아니오 | 기본 `edge:ko-KR-SunHiNeural` |
| `QUICKTTS_API_DOCS_ENABLED` | 아니오 | 기본 `true`. 공개 서버는 `false` |
| `QUICKTTS_GUILD_ALLOWLIST_ENABLED` | 아니오 | 기본 `true` |

자세한 설명은 [`.env.example`](../../.env.example) 과 [`application.yml`](src/main/resources/application.yml) 에 있습니다.

## 띄우기

```bash
./gradlew :apps:bot-server:bootRun                                   # JDK 로 바로
docker build -f apps/bot-server/Dockerfile -t quicktts-bot-server .  # 이미지 (저장소 루트에서)
kubectl apply -k deploy/k8s/overlays/local                           # k3d 클러스터
```
