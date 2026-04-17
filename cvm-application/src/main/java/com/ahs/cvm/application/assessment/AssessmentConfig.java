package com.ahs.cvm.application.assessment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Konfigurationsparameter fuer den Bewertungs-Workflow. Defaults sind so
 * gewaehlt, dass keine application.yaml-Aenderung noetig ist.
 *
 * <ul>
 *   <li>{@code cvm.assessment.default-valid-months} &mdash; Default-Lebenszeit
 *       eines APPROVED-Assessments (Standard: 12 Monate).</li>
 * </ul>
 */
@Component
public class AssessmentConfig {

    private final int defaultValidMonths;

    public AssessmentConfig(
            @Value("${cvm.assessment.default-valid-months:12}") int defaultValidMonths) {
        this.defaultValidMonths = defaultValidMonths > 0 ? defaultValidMonths : 12;
    }

    public int defaultValidMonths() {
        return defaultValidMonths;
    }
}
