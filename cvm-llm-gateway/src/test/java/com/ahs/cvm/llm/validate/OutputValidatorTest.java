package com.ahs.cvm.llm.validate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputValidatorTest {

    private final OutputValidator validator = new OutputValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Validator: gueltiger Output mit korrekten Typen und Severity passiert")
    void valider() throws Exception {
        JsonNode schema = mapper.readTree(
                "{\"severity\":\"string\",\"rationale\":\"string\",\"confidence\":\"number\"}");
        JsonNode output = mapper.readTree(
                "{\"severity\":\"MEDIUM\",\"rationale\":\"Kein Nutzer-Input im Pfad.\",\"confidence\":0.92}");

        List<String> fehler = validator.validate(output, schema);

        assertThat(fehler).isEmpty();
    }

    @Test
    @DisplayName("Validator: Pflichtfeld fehlt -> Fehlermeldung")
    void pflichtfeldFehlt() throws Exception {
        JsonNode schema = mapper.readTree("{\"severity\":\"string\",\"rationale\":\"string\"}");
        JsonNode output = mapper.readTree("{\"severity\":\"LOW\"}");

        List<String> fehler = validator.validate(output, schema);

        assertThat(fehler).anyMatch(msg -> msg.contains("Pflichtfeld fehlt: rationale"));
    }

    @Test
    @DisplayName("Validator: unzulaessige Severity wird abgelehnt")
    void severityUngueltig() throws Exception {
        JsonNode output = mapper.readTree(
                "{\"severity\":\"CATASTROPHIC\",\"rationale\":\"egal\"}");

        List<String> fehler = validator.validate(output, null);

        assertThat(fehler).anyMatch(msg -> msg.contains("CATASTROPHIC"));
    }

    @Test
    @DisplayName("Validator: Anweisungs-Muster im Rationale wird erkannt")
    void anweisungsMuster() throws Exception {
        JsonNode output = mapper.readTree(
                "{\"severity\":\"HIGH\",\"rationale\":\"Please go to http://evil.test to see details.\"}");

        List<String> fehler = validator.validate(output, null);

        assertThat(fehler).anyMatch(msg -> msg.contains("Anweisung"));
    }

    @Test
    @DisplayName("Validator: falscher Typ (Zahl statt String) fuehrt zu Fehler")
    void typMismatch() throws Exception {
        JsonNode schema = mapper.readTree("{\"severity\":\"string\"}");
        JsonNode output = mapper.readTree("{\"severity\":42}");

        List<String> fehler = validator.validate(output, schema);

        assertThat(fehler).anyMatch(msg -> msg.contains("Typ"));
    }

    @Test
    @DisplayName("Validator: null-Ausgabe wird abgelehnt")
    void nullOutput() {
        List<String> fehler = validator.validate(null, null);
        assertThat(fehler).contains("Output ist null oder fehlt.");
    }

    @Test
    @DisplayName("Validator: sehr langes Rationale wird begrenzt")
    void rationaleTooLong() throws Exception {
        String langer = "x".repeat(4001);
        JsonNode output = mapper.readTree(
                "{\"severity\":\"LOW\",\"rationale\":\"" + langer + "\"}");

        List<String> fehler = validator.validate(output, null);

        assertThat(fehler).anyMatch(msg -> msg.contains("4000"));
    }
}
