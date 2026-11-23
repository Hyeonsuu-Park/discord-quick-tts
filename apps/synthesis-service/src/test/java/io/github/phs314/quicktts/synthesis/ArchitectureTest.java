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
 * synthesis 서비스 안의 헥사고날 의존 방향. 봇 서버의 ArchitectureTest 와 같은 규칙을 이 서비스에 건다 (ADR 0032, ADR 0065).
 * 웹 API 약속(libs/synthesis-contract 의 {@code synthesis.contract})은 웹 어댑터만 쓴다.
 */
@AnalyzeClasses(packages = "io.github.phs314.quicktts.synthesis", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String CONTRACT = "io.github.phs314.quicktts.synthesis.contract..";

    @ArchTest
    static final ArchRule synthesisIsHexagonal = onionArchitecture()
            .domainModels("..synthesis.domain..")
            .applicationServices("..synthesis.application..")
            .adapter("web", "..synthesis.adapter.in.web..")
            .adapter("tts", "..synthesis.adapter.out.tts..")
            .withOptionalLayers(true);

    /** 이 서비스는 다른 앱(봇 서버, 데스크톱 앱)의 코드를 모른다. 봇 서버와는 웹 API 약속으로만 이어진다. */
    @ArchTest
    static final ArchRule serviceDoesNotKnowOtherApps = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.github.phs314.quicktts.bot..", "io.github.phs314.quicktts.client..");

    /** 도메인은 자바 표준 라이브러리와 도메인, API 약속(오류 코드)만 쓴다 (ADR 0035). */
    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndDomain = classes()
            .that().resideInAPackage("..synthesis.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "..synthesis.domain..", "io.github.phs314.quicktts.api..");

    /** 도메인과 애플리케이션은 어댑터도, 웹 API 약속도 모른다 (ADR 0035). */
    @ArchTest
    static final ArchRule domainAndApplicationDoNotKnowAdapters = noClasses()
            .that().resideInAnyPackage("..synthesis.domain..", "..synthesis.application..")
            .should().dependOnClassesThat().resideInAnyPackage("..synthesis.adapter..", CONTRACT);

    /** 웹 API 약속은 웹 어댑터만 구현한다. 엔진 어댑터가 약속 모양에 묶이지 않게 한다. */
    @ArchTest
    static final ArchRule onlyWebAdapterUsesContract = noClasses()
            .that().resideOutsideOfPackages("..synthesis.adapter.in.web..", CONTRACT)
            .should().dependOnClassesThat().resideInAPackage(CONTRACT);

    /** 서비스에는 빈 등록(@Service)과 트랜잭션(@Transactional) 애너테이션만 허용한다 (ADR 0009). */
    @ArchTest
    static final ArchRule applicationUsesOnlySpringAnnotations = noClasses()
            .that().resideInAPackage("..synthesis.application..")
            .should().dependOnClassesThat(resideInAnyPackage("org.springframework..")
                    .and(resideOutsideOfPackages(
                            "org.springframework.stereotype..", "org.springframework.transaction.annotation..")));
}
