package com.ahs.cvm.application.profile;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Liest einen YAML-Profiltext, validiert ihn gegen das JSON-Schema
 * {@code /profile/profile-schema-v1.json} und liefert eine {@link ParsedProfile}.
 *
 * <p>Syntaxfehler und Schemaverletzungen werden als
 * {@link ProfileValidationException} mit einer deutschsprachigen,
 * feldgenauen Meldung signalisiert. SnakeYAML wird ueber den
 * {@link YAMLMapper} genutzt, der safe-loading standardmaessig aktiv hat.
 */
@Component
public class ContextProfileYamlParser {

    private static final String SCHEMA_RESOURCE = "/profile/profile-schema-v1.json";

    private final YAMLMapper yamlMapper;
    private final JsonSchema schema;

    public ContextProfileYamlParser() {
        this.yamlMapper = new YAMLMapper();
        JsonSchemaFactory factory =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        try (InputStream in = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Profil-Schema fehlt: " + SCHEMA_RESOURCE);
            }
            this.schema = factory.getSchema(in);
        } catch (IOException e) {
            throw new IllegalStateException("Profil-Schema konnte nicht geladen werden", e);
        }
    }

    public ParsedProfile parse(String yamlSource) {
        if (yamlSource == null || yamlSource.isBlank()) {
            throw new ProfileValidationException("Profil ist leer.");
        }
        JsonNode tree;
        try {
            tree = yamlMapper.readTree(yamlSource);
        } catch (JacksonException e) {
            throw new ProfileValidationException(
                    "YAML konnte nicht geparst werden: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new ProfileValidationException("YAML-Lesefehler: " + e.getMessage(), e);
        }
        if (tree == null || tree.isMissingNode() || tree.isNull()) {
            throw new ProfileValidationException("Profil ist leer.");
        }

        Set<ValidationMessage> fehler = schema.validate(tree);
        if (!fehler.isEmpty()) {
            String uebersetzt =
                    fehler.stream()
                            .map(this::uebersetze)
                            .sorted()
                            .distinct()
                            .collect(Collectors.joining("; "));
            throw new ProfileValidationException("Profil verletzt Schema: " + uebersetzt);
        }
        return new ParsedProfile(tree, yamlSource);
    }

    private String uebersetze(ValidationMessage msg) {
        String pfad = pfadVon(msg);
        String typ = msg.getType();
        return switch (typ) {
            case "required" -> "Pflichtfeld fehlt: " + pfad + "."
                    + extractRequiredProperty(msg);
            case "type" -> "Feld '" + pfad + "' hat falschen Typ: "
                    + msg.getMessage().replace("$", pfad);
            case "enum" -> "Feld '" + pfad + "' hat unzulaessigen Wert. Erlaubt: "
                    + extractAllowed(msg);
            case "additionalProperties" -> "Unbekanntes Feld unter '" + pfad + "': "
                    + extractProperty(msg);
            case "minLength" -> "Feld '" + pfad + "' darf nicht leer sein.";
            case "const" -> "Feld '" + pfad + "' muss exakt den vorgegebenen Wert haben.";
            case "uniqueItems" -> "Feld '" + pfad + "' enthaelt Duplikate.";
            default -> "Feld '" + pfad + "': " + msg.getMessage();
        };
    }

    private String pfadVon(ValidationMessage msg) {
        String instanz = String.valueOf(msg.getInstanceLocation());
        if (instanz.isEmpty() || "$".equals(instanz)) {
            return "(root)";
        }
        if (instanz.startsWith("$.")) {
            instanz = instanz.substring(2);
        } else if (instanz.startsWith("$")) {
            instanz = instanz.substring(1);
        } else if (instanz.startsWith("/")) {
            instanz = instanz.substring(1).replace('/', '.');
        }
        return instanz;
    }

    private String extractRequiredProperty(ValidationMessage msg) {
        Object[] args = msg.getArguments();
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return msg.getMessage();
    }

    private String extractProperty(ValidationMessage msg) {
        Object[] args = msg.getArguments();
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return msg.getMessage();
    }

    private String extractAllowed(ValidationMessage msg) {
        Object[] args = msg.getArguments();
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return msg.getMessage();
    }
}
