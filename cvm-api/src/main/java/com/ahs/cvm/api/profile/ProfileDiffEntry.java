package com.ahs.cvm.api.profile;

import com.ahs.cvm.application.profile.ProfileFieldDiff;
import com.fasterxml.jackson.databind.JsonNode;

public record ProfileDiffEntry(
        String path,
        ProfileFieldDiff.ChangeType changeType,
        JsonNode altWert,
        JsonNode neuWert) {

    public static ProfileDiffEntry from(ProfileFieldDiff diff) {
        return new ProfileDiffEntry(
                diff.path(), diff.changeType(), diff.altWert(), diff.neuWert());
    }
}
