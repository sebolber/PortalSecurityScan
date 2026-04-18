package com.ahs.cvm.application.branding;

/**
 * Befehls-DTO fuer {@code PUT /api/v1/admin/theme} (Iteration 27).
 *
 * <p>Optimistisches Locking ueber {@code version}; das Frontend
 * schickt den zuletzt gelesenen Stand mit.
 */
public record BrandingUpdateCommand(
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
        int expectedVersion) {}
