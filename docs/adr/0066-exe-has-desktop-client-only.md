# exe 에는 데스크톱 클라이언트만 넣고, 서버는 따로 띄운다

- 상태: 채택
- 날짜: 2026-11-23

exe 는 데스크톱 클라이언트 `QuickTTS.exe` 와 봇 서버 `QuickTTS-Server.exe` 를 함께 담았고, 클라이언트가 켜질 때 이 PC 의 봇 서버를 같이 켰습니다(`LocalBotServer`). 서버를 따로 띄울 곳이 없던 때에 설치 하나로 다 되게 하려던 것입니다. 이제 음성을 synthesis 서비스가 만들고(ADR 0061), 서버는 이미지(ADR 0055), compose(ADR 0056), 쿠버네티스(ADR 0059)로 띄웁니다. exe 안의 서버가 계속 돌려면 synthesis 서비스까지 exe 에 넣어 프로세스 셋을 관리해야 합니다. 봇 토큰을 가진 서버는 봇 주인 한 사람만 띄우면 되고, 클라이언트를 쓰는 다른 사람은 서버가 필요 없습니다.

## 결정

1. **exe(설치 마법사, 압축 파일)에는 `QuickTTS.exe` 하나만 넣습니다.** `QuickTTS-Server.exe` 실행기와 봇 서버 jar, 클라이언트가 서버를 켜고 끄던 `LocalBotServer` 를 없앱니다.
2. **봇 서버와 synthesis 서비스는 exe 밖에서 띄웁니다.** 개발 PC 는 `bootRun` 두 개나 `docker compose --profile app up`, 배포는 쿠버네티스입니다.
3. **클라이언트는 서버 주소로만 붙습니다.** 기본 주소는 지금처럼 `http://localhost:8080` 이고, `client.properties` 의 `server-url` 이나 만들 때 `-PserverUrl` 로 바꿉니다.
4. **exe 전용 서버 설정을 없앱니다.** `packaged` 프로필(데이터와 로그를 `~/.discord-quick-tts` 에 두던 것)과 `~/.discord-quick-tts/server.env` 읽기를 뺍니다. 봇 서버 설정은 저장소의 `.env` 나 환경변수로만 받습니다.

## 결과

- 설치 파일이 작아지고(봇 서버 jar 와 그 라이브러리가 빠짐), 클라이언트가 서버 프로세스를 다루지 않습니다.
- 예전 exe 를 쓰던 사람은 봇 서버를 따로 띄워야 합니다. 예전 exe 가 만든 DB(`~/.discord-quick-tts/data/`)는 남아 있으므로, 봇 서버의 `QUICKTTS_DB_URL` 을 그 파일로 두면 등록한 PC, 목소리, 허용 목록을 그대로 이어 씁니다.
- 새 설치 파일로 업그레이드하면 옛 `QuickTTS-Server.exe` 는 설치 폴더에서 지워집니다. 사용자 폴더의 `server.env`, `data/`, `logs/` 는 남습니다.
- H2 파일 DB 는 개발 PC 의 `bootRun` 기본값으로 남깁니다. 배포는 MySQL 입니다(ADR 0016).

## 고르지 않은 것

- exe 에 synthesis 서비스 실행기도 넣어 클라이언트가 서버 둘을 켜고 끄기: 쓰는 사람이 봇 주인 한 명뿐인데 프로세스 셋의 켜기·끄기·포트를 설치 파일이 떠안습니다.
- 봇 서버 실행기만 남기고 synthesis 를 그 안에서 돌리기: 같은 코드를 두 곳에 두는 짐(ADR 0065 가 없앤 것)이 다시 생깁니다.
