package com.ahs.cvm.persistence.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;
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

/**
 * Mandantenspezifischer Systemparameter (Vorlage: PortalCore
 * {@code PortalParameter}).
 *
 * <p>Der Schluessel ist eindeutig pro Mandant. Das Feld {@code value}
 * traegt den aktuell gueltigen Wert; {@code defaultValue} bleibt fuer
 * Reset-Operationen erhalten. Sensitive Werte (z.B. Tokens) werden im
 * Service maskiert ausgeliefert - die Entity selbst kennt nur die
 * Markierung {@code sensitive}.
 */
@Entity
@Table(name = "system_parameter")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemParameter {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "param_key", nullable = false, length = 255)
    private String paramKey;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "handbook", columnDefinition = "text")
    private String handbook;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private SystemParameterType type;

    @Column(name = "value", columnDefinition = "text")
    private String value;

    @Column(name = "default_value", columnDefinition = "text")
    private String defaultValue;

    @Column(name = "required", nullable = false)
    private boolean required;

    /**
     * Optional zusaetzliche Validierung als JSON-String, vom
     * Service interpretiert. Beispielformat:
     * {@code {"regex":"^[A-Z]+$","min":1,"max":100} }.
     */
    @Column(name = "validation_rules", columnDefinition = "text")
    private String validationRules;

    /**
     * Erlaubte Werte fuer SELECT/MULTISELECT (kommagetrennt) oder
     * JSON-Array.
     */
    @Column(name = "options", columnDefinition = "text")
    private String options;

    @Column(name = "unit", length = 64)
    private String unit;

    @Column(name = "sensitive", nullable = false)
    private boolean sensitive;

    @Column(name = "hot_reload", nullable = false)
    private boolean hotReload;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "admin_only", nullable = false)
    private boolean adminOnly;

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
