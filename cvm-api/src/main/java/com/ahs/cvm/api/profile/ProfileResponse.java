package com.ahs.cvm.api.profile;

import com.ahs.cvm.domain.enums.ProfileState;
import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID environmentId,
        int versionNumber,
        ProfileState state,
        String yamlSource,
        String proposedBy,
        String approvedBy,
        Instant approvedAt,
        Instant validFrom) {}
