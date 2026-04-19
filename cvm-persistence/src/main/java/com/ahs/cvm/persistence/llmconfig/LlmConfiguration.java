package com.ahs.cvm.persistence.llmconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin-verwaltete LLM-Konfiguration pro Mandant (Iteration 34, CVM-78).
 *
 * <p>Schlank und unabhaengig vom bestehenden {@code LlmModelProfile},
 * das Governance-Pflichten (GKV-Freigabe, Budget, Vier-Augen) abbildet.
 * Diese Entity beschreibt nur, wie sich das CVM gegen einen LLM-
 * Anbieter authentifiziert. Die Anbindung an den LlmGateway erfolgt in
 * Iteration 35.
 *
 * <p>{@code secretRef} enthaelt bereits den AES-GCM-Ciphertext als
 * Base64-String. Der Service verschluesselt/entschluesselt - die
 * Entity kennt den Klartext nie.
 */
@Entity
@Table(name = "llm_configuration")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmConfiguration {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 255)
    private String model;

    @Column(name = "base_url", length = 2048)
    private String baseUrl;

    /**
     * Verschluesselter API-Key (Base64-AES-GCM-Ciphertext).
     * Der Entity-Code kennt den Klartext nicht.
     */
    @Column(name = "secret_ref", columnDefinition = "text")
    private String secretRef;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "temperature", precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    void initialisiere() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void zeitstempeln() {
        updatedAt = Instant.now();
    }
}
