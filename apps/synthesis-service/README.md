# synthesis-service

문장과 목소리를 받아 음성(mp3)을 만들어 주는 서비스입니다. TTS 엔진(Edge, Google 번역)과 목소리 목록을 맡는 synthesis 컨텍스트 하나가 이 서비스 전체입니다(ADR 0065). 봇 서버만 부르고, 바깥에는 내보내지 않습니다. 상태가 없어 몇 벌이든 띄울 수 있습니다.

| 항목 | 값 |
| --- | --- |
| 이미지 | `ghcr.io/phs314/quicktts-synthesis-service` (태그: `main`, `main-<커밋>`, 버전) |
| 웹 API 포트 | 8080 (`SYNTHESIS_PORT`). `bootRun` 은 봇 서버와 겹치지 않게 8082 |
| 관리 포트 | `MANAGEMENT_SERVER_PORT` 를 줄 때만 열림. 쿠버네티스는 8081 |
| 상태 확인 | `/actuator/health/liveness`, `/actuator/health/readiness` |
| 지표 | `/actuator/prometheus` (TTS 성공·실패 `quicktts.tts.synthesis`, 대신 읽기 `quicktts.tts.fallback`, 캐시) |
| 웹 API 약속 | [`libs/synthesis-contract`](../../libs/synthesis-contract) 의 `SynthesisApi` |

## 웹 API

| 요청 | 응답 |
| --- | --- |
| `POST /internal/v1/syntheses` `{"text": "안녕", "voiceId": "edge:ko-KR-SunHiNeural"}` | `200`, `Content-Type: audio/mpeg`, 본문은 음성 바이트 |
| `GET /internal/v1/voices` | `200`, `{"voices": [{"id": ..., "label": ...}], "defaultVoiceId": ...}` |

실패는 봇 서버와 같은 모양(`application/problem+json`, `code` 에 오류 코드)입니다. 엔진 실패는 `502` 와 `speech-synthesis-failed`, 비어 있는 목소리는 `400` 과 `invalid-value` 입니다.

## 환경변수

| 이름 | 필수 | 내용 |
| --- | --- | --- |
| `QUICKTTS_DEFAULT_VOICE` | 아니오 | 목소리를 고르지 않은 사람에게 쓸 목소리. 기본 `edge:ko-KR-SunHiNeural` |
| `MANAGEMENT_SERVER_PORT` | 아니오 | 프로브와 지표를 받을 포트 |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | 아니오 | `ecs` 면 로그를 한 줄에 JSON 하나로 |

## 띄우기

```bash
./gradlew :apps:synthesis-service:bootRun    # http://localhost:8082, 저장소 루트의 .env 를 읽음
docker build -f apps/synthesis-service/Dockerfile -t quicktts-synthesis-service .
```
