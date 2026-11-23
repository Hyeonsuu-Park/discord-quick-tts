pluginManagement {
    // 모듈이 같이 쓰는 빌드 설정(quicktts.java, quicktts.spring-app)
    includeBuild("build-logic")
}

rootProject.name = "discord-quick-tts"

// apps: 띄우는 프로그램, contexts: 앱에서 떼어 낸 업무 모듈, libs: 앱끼리 나눠 쓰는 약속
include(
    ":apps:bot-server",
    ":apps:desktop-client",
    ":apps:synthesis-service",
    ":contexts:shared",
    ":contexts:synthesis",
    ":libs:api-contract",
    ":libs:synthesis-contract",
)
