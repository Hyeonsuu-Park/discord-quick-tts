# syntax=docker/dockerfile:1
#
# 봇 서버 이미지. 저장소 루트에서 만든다 (빌드에 build-logic, libs 가 필요하다).
#   docker build -f apps/bot-server/Dockerfile -t quicktts-bot-server .
# 설정은 .env 와 같은 이름의 환경변수로 넣는다 (DISCORD_TOKEN, QUICKTTS_DB_URL 등, .env.example 참고).
#   docker run --rm -p 8080:8080 --env-file .env quicktts-bot-server

# 1. jar 만들기. jar 는 CPU 종류와 상관없으므로 arm64 이미지를 만들 때도 빌드하는 PC 의 CPU 로 한 번만 돈다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
# 의존성과 Gradle 배포판을 먼저 받아 두어, 코드만 바뀌면 이 단계를 캐시에서 쓴다.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY build-logic/ build-logic/
COPY libs/api-contract/build.gradle.kts libs/api-contract/
COPY contexts/shared/build.gradle.kts contexts/shared/
COPY contexts/synthesis/build.gradle.kts contexts/synthesis/
COPY apps/bot-server/build.gradle.kts apps/bot-server/
# settings.gradle.kts 가 데스크톱 앱도 포함하므로 빌드 설정 파일만 넣는다. 소스는 넣지 않는다.
COPY apps/desktop-client/build.gradle.kts apps/desktop-client/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:bot-server:dependencies --configuration runtimeClasspath > /dev/null
COPY libs/api-contract/src/ libs/api-contract/src/
COPY contexts/shared/src/main/ contexts/shared/src/main/
COPY contexts/synthesis/src/main/ contexts/synthesis/src/main/
COPY apps/bot-server/src/main/ apps/bot-server/src/main/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:bot-server:bootJar \
    && cp apps/bot-server/build/libs/bot-server-*.jar application.jar
# 의존성(잘 안 바뀜)과 우리 코드(자주 바뀜)를 다른 레이어로 나눈다. 코드만 바뀌면 받을 레이어가 작다.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2. 실행 이미지. 음성 네이티브(jdave, LavaPlayer)가 glibc 를 쓰므로 Alpine(musl) 이 아닌 Ubuntu 기반을 쓴다.
FROM eclipse-temurin:25-jre
RUN groupadd --system --gid 10001 quicktts \
    && useradd --system --uid 10001 --gid quicktts --home-dir /app --no-create-home quicktts
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
# QUICKTTS_DB_URL 을 주지 않으면 H2 파일 DB 를 /app/data 에 만든다. 남기려면 이 폴더에 볼륨을 붙인다.
RUN mkdir data && chown quicktts:quicktts data
USER 10001:10001
EXPOSE 8080
# --enable-native-access: 음성 네이티브(jdave)가 JDK 의 네이티브 접근 경고 없이 돌게 한다 (bootRun 과 같음).
# MaxRAMPercentage: 컨테이너 메모리 한도의 75% 까지 힙으로 쓴다. 나머지는 네이티브 음성 버퍼와 스레드 몫이다.
# JVM 옵션을 더하려면 JAVA_TOOL_OPTIONS 환경변수를 쓴다.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
