package com.ahs.cvm.application.waiver;

import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.waiver.Waiver;
import java.time.Instant;
import java.util.UUID;

/** Read-Model fuer Waiver (haelt Controller frei von JPA-Entities). */
public record WaiverView(
        UUID id,
        UUID assessmentId,
        String reason,
        String grantedBy,
        Instant validUntil,
        int reviewIntervalDays,
        WaiverStatus status,
        Instant createdAt,
        Instant extendedAt,
        Instant revokedAt) {

    public static WaiverView from(Waiver w) {
        return new WaiverView(
                w.getId(),
                w.getAssessment() == null ? null : w.getAssessment().getId(),
                w.getReason(), w.getGrantedBy(), w.getValidUntil(),
                w.getReviewIntervalDays() == null ? 90 : w.getReviewIntervalDays(),
                w.getStatus(), w.getCreatedAt(),
                w.getExtendedAt(), w.getRevokedAt());
    }
}
