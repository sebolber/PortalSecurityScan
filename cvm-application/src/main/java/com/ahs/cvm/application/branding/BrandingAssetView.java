package com.ahs.cvm.application.branding;

import java.util.UUID;

/**
 * Lese-Projektion eines Branding-Assets (Iteration 27b, CVM-62).
 * Fuer Metadaten-Response im Upload-Handler und als Rueckgabe
 * fuer den Download-Endpunkt.
 */
public record BrandingAssetView(
        UUID id,
        UUID tenantId,
        String kind,
        String contentType,
        int sizeBytes,
        String sha256,
        byte[] bytes) {}
