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
        // Nie null uebergeben: sonst kann Hibernate den Parametertyp
        // beim Binden nicht zuverlaessig ableiten und PG-JDBC bindet als
        // bytea, wodurch die Typpruefung im LIKE ('text ~~ bytea')
        // scheitert - auch wenn der OR-Zweig logisch kurzgeschlossen
        // waere. Empty-String als "kein Filter" ist eindeutig typbar.
        String searchLower = (searchTerm == null || searchTerm.isBlank())
                ? ""
                : searchTerm.trim().toLowerCase(Locale.ROOT);
        BigDecimal minScore = untergrenze(severityFilter);
        BigDecimal maxScore = obergrenze(severityFilter);
        boolean informational = severityFilter == AhsSeverity.INFORMATIONAL;

        // Server-seitiges Paging (Iteration 39, CVM-83). Die frueher
        // genutzte Stream-Filter-Loesung fiel fuer grosse CVE-Tabellen
        // in den OOM-Bereich.
        Page<Cve> page0 = repository.searchPage(
                searchLower,
                minScore,
                maxScore,
                informational,
                onlyKev,
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Direction.DESC, "publishedAt", "cveId")));

        return page0.map(CveView::from);
    }

    /**
     * Liefert die inklusiv gemeinten Unter-Grenzen fuer die Severity-
     * Filter. NULL = keine Grenze.
     */
    static BigDecimal untergrenze(AhsSeverity sev) {
        if (sev == null) return null;
        return switch (sev) {
            case CRITICAL -> new BigDecimal("9.0");
            case HIGH -> new BigDecimal("7.0");
            case MEDIUM -> new BigDecimal("4.0");
            case LOW -> new BigDecimal("0.1");
            case INFORMATIONAL, NOT_APPLICABLE -> null;
        };
    }

    /**
     * Liefert die exklusiv gemeinten Obergrenzen fuer die Severity-
     * Filter. NULL = keine Grenze.
     */
    static BigDecimal obergrenze(AhsSeverity sev) {
        if (sev == null) return null;
        return switch (sev) {
            case CRITICAL -> null;
            case HIGH -> new BigDecimal("9.0");
            case MEDIUM -> new BigDecimal("7.0");
            case LOW -> new BigDecimal("4.0");
            case INFORMATIONAL, NOT_APPLICABLE -> null;
        };
    }

}
