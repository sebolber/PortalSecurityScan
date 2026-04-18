package com.ahs.cvm.persistence.branding;

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

/**
 * Historien-Snapshot einer {@link BrandingConfig}-Zeile (Iteration 31, CVM-72).
 *
 * <p>Eine Zeile wird vor jedem {@code update} in {@code BrandingService}
 * geschrieben und enthaelt den Zustand <em>vor</em> der Aenderung. Damit
 * kann ein Rollback auf eine beliebige frueherer Version erfolgen.
 */
@Entity
@Table(name = "branding_config_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BrandingConfigHistory {

    @Id
    @Column(name = "history_id", nullable = false, updatable = false)
    private UUID historyId;

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

    @Column(name = "version", nullable = false, updatable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, updatable = false)
    private String updatedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, updatable = false)
    private String recordedBy;

    @PrePersist
    void init() {
        if (historyId == null) {
            historyId = UUID.randomUUID();
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
