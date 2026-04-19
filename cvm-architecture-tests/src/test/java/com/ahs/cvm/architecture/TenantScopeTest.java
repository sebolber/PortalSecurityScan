package com.ahs.cvm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * Iteration 62F (CVM-62): Invariante fuer den Tenant-Rollout.
 * Entities, die den Mandanten tragen, MUESSEN ein Feld {@code tenantId}
 * mit {@code @Column(name = "tenant_id")} haben. Entities ohne Scope
 * (Stammdaten, Tenant selbst, globale Konfig) stehen auf der
 * Ausnahmeliste.
 */
@AnalyzeClasses(
        packages = "com.ahs.cvm.persistence",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class TenantScopeTest {

    /**
     * Entities, die bewusst mandantenuebergreifend gefuehrt werden -
     * entweder globale Stammdaten (CVE, Component, Rule, Tenant selbst)
     * oder sekundaere Daten, deren Mandanten-Scope ueber eine
     * Parent-Entity (Finding, Assessment, Environment) indirekt
     * durchgesetzt wird. Ein direkter `tenant_id`-FK auf diesen Tabellen
     * waere eine sinnvolle Follow-up-Haertung.
     */
    private static final Set<String> MANDANTENUEBERGREIFEND = Set.of(
            "Tenant",
            "Cve",
            "Component",
            "Rule",
            "ModelProfile",
            "LlmModelProfile",
            "ModelProfileChangeLog",
            "BrandingConfig",
            "BrandingAsset",
            "BrandingConfigHistory",
            "LlmConfiguration",
            "SystemParameter",
            "SystemParameterAuditLog",
            "AiCallAudit",
            "AiEmbedding",
            "AiSourceRef",
            "AiSuggestion",
            "Embedding",
            "ScanIngestAuditEvent",
            "AlertEvent",
            "AlertDispatch",
            "KpiSnapshotDaily",
            "ScanDeltaSummaryEntity",
            "AnomalyEvent",
            "AnomalyEventEntity",
            "ContextProfile",
            "ContextProfileVersion",
            "ProfileAssistSession",
            "ProfileAssistantSession",
            "ProfileAssistantMessage",
            "RuleDryRunResult",
            "RuleSuggestion",
            "RuleSuggestionConflict",
            "RuleSuggestionExample",
            "RuleOverride",
            "MitigationPlan",
            "MitigationVerification",
            "FixVerification",
            "FixVerificationResultEntry",
            "EnvironmentDeployment",
            "ScanSection",
            "GeneratedReport",
            "VexWaiverImport");

    private static final DescribedPredicate<JavaClass> ERWARTET_MANDANT_SCOPE =
            new DescribedPredicate<JavaClass>("nicht auf Ausnahmeliste") {
                @Override
                public boolean test(JavaClass c) {
                    return !MANDANTENUEBERGREIFEND.contains(c.getSimpleName());
                }
            };

    @ArchTest
    static final ArchRule fachliche_entities_haben_tenant_id = classes()
            .that().areAnnotatedWith(Entity.class)
            .and(ERWARTET_MANDANT_SCOPE)
            .should(new ArchCondition<JavaClass>(
                    "ein Feld 'tenantId' mit @Column(name = \"tenant_id\") tragen") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean ok = item.getFields().stream()
                            .filter(f -> "tenantId".equals(f.getName()))
                            .anyMatch(TenantScopeTest::hatTenantIdColumn);
                    if (!ok) {
                        String tbl = item.isAnnotatedWith(Table.class)
                                ? item.getAnnotationOfType(Table.class).name()
                                : item.getSimpleName();
                        events.add(SimpleConditionEvent.violated(item,
                                "Entity " + item.getSimpleName()
                                        + " (Table: " + tbl + ") hat kein tenantId-Feld "
                                        + "mit @Column(name=\"tenant_id\"). Falls die Tabelle "
                                        + "bewusst global ist, bitte in "
                                        + "TenantScopeTest.MANDANTENUEBERGREIFEND eintragen."));
                    }
                }
            });

    @ArchTest
    static final ArchRule tenantId_felder_heissen_tenant_id_in_db = fields()
            .that().haveName("tenantId")
            .and().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
            .should(new ArchCondition<JavaField>(
                    "@Column(name = \"tenant_id\") tragen") {
                @Override
                public void check(JavaField field, ConditionEvents events) {
                    if (!hatTenantIdColumn(field)) {
                        events.add(SimpleConditionEvent.violated(field,
                                "Feld " + field.getFullName()
                                        + " sollte @Column(name=\"tenant_id\") tragen."));
                    }
                }
            });

    private static boolean hatTenantIdColumn(JavaField field) {
        if (!field.isAnnotatedWith(Column.class)) {
            return false;
        }
        Column c = field.getAnnotationOfType(Column.class);
        return "tenant_id".equals(c.name());
    }
}
