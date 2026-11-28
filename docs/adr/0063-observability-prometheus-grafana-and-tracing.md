# 지표는 kube-prometheus-stack 으로 모으고, 서비스 사이 요청은 OpenTelemetry 로 추적한다

- 상태: 일부 대체됨
  - Alertmanager 를 켜고 알림 규칙을 PrometheusRule 로 둠, 이 PC 배포에도 Prometheus·Alertmanager → ADR 0074
- 날짜: 2026-11-23

지표는 이미 있습니다(TTS 성공·실패와 대신 읽기 ADR 0036, quick chat 빠르기 ADR 0053, Prometheus 형식 ADR 0057). 그래프 화면이 없고, ADR 0053 의 목표 값은 "배포 뒤"로 미뤄져 있습니다. 이제 서비스가 둘(봇 서버, synthesis 서비스)이 되어, quick chat 이 느릴 때 어느 쪽이 느린지 지표만으로는 가르기 어렵습니다.

## 결정

1. **클러스터의 지표는 kube-prometheus-stack(Prometheus, Grafana) Helm 차트로 모읍니다.** 쿠버네티스에서 가장 흔한 조합이고, 노드·파드 지표도 함께 옵니다. 설치 값은 `deploy/monitoring/kube-prometheus-stack/values.yaml` 하나이고, 노드 하나짜리 클러스터에 맞게 작게 둡니다(알림 끔, 7일 보관).
2. **우리 서비스를 긁어 가게 하는 ServiceMonitor 와 대시보드는 Kustomize component(`deploy/k8s/components/monitoring`)로 둡니다.** 차트를 설치하기 전에는 ServiceMonitor 종류가 없어 적용이 실패하므로, 오버레이에서 켜고 끕니다. 대시보드는 `grafana_dashboard` 라벨 ConfigMap 으로 올려 Grafana 가 저절로 읽습니다.
3. **두 서비스는 Micrometer Tracing + OpenTelemetry 로 추적합니다**(`spring-boot-starter-opentelemetry`). 봇 서버가 synthesis 서비스를 부를 때 `traceparent` 머리말이 붙어, 요청 하나가 두 서비스를 거친 길이 한 추적으로 이어집니다. 로그(ECS JSON)에도 같은 추적 번호가 찍힙니다.
   - 내보내기는 표준 환경변수 `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` 를 줄 때만 합니다. exe 와 `bootRun` 은 아무 데도 보내지 않습니다.
   - 요청이 적어 모두 남깁니다(`QUICKTTS_TRACING_SAMPLING`, 기본 1.0).
4. **개발 PC 에서는 `grafana/otel-lgtm` 한 컨테이너로 추적을 봅니다.** `docker compose --profile app up` 이 봇 서버, synthesis 서비스와 함께 띄우고, http://localhost:3000 에서 봅니다.
5. **클러스터의 추적 저장소(Tempo)와 로그 저장소(Loki)는 아직 두지 않습니다.** 로그는 이미 한 줄 JSON 으로 표준 출력에 나가 `kubectl logs` 로 보고, 추적은 개발 PC 에서 봅니다. 배포 서버를 정하면 그 메모리에 맞춰 클러스터 안에 둘지, Grafana Cloud 무료 등급으로 보낼지 정합니다. 두 서비스는 환경변수 하나만 더하면 됩니다.

## 결과

- Grafana 의 QuickTTS 대시보드에서 quick chat 빠르기(중간값, 95%), 재생 순서 기다림, quick chat 결과, TTS 목소리·결과별 횟수, 대신 읽기, 캐시 적중률, synthesis 서비스 응답 시간, JVM 힙을 봅니다.
- OpenTelemetry 스타터가 함께 들여오는 OTLP 지표 내보내기는 끕니다(`management.otlp.metrics.export.enabled: false`). 켜 두면 수집기가 없는 exe 에서도 내보내기를 시도하며 경고를 남깁니다.
- synthesis 서비스는 응답 시간 95% 값을 Prometheus 에서 계산하도록 `http.server.requests` 구간별 개수를 냅니다.
- CI 가 모니터링 component 를 붙인 매니페스트도 공개 CRD 스키마로 검사합니다.

## 고르지 않은 것

- Grafana Cloud 만 쓰기: 설치가 없지만 계정과 토큰이 필요하고, 클러스터 밖으로 지표가 나갑니다. 클러스터 메모리가 모자라면 그때 옮깁니다.
- 지표도 OTLP 로 보내기: 이미 Prometheus 형식으로 열어 두었고 kube-prometheus-stack 이 긁어 가는 방식이 가장 흔합니다.
- 서비스 메시(Istio, Linkerd)로 추적: 서비스 둘에 메시를 들이는 것보다 앱 안의 OpenTelemetry 가 가볍습니다.
