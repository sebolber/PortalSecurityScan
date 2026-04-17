package com.ahs.cvm.integration.feed;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Pro Feed ein Token-Bucket. Beispiel NVD: 50 Requests pro 30 Sekunden
 * mit API-Key (Default in {@link FeedProperties}). Die Aufrufer rufen
 * {@link #acquire(FeedSource)} und erhalten blockierend einen Token.
 */
@Component
public class FeedRateLimiter {

    private final Map<FeedSource, Bucket> buckets = new EnumMap<>(FeedSource.class);

    public FeedRateLimiter(FeedProperties properties) {
        buckets.put(FeedSource.NVD, erzeuge(properties.getNvd()));
        buckets.put(FeedSource.GHSA, erzeuge(properties.getGhsa()));
        buckets.put(FeedSource.KEV, erzeuge(properties.getKev()));
        buckets.put(FeedSource.EPSS, erzeuge(properties.getEpss()));
    }

    private static Bucket erzeuge(FeedProperties.FeedConfig config) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(config.getRequestsPerWindow())
                        .refillGreedy(
                                config.getRequestsPerWindow(),
                                Duration.ofSeconds(config.getWindowSeconds()))
                        .build())
                .build();
    }

    public void acquire(FeedSource source) {
        try {
            buckets.get(source).asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FeedClientException(source, "Rate-Limiter unterbrochen", e);
        }
    }
}
