package com.ahs.cvm.ai.profileassistant;

import com.ahs.cvm.persistence.profileassist.ProfileAssistSession;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raeumt EXPIRED/FINALIZED-Sessions aus {@code profile_assist_session}
 * auf, die aelter als {@code cvm.ai.profile-assist.cleanup-days} sind
 * (Go-Live-Nachzug zu Iteration 18, CVM-43).
 */
@Component
@ConditionalOnProperty(
        prefix = "cvm.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProfileAssistSessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(
            ProfileAssistSessionCleanupJob.class);

    private final ProfileAssistSessionRepository repository;
    private final Clock clock;
    private final int cleanupDays;

    public ProfileAssistSessionCleanupJob(
            ProfileAssistSessionRepository repository,
            Clock clock,
            @Value("${cvm.ai.profile-assist.cleanup-days:7}") int cleanupDays) {
        this.repository = repository;
        this.clock = clock;
        this.cleanupDays = Math.max(1, cleanupDays);
    }

    @Scheduled(cron = "${cvm.ai.profile-assist.cleanup-cron:0 15 2 * * *}")
    @Transactional
    public int laufeTaeglich() {
        Instant grenze = Instant.now(clock).minus(Duration.ofDays(cleanupDays));
        List<ProfileAssistSession> kandidaten =
                repository.findCleanupKandidaten(grenze);
        if (kandidaten.isEmpty()) {
            return 0;
        }
        List<UUID> ids = kandidaten.stream().map(ProfileAssistSession::getId).toList();
        int geloescht = repository.deleteByIds(ids);
        log.info("ProfileAssistSession-Cleanup: {} Sessions geloescht "
                + "(aelter als {} Tage).", geloescht, cleanupDays);
        return geloescht;
    }
}
