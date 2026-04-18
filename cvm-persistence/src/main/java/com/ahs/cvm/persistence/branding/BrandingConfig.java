package com.ahs.cvm.persistence.branding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Branding-Konfiguration eines Mandanten (Iteration 27, CVM-61).
 *
 * <p>Eine Zeile pro Mandant. Beinhaltet die zentralen
 * Farb-/Logo-/Schrift-Entscheidungen, die der Angular-Frontend-
 * {@code ThemeService} beim Start abholt und als CSS-Custom-
 * Properties setzt. Die optimistische Sperre ueber {@code version}
 * verhindert verlorene Updates bei parallelen Admin-Sitzungen.
 */
@Entity
@Table(name = "branding_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BrandingConfig {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "primary_color", nullable = false)
    private String primaryColor;

    @Column(name = "primary_contrast_color", nullable = false)
    private String primaryContrastColor;

    @Column(name = "accent_color")
    private String accentColor;

    @Column(name = "font_family_name", nullable = false)
    private String fontFamilyName;

    @Column(name = "font_family_mono_name")
    private String fontFamilyMonoName;

    @Column(name = "app_title")
    private String appTitle;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_alt_text")
    private String logoAltText;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "font_family_href")
    private String fontFamilyHref;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @PrePersist
    void initialisiere() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    void aktualisiere() {
        updatedAt = Instant.now();
    }
}
