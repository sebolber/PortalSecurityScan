package com.ahs.cvm.api.alert;

import com.ahs.cvm.application.alert.AlertBannerService;
import com.ahs.cvm.application.alert.AlertBannerService.BannerStatus;
import com.ahs.cvm.application.alert.AlertConfig;
import com.ahs.cvm.application.alert.AlertContext;
import com.ahs.cvm.application.alert.AlertEvaluator;
import com.ahs.cvm.application.alert.AlertEvaluator.AlertOutcome;
import com.ahs.cvm.application.alert.AlertHistoryService;
import com.ahs.cvm.application.alert.AlertHistoryView;
import com.ahs.cvm.application.alert.AlertRuleService;
import com.ahs.cvm.application.alert.AlertRuleService.CreateRuleCommand;
import com.ahs.cvm.application.alert.AlertRuleView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpunkte fuer das Alert-Subsystem.
 *
 * <ul>
 *   <li>{@code GET /api/v1/alerts/rules} &mdash; alle Regeln.</li>
 *   <li>{@code POST /api/v1/alerts/rules} &mdash; neue Regel anlegen
 *       (CVM_ADMIN).</li>
 *   <li>{@code POST /api/v1/alerts/test} &mdash; Dry-Run-Trigger fuer
 *       Test-Zwecke. Erzwingt {@code dryRun=true} im Audit, unabhaengig
 *       vom konfigurierten Modus.</li>
 *   <li>{@code GET /api/v1/alerts/banner} &mdash; Frontend-Banner-
 *       Status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Konfiguration und Audit der SMTP-Alerts")
public class AlertsController {

    private final AlertRuleService ruleService;
    private final AlertEvaluator evaluator;
    private final AlertBannerService bannerService;
    private final AlertHistoryService historyService;
    private final AlertConfig config;
    private final Clock clock;

    public AlertsController(
            AlertRuleService ruleService,
            AlertEvaluator evaluator,
            AlertBannerService bannerService,
            AlertHistoryService historyService,
            AlertConfig config,
            Clock clock) {
        this.ruleService = ruleService;
        this.evaluator = evaluator;
        this.bannerService = bannerService;
        this.historyService = historyService;
        this.config = config;
        this.clock = clock;
    }

    @GetMapping("/rules")
    @Operation(summary = "Alle Alert-Regeln")
    public ResponseEntity<List<AlertRuleView>> liste() {
        return ResponseEntity.ok(ruleService.findeAlle());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('CVM_ADMIN')")
    @Operation(summary = "Neue Alert-Regel (CVM_ADMIN)")
    public ResponseEntity<AlertRuleView> anlegen(@Valid @RequestBody CreateRuleRequest request) {
        AlertRuleView view = ruleService.anlegen(new CreateRuleCommand(
                request.name(),
                request.description(),
                request.triggerArt(),
                request.severity(),
                request.cooldownMinutes(),
                request.subjectPrefix(),
                request.templateName(),
                request.recipients(),
                request.conditionJson(),
                request.enabled()));
        return ResponseEntity.created(URI.create("/api/v1/alerts/rules/" + view.id()))
                .body(view);
    }

    @PostMapping("/test")
    @Operation(summary = "Dry-Run: Alert-Evaluation ohne Mailversand")
    public ResponseEntity<TestResponse> test(@Valid @RequestBody TestRequest request) {
        AlertContext ctx = new AlertContext(
                request.triggerArt(),
                request.triggerKey(),
                request.severity(),
                null,
                null,
                null,
                null,
                request.summary(),
                Instant.now(clock),
                request.attributes() == null ? Map.of() : request.attributes());
        AlertOutcome outcome = evaluator.evaluate(ctx);
        return ResponseEntity.ok(new TestResponse(
                outcome.gefeuert(),
                outcome.unterdrueckt(),
                config.dryRunEffective()));
    }

    @GetMapping("/banner")
    @Operation(summary = "Banner-Status (T2-Eskalation offen?)")
    public ResponseEntity<BannerStatus> banner() {
        return ResponseEntity.ok(bannerService.aktuellerStatus());
    }

    @GetMapping("/history")
    @Operation(summary = "Letzte Alert-Dispatches absteigend nach Zeit")
    public ResponseEntity<List<AlertHistoryView>> history(
            @org.springframework.web.bind.annotation.RequestParam(
                    name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(historyService.recent(limit));
    }

    /** Eingabe fuer {@code POST /rules}. */
    public record CreateRuleRequest(
            @NotBlank String name,
            String description,
            @NotNull AlertTriggerArt triggerArt,
            AlertSeverity severity,
            Integer cooldownMinutes,
            String subjectPrefix,
            @NotBlank String templateName,
            @NotEmpty List<@NotBlank String> recipients,
            String conditionJson,
            Boolean enabled) {}

    /** Eingabe fuer {@code POST /test}. */
    public record TestRequest(
            @NotNull AlertTriggerArt triggerArt,
            @NotBlank String triggerKey,
            AhsSeverity severity,
            String summary,
            Map<String, Object> attributes) {}

    /** Antwort fuer {@code POST /test}. */
    public record TestResponse(int gefeuert, int unterdrueckt, boolean dryRunGlobal) {}
}
