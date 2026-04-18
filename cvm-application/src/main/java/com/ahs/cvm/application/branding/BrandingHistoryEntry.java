package com.ahs.cvm.application.branding;

import java.time.Instant;

/**
 * Historien-Eintrag (Iteration 31, CVM-72). Liefert pro Aenderung
 * den Zustand <em>vor</em> dem Update sowie die Audit-Felder
 * ({@code recordedAt}, {@code recordedBy}), die der
 * {@link BrandingService} setzt.
 */
public record BrandingHistoryEntry(
        int version,
        String primaryColor,
        String primaryContrastColor,
        String accentColor,
        String fontFamilyName,
        String fontFamilyMonoName,
        String appTitle,
        String logoUrl,
        String logoAltText,
        String faviconUrl,
        String fontFamilyHref,
        Instant updatedAt,
        String updatedBy,
        Instant recordedAt,
        String recordedBy) {}
