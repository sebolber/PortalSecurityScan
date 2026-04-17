package com.ahs.cvm.application.profile;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(UUID id) {
        super("Profil-Version " + id + " nicht gefunden.");
    }

    public ProfileNotFoundException(String message) {
        super(message);
    }
}
