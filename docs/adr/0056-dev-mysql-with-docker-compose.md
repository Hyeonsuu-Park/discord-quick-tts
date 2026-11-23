# 개발 PC 에서도 Docker Compose 로 MySQL 을 띄워 쓸 수 있게 한다

- 상태: 채택
- 날짜: 2026-11-23

배포 서버는 MySQL, 개발 PC 와 exe 는 H2 파일(MySQL 호환 모드)을 씁니다(ADR 0016). 테스트는 Testcontainers 로 MySQL 에서도 돌지만, `bootRun` 으로 봇을 직접 써 볼 때는 늘 H2 였습니다. 봇 서버를 컨테이너로 올리면(ADR 0055) 배포에서는 MySQL 만 쓰므로, 개발 PC 에서도 같은 DB 로 돌려 볼 수 있어야 H2 호환 모드와의 차이에서 오는 문제를 배포 전에 봅니다. 다만 개발할 때마다 Docker Desktop 을 켜야 하는 건 원하지 않습니다.

## 결정

1. **저장소 루트에 `compose.yaml` 을 둡니다.** `mysql`(8.4, 테스트와 같은 판)과 데이터 볼륨 하나입니다. 비밀번호는 이 PC 안에서만 쓰는 개발용 값입니다.
2. **스프링 부트의 Docker Compose 지원(`spring-boot-docker-compose`)을 `developmentOnly` 로 넣습니다.** `bootRun` 이 이 파일의 MySQL 을 띄우고 주소, 사용자, 비밀번호를 알아서 넣습니다. exe 와 이미지에는 들어가지 않습니다.
3. **기본은 꺼 둡니다.** `.env` 에 `QUICKTTS_DEV_MYSQL=true` 를 넣을 때만 켜집니다. 꺼 두면 지금처럼 H2 파일입니다.
4. **같은 파일에 봇 서버 이미지를 `app` 프로필로 둡니다.** `docker compose --profile app up --build` 로 봇 서버 컨테이너와 MySQL 을 함께 띄워 배포와 같은 모양을 이 PC 에서 돌려 봅니다. `bootRun` 이 파일을 읽을 때는 이 서비스를 건너뜁니다.

## 결과

- 개발 PC 에서 MySQL 로 Flyway 마이그레이션과 JPA 엔티티 확인(`ddl-auto: validate`)을 바로 봅니다.
- 켜면 `bootRun` 이 MySQL 이 준비될 때까지(처음 10초 남짓) 기다렸다가 뜹니다. 끌 때 MySQL 컨테이너도 멈추고, 데이터는 볼륨에 남습니다.
- 테스트는 지금처럼 Testcontainers 를 씁니다. 스프링 부트는 테스트에서 Compose 지원을 쓰지 않습니다.

## 고르지 않은 것

- 개발도 늘 MySQL: 개발할 때마다 Docker Desktop 이 떠 있어야 하고, exe 와 같은 H2 경로를 개발에서 쓰지 않게 됩니다.
- Compose 지원 없이 `docker compose up` 따로 하고 `.env` 에 주소 적기: 같은 일을 손으로 두 번 하고, 포트가 겹치면 직접 고쳐야 합니다.
