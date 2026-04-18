package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibender Zugriff auf Assessments. Kapselt die Immutable-Versionierung
 * (alte Zeile wird {@code SUPERSEDED}, neue Zeile als {@code version+1}
 * angelegt) sowie das Vier-Augen-Prinzip fuer Downgrades auf
 * {@link AhsSeverity#NOT_APPLICABLE} und {@link AhsSeverity#INFORMATIONAL}.
 */
@Service
public class AssessmentWriteService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentWriteService.class);

    private static final Set<AhsSeverity> VIER_AUGEN_DOWNGRADE = EnumSet.of(
            AhsSeverity.NOT_APPLICABLE, AhsSeverity.INFORMATIONAL);

    private final AssessmentRepository assessmentRepository;
    private final FindingRepository findingRepository;
    private final MitigationPlanRepository mitigationRepository;
    private final AssessmentStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;
    private final AssessmentConfig config;

    public AssessmentWriteService(
            AssessmentRepository assessmentRepository,
            FindingRepository findingRepository,
            MitigationPlanRepository mitigationRepository,
            AssessmentStateMachine stateMachine,
            ApplicationEventPublisher eventPublisher,
            AssessmentConfig config) {
        this.assessmentRepository = assessmentRepository;
        this.findingRepository = findingRepository;
        this.mitigationRepository = mitigationRepository;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.config = config;
    }

    /**
     * Cascade-Einstieg. Wird vom {@code FindingsCreatedListener} aufgerufen.
     * Liefert {@code null}, wenn die Cascade {@link ProposalSource#HUMAN}
     * ergibt &mdash; in diesem Fall soll die Queue spaeter manuell bedient
     * werden, ohne dass ein Datensatz entsteht.
     */
    @Transactional
    public Assessment propose(ProposeCommand cmd) {
        if (cmd.source() == ProposalSource.HUMAN) {
            log.debug("Cascade: HUMAN-Outcome, kein Auto-Write fuer Finding {}", cmd.findingId());
            return null;
        }

        Finding finding = findingRepository.findById(cmd.findingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekanntes Finding: " + cmd.findingId()));
        int naechsteVersion = naechsteVersionFuer(cmd.findingId());

        Assessment.AssessmentBuilder builder = Assessment.builder()
                .finding(finding)
                .productVersion(refProductVersion(cmd.productVersionId()))
                .environment(refEnvironment(cmd.environmentId()))
                .cve(refCve(cmd.cveId()))
                .version(naechsteVersion)
                .severity(cmd.severity())
                .proposalSource(cmd.source())
                .rationale(cmd.rationale())
                .rationaleSourceFields(cmd.sourceFields())
                .aiSuggestionId(cmd.aiSuggestionId());

        if (cmd.source() == ProposalSource.REUSE) {
            Instant now = Instant.now();
            builder.status(AssessmentStatus.APPROVED)
                    .decidedBy("system:reuse")
                    .decidedAt(now)
                    .validUntil(standardValidUntil(now));
        } else if (cmd.targetStatus() != null) {
            builder.status(cmd.targetStatus());
        } else {
            builder.status(AssessmentStatus.PROPOSED);
        }

        Assessment neu = assessmentRepository.save(builder.build());
        log.info(
                "Assessment angelegt: finding={}, version={}, source={}, status={}",
                cmd.findingId(), neu.getVersion(), cmd.source(), neu.getStatus());
        return neu;
    }

    /**
     * REST-Variante: gibt das gespeicherte Assessment als View zurueck.
     */
    @Transactional
    public FindingQueueView manualProposeView(ManualProposeCommand cmd) {
        return FindingQueueView.from(manualPropose(cmd));
    }

    /**
     * Manuelle Vorschlagserstellung durch einen Bewerter (REST-POST
     * {@code /assessments}). Liegt bereits ein offenes Assessment vor, wird
     * dieses {@code SUPERSEDED}.
     */
    @Transactional
    public Assessment manualPropose(ManualProposeCommand cmd) {
        requireNichtLeer(cmd.decidedBy(), "decidedBy");
        if (cmd.severity() == null) {
            throw new IllegalArgumentException("severity muss gesetzt sein.");
        }
        Finding finding = findingRepository.findById(cmd.findingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekanntes Finding: " + cmd.findingId()));
        Optional<Assessment> aktuell =
                assessmentRepository.findFirstByFindingIdOrderByVersionDesc(cmd.findingId());
        int naechsteVersion =
                aktuell.map(a -> a.getVersion() + 1).orElse(1);

        aktuell.ifPresent(vorherig -> {
            stateMachine.pruefeUebergang(vorherig.getStatus(), AssessmentStatus.SUPERSEDED);
            vorherig.markiereAlsUeberholt(Instant.now());
            assessmentRepository.save(vorherig);
        });

        Assessment neu = Assessment.builder()
                .finding(finding)
                .productVersion(refProductVersion(extractProductVersionId(aktuell, cmd)))
                .environment(refEnvironment(extractEnvironmentId(aktuell, cmd)))
                .cve(refCve(extractCveId(aktuell, cmd, finding)))
                .version(naechsteVersion)
                .status(AssessmentStatus.PROPOSED)
                .severity(cmd.severity())
                .proposalSource(ProposalSource.HUMAN)
                .rationale(cmd.rationale())
                .rationaleSourceFields(cmd.sourceFields())
                .decidedBy(cmd.decidedBy())
                .decidedAt(Instant.now())
                .build();
        return assessmentRepository.save(neu);
    }

    /**
     * Freigabe eines PROPOSED-Assessments. Erzeugt eine neue Version mit
     * {@code APPROVED}; die Vorgaengerversion wird {@code SUPERSEDED}.
     * Publiziert {@link AssessmentApprovedEvent}.
     */
    @Transactional
    public Assessment approve(UUID assessmentId, String approverId) {
        return approveMitMitigation(assessmentId, approverId, null);
    }

    @Transactional
    public FindingQueueView approveView(
            UUID assessmentId, String approverId, MitigationInput mitigation) {
        return FindingQueueView.from(approveMitMitigation(assessmentId, approverId, mitigation));
    }

    @Transactional
    public FindingQueueView rejectView(UUID assessmentId, String approverId, String kommentar) {
        return FindingQueueView.from(reject(assessmentId, approverId, kommentar));
    }

    @Transactional
    public Assessment approveMitMitigation(
            UUID assessmentId, String approverId, MitigationInput mitigation) {
        requireNichtLeer(approverId, "approverId");
        Assessment bestehend = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));

        if (bestehend.getStatus() != AssessmentStatus.PROPOSED
                && bestehend.getStatus() != AssessmentStatus.NEEDS_REVIEW
                && bestehend.getStatus() != AssessmentStatus.NEEDS_VERIFICATION) {
            throw new InvalidAssessmentTransitionException(
                    bestehend.getStatus(), AssessmentStatus.APPROVED);
        }

        boolean viererAugenPflicht = VIER_AUGEN_DOWNGRADE.contains(bestehend.getSeverity());
        if (viererAugenPflicht && Objects.equals(approverId, bestehend.getDecidedBy())) {
            throw new AssessmentFourEyesViolationException(
                    "Vier-Augen-Prinzip verletzt: Downgrade auf "
                            + bestehend.getSeverity() + " durch Autor '"
                            + approverId + "' nicht erlaubt.");
        }

        stateMachine.pruefeUebergang(bestehend.getStatus(), AssessmentStatus.SUPERSEDED);
        Instant now = Instant.now();
        bestehend.markiereAlsUeberholt(now);
        assessmentRepository.save(bestehend);

        Assessment genehmigt = Assessment.builder()
                .finding(bestehend.getFinding())
                .productVersion(bestehend.getProductVersion())
                .environment(bestehend.getEnvironment())
                .cve(bestehend.getCve())
                .version(bestehend.getVersion() + 1)
                .status(AssessmentStatus.APPROVED)
                .severity(bestehend.getSeverity())
                .proposalSource(bestehend.getProposalSource())
                .rationale(bestehend.getRationale())
                .rationaleSourceFields(bestehend.getRationaleSourceFields())
                .decidedBy(approverId)
                .decidedAt(now)
                .reviewedBy(viererAugenPflicht ? approverId : null)
                .validUntil(standardValidUntil(now))
                .aiSuggestionId(bestehend.getAiSuggestionId())
                .build();
        Assessment persistiert = assessmentRepository.save(genehmigt);

        if (mitigation != null && mitigation.strategy() != null) {
            mitigationRepository.save(MitigationPlan.builder()
                    .assessment(persistiert)
                    .strategy(mitigation.strategy())
                    .status(MitigationStatus.PLANNED)
                    .targetVersion(mitigation.targetVersion())
                    .plannedFor(mitigation.plannedFor())
                    .notes(mitigation.notes())
                    .owner(approverId)
                    .build());
        }

        eventPublisher.publishEvent(new AssessmentApprovedEvent(
                persistiert.getId(),
                persistiert.getFinding().getId(),
                persistiert.getCve().getId(),
                persistiert.getEnvironment().getId(),
                persistiert.getProductVersion().getId(),
                persistiert.getSeverity(),
                approverId,
                now));
        log.info(
                "Assessment freigegeben: finding={}, version={}, approver={}",
                persistiert.getFinding().getId(), persistiert.getVersion(), approverId);
        return persistiert;
    }

    @Transactional
    public Assessment reject(UUID assessmentId, String approverId, String kommentar) {
        requireNichtLeer(approverId, "approverId");
        requireNichtLeer(kommentar, "kommentar");
        Assessment bestehend = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        if (bestehend.getStatus() != AssessmentStatus.PROPOSED
                && bestehend.getStatus() != AssessmentStatus.NEEDS_REVIEW
                && bestehend.getStatus() != AssessmentStatus.NEEDS_VERIFICATION) {
            throw new InvalidAssessmentTransitionException(
                    bestehend.getStatus(), AssessmentStatus.REJECTED);
        }

        stateMachine.pruefeUebergang(bestehend.getStatus(), AssessmentStatus.SUPERSEDED);
        Instant now = Instant.now();
        bestehend.markiereAlsUeberholt(now);
        assessmentRepository.save(bestehend);

        Assessment abgelehnt = Assessment.builder()
                .finding(bestehend.getFinding())
                .productVersion(bestehend.getProductVersion())
                .environment(bestehend.getEnvironment())
                .cve(bestehend.getCve())
                .version(bestehend.getVersion() + 1)
                .status(AssessmentStatus.REJECTED)
                .severity(bestehend.getSeverity())
                .proposalSource(bestehend.getProposalSource())
                .rationale(kommentar)
                .rationaleSourceFields(bestehend.getRationaleSourceFields())
                .decidedBy(approverId)
                .decidedAt(now)
                .build();
        Assessment persistiert = assessmentRepository.save(abgelehnt);
        log.info(
                "Assessment abgelehnt: finding={}, version={}, approver={}",
                persistiert.getFinding().getId(), persistiert.getVersion(), approverId);
        return persistiert;
    }

    /**
     * Setzt {@code APPROVED}-Assessments mit abgelaufenem {@code validUntil}
     * auf {@code EXPIRED}. Umgeht den {@code AssessmentImmutabilityListener}
     * bewusst via {@code @Modifying}-Query, weil die Transition keine
     * inhaltliche Aenderung ist.
     */
    @Transactional
    public int expireIfDue(Instant grenze) {
        List<Assessment> kandidaten = assessmentRepository
                .findByStatusAndValidUntilLessThanEqualAndSupersededAtIsNull(
                        AssessmentStatus.APPROVED, grenze);
        if (kandidaten.isEmpty()) {
            return 0;
        }
        List<UUID> ids = kandidaten.stream().map(Assessment::getId).toList();
        Instant jetzt = Instant.now();
        int betroffen = assessmentRepository.markiereAlsAbgelaufen(ids, jetzt);
        log.info("Expiry: {} Assessments auf EXPIRED gesetzt.", betroffen);
        eventPublisher.publishEvent(new AssessmentExpiredEvent(ids, jetzt));
        return betroffen;
    }

    private int naechsteVersionFuer(UUID findingId) {
        Optional<Assessment> aktuell =
                assessmentRepository.findFirstByFindingIdOrderByVersionDesc(findingId);
        return aktuell.map(a -> a.getVersion() + 1).orElse(1);
    }

    private Instant standardValidUntil(Instant basis) {
        return ZonedDateTime.ofInstant(basis, ZoneOffset.UTC)
                .plusMonths(config.defaultValidMonths())
                .toInstant();
    }

    private static void requireNichtLeer(String wert, String name) {
        if (wert == null || wert.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }

    private static ProductVersion refProductVersion(UUID id) {
        return ProductVersion.builder().id(id).build();
    }

    private static Environment refEnvironment(UUID id) {
        return Environment.builder().id(id).build();
    }

    private static Cve refCve(UUID id) {
        return Cve.builder().id(id).build();
    }

    private static UUID extractProductVersionId(
            Optional<Assessment> aktuell, ManualProposeCommand cmd) {
        if (cmd.productVersionId() != null) {
            return cmd.productVersionId();
        }
        return aktuell.map(a -> a.getProductVersion().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "productVersionId muss angegeben werden fuer neues Finding."));
    }

    private static UUID extractEnvironmentId(
            Optional<Assessment> aktuell, ManualProposeCommand cmd) {
        if (cmd.environmentId() != null) {
            return cmd.environmentId();
        }
        return aktuell.map(a -> a.getEnvironment().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "environmentId muss angegeben werden fuer neues Finding."));
    }

    private static UUID extractCveId(
            Optional<Assessment> aktuell, ManualProposeCommand cmd, Finding finding) {
        if (aktuell.isPresent()) {
            return aktuell.get().getCve().getId();
        }
        return finding.getCve().getId();
    }

    /** Auto-Propose-Command aus der Cascade. */
    public record ProposeCommand(
            UUID findingId,
            UUID cveId,
            UUID productVersionId,
            UUID environmentId,
            ProposalSource source,
            AhsSeverity severity,
            String rationale,
            List<String> sourceFields,
            UUID ruleId,
            UUID reusedAssessmentId,
            UUID aiSuggestionId,
            AssessmentStatus targetStatus) {

        /**
         * Backwards-kompatibler Konstruktor fuer Iterationen 06-12,
         * die den AI-Pfad noch nicht kannten. Setzt {@code aiSuggestionId}
         * und {@code targetStatus} auf {@code null}.
         */
        public ProposeCommand(
                UUID findingId,
                UUID cveId,
                UUID productVersionId,
                UUID environmentId,
                ProposalSource source,
                AhsSeverity severity,
                String rationale,
                List<String> sourceFields,
                UUID ruleId,
                UUID reusedAssessmentId) {
            this(findingId, cveId, productVersionId, environmentId, source,
                    severity, rationale, sourceFields, ruleId,
                    reusedAssessmentId, null, null);
        }
    }

    /** Manueller Vorschlag ueber REST. */
    public record ManualProposeCommand(
            UUID findingId,
            AhsSeverity severity,
            String rationale,
            List<String> sourceFields,
            String decidedBy,
            UUID productVersionId,
            UUID environmentId) {}

    /** Optionaler Mitigation-Plan, der mit dem Approve persistiert wird. */
    public record MitigationInput(
            MitigationStrategy strategy,
            String targetVersion,
            Instant plannedFor,
            String notes) {}
}
