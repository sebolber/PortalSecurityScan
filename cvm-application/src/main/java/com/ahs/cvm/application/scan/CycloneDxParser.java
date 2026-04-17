package com.ahs.cvm.application.scan;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Deserialisiert eine CycloneDX-1.6-SBOM (JSON) in das interne
 * {@link CycloneDxBom}-Modell und prueft, dass die CVM-Pflichtfelder
 * vorhanden sind.
 */
@Component
public class CycloneDxParser {

    private static final String ERWARTETER_BOM_FORMAT = "CycloneDX";

    private final ObjectMapper objectMapper;

    public CycloneDxParser() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CycloneDxBom parse(byte[] rawSbom) {
        CycloneDxBom bom;
        try {
            bom = objectMapper.readValue(rawSbom, CycloneDxBom.class);
        } catch (IOException e) {
            throw new SbomParseException(
                    "SBOM konnte nicht als CycloneDX-1.6-JSON gelesen werden: "
                            + e.getMessage(),
                    e);
        }
        validiereSchema(bom);
        return bom;
    }

    public CycloneDxBom parse(String rawSbom) {
        return parse(rawSbom.getBytes(StandardCharsets.UTF_8));
    }

    private void validiereSchema(CycloneDxBom bom) {
        if (bom == null) {
            throw new SbomSchemaException("SBOM ist leer.");
        }
        if (!ERWARTETER_BOM_FORMAT.equalsIgnoreCase(bom.bomFormat())) {
            throw new SbomSchemaException(
                    "Unerwartetes bomFormat: %s (erwartet: %s)"
                            .formatted(bom.bomFormat(), ERWARTETER_BOM_FORMAT));
        }
        if (bom.specVersion() == null
                || !bom.specVersion().startsWith("1.")) {
            throw new SbomSchemaException(
                    "specVersion fehlt oder ist < 1.x: " + bom.specVersion());
        }
        if (bom.components() == null || bom.components().isEmpty()) {
            throw new SbomSchemaException(
                    "SBOM enthaelt keine Komponenten.");
        }
    }
}
