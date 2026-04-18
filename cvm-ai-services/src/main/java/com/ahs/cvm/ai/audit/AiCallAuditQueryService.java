package com.ahs.cvm.ai.audit;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-Service fuer {@link com.ahs.cvm.persistence.ai.AiCallAudit}
 * (Iteration 11 Nachzug). Bietet eine Listing-API mit optionalen
 * Filtern auf Status und Use-Case. System-/User-Prompts werden vom
 * {@link AiCallAuditView} bewusst nicht uebernommen.
 */
@Service
public class AiCallAuditQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    private final AiCallAuditRepository repository;

    public AiCallAuditQueryService(AiCallAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AiCallAuditView> liste(AiCallStatus status, String useCase,
            int page, int size) {
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository
                .findeAudits(status, normalize(useCase), pageable)
                .map(AiCallAuditView::from);
    }

    private static String normalize(String useCase) {
        if (useCase == null || useCase.isBlank()) {
            return null;
        }
        return useCase.trim();
    }
}
