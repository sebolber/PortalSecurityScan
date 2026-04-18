package com.ahs.cvm.application.pipeline;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rate-Limiter fuer den {@code /api/v1/pipeline/gate}-Endpunkt
 * (Go-Live-Nachzug zu Iteration 21, CVM-52).
 *
 * <p>Pro Produkt-Version ein Bucket4j-Bucket. Standard: 20 Calls pro
 * Minute. Per {@code cvm.pipeline.gate.per-minute} konfigurierbar.
 */
@Component
public class PipelineGateRateLimiter {

    private final ConcurrentMap<String, Bucket> perKey = new ConcurrentHashMap<>();
    private final int perMinute;

    public PipelineGateRateLimiter(
            @Value("${cvm.pipeline.gate.per-minute:20}") int perMinute) {
        this.perMinute = perMinute <= 0 ? 20 : perMinute;
    }

    /**
     * Versucht, einen Token fuer den gegebenen Schluessel zu ziehen.
     * @param key typischerweise {@code productVersionId}. {@code null}
     *            =&gt; erlaubt (z.&nbsp;B. Smoke-Tests).
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        Bucket bucket = perKey.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(perMinute, Duration.ofMinutes(1)))
                .build());
        return bucket.tryConsume(1);
    }
}
