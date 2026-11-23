# 봇 서버는 Dockerfile 하나로 컨테이너 이미지를 만든다

- 상태: 채택
- 날짜: 2026-11-23

봇 서버는 개발 PC 의 `bootRun` 과 exe 안의 `QuickTTS-Server.exe` 로만 떴습니다. 배포 서버(Oracle, 현수 PC)나 쿠버네티스에 올리려면 어디서나 같은 모양으로 뜨는 이미지가 있어야 합니다. 설정은 이미 환경변수(`.env` 와 같은 이름)로 받고 DB 도 `QUICKTTS_DB_URL` 로 바꿀 수 있어서, 이미지만 있으면 됩니다. 하나 걸리는 게 있었습니다. 디스코드 음성 암호화(DAVE) 네이티브를 Windows 와 x86 리눅스 것만 넣어서, ARM 리눅스 서버(Oracle A1 등)에서는 음성 연결이 안 됩니다.

## 결정

1. **`apps/bot-server/Dockerfile` 하나로 만듭니다.** 앱 폴더마다 Dockerfile 을 두고, 빌드는 저장소 루트에서 합니다(`docker build -f apps/bot-server/Dockerfile .`). `build-logic` 과 `libs` 가 빌드에 필요하기 때문입니다.
2. **두 단계(multi-stage)로 만듭니다.** 첫 단계는 JDK 이미지 안에서 Gradle 로 `bootJar` 를 만들고, 둘째 단계는 JRE 이미지에 결과만 옮깁니다. PC 에 JDK 가 없어도 Docker 만 있으면 이미지를 만들 수 있고, jar 를 먼저 만들어야 하는 순서를 잊을 일이 없습니다. jar 는 CPU 종류와 상관없으므로 첫 단계는 빌드하는 PC 의 CPU 로만 돕니다(`--platform=$BUILDPLATFORM`).
3. **jar 를 스프링 부트 레이어로 풀어 넣습니다**(`java -Djarmode=tools -jar ... extract --layers`). 의존성(약 128MB)과 우리 코드(약 300KB)가 다른 레이어라서, 코드만 바뀐 새 이미지는 작은 레이어 하나만 받습니다.
4. **실행 이미지는 `eclipse-temurin:25-jre`(Ubuntu) 입니다.** 음성 네이티브(jdave, LavaPlayer)가 glibc 로 만들어져 있어서 Alpine(musl) 은 쓰지 않습니다.
5. **root 가 아닌 사용자(`quicktts`, uid 10001)로 돕니다.** H2 를 쓸 때의 파일 자리 `/app/data` 만 이 사용자가 씁니다.
6. **JVM 옵션은 `--enable-native-access=ALL-UNNAMED`(bootRun 과 같음)와 `-XX:MaxRAMPercentage=75` 입니다.** 컨테이너 메모리 한도에 맞춰 힙을 정하고, 나머지는 네이티브 음성 버퍼와 스레드 몫으로 둡니다. 더할 옵션은 `JAVA_TOOL_OPTIONS` 로 넣습니다.
7. **jdave 네이티브에 `linux-aarch64` 를 더합니다.** 같은 이미지가 ARM 리눅스에서도 음성 연결을 합니다. LavaPlayer 네이티브는 이미 ARM 리눅스 것을 들고 있습니다.

## 결과

- `docker build`, `docker run --env-file .env` 로 봇 서버를 띄울 수 있습니다(README "Docker 로 봇 서버 띄우기").
- 서버 jar 와 exe 에 ARM 네이티브(약 3.4MB)가 더 들어갑니다. exe 는 Windows 네이티브만 쓰지만, 서버 jar 를 나누지 않으려고 같이 둡니다.
- 이미지를 레지스트리에 올리는 일, 쿠버네티스에 맞춘 상태 확인은 이 ADR 에 들어 있지 않습니다. 따로 정합니다.
- ARM 에서 실제 음성이 나는지는 ARM 서버에 이미지를 띄워 디스코드에 붙여 봐야 압니다.

## 고르지 않은 것

- Gradle 이 밖에서 만든 jar 를 복사만 하는 Dockerfile: 이미지 빌드는 빨라지지만, jar 를 먼저 만들어야 해서 PC 에 JDK 가 있어야 하고 순서를 잊으면 옛 jar 가 들어갑니다.
- Jib, `bootBuildImage`(Buildpacks): Dockerfile 없이 이미지를 만들지만, 무엇이 들어가는지 파일로 보이지 않고 JVM 옵션과 베이스 이미지를 바꾸는 법이 도구마다 다릅니다. Dockerfile 이 가장 흔합니다.
- Alpine 이나 distroless 처럼 더 작은 베이스: Alpine 은 음성 네이티브가 돌지 않고, distroless 는 셸이 없어 처음 운영할 때 들여다보기 어렵습니다.
