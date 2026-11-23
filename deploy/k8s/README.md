# 쿠버네티스 매니페스트

Kustomize 로 둡니다 ([ADR 0059](../../docs/adr/0059-kustomize-manifests-in-deploy.md)).

```
deploy/k8s/
├── base/                    모든 환경 공통 (네임스페이스 quicktts)
│   ├── bot-server/          한 벌, Recreate
│   ├── synthesis-service/   두 벌부터 CPU 로 네 벌까지, 봇 서버만 부름
│   └── mysql/
├── components/              환경에 따라 켜고 끄는 것
│   ├── cloudflared/         Cloudflare Tunnel (prod)
│   └── monitoring/          ServiceMonitor, 대시보드 (kube-prometheus-stack 설치 뒤)
└── overlays/
    ├── local/               PC 의 k3d 클러스터
    └── prod/                배포 서버
```

## PC 에서 k3d 로 띄우기 (Windows)

Docker Desktop 이 켜져 있어야 합니다. 클러스터가 메모리를 2GB 안팎 씁니다.

### 1. 한 번만: 도구와 개발용 봇

1. PowerShell 에서 도구를 설치합니다.

   ```powershell
   winget install k3d.k3d Kubernetes.kubectl Helm.Helm
   ```

2. 개발용 디스코드 봇을 하나 더 만듭니다. 배포 봇이나 `bootRun` 과 같은 토큰을 클러스터에서 함께 쓰면 명령이 두 번 처리되고 음성 연결을 서로 끊습니다.
   1. [Discord Developer Portal](https://discord.com/developers/applications) > New Application (예: `QuickTTS Dev`)
   2. Bot > Reset Token 으로 토큰을 받아 둡니다.
   3. OAuth2 > URL Generator 에서 `bot`, `applications.commands` 와 Connect, Speak 권한을 골라 만든 주소로 내 디스코드 서버에 초대합니다.
   4. 봇을 먼저 초대한 뒤 클러스터를 띄웁니다. 처음 켜질 때 들어가 있는 디스코드 서버가 허용 목록에 저절로 들어갑니다.

3. 비밀값 파일을 만듭니다(커밋되지 않습니다).

   ```powershell
   cd deploy/k8s/overlays/local
   copy secrets.env.example secrets.env   # DISCORD_TOKEN= 에 개발용 봇 토큰
   copy mysql.env.example mysql.env
   cd ../../../..
   ```

### 2. 클러스터 만들고 띄우기

```powershell
k3d cluster create quicktts
kubectl apply -k deploy/k8s/overlays/local
kubectl -n quicktts get pods -w        # 모두 Running, READY 1/1 이 될 때까지 (처음엔 몇 분)
kubectl -n quicktts port-forward svc/bot-server 8080:8080
```

데스크톱 앱을 `.\gradlew.bat :apps:desktop-client:run` 으로 띄우고, 서버 주소는 `http://localhost:8080` 그대로, 개발용 봇에서 `/연결` 로 받은 코드를 넣습니다.

이미지는 ghcr.io 에서 받습니다. 패키지가 비공개면 파드가 `ImagePullBackOff` 에 멈춥니다. GitHub 의 패키지 설정에서 `quicktts-bot-server`, `quicktts-synthesis-service` 를 공개로 바꿉니다.

### 자주 쓰는 명령

| 하고 싶은 것 | 명령 |
| --- | --- |
| 로그 보기 | `kubectl -n quicktts logs deploy/bot-server -f` |
| 새 이미지로 다시 띄우기 | `kubectl -n quicktts rollout restart deploy/bot-server deploy/synthesis-service` |
| 잠깐 끄기 / 다시 켜기 | `k3d cluster stop quicktts` / `k3d cluster start quicktts` |
| 모두 지우기 (DB 포함) | `k3d cluster delete quicktts` |

그래프를 보려면 [deploy/monitoring/README.md](../monitoring/README.md) 를 따릅니다.
