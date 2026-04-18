package com.ahs.cvm.api.copilot;

import com.ahs.cvm.ai.copilot.CopilotRequest;
import com.ahs.cvm.ai.copilot.CopilotService;
import com.ahs.cvm.ai.copilot.CopilotSuggestion;
import com.ahs.cvm.ai.copilot.CopilotSuggestion.SourceRef;
import com.ahs.cvm.ai.copilot.CopilotUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Copilot-REST-Endpoint (Iteration 14, CVM-33).
 *
 * <p>Liefert eine NDJSON-Streaming-Antwort: zwei Zeilen, jeweils ein
 * JSON-Objekt
 * ({@code {"type":"text",...}} und {@code {"type":"sources",...}}).
 * Ein NDJSON-Vertrag bleibt deterministisch testbar.
 */
@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Copilot", description = "Inline-Copilot pro Assessment")
public class CopilotController {

    private static final String NDJSON = "application/x-ndjson";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping(value = "/{id}/copilot", produces = NDJSON)
    @Operation(summary = "Copilot-Vorschlag fuer ein Assessment (Stream).")
    public ResponseEntity<StreamingResponseBody> copilot(
            @PathVariable("id") UUID assessmentId,
            @Valid @RequestBody CopilotApiRequest request) {
        CopilotSuggestion suggestion = copilotService.suggest(new CopilotRequest(
                assessmentId,
                request.useCase(),
                request.userInstruction(),
                request.triggeredBy(),
                request.attachments() == null ? Map.of() : request.attachments()));

        StreamingResponseBody body = output -> {
            schreibeZeile(output, baueTextZeile(suggestion));
            schreibeZeile(output, baueSourcesZeile(suggestion));
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(NDJSON))
                .body(body);
    }

    private static ObjectNode baueTextZeile(CopilotSuggestion s) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "text");
        n.put("useCase", s.useCase().name());
        n.put("content", s.text());
        return n;
    }

    private static ObjectNode baueSourcesZeile(CopilotSuggestion s) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "sources");
        ArrayNode arr = n.putArray("items");
        for (SourceRef src : s.sources()) {
            ObjectNode item = arr.addObject();
            item.put("kind", src.kind());
            item.put("ref", src.ref());
            item.put("excerpt", src.excerpt());
        }
        return n;
    }

    private static void schreibeZeile(java.io.OutputStream out, ObjectNode node)
            throws IOException {
        out.write(MAPPER.writeValueAsBytes(node));
        out.write('\n');
        out.flush();
    }

    /** Eingabe-Body. */
    public record CopilotApiRequest(
            @NotNull CopilotUseCase useCase,
            @NotBlank String userInstruction,
            @NotBlank String triggeredBy,
            Map<String, String> attachments) {}
}
