// 공유 커널: 여러 컨텍스트가 함께 쓰는 값 객체(DiscordUserId, GuildId, VoiceId)와 도메인 예외의 공통 부모.
// 프레임워크 없이 자바와 API 약속(오류 코드)만 쓴다.
plugins {
    id("quicktts.java")
    `java-library`
}

dependencies {
    // 도메인 예외가 클라이언트에 내보낼 오류 코드를 들고 있다.
    api(project(":libs:api-contract"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
