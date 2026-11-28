// 데스크톱 앱과 봇 서버가 함께 쓰는 웹 API 약속(경로, 요청·응답 형식, 오류 코드).
plugins {
    id("quicktts.java")
    `java-library`
}

dependencies {
    // 응답에서 null 일 수 있는 필드에 @Nullable 을 단다. 봇 서버가 API 약속(openapi.json)에 그대로 옮긴다 (ADR 0076).
    api(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    // 오류 코드 목록 파일이 바뀌어도 테스트를 다시 돌린다.
    inputs.file("error-codes.txt").withPropertyName("errorCodes")
}
