package com.ahs.cvm.persistence.rulesuggestion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleSuggestionConflictRepository
        extends JpaRepository<RuleSuggestionConflict, UUID> {
    List<RuleSuggestionConflict> findByRuleSuggestionId(UUID ruleSuggestionId);
}
