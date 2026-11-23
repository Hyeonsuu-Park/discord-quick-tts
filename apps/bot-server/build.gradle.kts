plugins {
    id("quicktts.spring-app")
}

dependencies {
    implementation(project(":libs:api-contract"))
    // 떼어 낸 컨텍스트. 같은 프로세스 안에서 입력 포트를 바로 부른다 (ADR 0060).
    implementation(project(":contexts:shared"))
    implementation(project(":contexts:synthesis"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // 기기 토큰 인증. 토큰으로 사람을 알아내는 필터만 우리가 쓰고 나머지는 스프링 시큐리티 기본을 따른다.
    implementation("org.springframework.boot:spring-boot-starter-security")
    // 저장소 어댑터는 JPA 엔티티를 도메인과 따로 두고 매퍼로 옮긴다 (ADR 0038).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // 설정 값(@ConfigurationProperties)의 필수 값을 켜질 때 검사한다.
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // 스키마는 Flyway 마이그레이션(resources/db/migration)으로 관리한다.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // 상태 확인(/actuator/health)과 지표(Micrometer).
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Prometheus 가 긁어 가는 /actuator/prometheus (쿠버네티스에서는 관리 포트로만 연다, ADR 0057).
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // 같은 문장의 음성을 기억해 두는 캐시.
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-mysql")
    // 웹 API 문서(OpenAPI). 컨트롤러에서 /v3/api-docs 를 만들고 /swagger-ui.html 로 보여 준다.
    implementation(libs.springdoc.webmvc.ui)
    // 배포 서버는 MySQL, 이 PC 에서 띄우는 서버(개발, exe)는 설치가 필요 없는 H2 파일 DB.
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    // bootRun 때만 저장소 루트의 compose.yaml 로 개발용 MySQL 을 띄운다(QUICKTTS_DEV_MYSQL=true). exe 와 이미지에는 들어가지 않는다.
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation(libs.jda)
    implementation(libs.lavaplayer)
    // 디스코드 음성 연결에 필요한 DAVE(종단간 암호화) 구현. 네이티브는 exe(Windows), x86 리눅스 서버, ARM 리눅스 서버(Oracle A1 등) 몫이다.
    implementation(libs.jdave.api)
    runtimeOnly(libs.jdave.natives.windows)
    runtimeOnly(libs.jdave.natives.linux)
    runtimeOnly(libs.jdave.natives.linux.arm)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // 컨트롤러 테스트(@WebMvcTest)가 MVC, JSON, 보안 필터를 실제 설정 그대로 올린다.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.archunit.junit5)
    // API 통합 테스트가 진짜 TTS 엔진 대신 쓰는 가짜 엔진.
    testImplementation(testFixtures(project(":contexts:synthesis")))
    // 저장소 테스트를 진짜 MySQL 에서도 돌린다. Docker 가 있어야 한다.
    testImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootRun {
    // 저장소 루트의 .env 를 읽을 수 있도록 루트에서 실행한다.
    workingDir = rootProject.projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
