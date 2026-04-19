package com.ahs.cvm.persistence.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Iteration 62A (CVM-62): Mandanten-Zuordnung. Pflicht seit V0035.
     * Der Key ist nur innerhalb eines Mandanten eindeutig.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Soft-Delete-Marker (Iteration 38, CVM-82). {@code null} =
     * aktiv; gesetzt = logisch entfernt. Scans und Findings bleiben
     * erhalten, das Produkt verschwindet aber aus der Admin-UI.
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
