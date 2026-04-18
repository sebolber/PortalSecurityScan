package com.ahs.cvm.application.assessment;

import com.ahs.cvm.application.cascade.CascadeInput;
import com.ahs.cvm.application.cascade.CascadeOutcome;
import com.ahs.cvm.application.cascade.CascadeService;
import com.ahs.cvm.application.cve.ComponentMatchedFindingsEvent;
import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.application.rules.RuleEvaluationContext.ComponentSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.CveSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.FindingSnapshot;
import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Haengt die Cascade aus Iteration 05 an die Scan-Ingest-Pipeline aus
 * Iteration 02. Pro Finding wird {@link CascadeService#bewerte(CascadeInput)}
 * aufgerufen und das Ergebnis ueber {@link AssessmentWriteService#propose}
 * persistiert. Doppelte PROPOSED-Eintraege je Finding werden vermieden,
 * indem die naechste Version nur dann angelegt wird, wenn noch kein
 * offenes Assessment existiert.
 */
@Component
public class FindingsCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(FindingsCreatedListener.class);

    private final FindingRepository findingRepository;
    private final ContextProfileService profileService;
    private final CascadeService cascadeService;
    private final AssessmentWriteService writeService;
    private final YAMLMapper yamlMapper;

    public FindingsCreatedListener(
            FindingRepository findingRepository,
            ContextProfileService profileService,
            CascadeService cascadeService,
            AssessmentWriteService writeService) {
        this.findingRepository = findingRepository;
        this.profileService = profileService;
        this.cascadeService = cascadeService;
        this.writeService = writeService;
        this.yamlMapper = new YAMLMapper();
    }

    /**
     * Propagation.REQUIRES_NEW ist notwendig, damit
     * {@code writeService.propose(...)}-Aufrufe in einer echten
     * Transaktion laufen und committed werden. Ohne @Transactional
     * liefert Spring 6 im AFTER_COMMIT-Listener eine Pseudo-Transaktion,
     * bei der die Saves zwar ausgefuehrt werden, aber beim Listener-
     * Ende weder committed noch rollbacked werden. Symptom vorher:
     * Logs "Assessment angelegt" fuer alle Findings, aber 0 Zeilen
     * in der assessment-Tabelle.
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScanIngested(ScanIngestedEvent event) {
        runCascade(event.scanId(), event.productVersionId(),
                event.environmentId(), "ScanIngested");
    }

    /**
     * Iteration 33 (CVM-77): OSV-Listener legt Findings erst nach dem
     * Ingest an und feuert dieses Event. Ohne den zweiten Listener hier
     * bliebe die Queue bei reinen Komponenten-SBOMs leer, weil
     * {@link #onScanIngested} den Scan ohne Findings sieht und sofort
     * zurueckkehrt. Propagation wie oben: REQUIRES_NEW erzwingt einen
     * echten Commit pro Cascade-Run.
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onComponentMatchedFindings(ComponentMatchedFindingsEvent event) {
        runCascade(event.scanId(), event.productVersionId(),
                event.environmentId(), "ComponentMatchedFindings");
    }

    private void runCascade(
            UUID scanId, UUID productVersionId, UUID environmentId, String quelle) {
        List<Finding> findings = findingRepository.findByScanId(scanId);
        if (findings.isEmpty()) {
            return;
        }
        if (environmentId == null) {
            // Assessment.environment ist NOT NULL; ohne Umgebungs-Bezug
            // koennen wir fachlich keine Relevanz bewerten. Ueberspringen
            // statt Hibernate-Crash ("There are delayed insert actions
            // before operation as cascade level 0").
            log.warn(
                    "Cascade-Run ({}) fuer Scan {} uebersprungen: keine "
                            + "Environment gesetzt. Upload mit --environment-id "
                            + "wiederholen, dann werden die {} Findings bewertet.",
                    quelle, scanId, findings.size());
            return;
        }
        JsonNode profileTree = ladeProfilbaum(environmentId);
        log.info(
                "Cascade-Run ({}) fuer Scan {} ({} Findings)",
                quelle, scanId, findings.size());
        ScanIngestedEvent event = new ScanIngestedEvent(
                scanId,
                productVersionId,
                environmentId,
                0,
                findings.size(),
                java.time.Instant.now());
        for (Finding f : findings) {
            try {
                CascadeInput input = baueInput(f, event, profileTree);
                CascadeOutcome outcome = cascadeService.bewerte(input);
                writeService.propose(new AssessmentWriteService.ProposeCommand(
                        f.getId(),
                        f.getCve().getId(),
                        event.productVersionId(),
                        event.environmentId(),
                        outcome.source(),
                        outcome.severity(),
                        outcome.rationale(),
                        outcome.sourceFields(),
                        outcome.ruleId(),
                        outcome.reusedAssessmentId(),
                        outcome.aiSuggestionId(),
                        outcome.targetStatus()));
            } catch (RuntimeException ex) {
                log.warn("Cascade fuer Finding {} fehlgeschlagen: {}",
                        f.getId(), ex.getMessage());
            }
        }
    }

    private CascadeInput baueInput(Finding f, ScanIngestedEvent event, JsonNode profileTree) {
        Cve cve = f.getCve();
        ComponentOccurrence occ = f.getComponentOccurrence();
        CveSnapshot cveSnap = new CveSnapshot(
                cve.getId(),
                cve.getCveId(),
                cve.getSummary(),
                cve.getCwes() == null ? List.of() : List.copyOf(cve.getCwes()),
                cve.getKevListed() != null && cve.getKevListed(),
                Optional.ofNullable(cve.getEpssScore()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(cve.getCvssBaseScore()).orElse(BigDecimal.ZERO));
        ComponentSnapshot componentSnap = new ComponentSnapshot(
                occ.getComponent().getType(),
                occ.getComponent().getName(),
                occ.getComponent().getVersion());
        FindingSnapshot findingSnap = new FindingSnapshot(f.getId(), f.getDetectedAt());
        RuleEvaluationContext ctx = new RuleEvaluationContext(
                cveSnap, profileTree, componentSnap, findingSnap);
        return new CascadeInput(cve.getId(), event.productVersionId(),
                event.environmentId(), ctx);
    }

    private JsonNode ladeProfilbaum(UUID environmentId) {
        if (environmentId == null) {
            return JsonNodeFactory.instance.objectNode();
        }
        Optional<ProfileView> aktiv = profileService.latestActiveFor(environmentId);
        if (aktiv.isEmpty()) {
            log.debug(
                    "Kein aktives Profil fuer Umgebung {}; Cascade nutzt leeres Profil.",
                    environmentId);
            return JsonNodeFactory.instance.objectNode();
        }
        try {
            return yamlMapper.readTree(aktiv.get().yamlSource());
        } catch (JsonProcessingException e) {
            log.warn("Profil {} nicht parsebar, leeres Profil fuer Cascade verwendet.",
                    aktiv.get().id());
            return JsonNodeFactory.instance.objectNode();
        }
    }
}
