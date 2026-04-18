package com.ahs.cvm.integration.osv;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config-Binding fuer den OSV-Adapter (Iteration 33, CVM-77).
 *
 * <pre>
 * cvm:
 *   enrichment:
 *     osv:
 *       enabled: false
 *       base-url: https://api.osv.dev
 *       batch-size: 500
 *       timeout-ms: 15000
 * </pre>
 */
@ConfigurationProperties(prefix = "cvm.enrichment.osv")
public class OsvProperties {

    /** Feature-Flag. Default {@code false}, damit offline-Setups nichts tun. */
    private boolean enabled = false;

    /** Basis-URL des OSV-Endpoints. */
    private String baseUrl = "https://api.osv.dev";

    /** OSV erlaubt bis 1000 Queries pro Batch-Request; wir bleiben darunter. */
    private int batchSize = 500;

    /** HTTP-Timeout in Millisekunden. */
    private int timeoutMs = 15_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
