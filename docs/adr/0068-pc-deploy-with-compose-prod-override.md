# 이 PC 를 배포 서버로 쓸 때는 compose.prod.yaml 을 덮어써서 띄운다

- 상태: 채택
- 날짜: 2026-11-28

ADR 0067 로 이 PC 의 `docker compose --profile app --profile tunnel up --build` 가 다른 사람이 붙는 배포가 됐습니다. 그런데 `compose.yaml` 은 개발용으로 만든 파일이라 운영에 쓰기에는 다음이 맞지 않았습니다.

- 재시작 정책이 없어 컨테이너가 죽거나 PC 를 다시 켜면 봇이 돌아오지 않습니다.
- MySQL, synthesis 서비스, Grafana 포트가 PC 의 모든 네트워크 주소에 열리고, 비밀번호가 파일에 적힌 개발용 값입니다.
- `--build` 로 PC 작업 폴더에서 이미지를 만들어서, 떠 있는 것이 CI 를 통과한 커밋인지 알 수 없고 커밋하지 않은 변경도 들어갑니다.
- 개발용 추적 화면(otel-lgtm)이 같이 뜨고, 컨테이너 로그에 크기 제한이 없습니다.
- 봇 서버와 synthesis 서비스가 떴는지 compose 가 모릅니다.

## 결정

1. **운영 설정은 `compose.prod.yaml` 에 두고 `compose.yaml` 위에 덮어씁니다.** Docker 문서가 권하는 방식(Use Compose in production)입니다. `docker compose -f compose.yaml -f compose.prod.yaml --profile app --profile tunnel up -d` 로 띄웁니다. 서비스 정의는 `compose.yaml` 한 곳에 두고, 운영에서 다른 것만 덮어씁니다.
2. **프로젝트 이름을 `quicktts-prod` 로 따로 둡니다.** 개발할 때 `bootRun` 이 쓰는 MySQL(ADR 0056)과 컨테이너, 네트워크, 데이터가 섞이지 않습니다.
3. **이미지는 ghcr.io 의 고정 태그를 받습니다(ADR 0058).** `.env` 의 `QUICKTTS_IMAGE_TAG`(예: `main-b8cc839`)로 정하고, 비어 있으면 띄우지 않습니다. 두 서비스는 같은 워크플로에서 같은 커밋으로 만들어지므로 태그 하나를 같이 씁니다.
4. **비밀번호는 `.env` 의 `QUICKTTS_PROD_DB_PASSWORD`, `QUICKTTS_PROD_DB_ROOT_PASSWORD` 로 받습니다.** `QUICKTTS_DB_PASSWORD` 와 이름을 다르게 둔 것은, 같은 `.env` 를 읽는 `bootRun` 이 H2 파일에 그 비밀번호로 붙으려다 실패하지 않게 하려는 것입니다.
5. **포트는 열지 않습니다.** MySQL 과 synthesis 서비스는 compose 네트워크 안에서만 닿습니다. 봇 서버는 이 PC 의 데스크톱 앱을 위해 `127.0.0.1` 에만 열고, 바깥은 cloudflared 로만 붙습니다.
6. **모든 서비스에 `restart: unless-stopped`, 로그는 서비스마다 10MB 파일 3개까지.** PC 를 다시 켜도 돌아오려면 Docker Desktop 의 "로그인할 때 켜기"가 켜져 있어야 합니다.
7. **봇 서버와 synthesis 서비스에 healthcheck 를 둡니다.** 실행 이미지에 curl 이 없어서 bash 로 `/actuator/health/liveness` 를 부릅니다. 봇 서버는 synthesis 서비스가, cloudflared 는 봇 서버가 healthy 가 된 뒤에 뜹니다. 봇 서버의 기다리는 시간은 쿠버네티스 startupProbe 와 같은 2분 30초입니다.
8. **otel-lgtm 은 `observability` 프로필로 옮겨 기본으로 띄우지 않고, 두 서비스의 추적 내보내기 주소도 뺍니다.**

## 결과

- 떠 있는 것이 어느 커밋인지 `.env` 의 태그로 압니다. 옛 판으로 돌아가려면 태그를 바꾸고 `up -d` 합니다.
- 같은 와이파이의 다른 기기가 MySQL, synthesis 서비스, Grafana 에 닿지 않습니다.
- 개발용 compose 와 운영 compose 는 데이터가 따로입니다. 옛 배포(ADR 0067 의 명령)의 데이터는 README 의 순서대로 한 번 옮깁니다.
- quick tunnel 은 다시 켤 때마다 주소가 바뀌는 것은 그대로입니다. 재시작 정책으로 PC 를 다시 켜면 저절로 뜨지만 주소는 새로 생깁니다. 고정 주소는 named tunnel 로 바꿀 때 생깁니다(ADR 0067 3번).
- 켜는 명령에 `-f` 가 두 개 붙습니다. `logs`, `ps`, `down` 에도 같은 `-f` 와 `--profile` 을 붙입니다.
- 이미지가 ghcr.io 에 올라간 커밋만 띄울 수 있습니다. 서버 코드를 고치지 않은 커밋에는 새 이미지가 없으므로, 태그는 ghcr.io 패키지 화면에서 고릅니다.

## 고르지 않은 것

- `compose.yaml` 을 운영용으로 바꾸기: 개발할 때 `bootRun` 이 쓰는 MySQL 과 `--build` 로 막 고친 코드를 띄워 보는 흐름이 사라집니다.
- 운영용 파일을 따로 통째로 쓰기(`compose.prod.yaml` 하나로): 서비스 정의가 두 곳에 생겨 한쪽만 고치는 일이 생깁니다.
- 같은 프로젝트 이름으로 데이터 이어 쓰기: 개발용 MySQL 과 같은 볼륨이라 비밀번호를 바꾸면 `bootRun` 쪽이 붙지 못하고, 개발하다 지운 데이터가 운영에서도 사라집니다.
- `docker compose` 대신 Windows 서비스나 작업 스케줄러로 띄우기: Docker 의 재시작 정책으로 같은 일을 하고, 나중에 다른 서버로 옮겨도 같은 파일을 씁니다.
