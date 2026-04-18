package com.ahs.cvm.application.vex;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VEX-Export in CycloneDX 1.6 und CSAF 2.0 (Iteration 20, CVM-51).
 *
 * <p>Die Ausgabe ist deterministisch - gleiche Eingabe (Assessments
 * + Mitigation-Plaene + Clock) erzeugt byte-gleiches JSON. Timestamps
 * stammen aus dem injizierten {@link Clock}, Statements sind nach
 * CVE-Key sortiert.
 */
@Service
public class VexExporter {

    public static final String FORMAT_CYCLONEDX = "cyclonedx";
    public static final String FORMAT_CSAF = "csaf";

    private final AssessmentRepository assessmentRepository;
    private final MitigationPlanRepository mitigationRepository;
    private final ProductVersionRepository productVersionRepository;
    private final VexStatementMapper mapper;
    private final Clock clock;
    private final ObjectMapper json;

    public VexExporter(
            AssessmentRepository assessmentRepository,
            MitigationPlanRepository mitigationRepository,
            ProductVersionRepository productVersionRepository,
            VexStatementMapper mapper,
            Clock clock) {
        this.assessmentRepository = assessmentRepository;
        this.mitigationRepository = mitigationRepository;
        this.productVersionRepository = productVersionRepository;
        this.mapper = mapper;
        this.clock = clock;
        this.json = new ObjectMapper();
        this.json.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.json.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Transactional(readOnly = true)
    public String export(UUID productVersionId, String format) {
        ProductVersion pv = productVersionRepository.findById(productVersionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekannte productVersionId: " + productVersionId));

        List<VexStatement> statements = assessmentRepository.findAll().stream()
                .filter(a -> a.getSupersededAt() == null)
                .filter(a -> pvMatches(a, productVersionId))
                .filter(a -> a.getStatus() != AssessmentStatus.SUPERSEDED)
                .map(a -> mapper.toStatement(a,
                        mitigationRepository.findByAssessmentId(a.getId())))
                .sorted((x, y) -> safeCompare(x.cveKey(), y.cveKey()))
                .toList();

        Instant erzeugt = Instant.now(clock);
        return switch (format == null ? "" : format.toLowerCase(Locale.ROOT)) {
            case FORMAT_CSAF -> exportCsaf(pv, statements, erzeugt);
            default -> exportCycloneDx(pv, statements, erzeugt);
        };
    }

    private String exportCycloneDx(ProductVersion pv, List<VexStatement> statements,
            Instant erzeugt) {
        ObjectNode root = json.createObjectNode();
        root.put("bomFormat", "CycloneDX");
        root.put("specVersion", "1.6");
        root.put("version", 1);
        root.put("serialNumber", "urn:uuid:" + deterministicUrn(pv));
        ObjectNode metadata = root.putObject("metadata");
        metadata.put("timestamp", erzeugt.toString());
        ArrayNode vulns = root.putArray("vulnerabilities");
        for (VexStatement s : statements) {
            ObjectNode v = vulns.addObject();
            v.put("id", s.cveKey());
            ObjectNode analysis = v.putObject("analysis");
            analysis.put("state", cycloneState(s.status()));
            if (s.justification() != null) {
                analysis.put("justification", s.justification());
            }
            analysis.put("detail", s.detail());
            if (s.productPurl() != null) {
                ArrayNode affects = v.putArray("affects");
                affects.addObject().put("ref", s.productPurl());
            }
        }
        return writeJson(root);
    }

    private String exportCsaf(ProductVersion pv, List<VexStatement> statements,
            Instant erzeugt) {
        ObjectNode root = json.createObjectNode();
        ObjectNode doc = root.putObject("document");
        doc.put("category", "csaf_vex");
        doc.put("csaf_version", "2.0");
        doc.put("title", "VEX fuer " + pv.getProduct().getName() + " "
                + pv.getVersion());
        ObjectNode publisher = doc.putObject("publisher");
        publisher.put("category", "vendor");
        publisher.put("name", "adesso health solutions GmbH");
        ObjectNode tracking = doc.putObject("tracking");
        tracking.put("id", "cvm-vex-" + pv.getId());
        tracking.put("initial_release_date", erzeugt.toString());
        tracking.put("current_release_date", erzeugt.toString());

        ArrayNode vulns = root.putArray("vulnerabilities");
        for (VexStatement s : statements) {
            ObjectNode v = vulns.addObject();
            v.put("cve", s.cveKey());
            ObjectNode statusNode = v.putObject("product_status");
            ArrayNode arr = statusNode.putArray(csafStatusArray(s.status()));
            if (s.productPurl() != null) {
                arr.add(s.productPurl());
            }
            if (s.detail() != null && !s.detail().isBlank()) {
                v.put("details", s.detail());
            }
            if (s.justification() != null) {
                v.put("flags", "[{\"label\":\"" + s.justification() + "\"}]");
            }
        }
        return writeJson(root);
    }

    static String cycloneState(VexStatus s) {
        return switch (s) {
            case NOT_AFFECTED -> "not_affected";
            case AFFECTED -> "exploitable";
            case FIXED -> "resolved";
            case UNDER_INVESTIGATION -> "in_triage";
        };
    }

    static String csafStatusArray(VexStatus s) {
        return switch (s) {
            case NOT_AFFECTED -> "known_not_affected";
            case AFFECTED -> "known_affected";
            case FIXED -> "fixed";
            case UNDER_INVESTIGATION -> "under_investigation";
        };
    }

    private String writeJson(ObjectNode root) {
        try {
            return json.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("VEX-Serialisierung fehlgeschlagen", ex);
        }
    }

    private static boolean pvMatches(Assessment a, UUID productVersionId) {
        return a.getProductVersion() != null
                && Objects.equals(a.getProductVersion().getId(), productVersionId);
    }

    private static int safeCompare(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private static String deterministicUrn(ProductVersion pv) {
        return pv.getId().toString();
    }
}
