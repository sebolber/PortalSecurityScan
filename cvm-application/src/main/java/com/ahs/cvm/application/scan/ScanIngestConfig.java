package com.ahs.cvm.application.scan;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedizierter Executor fuer die asynchrone SBOM-Verarbeitung.
 *
 * <p>2–4 Worker, bounded Queue (100). Bei Ueberlauf wirft der Executor eine
 * {@link java.util.concurrent.RejectedExecutionException}, die vom
 * Controller in {@code 503 Service Unavailable} uebersetzt wird.
 */
@Configuration
@EnableAsync
public class ScanIngestConfig {

    public static final String EXECUTOR_NAME = "sbom-ingest";

    @Bean(EXECUTOR_NAME)
    public Executor sbomIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sbom-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
