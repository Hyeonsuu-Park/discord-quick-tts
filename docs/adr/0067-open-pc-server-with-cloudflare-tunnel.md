# 이 PC 에 띄운 서버는 Cloudflare Tunnel 로 인터넷에 연다

- 상태: 일부 대체됨
  - 배포로 띄울 때는 `compose.prod.yaml` 을 덮어써서 ghcr.io 이미지, 재시작 정책, 닫힌 포트로 띄움 → ADR 0068
- 날짜: 2026-11-23

첫 배포는 따로 서버를 빌리지 않고 이 PC 에서 `docker compose --profile app` 으로 띄운 봇 서버, synthesis 서비스, MySQL 을 씁니다(ADR 0056). 다른 PC 의 데스크톱 앱이 붙으려면 봇 서버에 인터넷 주소가 있어야 합니다. 집 공유기는 포트를 열기 번거롭고, 공인 IP 가 바뀌며, HTTPS 인증서도 따로 챙겨야 합니다.

## 결정

1. **Cloudflare Tunnel(`cloudflared`)을 `compose.yaml` 에 `tunnel` 프로필로 둡니다.** `cloudflared` 가 이 PC 에서 Cloudflare 로 바깥쪽 연결만 맺고, Cloudflare 가 받은 HTTPS 요청을 compose 네트워크 안의 `bot-server:8080` 으로 넘깁니다. 공유기 포트는 열지 않습니다.
2. **처음에는 계정 없이 쓰는 quick tunnel 로 엽니다.** `https://<무작위>.trycloudflare.com` 주소가 켤 때마다 새로 생깁니다. 데스크톱 앱은 `client.properties` 의 `server-url` 을 그 주소로 바꿔 붙습니다.
3. **주소를 고정해야 할 때(서버 주소를 넣은 exe 를 나눠 줄 때) named tunnel 로 바꿉니다.** Cloudflare 계정과 Cloudflare 에 올린 도메인이 있어야 합니다. 대시보드에서 터널을 만들고 받은 토큰으로 `tunnel --no-autoupdate run` 을 띄우는 방식이며, 같은 `cloudflared` 서비스의 실행 명령과 토큰만 바뀝니다.
4. **PC 의 8080 이 이미 쓰이고 있을 수 있어 봇 서버의 PC 쪽 포트를 `QUICKTTS_HOST_PORT` 로 바꿀 수 있게 합니다.** 터널은 compose 네트워크 안에서 붙으므로 이 값과 상관없습니다.

## 결과

- 공유기 설정, 공인 IP, 인증서 없이 HTTPS 주소가 생깁니다.
- 바깥에 열리는 것은 봇 서버의 웹 API 하나입니다. quick chat 과 지표는 기기 토큰이 있어야 하고, 기기 등록은 디스코드 `/연결` 로 받은 5분짜리 일회용 코드가 있어야 합니다. synthesis 서비스와 MySQL 은 터널로 열리지 않습니다.
- 기기 토큰이 있는 사람이 보내는 요청 수는 아직 막지 않습니다. 모르는 사람에게 나눠 주기 전에 사용량 제한이 필요합니다.
- PC 와 Docker Desktop 이 켜져 있는 동안만 봇이 돕니다.
- quick tunnel 은 Cloudflare 가 시험용으로 내주는 것이라 가용성 약속이 없고, 다시 켜면 주소가 바뀌어 앱 설정도 다시 바꿔야 합니다.

## 고르지 않은 것

- 공유기 포트 포워딩: 공유기마다 설정이 다르고, 집 공인 IP 가 바깥에 드러나며, 인증서와 바뀌는 IP 를 따로 챙겨야 합니다.
- ngrok: 같은 방식이지만 무료판은 고정 주소가 하나뿐이고 요청 수 제한과 안내 화면이 있습니다.
- Tailscale Funnel: 주소가 tailnet 이름에 묶이고, 개인 도메인을 붙일 수 없습니다.
- `cloudflared` 를 Windows 서비스로 설치: 터널이 compose 와 따로 켜지고 꺼집니다. compose 에 두면 `up`, `down` 한 번으로 같이 움직이고, 나중에 다른 서버로 옮겨도 같은 파일을 씁니다.
