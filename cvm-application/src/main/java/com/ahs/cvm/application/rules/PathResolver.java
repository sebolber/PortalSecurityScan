package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Objects;

/**
 * Aufloesung von Pfaden wie {@code cve.kev}, {@code profile.architecture.windows_hosts}
 * oder {@code component.name} auf konkrete Werte im {@link RuleEvaluationContext}.
 *
 * <p>Rueckgabewert ist ein {@link JsonNode}, damit die nachfolgenden Operatoren
 * einheitlich arbeiten koennen. Fehlende Pfade liefern {@link MissingNode}.
 */
public final class PathResolver {

    private PathResolver() {}

    public static JsonNode resolve(String path, RuleEvaluationContext ctx) {
        if (path.startsWith("cve.")) {
            return cveFeld(path.substring("cve.".length()), ctx.cve());
        }
        if (path.startsWith("component.")) {
            return componentFeld(path.substring("component.".length()), ctx.component());
        }
        if (path.startsWith("profile.")) {
            return profilFeld(path.substring("profile.".length()), ctx.profile());
        }
        if (path.startsWith("finding.")) {
            return findingFeld(path.substring("finding.".length()), ctx.finding());
        }
        throw new RuleConditionException(
                "Unbekannter Pfad-Praefix in '" + path + "'.");
    }

    private static JsonNode cveFeld(String feld, RuleEvaluationContext.CveSnapshot cve) {
        com.fasterxml.jackson.databind.node.JsonNodeFactory f =
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
        if (cve == null) return MissingNode.getInstance();
        return switch (feld) {
            case "id" -> cve.cveId() == null ? NullNode.instance : f.textNode(cve.cveId());
            case "description" ->
                    cve.description() == null ? NullNode.instance : f.textNode(cve.description());
            case "cwes" -> {
                var arr = f.arrayNode();
                if (cve.cwes() != null) cve.cwes().forEach(arr::add);
                yield arr;
            }
            case "kev" -> f.booleanNode(cve.kev());
            case "epss" -> cve.epss() == null ? NullNode.instance : f.numberNode(cve.epss());
            case "cvssScore" ->
                    cve.cvssScore() == null ? NullNode.instance : f.numberNode(cve.cvssScore());
            default -> throw new RuleConditionException(
                    "Unbekanntes CVE-Feld: 'cve." + feld + "'.");
        };
    }

    private static JsonNode componentFeld(
            String feld, RuleEvaluationContext.ComponentSnapshot component) {
        com.fasterxml.jackson.databind.node.JsonNodeFactory f =
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
        if (component == null) return MissingNode.getInstance();
        return switch (feld) {
            case "pkgType" ->
                    component.pkgType() == null ? NullNode.instance : f.textNode(component.pkgType());
            case "name" ->
                    component.name() == null ? NullNode.instance : f.textNode(component.name());
            case "version" ->
                    component.version() == null ? NullNode.instance : f.textNode(component.version());
            default -> throw new RuleConditionException(
                    "Unbekanntes Component-Feld: 'component." + feld + "'.");
        };
    }

    private static JsonNode profilFeld(String restPfad, JsonNode profil) {
        if (profil == null) return MissingNode.getInstance();
        JsonNode aktuell = profil;
        for (String segment : restPfad.split("\\.")) {
            if (aktuell == null || aktuell.isMissingNode() || aktuell.isNull()) {
                return MissingNode.getInstance();
            }
            aktuell = aktuell.path(segment);
        }
        return Objects.requireNonNullElse(aktuell, MissingNode.getInstance());
    }

    private static JsonNode findingFeld(String feld, RuleEvaluationContext.FindingSnapshot f) {
        com.fasterxml.jackson.databind.node.JsonNodeFactory fac =
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
        if (f == null) return MissingNode.getInstance();
        return switch (feld) {
            case "id" -> f.id() == null ? NullNode.instance : fac.textNode(f.id().toString());
            case "detectedAt" ->
                    f.detectedAt() == null ? NullNode.instance : fac.textNode(f.detectedAt().toString());
            default -> throw new RuleConditionException(
                    "Unbekanntes Finding-Feld: 'finding." + feld + "'.");
        };
    }
}
