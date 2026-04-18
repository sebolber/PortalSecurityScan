package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestion;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Freigabe/Ablehnung von Regel-Vorschlaegen (Iteration 17, CVM-42).
 * Bei Freigabe entsteht eine aktive {@link Rule} mit
 * {@code origin=AI_EXTRACTED} und dem Verweis auf den
 * {@code ai_suggestion}-Eintrag.
 *
 * <p>Vier-Augen: {@code approvedBy} darf nicht gleich
 * {@code suggestedBy} sein. Da der System-User
 * {@code system:rule-extraction} niemals einen Login-Wert darstellt,
 * ist in der Regel jeder menschliche Approver OK; die Pruefung
 * greift trotzdem fuer den Fall, dass ein anderer Service den
 * Vorschlag unter einem menschlichen Account produziert hat.
 */
@Service
public class RuleSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(RuleSuggestionService.class);

    private final RuleSuggestionRepository suggestionRepository;
    private final RuleRepository ruleRepository;

    public RuleSuggestionService(
            RuleSuggestionRepository suggestionRepository,
            RuleRepository ruleRepository) {
        this.suggestionRepository = suggestionRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public List<RuleSuggestionView> listeOffene() {
        return suggestionRepository.findByStatusOrderByCreatedAtDesc(
                RuleSuggestionStatus.PROPOSED)
                .stream().map(RuleSuggestionView::from).toList();
    }

    @Transactional
    public ApproveRuleResult approveAsView(UUID suggestionId, String approvedBy) {
        return ApproveRuleResult.from(approve(suggestionId, approvedBy));
    }

    @Transactional
    public RuleSuggestionView rejectAsView(UUID suggestionId,
            String rejectedBy, String comment) {
        return RuleSuggestionView.from(reject(suggestionId, rejectedBy, comment));
    }

    @Transactional
    public Rule approve(UUID suggestionId, String approvedBy) {
        if (approvedBy == null || approvedBy.isBlank()) {
            throw new IllegalArgumentException("approvedBy darf nicht leer sein.");
        }
        RuleSuggestion s = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "RuleSuggestion nicht gefunden: " + suggestionId));
        if (s.getStatus() != RuleSuggestionStatus.PROPOSED) {
            throw new IllegalStateException(
                    "RuleSuggestion " + suggestionId + " ist nicht PROPOSED: " + s.getStatus());
        }
        if (approvedBy.equals(s.getSuggestedBy())) {
            throw new IllegalStateException(
                    "Vier-Augen: approver == suggester (" + approvedBy + ")");
        }
        Instant now = Instant.now();
        Rule rule = ruleRepository.save(Rule.builder()
                .ruleKey("ai-" + s.getId().toString().substring(0, 8))
                .name(s.getName())
                .description(s.getClusterRationale())
                .origin(RuleOrigin.AI_EXTRACTED)
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(s.getProposedSeverity())
                .conditionJson(s.getConditionJson())
                .rationaleTemplate(s.getRationaleTemplate())
                .rationaleSourceFields(List.of())
                .createdBy(s.getSuggestedBy())
                .activatedBy(approvedBy)
                .activatedAt(now)
                .extractedFromAiSuggestionId(s.getAiSuggestion().getId())
                .build());
        s.setStatus(RuleSuggestionStatus.APPROVED);
        s.setApprovedBy(approvedBy);
        s.setApprovedAt(now);
        suggestionRepository.save(s);
        log.info("RuleSuggestion {} freigegeben, Rule {} (ACTIVE).",
                suggestionId, rule.getRuleKey());
        return rule;
    }

    @Transactional
    public RuleSuggestion reject(UUID suggestionId, String rejectedBy, String comment) {
        if (rejectedBy == null || rejectedBy.isBlank()) {
            throw new IllegalArgumentException("rejectedBy darf nicht leer sein.");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment ist Pflicht.");
        }
        RuleSuggestion s = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "RuleSuggestion nicht gefunden: " + suggestionId));
        if (s.getStatus() != RuleSuggestionStatus.PROPOSED) {
            throw new IllegalStateException(
                    "RuleSuggestion " + suggestionId + " ist nicht PROPOSED: " + s.getStatus());
        }
        s.setStatus(RuleSuggestionStatus.REJECTED);
        s.setRejectedBy(rejectedBy);
        s.setRejectedAt(Instant.now());
        s.setRejectComment(comment);
        return suggestionRepository.save(s);
    }
}
