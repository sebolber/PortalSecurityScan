package com.ahs.cvm.persistence.modelprofile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "model_profile_change_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelProfileChangeLog {

    public enum Action { PROFILE_SWITCHED, PROFILE_CREATED }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "environment_id")
    private UUID environmentId;

    @Column(name = "previous_profile_id")
    private UUID previousProfileId;

    @Column(name = "new_profile_id", nullable = false)
    private UUID newProfileId;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "four_eyes_confirmer", nullable = false)
    private String fourEyesConfirmer;

    @Column(name = "reason")
    private String reason;

    @Column(name = "action", nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Action action;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
        if (action == null) {
            action = Action.PROFILE_SWITCHED;
        }
    }
}
