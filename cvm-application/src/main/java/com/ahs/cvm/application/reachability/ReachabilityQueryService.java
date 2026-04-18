package com.ahs.cvm.application.reachability;

import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uebersichts-Service fuer Reachability-Analysen
 * (Iteration 27e, CVM-65). Liest zuletzt erzeugte AiSuggestions
 * mit Use-Case {@code REACHABILITY}.
 */
@Service
public class ReachabilityQueryService {

    /** Use-Case-Kennung im {@code ai_suggestion}-Eintrag. */
    public static final String USE_CASE = "REACHABILITY";

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final AiSuggestionRepository repository;

    public ReachabilityQueryService(AiSuggestionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ReachabilitySummaryView> recent(int limit) {
        int effective = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return repository
                .findByUseCaseOrderByCreatedAtDesc(USE_CASE, PageRequest.of(0, effective))
                .stream()
                .map(ReachabilitySummaryView::from)
                .toList();
    }
}
