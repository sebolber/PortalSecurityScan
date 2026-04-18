package com.ahs.cvm.ai.profileassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.persistence.profileassist.ProfileAssistSession;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileAssistSessionCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");

    @Test
    @DisplayName("Cleanup: loescht alte EXPIRED/FINALIZED-Sessions")
    void loeschen() {
        ProfileAssistSessionRepository repo = mock(ProfileAssistSessionRepository.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ProfileAssistSession s1 = ProfileAssistSession.builder().id(id1).build();
        ProfileAssistSession s2 = ProfileAssistSession.builder().id(id2).build();
        given(repo.findCleanupKandidaten(any())).willReturn(List.of(s1, s2));
        given(repo.deleteByIds(anyList())).willReturn(2);

        ProfileAssistSessionCleanupJob job = new ProfileAssistSessionCleanupJob(
                repo, Clock.fixed(NOW, ZoneOffset.UTC), 7);
        int geloescht = job.laufeTaeglich();

        assertThat(geloescht).isEqualTo(2);
        verify(repo).deleteByIds(anyList());
    }

    @Test
    @DisplayName("Cleanup: keine Kandidaten -> kein deleteByIds")
    void nichtsZuTun() {
        ProfileAssistSessionRepository repo = mock(ProfileAssistSessionRepository.class);
        given(repo.findCleanupKandidaten(any())).willReturn(List.of());

        ProfileAssistSessionCleanupJob job = new ProfileAssistSessionCleanupJob(
                repo, Clock.fixed(NOW, ZoneOffset.UTC), 7);

        assertThat(job.laufeTaeglich()).isZero();
        verify(repo, never()).deleteByIds(anyList());
    }
}
