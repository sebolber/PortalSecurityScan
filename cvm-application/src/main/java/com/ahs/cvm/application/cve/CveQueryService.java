package com.ahs.cvm.application.cve;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-Service fuer das CVE-Inventar (Iteration 26, CVM-57).
 *
 * <p>Aktuell Stream-Filter, weil die CVE-Tabelle ueberschaubar ist;
 * sobald eine fuenfstellige Menge erreicht ist, sollte auf JPA-
 * Criteria-Paging umgestellt werden.
 */
@Service
public class CveQueryService {

    private final CveRepository repository;
    private final FindingRepository findings;
    private final AssessmentRepository assessments;

    public CveQueryService(
            CveRepository repository,
            FindingRepository findings,
            AssessmentRepository assessments) {
        this.repository = repository;
        this.findings = findings;
        this.assessments = assessments;
    }

    @Transactional(readOnly = true)
    public Optional<CveDetailView> findDetail(String cveId) {
        if (cveId == null || cveId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCveId(cveId.trim())
                .map(this::toDetail);
    }

    private CveDetailView toDetail(Cve cve) {
        List<Finding> findingList = findings.findByCveId(cve.getId());
        List<CveDetailView.FindingEntry> findingEntries = findingList.stream()
                .map(f -> new CveDetailView.FindingEntry(
                        f.getId(),
                        f.getScan() != null ? f.getScan().getId() : null,
                        f.getComponentOccurrence() != null
                                ? f.getComponentOccurrence().getId() : null,
                        // Komponenten-Key = PURL (eindeutig) bzw. Name.
                        f.getComponentOccurrence() != null
                                && f.getComponentOccurrence().getComponent() != null
                                        ? (f.getComponentOccurrence().getComponent().getPurl() != null
                                                ? f.getComponentOccurrence().getComponent().getPurl()
                                                : f.getComponentOccurrence().getComponent().getName())
                                        : null,
                        f.getComponentOccurrence() != null
                                && f.getComponentOccurrence().getComponent() != null
                                        ? f.getComponentOccurrence().getComponent().getVersion()
                                        : null,
                        f.getFixedInVersion(),
                        f.getDetectedAt(),
                        f.getScan() != null && f.getScan().getProductVersion() != null
                                ? f.getScan().getProductVersion().getId() : null,
                        f.getScan() != null && f.getScan().getEnvironment() != null
                                ? f.getScan().getEnvironment().getId() : null))
                .sorted(Comparator.comparing(
                        CveDetailView.FindingEntry::detectedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<CveDetailView.AssessmentEntry> assessmentEntries = findingList.stream()
                .map(f -> assessments
                        .findFirstByFindingIdOrderByVersionDesc(f.getId())
                        .orElse(null))
                .filter(a -> a != null)
                .map(CveQueryService::toAssessmentEntry)
                .toList();

        return new CveDetailView(CveView.from(cve), findingEntries, assessmentEntries);
    }

    private static CveDetailView.AssessmentEntry toAssessmentEntry(Assessment a) {
        return new CveDetailView.AssessmentEntry(
                a.getId(),
                a.getFinding() != null ? a.getFinding().getId() : null,
                a.getVersion(),
                a.getSeverity(),
                a.getStatus(),
                a.getProposalSource(),
                a.getRationale(),
                a.getDecidedBy(),
                a.getCreatedAt(),
                a.getValidUntil());
    }

    @Transactional(readOnly = true)
    public Page<CveView> findPage(String searchTerm, AhsSeverity severityFilter,
            boolean onlyKev, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        List<Cve> all = repository.findAll(Sort.by(Sort.Direction.DESC,
                "publishedAt", "cveId"));
        List<CveView> gefiltert = all.stream()
                .filter(c -> matchesSearch(c, searchTerm))
                .filter(c -> matchesSeverity(c, severityFilter))
                .filter(c -> !onlyKev || Boolean.TRUE.equals(c.getKevListed()))
                .map(CveView::from)
                .toList();
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, gefiltert.size());
        List<CveView> slice = from >= gefiltert.size()
                ? List.of()
                : gefiltert.subList(from, to);
        return new PageImpl<>(slice,
                PageRequest.of(safePage, safeSize),
                gefiltert.size());
    }

    static boolean matchesSearch(Cve c, String term) {
        if (term == null || term.isBlank()) {
            return true;
        }
        String q = term.trim().toLowerCase(Locale.ROOT);
        if (c.getCveId() != null && c.getCveId().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        return c.getSummary() != null
                && c.getSummary().toLowerCase(Locale.ROOT).contains(q);
    }

    static boolean matchesSeverity(Cve c, AhsSeverity filter) {
        if (filter == null) {
            return true;
        }
        return ableiten(c.getCvssBaseScore()) == filter;
    }

    static AhsSeverity ableiten(BigDecimal score) {
        if (score == null) {
            return AhsSeverity.INFORMATIONAL;
        }
        double d = score.doubleValue();
        if (d >= 9.0) return AhsSeverity.CRITICAL;
        if (d >= 7.0) return AhsSeverity.HIGH;
        if (d >= 4.0) return AhsSeverity.MEDIUM;
        if (d > 0.0) return AhsSeverity.LOW;
        return AhsSeverity.INFORMATIONAL;
    }
}
