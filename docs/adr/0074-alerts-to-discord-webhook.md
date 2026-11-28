# 고장은 Prometheus 알림 규칙으로 잡아 디스코드 채널 웹훅으로 받는다

- 상태: 채택
- 날짜: 2026-11-28

지표, 추적, 대시보드는 있었지만(ADR 0053, ADR 0063) 사람에게 알리는 규칙이 하나도 없었습니다. 클러스터용 설정은 Alertmanager 를 꺼 두었고, 실제 배포인 이 PC 의 compose 에는 Prometheus 도 없었습니다. 봇이 멈추면 친구가 말해 줘야 알았습니다.

## 결정

1. **알림 규칙은 Prometheus 규칙 파일 하나(`deploy/monitoring/alerts.yml`)에 둡니다.** 사용자가 겪는 증상만 알립니다.

   | 알림 | 조건 | 무게 |
   | --- | --- | --- |
   | `QuickTtsServiceDown` | 봇 서버나 synthesis 의 지표를 2분 넘게 못 받음 | critical |
   | `QuickTtsDiscordDisconnected` | 붙어 있는 샤드가 전체보다 적은 채로 5분 | critical |
   | `QuickTtsQuickChatFailing` | 10분 동안 `speech-synthesis-failed`·`unexpected-failure` 로 3번 넘게 실패 | warning |
   | `QuickTtsEngineCircuitOpen` | TTS 엔진 차단기(ADR 0070)가 10분 동안 한 번도 닫히지 않음 | warning |

   사용자의 잘못(음성 채널에 없음, 사용량 제한 등)으로 난 실패는 알리지 않습니다. 봇 서버는 디스코드 샤드 수와 붙어 있는 샤드 수를 `quicktts.discord.shards`, `quicktts.discord.shards.connected` 지표로 냅니다.
2. **받는 곳은 디스코드 채널 웹훅입니다.** 이 서비스를 운영하는 사람이 이미 늘 보고 있는 곳이고, Alertmanager 가 디스코드를 바로 지원합니다. 같은 알림은 묶어서 보내고, 고쳐지지 않으면 4시간마다, 고쳐지면 한 번 더 알립니다. 웹훅 주소는 비밀이라 설정 파일에 적지 않고 파일로 붙입니다(`webhook_url_file`).
3. **이 PC 배포는 `compose.prod.yaml` 에 Prometheus 와 Alertmanager 를 더합니다.** 두 서비스의 관리 포트(8081)를 엽니다(compose 네트워크 안에서만). Prometheus 화면은 이 PC 의 `127.0.0.1:9090` 에서만 열립니다. 웹훅 주소(`QUICKTTS_ALERT_DISCORD_WEBHOOK_URL`)가 없으면 배포를 띄우지 않습니다.
4. **DB 백업 실패는 백업 컨테이너가 직접 웹훅으로 알립니다.** Prometheus 가 하루 한 번 도는 컨테이너를 보게 하려면 내보내는 도구가 하나 더 필요합니다. 클러스터에서는 kube-state-metrics 의 CronJob 지표로 "26시간 넘게 성공 없음" 을 알립니다.
5. **클러스터는 kube-prometheus-stack 의 Alertmanager 를 켜고 같은 규칙을 `PrometheusRule` 로 둡니다.** 두 파일의 같은 이름 묶음이 같은지 CI 가 확인합니다. 클러스터에만 있는 규칙은 `quicktts-kubernetes` 묶음에 둡니다.
6. **알림마다 할 일은 `docs/runbook.md` 의 "알림을 받았을 때" 에 둡니다.** 알림의 `runbook` 이 그 제목을 가리킵니다.

## 결과

- 봇이 멈추거나 디스코드 연결이 끊기면 몇 분 안에 디스코드 채널로 옵니다.
- 이 PC 가 꺼지거나 인터넷이 끊기면 Prometheus 도 같이 꺼져서 알림이 오지 않습니다. 고정 주소(named tunnel)를 만든 뒤 바깥의 상태 확인 서비스로 그 주소를 보게 하는 일이 남았습니다.
- 이 PC 에서 컨테이너가 둘 늘고 메모리를 200MB 안팎 더 씁니다. 지표는 15일 남깁니다.
- 규칙을 바꿀 때는 두 파일을 함께 바꿉니다.

## 고르지 않은 것

- 봇 서버가 스스로 디스코드 웹훅을 부르기: 봇이 죽으면 알릴 주체가 없고, 어떤 상태가 얼마나 오래 이어졌는지를 앱이 따로 세야 합니다.
- Grafana 알림: compose 배포에는 Grafana 가 없고, 규칙을 대시보드와 따로 코드로 두기 어렵습니다.
- 이메일·문자: 받을 메일 서버나 유료 서비스가 필요합니다.
