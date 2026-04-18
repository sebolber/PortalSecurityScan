package com.ahs.cvm.persistence.rulesuggestion;

import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleSuggestionRepository extends JpaRepository<RuleSuggestion, UUID> {

    List<RuleSuggestion> findByStatusOrderByCreatedAtDesc(RuleSuggestionStatus status);
}
