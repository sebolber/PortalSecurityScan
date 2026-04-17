package com.ahs.cvm.application.rules;

import java.util.UUID;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(UUID id) {
        super("Regel " + id + " nicht gefunden.");
    }
}
