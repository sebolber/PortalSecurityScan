package com.ahs.cvm.application.pipeline;

import com.ahs.cvm.application.scan.CycloneDxBom;
import com.ahs.cvm.application.scan.CycloneDxParser;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CI/CD-Gate (Iteration 21, CVM-52).
 *
 * <p>Parsen statt voll-ingesten: die Pipeline liefert eine SBOM, der
 * Gate parst sie und vergleicht die gefundenen CVEs mit den bereits
 * bekannten Findings fuer die {@code productVersionId}. Er liefert
 * {@code PASS|WARN|FAIL} zurueck. Die Pipeline entscheidet via
 * {@code allow_failure} ueber den Build-Bruch.
 */
@Service
public class PipelineGateService {

    private static final Logger log = LoggerFactory.getLogger(PipelineGateService.class);

    public enum GateDecision { PASS, WARN, FAIL }

    private final CycloneDxParser parser;
    private final CveRepository cveRepository;
    private final FindingRepository findingRepository;
    private final PipelineGateRateLimiter rateLimiter;
    private final Clock clock;

    public PipelineGateService(
            CycloneDxParser parser,
            CveRepository cveRepository,
            FindingRepository findingRepository,
            PipelineGateRateLimiter rateLimiter,
            Clock clock) {
        this.parser = parser;
        this.cveRepository = cveRepository;
        this.findingRepository = findingRepository;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GateResult evaluate(GateRequest req) {
        if (req == null || req.productVersionId() == null) {
            throw new IllegalArgumentException("productVersionId ist Pflicht.");
        }
        if (req.sbom() == null || req.sbom().length == 0) {
            throw new IllegalArgumentException("SBOM-Bytes duerfen nicht leer sein.");
        }
        if (!rateLimiter.tryAcquire(req.productVersionId().toString())) {
            throw new GateRateLimitException(req.productVersionId());
        }

        CycloneDxBom bom = parser.parse(req.sbom());
        Set<String> neueCves = sammleEindeutigeCves(bom);
        Map<String, AhsSeverity> severityImSbom = sammleSeveritiesAusBom(bom);
        List<GateFinding> details = new ArrayList<>();
        int newCritical = 0;
        int newHigh = 0;

        for (String cveKey : neueCves) {
            Optional<Cve> cveOpt = cveRepository.findByCveId(cveKey);
            AhsSeverity s = severityImSbom.getOrDefault(cveKey,
                    cveOpt.map(PipelineGateService::severityAusCve)
                            .orElse(AhsSeverity.INFORMATIONAL));
            if (cveOpt.isEmpty()) {
                details.add(new GateFinding(cveKey, null, s,
                        "unbekannt in CVE-Katalog (Enrichment steht aus)"));
                continue;
            }
            Cve cve = cveOpt.get();
            boolean bereitsBekannt = findingRepository.findByCveId(cve.getId()).stream()
                    .anyMatch(f -> bezieht(f, req.productVersionId()));
            if (bereitsBekannt) {
                details.add(new GateFinding(cveKey, cve.getId(), s, "bekannt"));
                continue;
            }
            details.add(new GateFinding(cveKey, cve.getId(), s, "neu"));
            if (s == AhsSeverity.CRITICAL) {
                newCritical++;
            } else if (s == AhsSeverity.HIGH) {
                newHigh++;
            }
        }

        GateDecision gate = entscheide(newCritical, newHigh);
        log.info("Pipeline-Gate {} (PV {}, MR {}): newCritical={}, newHigh={}",
                gate, req.productVersionId(), req.mergeRequestId(), newCritical, newHigh);
        return new GateResult(gate, newCritical, newHigh, Instant.now(clock),
                details);
    }

    static GateDecision entscheide(int newCritical, int newHigh) {
        if (newCritical > 0) {
            return GateDecision.FAIL;
        }
        if (newHigh > 0) {
            return GateDecision.WARN;
        }
        return GateDecision.PASS;
    }

    private static Set<String> sammleEindeutigeCves(CycloneDxBom bom) {
        Set<String> out = new HashSet<>();
        if (bom == null || bom.vulnerabilities() == null) {
            return out;
        }
        for (CycloneDxBom.Vulnerability v : bom.vulnerabilities()) {
            if (v == null || v.id() == null || v.id().isBlank()) {
                continue;
            }
            out.add(v.id().trim().toUpperCase(Locale.ROOT));
        }
        return out;
    }

    private static Map<String, AhsSeverity> sammleSeveritiesAusBom(CycloneDxBom bom) {
        Map<String, AhsSeverity> out = new HashMap<>();
        if (bom == null || bom.vulnerabilities() == null) {
            return out;
        }
        for (CycloneDxBom.Vulnerability v : bom.vulnerabilities()) {
            if (v == null || v.id() == null || v.ratings() == null) {
                continue;
            }
            AhsSeverity hoechste = null;
            for (CycloneDxBom.Rating r : v.ratings()) {
                AhsSeverity s = parseSeverity(r == null ? null : r.severity());
                if (s == null) {
                    continue;
                }
                if (hoechste == null || s.ordinal() < hoechste.ordinal()) {
                    hoechste = s;
                }
            }
            if (hoechste != null) {
                out.put(v.id().trim().toUpperCase(Locale.ROOT), hoechste);
            }
        }
        return out;
    }

    static AhsSeverity parseSeverity(String raw) {
        if (raw == null) {
            return null;
        }
        String n = raw.trim().toUpperCase(Locale.ROOT);
        return switch (n) {
            case "CRITICAL" -> AhsSeverity.CRITICAL;
            case "HIGH" -> AhsSeverity.HIGH;
            case "MEDIUM" -> AhsSeverity.MEDIUM;
            case "LOW" -> AhsSeverity.LOW;
            case "INFORMATIONAL", "NONE" -> AhsSeverity.INFORMATIONAL;
            default -> null;
        };
    }

    static AhsSeverity severityAusCve(Cve cve) {
        BigDecimal score = cve == null ? null : cve.getCvssBaseScore();
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

    private static boolean bezieht(Finding f, UUID productVersionId) {
        if (f.getScan() == null || f.getScan().getProductVersion() == null) {
            return false;
        }
        return productVersionId.equals(f.getScan().getProductVersion().getId());
    }

    public record GateRequest(
            UUID productVersionId, UUID environmentId,
            String branchRef, String mergeRequestId,
            byte[] sbom) {}

    public record GateResult(
            GateDecision gate,
            int newCritical,
            int newHigh,
            Instant evaluatedAt,
            List<GateFinding> details) {}

    public record GateFinding(
            String cveKey,
            UUID cveId,
            AhsSeverity severity,
            String status) {}

    /** Wird geworfen, wenn der Rate-Limiter fuer die Produkt-Version verweigert. */
    public static class GateRateLimitException extends RuntimeException {
        public GateRateLimitException(UUID productVersionId) {
            super("Rate-Limit ueberschritten fuer productVersionId=" + productVersionId);
        }
    }
}
