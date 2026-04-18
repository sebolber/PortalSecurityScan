package com.ahs.cvm.application.vex;

import java.util.List;

/** Ergebnis eines VEX-Imports (Iteration 20). */
public record VexImportResult(
        int totalStatements,
        int proposalsCreated,
        List<String> warnings,
        List<String> errors) {

    public VexImportResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean ok() { return errors.isEmpty(); }
}
