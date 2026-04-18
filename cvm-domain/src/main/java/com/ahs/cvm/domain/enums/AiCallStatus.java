package com.ahs.cvm.domain.enums;

/**
 * Status eines LLM-Calls im Audit-Log.
 *
 * <ul>
 *   <li>{@link #PENDING} &mdash; Vor dem Call, muss vor jedem Call
 *       geschrieben sein.</li>
 *   <li>{@link #OK} &mdash; Erfolg, Output gueltig.</li>
 *   <li>{@link #INVALID_OUTPUT} &mdash; Output-Validator hat
 *       abgelehnt.</li>
 *   <li>{@link #INJECTION_RISK} &mdash; Injection-Detector hat
 *       angeschlagen und der Modus war block.</li>
 *   <li>{@link #ERROR} &mdash; Technischer Fehler (Timeout, 5xx,
 *       Parsing).</li>
 *   <li>{@link #RATE_LIMITED} &mdash; Rate-Limit verhindert Call.</li>
 *   <li>{@link #DISABLED} &mdash; Feature-Flag war aus.</li>
 * </ul>
 */
public enum AiCallStatus {
    PENDING,
    OK,
    INVALID_OUTPUT,
    INJECTION_RISK,
    ERROR,
    RATE_LIMITED,
    DISABLED
}
