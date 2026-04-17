package com.ahs.cvm.application.assessment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Platzhalter-Listener fuer {@link AssessmentApprovedEvent}. Iteration 09
 * (SMTP-Alerts) ersetzt den Body durch Mail-Versand.
 */
@Component
public class AssessmentApprovedLogger {

    private static final Logger log = LoggerFactory.getLogger(AssessmentApprovedLogger.class);

    @EventListener
    public void onApproved(AssessmentApprovedEvent event) {
        log.info(
                "AssessmentApprovedEvent: assessment={}, severity={}, approver={}, at={}",
                event.assessmentId(), event.severity(), event.approverId(), event.approvedAt());
    }
}
