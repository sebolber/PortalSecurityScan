package com.ahs.cvm.application.cve;

import java.util.UUID;

/**
 * Wird vom {@link ComponentCveMatchingOnScanIngestedListener} publiziert,
 * nachdem die OSV-Findings persistiert sind (Iteration 33, CVM-77).
 *
 * <p>Der {@code FindingsCreatedListener} horcht auf
 * {@link com.ahs.cvm.application.scan.ScanIngestedEvent} **und** auf
 * dieses Event. {@code ScanIngestedEvent} feuert am Ende der Ingestion,
 * wenn die SBOM selbst Vulnerabilities mitbringt. Dieses Event hier
 * schliesst die Luecke fuer den typischen CI-Fall (reine Komponenten-
 * SBOMs aus {@code cyclonedx-maven-plugin} / {@code cyclonedx-npm}),
 * bei denen Findings erst durch OSV entstehen - parallel zu den
 * anderen Listenern.
 */
public record ComponentMatchedFindingsEvent(
        UUID scanId,
        UUID productVersionId,
        UUID environmentId,
        int neueFindings) {}
