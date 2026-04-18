package com.ahs.cvm.llm.rate;

import com.ahs.cvm.llm.LlmGatewayConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Rate-Limiter fuer LLM-Calls. Zwei Ebenen:
 *
 * <ol>
 *   <li>Globales Bucket fuer alle Calls im System.</li>
 *   <li>Pro-Mandant-Bucket (Tenant-Id als Schluessel).</li>
 * </ol>
 *
 * <p>Bei Ueberschreitung liefert {@link #tryAcquire(String)}
 * {@code false}. Der {@link com.ahs.cvm.llm.AiCallAuditService} setzt
 * dann den Audit auf {@link com.ahs.cvm.domain.enums.AiCallStatus#RATE_LIMITED}.
 */
@Component
public class LlmRateLimiter {

    private final Bucket global;
    private final ConcurrentMap<String, Bucket> perTenant = new ConcurrentHashMap<>();
    private final int tenantPerMinute;

    public LlmRateLimiter(LlmGatewayConfig config) {
        this.global = Bucket.builder()
                .addLimit(Bandwidth.simple(config.globalPerMinute(), Duration.ofMinutes(1)))
                .build();
        this.tenantPerMinute = config.tenantPerMinute();
    }

    /**
     * Versucht, 1 Token im globalen und im Tenant-Bucket zu belegen.
     * @param tenant Mandanten-Id. {@code null} -> nur globales Bucket.
     */
    public boolean tryAcquire(String tenant) {
        if (!global.tryConsume(1)) {
            return false;
        }
        if (tenant == null || tenant.isBlank()) {
            return true;
        }
        Bucket tenantBucket = perTenant.computeIfAbsent(tenant, t -> Bucket.builder()
                .addLimit(Bandwidth.simple(tenantPerMinute, Duration.ofMinutes(1)))
                .build());
        return tenantBucket.tryConsume(1);
    }
}
