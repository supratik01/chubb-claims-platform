package com.chubb.claims.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Makes the layering rule from the architecture document executable: the build fails if a class reaches
 * across layers in a direction that is not allowed.
 */
@AnalyzeClasses(packages = "com.chubb.claims", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule layersOnlyDependInwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("api").definedBy("com.chubb.claims.api..")
            .layer("service").definedBy("com.chubb.claims.service..")
            .layer("domain").definedBy("com.chubb.claims.domain..")
            .layer("infrastructure").definedBy("com.chubb.claims.infrastructure..")
            .whereLayer("api").mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("service").mayOnlyBeAccessedByLayers("api", "infrastructure")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("api", "service", "infrastructure");

    @ArchTest
    static final ArchRule controllersDoNotTouchRepositories = noClasses()
            .that().resideInAPackage("com.chubb.claims.api..")
            .should().dependOnClassesThat().resideInAPackage("com.chubb.claims.domain.repository..");

    @ArchTest
    static final ArchRule theDomainKnowsNothingAboutWebMessagingOrSecurity = noClasses()
            .that().resideInAPackage("com.chubb.claims.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "org.springframework.kafka..",
                    "org.springframework.security..",
                    "tools.jackson..");

    @ArchTest
    static final ArchRule servicesKnowNothingAboutWebMessagingOrSecurity = noClasses()
            .that().resideInAPackage("com.chubb.claims.service..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "org.springframework.kafka..",
                    "org.springframework.security..",
                    "jakarta.persistence..");
}
