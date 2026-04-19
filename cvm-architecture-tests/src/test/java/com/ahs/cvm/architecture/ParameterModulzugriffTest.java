package com.ahs.cvm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Iteration 46: Nur das Parameter-Modul
 * ({@code com.ahs.cvm.application.parameter}) darf die System-Parameter-
 * Repositories und den Secret-Cipher direkt kennen.
 *
 * <p>Damit bleibt die Verantwortung fuer Tenant-Scope, Audit und AES-GCM-
 * Verschluesselung exklusiv beim Parameter-Modul; Aufrufer nutzen den
 * {@code SystemParameterResolver} (fuer Lese-Zugriff) oder den Service
 * (fuer Schreib-/Verwaltungs-Operationen).
 */
@AnalyzeClasses(
        packages = "com.ahs.cvm",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ParameterModulzugriffTest {

    @ArchTest
    static final ArchRule nur_parameter_modul_kennt_die_repositories =
            noClasses().that()
                    .resideOutsideOfPackage("com.ahs.cvm.application.parameter..")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName(
                            "com.ahs.cvm.persistence.parameter.SystemParameterRepository")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName(
                            "com.ahs.cvm.persistence.parameter.SystemParameterAuditLogRepository");

    @ArchTest
    static final ArchRule nur_parameter_modul_kennt_den_cipher =
            noClasses().that()
                    .resideOutsideOfPackage("com.ahs.cvm.application.parameter..")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName(
                            "com.ahs.cvm.application.parameter.SystemParameterSecretCipher");
}
