package com.ahs.cvm.application.reachability;

import com.ahs.cvm.persistence.ai.AiSuggestionRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.Scan;
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

    /**
     * Iteration 97 (CVM-339): Liefert Repo-URL und Commit-SHA aus
     * {@code Product}/{@code ProductVersion} fuer das Finding,
     * damit der Start-Dialog die Pflichtfelder vorbelegen kann.
     */
    @Transactional(readOnly = true)
    public ReachabilityStartContextView contextForFinding(UUID findingId) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new FindingNotFoundException(findingId));
        ProductVersion version = Optional.ofNullable(finding.getScan())
                .map(Scan::getProductVersion)
                .orElse(null);
        Product product = version == null ? null : version.getProduct();
        String repoUrl = product == null ? null : product.getRepoUrl();
        String commitSha = version == null ? null : version.getGitCommit();
        String rationale = rationalFor(repoUrl, commitSha);
        return new ReachabilityStartContextView(
                findingId,
                blankToNull(repoUrl),
                blankToNull(commitSha),
                rationale);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String rationalFor(String repoUrl, String commitSha) {
        boolean repoFehlt = repoUrl == null || repoUrl.isBlank();
        boolean commitFehlt = commitSha == null || commitSha.isBlank();
        if (repoFehlt && commitFehlt) {
            return "Produkt hat keine Repo-URL und keinen Commit-SHA "
                    + "- bitte beide manuell eintragen.";
        }
        if (repoFehlt) {
            return "Produkt hat keine Repo-URL - bitte manuell eintragen.";
        }
        if (commitFehlt) {
            return "Produkt-Version hat keinen Git-Commit - bitte manuell eintragen.";
        }
        return "Vorbelegt aus Produkt und Produkt-Version.";
    }

    /** 404-Marker fuer den ExceptionHandler. */
    public static final class FindingNotFoundException extends RuntimeException {
        public FindingNotFoundException(UUID id) {
            super("Finding " + id + " nicht gefunden.");
        }
    }
}
