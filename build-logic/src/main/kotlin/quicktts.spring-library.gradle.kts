// 스프링을 쓰지만 혼자 띄우지 않는 모듈(contexts/ 아래 업무 모듈). 앱이 가져다 조립한다.
// 라이브러리 버전은 앱과 같게 스프링 부트가 정한 것을 따르고, 실행 jar(bootJar)는 만들지 않는다.
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("quicktts.java")
    `java-library`
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}
