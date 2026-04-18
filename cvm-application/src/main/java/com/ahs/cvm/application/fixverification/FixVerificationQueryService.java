package com.ahs.cvm.application.fixverification;

import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-Service fuer die Fix-Verifikations-Uebersicht
 * (Iteration 27e, CVM-65).
 */
@Service
public class FixVerificationQueryService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final MitigationPlanRepository repository;

    public FixVerificationQueryService(MitigationPlanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FixVerificationSummaryView> recent(int limit) {
        int effective = effectiveLimit(limit);
        return repository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, effective))
                .stream()
                .map(FixVerificationSummaryView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FixVerificationSummaryView> byGrade(FixVerificationGrade grade, int limit) {
        int effective = effectiveLimit(limit);
        return repository
                .findByVerificationGradeOrderByCreatedAtDesc(
                        grade, PageRequest.of(0, effective))
                .stream()
                .map(FixVerificationSummaryView::from)
                .toList();
    }

    private static int effectiveLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
