package com.ahs.cvm.application.rules;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD fuer Regeln inkl. Vier-Augen-Aktivierung. Der Parser validiert die
 * Condition bei Draft-Erzeugung, damit defekte Regeln nicht persistiert werden.
 */
@Service
public class RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;
    private final ConditionParser parser;

    public RuleService(RuleRepository ruleRepository, ConditionParser parser) {
        this.ruleRepository = ruleRepository;
        this.parser = parser;
    }

    @Transactional(readOnly = true)
    public List<RuleView> listAll() {
        // Iteration 50 (CVM-100): Soft-Delete herausfiltern.
        return ruleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(RuleView::from)
                .toList();
    }

    /**
     * Soft-Delete (Iteration 50, CVM-100). Unterscheidet sich bewusst vom
     * Status {@code RETIRED}: RETIRED kennzeichnet eine fachlich abgeloeste
     * Regel (Supersede), {@code deleted_at} ist ein technischer Cleanup.
     * Die Regel-Engine beruecksichtigt soft-geloeschte Regeln nicht mehr;
     * historische Assessments, die auf die Regel verweisen, behalten
     * Zugriff ueber {@code findById}.
     */
    @Transactional
    public void loesche(UUID ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException("ruleId darf nicht null sein.");
        }
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        if (rule.getDeletedAt() == null) {
            rule.setDeletedAt(Instant.now());
            ruleRepository.save(rule);
            log.info("Rule soft-geloescht: key={}", rule.getRuleKey());
        }
    }

    @Transactional(readOnly = true)
    public Optional<RuleView> find(UUID id) {
        return ruleRepository.findById(id).map(RuleView::from);
    }

    /**
     * Iteration 53 (CVM-103): Felder eines DRAFT-Regel-Eintrags
     * aktualisieren. Der Schluessel bleibt unveraenderlich.
     */
    @Transactional
    public RuleView updateDraft(UUID ruleId, RuleDraftInput input, String editor) {
        if (ruleId == null) {
            throw new IllegalArgumentException("ruleId darf nicht null sein.");
        }
        if (editor == null || editor.isBlank()) {
            throw new IllegalArgumentException("editor darf nicht leer sein.");
        }
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        if (rule.getDeletedAt() != null) {
            throw new RuleNotFoundException(ruleId);
        }
        if (rule.getStatus() != RuleStatus.DRAFT) {
            throw new IllegalStateException(
                    "Regel " + ruleId + " ist nicht im Status DRAFT (ist "
                            + rule.getStatus() + ").");
        }
        parser.parse(input.conditionJson());
        rule.setName(input.name());
        rule.setDescription(input.description());
        rule.setProposedSeverity(input.proposedSeverity());
        rule.setConditionJson(input.conditionJson());
        rule.setRationaleTemplate(input.rationaleTemplate());
        rule.setRationaleSourceFields(input.rationaleSourceFields());
        if (input.origin() != null) {
            rule.setOrigin(input.origin());
        }
        log.info("Rule-Draft aktualisiert: key={}, by={}",
                rule.getRuleKey(), editor);
        return RuleView.from(ruleRepository.save(rule));
    }

    @Transactional
    public RuleView proposeRule(RuleDraftInput input, String createdBy) {
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("createdBy darf nicht leer sein.");
        }
        parser.parse(input.conditionJson());
        Rule rule = Rule.builder()
                .ruleKey(input.ruleKey())
                .name(input.name())
                .description(input.description())
                .proposedSeverity(input.proposedSeverity())
                .conditionJson(input.conditionJson())
                .rationaleTemplate(input.rationaleTemplate())
                .rationaleSourceFields(input.rationaleSourceFields())
                .origin(input.origin() == null ? RuleOrigin.MANUAL : input.origin())
                .status(RuleStatus.DRAFT)
                .createdBy(createdBy)
                .build();
        log.info("Rule-Draft angelegt: key={}, severity={}", input.ruleKey(), input.proposedSeverity());
        return RuleView.from(ruleRepository.save(rule));
    }

    @Transactional
    public RuleView activate(UUID ruleId, String approverId) {
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("approverId darf nicht leer sein.");
        }
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        if (rule.getStatus() != RuleStatus.DRAFT) {
            throw new IllegalStateException(
                    "Regel " + ruleId + " ist nicht im Status DRAFT (ist " + rule.getStatus() + ").");
        }
        if (Objects.equals(rule.getCreatedBy(), approverId)) {
            throw new RuleFourEyesViolationException(
                    "Vier-Augen-Prinzip verletzt: Approver '" + approverId
                            + "' ist identisch mit dem Autor.");
        }
        rule.setStatus(RuleStatus.ACTIVE);
        rule.setActivatedBy(approverId);
        rule.setActivatedAt(Instant.now());
        log.info("Rule aktiviert: key={}, by={}", rule.getRuleKey(), approverId);
        return RuleView.from(ruleRepository.save(rule));
    }

    /**
     * Eingabe-Record fuer Draft-Erzeugung. Entkoppelt den Service von
     * REST-DTOs.
     */
    public record RuleDraftInput(
            String ruleKey,
            String name,
            String description,
            AhsSeverity proposedSeverity,
            String conditionJson,
            String rationaleTemplate,
            List<String> rationaleSourceFields,
            RuleOrigin origin) {}
}
