package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.domain.enums.ProfileState;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleDryRunResult;
import com.ahs.cvm.persistence.rule.RuleDryRunResultRepository;
import com.ahs.cvm.persistence.rule.RuleRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.Scan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DryRunTest {

    private final FindingRepository findingRepository = mock(FindingRepository.class);
    private final AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final RuleDryRunResultRepository dryRunResultRepository =
            mock(RuleDryRunResultRepository.class);
    private final ContextProfileService profileService = mock(ContextProfileService.class);

    private final ConditionParser parser = new ConditionParser();
    private final RuleEvaluator evaluator = new RuleEvaluator();

    @Test
    @DisplayName("DryRun: Regel matcht 14 von 20 Findings, Coverage wird korrekt berechnet")
    void coverageKorrekt() {
        UUID ruleId = UUID.randomUUID();
        Rule rule = Rule.builder()
                .id(ruleId)
                .ruleKey("kev-linux-only")
                .name("KEV auf Linux")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.HIGH)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}")
                .rationaleTemplate("KEV-Eintrag")
                .build();
        given(ruleRepository.findById(ruleId)).willReturn(Optional.of(rule));

        // 20 synthetische Findings: 14 haben kev=true, 6 haben kev=false.
        List<Finding> findings = IntStream.range(0, 20)
                .mapToObj(i -> buildFinding(i < 14))
                .toList();
        given(findingRepository.findByDetectedAtBetween(any(), any())).willReturn(findings);
        given(profileService.latestActiveFor(any())).willReturn(Optional.of(
                new ProfileView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        ProfileState.ACTIVE,
                        "schemaVersion: 1\numgebung:\n  key: REF-TEST\n  stage: REF\n",
                        null, null, null, Instant.now())));
        given(assessmentRepository.findFirstByFindingIdOrderByVersionDesc(any()))
                .willReturn(Optional.empty());
        given(dryRunResultRepository.save(any(RuleDryRunResult.class)))
                .willAnswer(inv -> inv.getArgument(0));

        DryRunService dryRunService = new DryRunService(
                ruleRepository, findingRepository, assessmentRepository,
                dryRunResultRepository, profileService, parser, evaluator);

        DryRunResult result = dryRunService.dryRun(ruleId, 180);

        assertThat(result.totalFindings()).isEqualTo(20);
        assertThat(result.matchedFindings()).isEqualTo(14);
        assertThat(result.matchedAlreadyApproved()).isEqualTo(0);
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    @DisplayName("DryRun: Konflikt, wenn Regel HIGH vorschlaegt, aber vorhandenes APPROVED MEDIUM ist")
    void konfliktWirdErkannt() {
        UUID ruleId = UUID.randomUUID();
        Rule rule = Rule.builder()
                .id(ruleId)
                .ruleKey("kev-high")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.HIGH)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}")
                .rationaleTemplate("r")
                .build();
        given(ruleRepository.findById(ruleId)).willReturn(Optional.of(rule));

        Finding f = buildFinding(true);
        given(findingRepository.findByDetectedAtBetween(any(), any())).willReturn(List.of(f));
        given(profileService.latestActiveFor(any())).willReturn(Optional.of(
                new ProfileView(
                        UUID.randomUUID(), UUID.randomUUID(), 1, ProfileState.ACTIVE,
                        "schemaVersion: 1\numgebung:\n  key: REF-TEST\n  stage: REF\n",
                        null, null, null, Instant.now())));

        Assessment approved = Assessment.builder()
                .id(UUID.randomUUID())
                .status(AssessmentStatus.APPROVED)
                .severity(AhsSeverity.MEDIUM)
                .proposalSource(ProposalSource.HUMAN)
                .build();
        given(assessmentRepository.findFirstByFindingIdOrderByVersionDesc(any()))
                .willReturn(Optional.of(approved));
        given(dryRunResultRepository.save(any(RuleDryRunResult.class)))
                .willAnswer(inv -> inv.getArgument(0));

        DryRunService dryRunService = new DryRunService(
                ruleRepository, findingRepository, assessmentRepository,
                dryRunResultRepository, profileService, parser, evaluator);

        DryRunResult result = dryRunService.dryRun(ruleId, 90);

        assertThat(result.matchedFindings()).isEqualTo(1);
        assertThat(result.matchedAlreadyApproved()).isEqualTo(1);
        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).approvedSeverity()).isEqualTo(AhsSeverity.MEDIUM);
        assertThat(result.conflicts().get(0).ruleSeverity()).isEqualTo(AhsSeverity.HIGH);
    }

    private Finding buildFinding(boolean kev) {
        Environment env = Environment.builder()
                .key("REF-TEST").name("Ref").stage(EnvironmentStage.REF).build();
        env.setId(UUID.randomUUID());
        Scan scan = Scan.builder()
                .environment(env)
                .sbomFormat("CycloneDX")
                .sbomChecksum("x")
                .contentSha256("y")
                .scannedAt(Instant.now())
                .build();
        scan.setId(UUID.randomUUID());
        Component comp = Component.builder()
                .purl("pkg:maven/x/y@1.0").name("y").version("1.0").type("maven").build();
        comp.setId(UUID.randomUUID());
        ComponentOccurrence occ = ComponentOccurrence.builder()
                .scan(scan).component(comp).direct(true).build();
        occ.setId(UUID.randomUUID());
        Cve cve = Cve.builder()
                .cveId("CVE-2017-" + UUID.randomUUID().toString().substring(0, 4))
                .summary("dummy")
                .kevListed(kev)
                .epssScore(new BigDecimal("0.1"))
                .source("NVD")
                .build();
        cve.setId(UUID.randomUUID());
        Finding f = Finding.builder()
                .scan(scan)
                .componentOccurrence(occ)
                .cve(cve)
                .detectedAt(Instant.now())
                .build();
        f.setId(UUID.randomUUID());
        return f;
    }
}
