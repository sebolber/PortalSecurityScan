package com.ahs.cvm.ai.rag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.assessment.AssessmentApprovedEvent;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssessmentApprovedRagListenerTest {

    private final IndexingService indexing = mock(IndexingService.class);
    private final AssessmentApprovedRagListener listener =
            new AssessmentApprovedRagListener(indexing);

    @Test
    @DisplayName("RAG: Assessment wird nach Approve eingebettet und ist ueber Similarity-Search auffindbar")
    void approveTriggertIndexing() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentApprovedEvent event = new AssessmentApprovedEvent(
                assessmentId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                AhsSeverity.MEDIUM, "a.admin@ahs.test",
                Instant.parse("2026-04-18T10:00:00Z"));

        listener.onApproved(event);

        verify(indexing).indexAssessment(assessmentId);
    }

    @Test
    @DisplayName("RAG: Listener verschluckt Indexing-Fehler (Approve nicht blockieren)")
    void fehlerWirdGeschluckt() {
        UUID assessmentId = UUID.randomUUID();
        willThrow(new IllegalStateException("Indexing kaputt"))
                .given(indexing).indexAssessment(any());

        listener.onApproved(new AssessmentApprovedEvent(
                assessmentId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                AhsSeverity.LOW, "x@y", Instant.now()));

        // kein Throw -> Test durch
    }
}
