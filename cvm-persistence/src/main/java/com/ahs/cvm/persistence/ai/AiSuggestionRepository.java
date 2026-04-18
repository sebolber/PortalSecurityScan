package com.ahs.cvm.persistence.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    List<AiSuggestion> findByAiCallAuditId(UUID aiCallAuditId);

    List<AiSuggestion> findByFindingId(UUID findingId);
}
