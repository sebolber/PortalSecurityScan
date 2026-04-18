package com.ahs.cvm.application.report;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.ahs.cvm.persistence.profile.ContextProfile;
import com.ahs.cvm.persistence.profile.ContextProfileRepository;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut aus den Repositories ein {@link HardeningReportData}-Read-Model.
 *
 * <p>Die Komponenten-Kategorisierung (Plattform/Docker/Java/NodeJS/
 * Python) orientiert sich am PURL-Typ bzw. am Component-Type. Ein
 * Nicht-Treffer landet in {@code "Plattform"}. Sobald
 * {@code ComponentOccurrence} in den Reports gebraucht wird, kann die
 * Logik feiner werden; fuer Iteration 10 reicht die PURL-basierte
 * Heuristik.
 */
@Component
public class HardeningReportDataLoader {

    static final List<String> KATEGORIEN =
            List.of("Plattform", "Docker", "Java", "NodeJS", "Python");

    private static final DateTimeFormatter STICHTAG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final AssessmentRepository assessmentRepository;
    private final ProductVersionRepository productVersionRepository;
    private final EnvironmentRepository environmentRepository;
    private final ContextProfileRepository profileRepository;
    private final RuleRepository ruleRepository;

    public HardeningReportDataLoader(
            AssessmentRepository assessmentRepository,
            ProductVersionRepository productVersionRepository,
            EnvironmentRepository environmentRepository,
            ContextProfileRepository profileRepository,
            RuleRepository ruleRepository) {
        this.assessmentRepository = assessmentRepository;
        this.productVersionRepository = productVersionRepository;
        this.environmentRepository = environmentRepository;
        this.profileRepository = profileRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public HardeningReportData load(HardeningReportInput input) {
        ProductVersion pv = productVersionRepository.findById(input.productVersionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekannte productVersionId: " + input.productVersionId()));
        Environment env = environmentRepository.findById(input.environmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekannte environmentId: " + input.environmentId()));
        Optional<ContextProfile> profil = profileRepository
                .findFirstByEnvironmentIdOrderByVersionNumberDesc(env.getId());

        List<Assessment> assessments = assessmentRepository
                .findeOffeneQueue(env.getId(), pv.getId(), null);
        // Zusaetzlich alle aktiven (nicht-superseded) Assessments fuer die
        // Kennzahlen. Da die vorhandenen Repo-Methoden kein direktes
        // "alle aktiven fuer prodVersion" liefern, kombinieren wir die
        // Queue-Methode (PROPOSED/NEEDS_REVIEW) mit einer zweiten
        // Selektion ueber findeAktiveIdsByEnvironmentAndSourceFields?
        // Fuer Iteration 10 reicht die Queue-Menge plus die bereits
        // approved-Eintraege, die wir ueber findAll + Filter ziehen.
        // Pragmatisch ohne zusaetzliche Queries: alles aus findAll.
        List<Assessment> aktive = assessmentRepository.findAll().stream()
                .filter(a -> a.getSupersededAt() == null)
                .filter(a -> Objects.equals(a.getProductVersion().getId(), pv.getId()))
                .filter(a -> Objects.equals(a.getEnvironment().getId(), env.getId()))
                .toList();

        HardeningReportData.Kopf kopf = buildKopf(input, pv, env, profil);
        List<HardeningReportData.KennzahlZeile> kennzahlen = buildKennzahlen(aktive);
        List<HardeningReportData.CveZeile> cveListe = buildCveListe(aktive);
        List<HardeningReportData.OffenerPunkt> offenePunkte =
                buildOffenePunkte(assessments);
        HardeningReportData.Anhang anhang = buildAnhang(profil, aktive);

        return new HardeningReportData(
                kopf,
                input.gesamteinstufung(),
                input.freigeberKommentar() == null ? "" : input.freigeberKommentar(),
                kennzahlen,
                cveListe,
                offenePunkte,
                anhang);
    }

    private HardeningReportData.Kopf buildKopf(
            HardeningReportInput input,
            ProductVersion pv,
            Environment env,
            Optional<ContextProfile> profil) {
        String profilVersion = profil
                .map(p -> "v" + p.getVersionNumber())
                .orElse("keine");
        Instant profilGueltig = profil.map(ContextProfile::getValidFrom).orElse(null);
        return new HardeningReportData.Kopf(
                pv.getProduct().getName(),
                pv.getVersion(),
                pv.getGitCommit() == null ? "-" : pv.getGitCommit(),
                env.getName(),
                env.getStage() == null ? "-" : env.getStage().name(),
                STICHTAG_FORMAT.format(input.stichtag()),
                input.erzeugtVon(),
                profilVersion,
                profilGueltig);
    }

    private List<HardeningReportData.KennzahlZeile> buildKennzahlen(
            List<Assessment> aktive) {
        Map<String, Map<AhsSeverity, Integer>> matrix = new LinkedHashMap<>();
        for (String kat : KATEGORIEN) {
            matrix.put(kat, new EnumMap<>(AhsSeverity.class));
        }
        for (Assessment a : aktive) {
            String kat = kategorieVon(a);
            AhsSeverity s = a.getSeverity();
            matrix.get(kat).merge(s, 1, Integer::sum);
        }
        List<HardeningReportData.KennzahlZeile> zeilen = new ArrayList<>();
        for (String kat : KATEGORIEN) {
            Map<AhsSeverity, Integer> row = matrix.get(kat);
            zeilen.add(new HardeningReportData.KennzahlZeile(
                    kat,
                    row.getOrDefault(AhsSeverity.CRITICAL, 0),
                    row.getOrDefault(AhsSeverity.HIGH, 0),
                    row.getOrDefault(AhsSeverity.MEDIUM, 0),
                    row.getOrDefault(AhsSeverity.LOW, 0),
                    row.getOrDefault(AhsSeverity.INFORMATIONAL, 0)));
        }
        return zeilen;
    }

    private List<HardeningReportData.CveZeile> buildCveListe(List<Assessment> aktive) {
        return aktive.stream()
                .sorted(Comparator
                        .comparing((Assessment a) -> kategorieVon(a))
                        .thenComparing(a -> a.getSeverity().ordinal())
                        .thenComparing(a -> a.getCve().getCveId()))
                .map(a -> new HardeningReportData.CveZeile(
                        kategorieVon(a),
                        a.getCve().getCveId(),
                        "https://nvd.nist.gov/vuln/detail/" + a.getCve().getCveId(),
                        a.getCve().getCvssBaseScore() == null
                                ? "-"
                                : a.getCve().getCvssBaseScore().toPlainString(),
                        a.getSeverity(),
                        geplanteBehebung(a),
                        a.getRationale() == null ? "" : a.getRationale()))
                .toList();
    }

    private List<HardeningReportData.OffenerPunkt> buildOffenePunkte(
            List<Assessment> offene) {
        return offene.stream()
                .filter(a -> a.getStatus() == AssessmentStatus.PROPOSED
                        || a.getStatus() == AssessmentStatus.NEEDS_REVIEW)
                .sorted(Comparator
                        .comparing((Assessment a) -> a.getSeverity().ordinal())
                        .thenComparing(a -> a.getCve().getCveId()))
                .map(a -> new HardeningReportData.OffenerPunkt(
                        a.getCve().getCveId(),
                        a.getSeverity(),
                        a.getStatus().name(),
                        a.getRationale() == null ? "" : a.getRationale()))
                .toList();
    }

    private HardeningReportData.Anhang buildAnhang(
            Optional<ContextProfile> profil, List<Assessment> aktive) {
        String yamlAuszug = profil.map(ContextProfile::getYamlSource).orElse("");
        Map<UUID, Long> regelTreffer = aktive.stream()
                .filter(a -> a.getProposalSource()
                        == com.ahs.cvm.domain.enums.ProposalSource.RULE)
                .filter(a -> a.getAiSuggestionId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Assessment::getAiSuggestionId,
                        java.util.stream.Collectors.counting()));

        List<HardeningReportData.VerwendeteRegel> regeln = ruleRepository
                .findByStatusOrderByCreatedAtDesc(RuleStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(Rule::getRuleKey))
                .map(r -> new HardeningReportData.VerwendeteRegel(
                        r.getRuleKey(),
                        r.getName(),
                        r.getProposedSeverity(),
                        regelTreffer.getOrDefault(r.getId(), 0L).intValue()))
                .toList();
        return new HardeningReportData.Anhang(
                yamlAuszug,
                regeln,
                Map.of("vex", "Platzhalter - Ausgabe folgt in Iteration 20."));
    }

    static String kategorieVon(Assessment a) {
        String purl = Optional.ofNullable(a.getFinding())
                .map(f -> f.getComponentOccurrence())
                .map(c -> c.getComponent())
                .map(c -> c.getPurl())
                .orElse("");
        return kategorieFuerPurl(purl);
    }

    static String kategorieFuerPurl(String purl) {
        if (purl == null) {
            return "Plattform";
        }
        if (purl.startsWith("pkg:maven/") || purl.startsWith("pkg:gradle/")) {
            return "Java";
        }
        if (purl.startsWith("pkg:npm/")) {
            return "NodeJS";
        }
        if (purl.startsWith("pkg:pypi/")) {
            return "Python";
        }
        if (purl.startsWith("pkg:docker/") || purl.startsWith("pkg:oci/")) {
            return "Docker";
        }
        return "Plattform";
    }

    private static String geplanteBehebung(Assessment a) {
        if (a.getFinding() != null && a.getFinding().getFixedInVersion() != null) {
            return "Update auf " + a.getFinding().getFixedInVersion();
        }
        return switch (a.getStatus()) {
            case APPROVED -> "freigegeben (Bewertung gueltig)";
            case REJECTED -> "abgelehnt";
            case EXPIRED -> "abgelaufen - Re-Bewertung noetig";
            case NEEDS_REVIEW -> "wartet auf Re-Review";
            case NEEDS_VERIFICATION -> "wartet auf Verifikation (KI-Halluzinations-Check)";
            case PROPOSED -> "offen";
            case SUPERSEDED -> "ueberholt";
        };
    }
}
