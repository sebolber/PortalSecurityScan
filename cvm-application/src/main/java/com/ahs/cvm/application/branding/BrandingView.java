package com.ahs.cvm.application.branding;

/**
 * Lese-Projektion der Branding-Konfiguration fuer das Frontend
 * (Iteration 27, CVM-61).
 *
 * <p>Enthaelt bewusst keine Mandanten-UUID oder Audit-Felder,
 * da der Endpunkt {@code GET /api/v1/theme} den Mandanten bereits
 * aus dem JWT ableitet.
 */
public record BrandingView(
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
        int version) {}
