package com.ahs.cvm.persistence.environment;

import com.ahs.cvm.domain.enums.EnvironmentStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "environment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Environment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "key", nullable = false, unique = true)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private EnvironmentStage stage;

    @Column(name = "tenant")
    private String tenant;

    @Column(name = "llm_model_profile_id")
    private UUID llmModelProfileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Soft-Delete-Marker (Iteration 48, CVM-98). {@code null} = aktiv;
     * gesetzt = logisch entfernt.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void aktualisiereZeitstempel() {
        updatedAt = Instant.now();
    }
}
