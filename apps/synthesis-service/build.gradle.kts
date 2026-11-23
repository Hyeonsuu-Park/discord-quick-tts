// synthesis 서비스: synthesis 컨텍스트(contexts/synthesis)를 클러스터 안 웹 API 로 내놓는다 (ADR 0061).
// 상태가 없어 몇 벌이든 띄울 수 있다. 디스코드도 DB 도 모른다.
plugins {
    id("quicktts.spring-app")
}

dependencies {
    implementation(project(":contexts:synthesis"))
    implementation(project(":libs:synthesis-contract"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 상태 확인(쿠버네티스 프로브)과 지표. Prometheus 는 관리 포트로만 연다.
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // 분산 추적. 봇 서버가 보낸 추적 머리말(traceparent)을 이어 받는다 (ADR 0063).
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // 같은 문장의 음성을 기억해 두는 캐시. 봇 서버와 같은 설정이고, 벌마다 따로 기억한다.
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(testFixtures(project(":contexts:synthesis")))
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
