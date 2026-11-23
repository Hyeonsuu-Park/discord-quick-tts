package io.github.phs314.quicktts.shared;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** 공유 커널은 어느 컨텍스트도, 어느 프레임워크도 모른다. 자바와 API 약속(오류 코드)만 쓴다 (ADR 0035, ADR 0060). */
@AnalyzeClasses(packages = "io.github.phs314.quicktts.shared", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule sharedKernelDependsOnlyOnJavaAndApiContract = classes()
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "io.github.phs314.quicktts.shared..", "io.github.phs314.quicktts.api..");
}
