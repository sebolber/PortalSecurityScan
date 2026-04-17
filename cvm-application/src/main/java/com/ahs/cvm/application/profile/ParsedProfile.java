package com.ahs.cvm.application.profile;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Ergebnis des {@link ContextProfileYamlParser}: der urspruengliche
 * YAML-Quelltext und der geparste JSON-Baum. Immutable.
 */
public record ParsedProfile(JsonNode tree, String yamlSource) {}
