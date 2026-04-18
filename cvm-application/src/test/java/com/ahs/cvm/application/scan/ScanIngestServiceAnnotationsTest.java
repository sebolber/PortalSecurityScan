package com.ahs.cvm.application.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reflection-Guard: {@link ScanIngestService#verarbeiteAsync} muss
 * sowohl {@code @Async} als auch {@code @Transactional} tragen.
 *
 * <p>Hintergrund: Der eigentliche Persist-Schritt laeuft zwar in
 * {@code persistiere()} bereits in einer Transaktion - das Problem
 * ist aber {@code eventPublisher.publishEvent(...)} am Ende von
 * {@code verarbeiteAsync}. Spring verwirft den Event-Aufruf still,
 * wenn beim Publish keine Transaktion aktiv ist; dann bleiben alle
 * {@code @TransactionalEventListener}-Methoden (CVE-Enrichment,
 * OSV-Matching, Alert-Bus) stumm und Scans zeigen {@code
 * findingCount=0}, obwohl die Pipeline eigentlich laufen sollte.
 */
class ScanIngestServiceAnnotationsTest {

    @Test
    @DisplayName("verarbeiteAsync traegt @Async UND @Transactional")
    void asyncUndTransactional() throws Exception {
        Method m = ScanIngestService.class.getMethod(
                "verarbeiteAsync", java.util.UUID.class, byte[].class);

        assertThat(m.isAnnotationPresent(Async.class))
                .as("@Async muss vorhanden sein (eigener Executor)")
                .isTrue();
        assertThat(m.isAnnotationPresent(Transactional.class))
                .as("@Transactional muss vorhanden sein, damit "
                        + "TransactionalEventListener feuert")
                .isTrue();
    }
}
