package com.ahs.cvm.persistence.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSourceRefRepository extends JpaRepository<AiSourceRef, UUID> {

    List<AiSourceRef> findByAiSuggestionId(UUID aiSuggestionId);
}
