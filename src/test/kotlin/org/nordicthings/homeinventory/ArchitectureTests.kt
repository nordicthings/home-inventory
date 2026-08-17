package org.nordicthings.homeinventory

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["org.nordicthings.homeinventory"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTests {

    @ArchTest
    val domainShouldNotDependOnSpring: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .allowEmptyShould(true)

    @ArchTest
    val domainShouldNotDependOnApplicationOrAdapters: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
            .allowEmptyShould(true)

    @ArchTest
    val applicationShouldNotDependOnAdapters: ArchRule =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..")
            .allowEmptyShould(true)
}
