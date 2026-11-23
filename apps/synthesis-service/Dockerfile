# syntax=docker/dockerfile:1
#
# synthesis 서비스 이미지. 저장소 루트에서 만든다 (빌드에 build-logic, libs 가 필요하다).
#   docker build -f apps/synthesis-service/Dockerfile -t quicktts-synthesis-service .
#   docker run --rm -p 8080:8080 quicktts-synthesis-service

# 1. jar 만들기. jar 는 CPU 종류와 상관없으므로 빌드하는 PC 의 CPU 로 한 번만 돈다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY build-logic/ build-logic/
COPY libs/api-contract/build.gradle.kts libs/api-contract/
COPY libs/synthesis-contract/build.gradle.kts libs/synthesis-contract/
COPY apps/synthesis-service/build.gradle.kts apps/synthesis-service/
# settings.gradle.kts 가 포함한 다른 모듈도 폴더가 있어야 Gradle 이 켜진다. 빌드 설정 파일만 넣고 소스는 넣지 않는다.
COPY apps/bot-server/build.gradle.kts apps/bot-server/
COPY apps/desktop-client/build.gradle.kts apps/desktop-client/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:synthesis-service:dependencies --configuration runtimeClasspath > /dev/null
COPY libs/api-contract/src/ libs/api-contract/src/
COPY libs/synthesis-contract/src/ libs/synthesis-contract/src/
COPY apps/synthesis-service/src/main/ apps/synthesis-service/src/main/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:synthesis-service:bootJar \
    && cp apps/synthesis-service/build/libs/synthesis-service-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2. 실행 이미지. 네이티브 라이브러리를 쓰지 않지만 봇 서버와 같은 베이스를 써서 레이어를 나눠 쓴다.
FROM eclipse-temurin:25-jre
RUN groupadd --system --gid 10001 quicktts \
    && useradd --system --uid 10001 --gid quicktts --home-dir /app --no-create-home quicktts
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
