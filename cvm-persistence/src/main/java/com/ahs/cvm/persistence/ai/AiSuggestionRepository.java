package com.ahs.cvm.persistence.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    List<AiSuggestion> findByAiCallAuditId(UUID aiCallAuditId);

    List<AiSuggestion> findByFindingId(UUID findingId);

    /**
     * Zuletzt erzeugte Vorschlaege mit einem bestimmten Use-Case
     * (z.&nbsp;B. {@code REACHABILITY}) absteigend nach
     * {@code createdAt} - Iteration 27e fuer die
     * Reachability-Uebersicht.
     */
    List<AiSuggestion> findByUseCaseOrderByCreatedAtDesc(
            String useCase, Pageable pageable);
}
