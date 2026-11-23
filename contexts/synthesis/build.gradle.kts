// synthesis 컨텍스트: 문장과 목소리로 음성을 만든다 (TTS 엔진 Edge, Google).
// 봇 서버가 같은 프로세스 안에서 쓰고, 따로 띄우는 synthesis 서비스도 같은 코드를 쓴다 (ADR 0060).
plugins {
    id("quicktts.spring-library")
    // 다른 모듈의 테스트가 쓰는 가짜 엔진(FakeSpeechSynthesizerPort)을 src/testFixtures 에 둔다.
    `java-test-fixtures`
}

dependencies {
    api(project(":contexts:shared"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.micrometer:micrometer-core")
    implementation("org.slf4j:slf4j-api")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
