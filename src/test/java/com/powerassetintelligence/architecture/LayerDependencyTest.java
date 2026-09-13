package com.powerassetintelligence.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit-тест, фиксирующий архитектурные правила Clean Architecture:
 * <ul>
 *   <li>domain не зависит от Spring/Jakarta/persistence</li>
 *   <li>application не зависит от infrastructure и Spring Data JPA
 *       (исключение: org.springframework.stereotype/transaction)</li>
 *   <li>core.ai не зависит от Spring</li>
 *   <li>infrastructure может зависеть от application/domain/core</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.powerassetintelligence")
class LayerDependencyTest {

    // =========================================================================
    // 1. domain — чистый Java, никаких Spring / Jakarta / persistence
    // =========================================================================
    // noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat()
    //   .resideInAPackage("org.springframework..") — и так далее
    // .as(...) — описание правила

    @ArchTest
    ArchRule domainNoSpringJakarta =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .orShould()
                    .dependOnClassesThat()
                    .resideInAPackage("jakarta..")
                    .orShould()
                    .dependOnClassesThat()
                    .resideInAPackage("javax..")
                    .as("domain не должен зависеть от Spring, Jakarta или javax");

    // =========================================================================
    // 2a. application — не зависит от infrastructure
    // =========================================================================
    @ArchTest
    ArchRule applicationNoInfrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .as("application не должен зависеть от infrastructure");

    // =========================================================================
    // 2b. application.service — не зависит от Spring Data JPA
    //     (исключение: org.springframework.stereotype и
    //      org.springframework.transaction — разрешены)
    // =========================================================================
    @ArchTest
    ArchRule applicationNoSpringDataJpa =
            noClasses()
                    .that()
                    .resideInAPackage("..application.service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework.data.jpa..")
                    .as("application.service не должен зависеть от Spring Data JPA");

    // =========================================================================
    // 3. core.ai — чистый Java, никаких Spring-импортов
    // =========================================================================
    @ArchTest
    ArchRule coreAiNoSpring =
            noClasses()
                    .that()
                    .resideInAPackage("..core.ai..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .as("core.ai не должен зависеть от Spring");

    // =========================================================================
    // 4. infrastructure — самый внешний слой, может зависеть от application /
    //    domain / core.ai. Здесь мы НЕ ставим запретов — это логическое
    //    подтверждение архитектуры.
    // =========================================================================
}
