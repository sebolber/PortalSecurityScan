package com.ahs.cvm.application.scan;

import java.util.List;

/**
 * Leichtgewichtiges CycloneDX-1.6-Modell, reduziert auf die Felder, die der
 * CVE-Relevance-Manager fuer Ingestion braucht. Zusaetzliche Felder im BOM
 * werden beim Deserialisieren ignoriert.
 */
public record CycloneDxBom(
        String bomFormat,
        String specVersion,
        String serialNumber,
        Integer version,
        List<Component> components,
        List<Vulnerability> vulnerabilities) {

    public record Component(
            String bomRef,
            String type,
            String name,
            String version,
            String purl) {}

    public record Vulnerability(
            String bomRef,
            String id,
            VulnerabilitySource source,
            List<Rating> ratings,
            String description,
            List<Affect> affects) {}

    public record VulnerabilitySource(String name, String url) {}

    public record Rating(String method, String vector, Double score, String severity) {}

    public record Affect(String ref) {}
}
