package com.ahs.cvm.api.nlquery;

import com.ahs.cvm.ai.nlquery.NlQueryResult;
import com.ahs.cvm.ai.nlquery.NlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "NLQuery", description = "Natural-Language-Query aufs Portal")
public class NlQueryController {

    private final NlQueryService service;

    public NlQueryController(NlQueryService service) {
        this.service = service;
    }

    @PostMapping("/query")
    @Operation(summary = "NL-Frage in Filter uebersetzen und ausfuehren.")
    public ResponseEntity<NlQueryResult> query(
            @Valid @RequestBody NlQueryApiRequest body) {
        NlQueryResult r = service.query(body.nlQuestion(), body.triggeredBy());
        // Wenn die LLM-Antwort gegen die Whitelist verstossen hat,
        // liefern wir 422 (semantisch: Input ist syntaktisch ok,
        // aber fachlich nicht verarbeitbar).
        if (!r.rejectedReasons().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(r);
        }
        return ResponseEntity.ok(r);
    }

    public record NlQueryApiRequest(
            @NotBlank String nlQuestion,
            @NotBlank String triggeredBy) {}
}
