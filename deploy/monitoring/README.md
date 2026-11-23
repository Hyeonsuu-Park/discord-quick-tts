# 모니터링

봇 서버와 synthesis 서비스의 지표를 Prometheus 로 모으고 Grafana 로 봅니다 ([ADR 0063](../../docs/adr/0063-observability-prometheus-grafana-and-tracing.md)). 우리가 만든 것은 Kustomize(`deploy/k8s`), 남이 만든 것은 그쪽 Helm 차트에 값 파일만 둡니다.

| 폴더 | 내용 |
| --- | --- |
| `kube-prometheus-stack/values.yaml` | Prometheus, Grafana 설치 값 |
| `../k8s/components/monitoring` | 우리 서비스를 긁어 가게 하는 ServiceMonitor, QuickTTS 대시보드 |

## 설치 (클러스터에 한 번)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  -f deploy/monitoring/kube-prometheus-stack/values.yaml
```

설치한 차트 버전은 `helm list -n monitoring` 으로 보고, 올릴 때는 `helm upgrade` 에 같은 값 파일을 줍니다.

## 우리 서비스 붙이기

오버레이(`deploy/k8s/overlays/<환경>/kustomization.yaml`)의 `components` 에 모니터링을 더하고 다시 적용합니다.

```yaml
components:
  - ../../components/monitoring
```

## 보기

```bash
kubectl -n monitoring port-forward svc/monitoring-grafana 3000:80
```

http://localhost:3000 (admin / `values.yaml` 의 비밀번호) > Dashboards > QuickTTS 에서 quick chat 빠르기, TTS 성공·실패, 대신 읽기, 캐시 적중률을 봅니다.

## 추적

두 서비스는 OpenTelemetry 로 추적을 남기고, `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` 를 줄 때만 내보냅니다. 개발 PC 에서는 `docker compose --profile app up` 이 함께 띄우는 `otel-lgtm`(http://localhost:3000) 에서 봅니다. 클러스터에 추적 저장소(Tempo)를 둘 때 두 Deployment 에 이 환경변수를 더합니다.
