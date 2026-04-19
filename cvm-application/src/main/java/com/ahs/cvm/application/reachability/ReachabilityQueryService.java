package com.ahs.cvm.application.reachability;

import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uebersichts-Service fuer Reachability-Analysen
 * (Iteration 27e, CVM-65). Liest zuletzt erzeugte AiSuggestions
 * mit Use-Case {@code REACHABILITY} und liefert ausserdem einen
 * Symbol-Vorschlag pro Finding (aus der Component-PURL).
 */
@Service
public class ReachabilityQueryService {

    /** Use-Case-Kennung im {@code ai_suggestion}-Eintrag. */
    public static final String USE_CASE = "REACHABILITY";

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final AiSuggestionRepository repository;
    private final FindingRepository findingRepository;

    public ReachabilityQueryService(
            AiSuggestionRepository repository,
            FindingRepository findingRepository) {
        this.repository = repository;
        this.findingRepository = findingRepository;
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

    /**
     * Liefert einen abgeleiteten {@code vulnerableSymbol}-Vorschlag
     * fuer ein Finding. Quelle ist die PURL der verknuepften
     * Component; der Deriver ist unter
     * {@link PurlSymbolDeriver#derive(String)} getestet.
     *
     * <p>Liefert immer einen {@link ReachabilitySuggestionView}:
     * falls die PURL nicht parsebar ist, ist {@code symbol} {@code null}
     * und der Analyst muss manuell eingeben.
     */
    @Transactional(readOnly = true)
    public ReachabilitySuggestionView suggestionForFinding(UUID findingId) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new FindingNotFoundException(findingId));
        String purl = Optional.ofNullable(finding.getComponentOccurrence())
                .map(ComponentOccurrence::getComponent)
                .map(Component::getPurl)
                .orElse(null);
        Optional<PurlSymbolDeriver.Suggestion> derived =
                PurlSymbolDeriver.derive(purl);
        return new ReachabilitySuggestionView(
                findingId,
                purl,
                derived.map(PurlSymbolDeriver.Suggestion::symbol).orElse(null),
                derived.map(PurlSymbolDeriver.Suggestion::language).orElse(null),
                derived.map(PurlSymbolDeriver.Suggestion::rationale)
                        .orElse("PURL unbekannt oder nicht parsebar - bitte Symbol manuell eingeben."));
    }

    /** 404-Marker fuer den ExceptionHandler. */
    public static final class FindingNotFoundException extends RuntimeException {
        public FindingNotFoundException(UUID id) {
            super("Finding " + id + " nicht gefunden.");
        }
    }
}
