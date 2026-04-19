package com.ahs.cvm.application.assessment;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Konfigurationsparameter fuer den Bewertungs-Workflow. Defaults sind so
 * gewaehlt, dass keine application.yaml-Aenderung noetig ist.
 *
 * <p>{@link #defaultValidMonthsEffective()} liest zur Laufzeit den System-
 * Parameter-Store (Tenant-spezifisch) und faellt auf den Boot-Default
 * zurueck.
 *
 * <ul>
 *   <li>{@code cvm.assessment.default-valid-months} &mdash; Default-Lebenszeit
 *       eines APPROVED-Assessments (Standard: 12 Monate).</li>
 * </ul>
 */
@Component
public class AssessmentConfig {

    private final int defaultValidMonths;
    private SystemParameterResolver resolver;

    public AssessmentConfig(
            @Value("${cvm.assessment.default-valid-months:12}") int defaultValidMonths) {
        this.defaultValidMonths = defaultValidMonths > 0 ? defaultValidMonths : 12;
    }

    @Autowired(required = false)
    public void setResolver(SystemParameterResolver resolver) {
        this.resolver = resolver;
    }

    public int defaultValidMonths() {
        return defaultValidMonths;
    }

    public int defaultValidMonthsEffective() {
        if (resolver == null) {
            return defaultValidMonths;
        }
        int v = resolver.resolveInt(
                "cvm.assessment.default-valid-months", defaultValidMonths);
        return v > 0 ? v : defaultValidMonths;
    }
}
