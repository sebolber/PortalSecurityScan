package com.ahs.cvm.integration.feed;

/**
 * Herkunft eines angereicherten CVE-Datensatzes. Wird auch fuer
 * Metriken- und Feature-Flag-Schluessel verwendet.
 */
public enum FeedSource {
    NVD,
    GHSA,
    KEV,
    EPSS
}
