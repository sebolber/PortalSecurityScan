package com.ahs.cvm.ai.reachability;

import com.ahs.cvm.application.reachability.PurlSymbolDeriver;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductVersion;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produktiv-Adapter fuer den {@link ReachabilityAutoTriggerPort}
 * (Iteration 77, CVM-314).
 *
 * <p>Konsumiert {@link LowConfidenceAiSuggestionEvent}s aus dem
 * {@link ReachabilityAutoTriggerService}. Laedt das Finding, loest
 * Repo-URL ueber {@code Product.repoUrl}, Commit-SHA ueber
 * {@code ProductVersion.gitCommit} und Symbol/Language ueber den
 * {@link PurlSymbolDeriver} auf und ruft
 * {@link ReachabilityAgent#analyze(ReachabilityRequest)}.
 *
 * <p>Fehlen Repo-URL, Commit-SHA oder Symbol, wird nichts gestartet
 * (nur ein Info-Log). Der Subprozess-Lauf erfolgt {@code @Async},
 * damit der HTTP-Publisher-Thread nicht blockiert.
 *
 * <p>{@code @Primary} verdraengt den
 * {@link NoopReachabilityAutoTriggerAdapter} im Spring-Context.
 */
@Component
@Primary
public class ReachabilityAutoTriggerAdapter implements ReachabilityAutoTriggerPort {

    private static final Logger log = LoggerFactory.getLogger(
            ReachabilityAutoTriggerAdapter.class);

    private final FindingRepository findingRepository;
    private final ReachabilityAgent reachabilityAgent;
    private final String defaultBranch;

    public ReachabilityAutoTriggerAdapter(
            FindingRepository findingRepository,
            ReachabilityAgent reachabilityAgent,
            @Value("${cvm.ai.reachability.auto-trigger.branch:main}") String defaultBranch) {
        this.findingRepository = findingRepository;
        this.reachabilityAgent = reachabilityAgent;
        this.defaultBranch = defaultBranch == null || defaultBranch.isBlank()
                ? "main"
                : defaultBranch;
    }

    @Override
    @Async
    @Transactional
    public void trigger(UUID findingId, String triggeredBy) {
        Finding finding = findingRepository.findById(findingId).orElse(null);
        if (finding == null) {
            log.debug("Auto-Trigger: Finding {} nicht gefunden, skip.", findingId);
            return;
        }
        ProductVersion version = finding.getScan() == null
                ? null : finding.getScan().getProductVersion();
        Product product = version == null ? null : version.getProduct();
        String repoUrl = product == null ? null : product.getRepoUrl();
        String commitSha = version == null ? null : version.getGitCommit();
        if (repoUrl == null || repoUrl.isBlank()
                || commitSha == null || commitSha.isBlank()) {
            log.info(
                    "Auto-Trigger fuer finding={} uebersprungen: repoUrl/commitSha fehlen",
                    findingId);
            return;
        }
        String purl = purlVon(finding);
        if (purl == null || purl.isBlank()) {
            log.info("Auto-Trigger fuer finding={} uebersprungen: keine PURL", findingId);
            return;
        }
        Optional<PurlSymbolDeriver.Suggestion> suggestion = PurlSymbolDeriver.derive(purl);
        if (suggestion.isEmpty()) {
            log.info(
                    "Auto-Trigger fuer finding={} uebersprungen: keine Symbol-"
                            + "Ableitung aus PURL {}",
                    findingId, purl);
            return;
        }
        PurlSymbolDeriver.Suggestion s = suggestion.get();

        try {
            ReachabilityRequest request = new ReachabilityRequest(
                    findingId,
                    repoUrl,
                    defaultBranch,
                    commitSha,
                    s.symbol(),
                    s.language(),
                    "Auto-Trigger: niedrige AI-Confidence",
                    triggeredBy == null || triggeredBy.isBlank()
                            ? "system:auto-trigger"
                            : triggeredBy);
            ReachabilityResult result = reachabilityAgent.analyze(request);
            log.info(
                    "Auto-Trigger Reachability abgeschlossen: finding={}, status={}",
                    findingId, result);
        } catch (RuntimeException ex) {
            log.warn(
                    "Auto-Trigger Reachability fehlgeschlagen (finding={}): {}",
                    findingId, ex.getMessage());
        }
    }

    private static String purlVon(Finding finding) {
        if (finding.getComponentOccurrence() == null
                || finding.getComponentOccurrence().getComponent() == null) {
            return null;
        }
        return finding.getComponentOccurrence().getComponent().getPurl();
    }
}
