// synthesis 서비스: 문장과 목소리로 음성을 만들어(TTS 엔진 Edge, Google) 웹 API 로 내놓는다 (ADR 0061, ADR 0065).
// 봇 서버는 언제나 이 서비스를 HTTP 로 부른다. 상태가 없어 몇 벌이든 띄울 수 있다. 디스코드도 DB 도 모른다.
plugins {
    id("quicktts.spring-app")
}

dependencies {
    implementation(project(":libs:api-contract"))
    implementation(project(":libs:synthesis-contract"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 상태 확인(쿠버네티스 프로브)과 지표. Prometheus 는 관리 포트로만 연다.
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // 분산 추적. 봇 서버가 보낸 추적 머리말(traceparent)을 이어 받는다 (ADR 0063).
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // 같은 문장의 음성을 기억해 두는 캐시. 벌마다 따로 기억한다.
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    // 엔진이 연달아 실패하면 한동안 부르지 않고 바로 다음 엔진으로 넘기는 차단기와 그 지표 (ADR 0070).
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.micrometer)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootRun {
    // 저장소 루트의 .env 를 읽을 수 있도록 루트에서 실행한다.
    workingDir = rootProject.projectDir
    // 봇 서버(8080)와 같은 PC 에서 함께 띄우므로 8082 를 쓴다. 봇 서버의 기본 synthesis 주소도 이 포트다.
    // 컨테이너(Docker, 쿠버네티스)에서는 기본값 8080 그대로다.
    environment("SYNTHESIS_PORT", "8082")
}
