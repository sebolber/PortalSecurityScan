package com.ahs.cvm.integration.feed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Zentrale Metriken fuer alle Feed-Adapter. Pro Quelle werden die drei
 * Kernzahlen "requests", "errors" und "latency" gefuehrt.
 */
@Component
public class FeedMetrics {

    private final Map<FeedSource, Counter> requests = new EnumMap<>(FeedSource.class);
    private final Map<FeedSource, Counter> errors = new EnumMap<>(FeedSource.class);
    private final Map<FeedSource, Timer> latency = new EnumMap<>(FeedSource.class);

    public FeedMetrics(MeterRegistry registry) {
        for (FeedSource source : FeedSource.values()) {
            requests.put(source, Counter.builder("cvm.feed.requests")
                    .tag("source", source.name())
                    .register(registry));
            errors.put(source, Counter.builder("cvm.feed.errors")
                    .tag("source", source.name())
                    .register(registry));
            latency.put(source, Timer.builder("cvm.feed.latency")
                    .tag("source", source.name())
                    .register(registry));
        }
    }

    public <T> T messe(FeedSource source, Supplier<T> ausfuehrung) {
        requests.get(source).increment();
        try {
            return latency.get(source).recordCallable(ausfuehrung::get);
        } catch (RuntimeException e) {
            errors.get(source).increment();
            throw e;
        } catch (Exception e) {
            errors.get(source).increment();
            throw new FeedClientException(source, "Unerwarteter Fehler", e);
        }
    }
}
