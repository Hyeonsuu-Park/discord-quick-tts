package io.github.phs314.quicktts.synthesis;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * synthesis 모듈 안의 헥사고날 의존 방향. 봇 서버의 ArchitectureTest 와 같은 규칙을 이 모듈에 건다 (ADR 0032, ADR 0060).
 * 이 모듈은 봇 서버와 synthesis 서비스가 함께 쓰므로 어느 앱의 코드도 알면 안 된다.
 */
@AnalyzeClasses(packages = "io.github.phs314.quicktts.synthesis", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule synthesisIsHexagonal = onionArchitecture()
            .domainModels("..synthesis.domain..")
            .applicationServices("..synthesis.application..")
            .adapter("tts", "..synthesis.adapter.out.tts..")
            .withOptionalLayers(true);

    /** 모듈은 앱(봇 서버, 데스크톱 앱)을 모른다. 앱이 모듈을 가져다 조립한다. */
    @ArchTest
    static final ArchRule moduleDoesNotKnowApps = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.github.phs314.quicktts.bot..", "io.github.phs314.quicktts.client..");

    /** 도메인은 자바 표준 라이브러리와 도메인(공유 커널 포함), API 약속(오류 코드)만 쓴다 (ADR 0035). */
    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndDomain = classes()
            .that().resideInAPackage("..synthesis.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "..synthesis.domain..", "io.github.phs314.quicktts.shared.domain..",
                    "io.github.phs314.quicktts.api..");

    /** 도메인과 애플리케이션은 어댑터를 모른다 (ADR 0035). */
    @ArchTest
    static final ArchRule domainAndApplicationDoNotKnowAdapters = noClasses()
            .that().resideInAnyPackage("..synthesis.domain..", "..synthesis.application..")
            .should().dependOnClassesThat().resideInAPackage("..synthesis.adapter..");

    /** 서비스에는 빈 등록(@Service)과 트랜잭션(@Transactional) 애너테이션만 허용한다 (ADR 0009). */
    @ArchTest
    static final ArchRule applicationUsesOnlySpringAnnotations = noClasses()
            .that().resideInAPackage("..synthesis.application..")
            .should().dependOnClassesThat(resideInAnyPackage("org.springframework..")
                    .and(resideOutsideOfPackages(
                            "org.springframework.stereotype..", "org.springframework.transaction.annotation..")));
}
