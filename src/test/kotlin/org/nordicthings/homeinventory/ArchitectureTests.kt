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
}
