package com.ahs.cvm.ai.rag;

import com.ahs.cvm.application.assessment.AssessmentApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Triggert die RAG-Indexierung sobald ein Assessment freigegeben
 * wurde. Fehler beim Indexieren werden geloggt, sind aber nicht
 * fatal fuer den Approve-Flow.
 */
@Component
public class AssessmentApprovedRagListener {

    private static final Logger log =
            LoggerFactory.getLogger(AssessmentApprovedRagListener.class);

    private final IndexingService indexingService;

    public AssessmentApprovedRagListener(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @EventListener
    public void onApproved(AssessmentApprovedEvent event) {
        try {
            int chunks = indexingService.indexAssessment(event.assessmentId());
            log.info("RAG: Assessment {} indiziert ({} Chunks).",
                    event.assessmentId(), chunks);
        } catch (RuntimeException ex) {
            log.warn("RAG-Indexierung fuer Assessment {} fehlgeschlagen: {}",
                    event.assessmentId(), ex.getMessage());
        }
    }
}
