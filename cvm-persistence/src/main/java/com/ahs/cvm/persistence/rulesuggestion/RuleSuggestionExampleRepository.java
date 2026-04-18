package com.ahs.cvm.persistence.rulesuggestion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleSuggestionExampleRepository extends JpaRepository<RuleSuggestionExample, UUID> {
    List<RuleSuggestionExample> findByRuleSuggestionId(UUID ruleSuggestionId);
}
