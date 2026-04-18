package com.ahs.cvm.ai.fixverify;

import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ergebnis einer Fix-Verifikation (Iteration 16, CVM-41).
 *
 * @param mitigationId Bezugs-Mitigation.
 * @param suggestionId persistierter {@code ai_suggestion}-Eintrag.
 * @param grade        Grade A/B/C bzw. UNKNOWN.
 * @param evidenceType Evidenz-Typ (CVE-Mention, Commit-Match, Inferred, None).
 * @param confidence   Modell-Confidence [0..1].
 * @param addressedBy  Liste der Evidenz-Commits.
 * @param caveats      Hinweise des Modells (z.&nbsp;B. ungewisse Quellen).
 * @param verifiedAt   Zeitstempel der Verifikation.
 * @param available    {@code false}, wenn Verifikation nicht moeglich war
 *                     (Provider 404, LLM-Fehler).
 * @param note         Hinweistext bei {@code !available}.
 */
public record FixVerificationResult(
        UUID mitigationId,
        UUID suggestionId,
        FixVerificationGrade grade,
        FixEvidenceType evidenceType,
        BigDecimal confidence,
        List<CommitEvidence> addressedBy,
        List<String> caveats,
        Instant verifiedAt,
        boolean available,
        String note) {

    public FixVerificationResult {
        addressedBy = addressedBy == null ? List.of() : List.copyOf(addressedBy);
        caveats = caveats == null ? List.of() : List.copyOf(caveats);
    }

    public record CommitEvidence(String commit, String message, String url) {}
}
