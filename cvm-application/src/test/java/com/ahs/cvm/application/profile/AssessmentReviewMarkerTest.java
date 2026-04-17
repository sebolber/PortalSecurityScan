package com.ahs.cvm.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssessmentReviewMarkerTest {

    private final AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
    private final AssessmentReviewMarker marker = new AssessmentReviewMarker(assessmentRepository);

    @Test
    @DisplayName("Profil: Aktivierung setzt betroffene Assessments auf NEEDS_REVIEW")
    void markiertBetroffeneAssessments() {
        UUID envId = UUID.randomUUID();
        UUID profileVersionId = UUID.randomUUID();
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();

        String[] erwartetePfade = {"architecture.windows_hosts", "network.internet_exposure"};
        given(assessmentRepository.findAktiveIdsByEnvironmentAndSourceFields(
                        org.mockito.ArgumentMatchers.eq(envId),
                        org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(a1, a2));
        given(assessmentRepository.markiereAlsReview(
                        org.mockito.ArgumentMatchers.anySet(),
                        org.mockito.ArgumentMatchers.eq(profileVersionId)))
                .willReturn(2);

        ContextProfileActivatedEvent event = new ContextProfileActivatedEvent(
                envId,
                profileVersionId,
                4,
                Set.of(erwartetePfade));

        marker.onActivation(event);

        ArgumentCaptor<Set<UUID>> idsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(assessmentRepository).markiereAlsReview(idsCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(profileVersionId));

        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(a1, a2);
    }

    @Test
    @DisplayName("Profil: leere Diff-Liste loest kein Update aus")
    void leeresDiffKeinUpdate() {
        UUID envId = UUID.randomUUID();
        UUID profileVersionId = UUID.randomUUID();

        ContextProfileActivatedEvent event = new ContextProfileActivatedEvent(
                envId, profileVersionId, 4, Set.of());

        marker.onActivation(event);

        verify(assessmentRepository, never())
                .findAktiveIdsByEnvironmentAndSourceFields(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verify(assessmentRepository, never())
                .markiereAlsReview(
                        org.mockito.ArgumentMatchers.anySet(),
                        org.mockito.ArgumentMatchers.any());
    }
}
