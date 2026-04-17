package com.ahs.cvm.persistence.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findFirstByFindingIdOrderByVersionDesc(UUID findingId);

    List<Assessment> findByFindingIdOrderByVersionAsc(UUID findingId);

    List<Assessment>
            findByCveIdAndProductVersionIdAndEnvironmentIdAndStatusAndSupersededAtIsNull(
                    UUID cveId, UUID productVersionId, UUID environmentId,
                    AssessmentStatus status);

    /**
     * Liefert alle aktiven Assessments (nicht superseded), deren
     * {@code rationaleSourceFields} mindestens einen der angegebenen
     * Profilpfade enthaelt. Treffer sind Kandidaten fuer einen
     * {@code NEEDS_REVIEW}-Ruecksprung.
     */
    @Query(value =
            "SELECT a.id FROM assessment a "
            + "WHERE a.superseded_at IS NULL "
            + "  AND a.environment_id = :environmentId "
            + "  AND a.rationale_source_fields ?| CAST(:paths AS text[])",
            nativeQuery = true)
    List<UUID> findAktiveIdsByEnvironmentAndSourceFields(
            @Param("environmentId") UUID environmentId,
            @Param("paths") String[] paths);

    /**
     * Batch-Update: setzt {@code status=NEEDS_REVIEW} und den Ausloeser
     * ({@code review_triggered_by_profile_version}) fuer alle Assessments
     * der uebergebenen IDs. Umgeht bewusst den
     * {@link AssessmentImmutabilityListener}, weil dies keine fachliche
     * Aenderung darstellt, sondern eine auditierbare System-Transition.
     */
    @Modifying
    @Query("UPDATE Assessment a "
            + "SET a.status = com.ahs.cvm.domain.enums.AssessmentStatus.NEEDS_REVIEW, "
            + "    a.reviewTriggeredByProfileVersion = :profileVersionId "
            + "WHERE a.id IN :ids")
    int markiereAlsReview(
            @Param("ids") Collection<UUID> ids,
            @Param("profileVersionId") UUID profileVersionId);

    /**
     * Queue-Query: offene Assessments je Umgebung. Die Filter auf
     * {@code productVersionId} und {@code source} sind optional
     * ({@code null} = keine Einschraenkung).
     */
    @Query("SELECT a FROM Assessment a "
            + "WHERE a.supersededAt IS NULL "
            + "  AND a.status IN (com.ahs.cvm.domain.enums.AssessmentStatus.PROPOSED, "
            + "                   com.ahs.cvm.domain.enums.AssessmentStatus.NEEDS_REVIEW) "
            + "  AND (:environmentId IS NULL OR a.environment.id = :environmentId) "
            + "  AND (:productVersionId IS NULL OR a.productVersion.id = :productVersionId) "
            + "  AND (:source IS NULL OR a.proposalSource = :source) "
            + "ORDER BY a.severity ASC, a.createdAt ASC")
    List<Assessment> findeOffeneQueue(
            @Param("environmentId") UUID environmentId,
            @Param("productVersionId") UUID productVersionId,
            @Param("source") ProposalSource source);

    /**
     * Findet APPROVED-Assessments, deren {@code validUntil} vor
     * {@code grenze} liegt und die noch nicht {@code supersededAt} sind.
     * Basis fuer den Expiry-Scheduler.
     */
    List<Assessment>
            findByStatusAndValidUntilLessThanEqualAndSupersededAtIsNull(
                    AssessmentStatus status, Instant grenze);

    /**
     * Batch-Update: setzt {@code status=EXPIRED} ohne den
     * {@link AssessmentImmutabilityListener} zu triggern. Analog zum
     * {@code NEEDS_REVIEW}-Fall in {@link #markiereAlsReview}.
     */
    @Modifying
    @Query("UPDATE Assessment a "
            + "SET a.status = com.ahs.cvm.domain.enums.AssessmentStatus.EXPIRED, "
            + "    a.updatedAt = :zeitpunkt "
            + "WHERE a.id IN :ids")
    int markiereAlsAbgelaufen(
            @Param("ids") Collection<UUID> ids,
            @Param("zeitpunkt") Instant zeitpunkt);
}
