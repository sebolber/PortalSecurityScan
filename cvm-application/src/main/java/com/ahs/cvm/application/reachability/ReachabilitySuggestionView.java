package com.ahs.cvm.application.reachability;

import java.util.UUID;

/**
 * Response des Symbol-Suggestion-Endpoints (Reachability).
 *
 * @param findingId Finding, fuer das der Vorschlag berechnet wurde.
 * @param sourcePurl Die PURL, aus der abgeleitet wurde (Debug/Anzeige).
 * @param symbol Vorgeschlagener Wert fuer das {@code vulnerableSymbol}-
 *               Feld oder {@code null}, wenn die PURL nicht parsebar war.
 * @param language Sprach-Hint fuer das Frontend-Dropdown (z.B. {@code java}).
 * @param rationale Menschliche Erklaerung, woraus der Vorschlag stammt.
 */
public record ReachabilitySuggestionView(
        UUID findingId,
        String sourcePurl,
        String symbol,
        String language,
        String rationale) {}
