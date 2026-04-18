package com.ahs.cvm.llm.rate;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.llm.LlmGatewayConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmRateLimiterTest {

    @Test
    @DisplayName("RateLimiter: tenant-per-minute wirkt und liefert false nach Quota")
    void tenantQuota() {
        LlmGatewayConfig config = new LlmGatewayConfig(true, "warn", "m", 1000, 2);
        LlmRateLimiter limiter = new LlmRateLimiter(config);

        assertThat(limiter.tryAcquire("tenantA")).isTrue();
        assertThat(limiter.tryAcquire("tenantA")).isTrue();
        assertThat(limiter.tryAcquire("tenantA")).isFalse();

        // Anderer Mandant hat eigenes Bucket.
        assertThat(limiter.tryAcquire("tenantB")).isTrue();
    }

    @Test
    @DisplayName("RateLimiter: globale Quota gilt fuer alle Mandanten")
    void globalQuota() {
        LlmGatewayConfig config = new LlmGatewayConfig(true, "warn", "m", 1, 100);
        LlmRateLimiter limiter = new LlmRateLimiter(config);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isFalse();
    }

    @Test
    @DisplayName("RateLimiter: ohne Tenant wird nur global gezaehlt")
    void ohneTenant() {
        LlmGatewayConfig config = new LlmGatewayConfig(true, "warn", "m", 1, 1);
        LlmRateLimiter limiter = new LlmRateLimiter(config);
        assertThat(limiter.tryAcquire(null)).isTrue();
        assertThat(limiter.tryAcquire(null)).isFalse();
    }
}
