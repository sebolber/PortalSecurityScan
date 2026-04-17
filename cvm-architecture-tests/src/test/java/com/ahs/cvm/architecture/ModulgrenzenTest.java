package com.ahs.cvm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Prueft die in {@code CLAUDE.md} Abschnitt 3 definierten Modulgrenzen.
 *
 * <p>Paketkonvention:
 * <ul>
 *   <li>{@code com.ahs.cvm.domain} &mdash; reiner Fachkern, keine Spring-Abhaengigkeiten.</li>
 *   <li>{@code com.ahs.cvm.persistence} &mdash; nur auf Domain.</li>
 *   <li>{@code com.ahs.cvm.application} &mdash; auf Domain und Persistence.</li>
 *   <li>{@code com.ahs.cvm.integration} &mdash; auf Domain und Application.</li>
 *   <li>{@code com.ahs.cvm.llm} &mdash; auf Domain.</li>
 *   <li>{@code com.ahs.cvm.ai} &mdash; auf Application, LLM-Gateway, Integration.</li>
 *   <li>{@code com.ahs.cvm.api} &mdash; auf alle darunterliegenden.</li>
 *   <li>{@code com.ahs.cvm.app} &mdash; auf alle.</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.ahs.cvm",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ModulgrenzenTest {

    @ArchTest
    static final ArchRule domain_hat_keine_frameworkabhaengigkeiten =
            classes().that()
                    .resideInAPackage("com.ahs.cvm.domain..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "com.ahs.cvm.domain..",
                            "java..",
                            "jakarta.validation..",
                            "lombok..");

    @ArchTest
    static final ArchRule persistence_greift_nur_auf_domain_zu =
            classes().that()
                    .resideInAPackage("com.ahs.cvm.persistence..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "com.ahs.cvm.persistence..",
                            "com.ahs.cvm.domain..",
                            "java..",
                            "jakarta..",
                            "lombok..",
                            "org.springframework..",
                            "org.hibernate..",
                            "org.postgresql..",
                            "com.pgvector..",
                            "org.flywaydb..");

    @ArchTest
    static final ArchRule api_greift_nicht_direkt_auf_persistence_zu =
            noClasses().that()
                    .resideInAPackage("com.ahs.cvm.api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.ahs.cvm.persistence..");

    @ArchTest
    static final ArchRule domain_hat_keinen_springframework_zugriff =
            noClasses().that()
                    .resideInAPackage("com.ahs.cvm.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule llm_gateway_greift_nur_auf_domain_zu =
            noClasses().that()
                    .resideInAPackage("com.ahs.cvm.llm..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.ahs.cvm.persistence..",
                            "com.ahs.cvm.application..",
                            "com.ahs.cvm.api..");

    @ArchTest
    static final ArchRule application_kennt_keine_api =
            noClasses().that()
                    .resideInAPackage("com.ahs.cvm.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.ahs.cvm.api..");

    @ArchTest
    static final ArchRule integration_kennt_keine_api =
            noClasses().that()
                    .resideInAPackage("com.ahs.cvm.integration..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.ahs.cvm.api..");
}
