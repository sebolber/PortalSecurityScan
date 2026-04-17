package com.ahs.cvm.application.profile;

import java.util.Set;
import java.util.UUID;

/**
 * Domain-Event: eine neue Profil-Version wurde aktiviert. Enthaelt die Menge
 * der Profilpfade, die sich gegenueber der vorherigen Version geaendert,
 * neu angelegt oder entfernt wurden. Wird vom {@link AssessmentReviewMarker}
 * abonniert.
 */
public record ContextProfileActivatedEvent(
        UUID environmentId,
        UUID newProfileVersionId,
        int newVersionNumber,
        Set<String> changedPaths) {}
