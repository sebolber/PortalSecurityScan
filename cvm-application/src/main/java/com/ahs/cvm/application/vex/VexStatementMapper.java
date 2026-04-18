package com.ahs.cvm.application.vex;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Mappt ein {@link Assessment} (plus zugehoerige
 * {@link MitigationPlan}) auf eine {@link VexStatement}-Zeile
 * (Iteration 20, CVM-51).
 */
@Component
public class VexStatementMapper {

    public VexStatement toStatement(Assessment a, List<MitigationPlan> plans) {
        VexStatus status = resolveStatus(a, plans);
        String justification = resolveJustification(a, status);
        String purl = a.getFinding() != null
                && a.getFinding().getComponentOccurrence() != null
                && a.getFinding().getComponentOccurrence().getComponent() != null
                ? a.getFinding().getComponentOccurrence().getComponent().getPurl()
                : null;
        return new VexStatement(
                a.getCve() == null ? null : a.getCve().getCveId(),
                purl,
                status,
                justification,
                a.getRationale() == null ? "" : a.getRationale(),
                a.getSeverity(),
                List.of());
    }

    static VexStatus resolveStatus(Assessment a, List<MitigationPlan> plans) {
        if (a.getStatus() == AssessmentStatus.PROPOSED
                || a.getStatus() == AssessmentStatus.NEEDS_REVIEW
                || a.getStatus() == AssessmentStatus.NEEDS_VERIFICATION) {
            return VexStatus.UNDER_INVESTIGATION;
        }
        if (a.getSeverity() == AhsSeverity.NOT_APPLICABLE) {
            return VexStatus.NOT_AFFECTED;
        }
        Optional<MitigationPlan> upgrade = plans.stream()
                .filter(p -> p.getStrategy() == MitigationStrategy.UPGRADE
                        || p.getStrategy() == MitigationStrategy.PATCH)
                .findFirst();
        if (upgrade.isPresent()
                && upgrade.get().getImplementedAt() != null) {
            return VexStatus.FIXED;
        }
        return VexStatus.AFFECTED;
    }

    static String resolveJustification(Assessment a, VexStatus status) {
        if (status != VexStatus.NOT_AFFECTED) {
            return null;
        }
        List<String> fields = a.getRationaleSourceFields() == null
                ? List.of() : a.getRationaleSourceFields();
        if (fields.stream().anyMatch(f -> f.contains("not_present")
                || f.contains("component.not_present"))) {
            return "component_not_present";
        }
        if (fields.stream().anyMatch(f -> f.contains("execute_path")
                || f.contains("unreachable"))) {
            return "vulnerable_code_not_in_execute_path";
        }
        if (fields.stream().anyMatch(f -> f.contains("mitigation")
                || f.contains("workaround"))) {
            return "inline_mitigations_already_exist";
        }
        return "vulnerable_code_cannot_be_controlled_by_adversary";
    }
}
