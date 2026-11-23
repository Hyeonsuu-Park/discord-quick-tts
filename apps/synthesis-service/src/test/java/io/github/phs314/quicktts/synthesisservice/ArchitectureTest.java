package io.github.phs314.quicktts.synthesisservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * synthesis 서비스는 웹 어댑터와 조립 코드만 가진다. synthesis 는 공개 입구(application.port.in)로만 부른다 (ADR 0061).
 */
@AnalyzeClasses(packages = "io.github.phs314.quicktts.synthesisservice", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule webAdapterUsesOnlyInputPorts = noClasses()
            .that().resideInAPackage("..synthesisservice.adapter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.github.phs314.quicktts.synthesis.application.service..",
                    "io.github.phs314.quicktts.synthesis.application.port.out..",
                    "io.github.phs314.quicktts.synthesis.adapter..",
                    "io.github.phs314.quicktts.synthesis.domain..");

    @ArchTest
    static final ArchRule serviceDoesNotKnowBotServer = noClasses()
            .should().dependOnClassesThat().resideInAPackage("io.github.phs314.quicktts.bot..");
}
