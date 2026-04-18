package com.ahs.cvm.persistence.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/**
 * Quellreferenz zu einem {@link AiSuggestion}. Erlaubt Nachvollziehbarkeit
 * ("auf welchen Profil-Pfad stuetzt sich der Vorschlag?"), Konzept 4.4.
 *
 * <p>{@code kind} ist eine von {@code PROFILE_PATH}, {@code CVE},
 * {@code RULE}, {@code DOCUMENT}, {@code CODE_REF}. DB-seitig als
 * Check-Constraint erzwungen.
 */
@Entity
@Table(name = "ai_source_ref")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiSourceRef {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_suggestion_id", nullable = false, updatable = false)
    private AiSuggestion aiSuggestion;

    @Column(name = "kind", nullable = false, updatable = false)
    private String kind;

    @Column(name = "reference", nullable = false, updatable = false)
    private String reference;

    @Column(name = "excerpt", updatable = false, columnDefinition = "text")
    private String excerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
