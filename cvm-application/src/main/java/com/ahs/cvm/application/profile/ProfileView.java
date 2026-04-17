package com.ahs.cvm.application.profile;

import com.ahs.cvm.domain.enums.ProfileState;
import com.ahs.cvm.persistence.profile.ContextProfile;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer das Kontextprofil. Erlaubt dem API-Modul, auf die fachliche
 * Sicht zuzugreifen, ohne gegen die Arch-Regel
 * {@code api -> persistence} zu verstossen.
 */
public record ProfileView(
        UUID id,
        UUID environmentId,
        int versionNumber,
        ProfileState state,
        String yamlSource,
        String proposedBy,
        String approvedBy,
        Instant approvedAt,
        Instant validFrom) {

    public static ProfileView from(ContextProfile p) {
        return new ProfileView(
                p.getId(),
                p.getEnvironment() == null ? null : p.getEnvironment().getId(),
                p.getVersionNumber() == null ? 0 : p.getVersionNumber(),
                p.getState(),
                p.getYamlSource(),
                p.getProposedBy(),
                p.getApprovedBy(),
                p.getApprovedAt(),
                p.getValidFrom());
    }
}
