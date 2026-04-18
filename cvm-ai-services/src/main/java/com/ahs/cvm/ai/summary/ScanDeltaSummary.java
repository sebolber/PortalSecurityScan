package com.ahs.cvm.ai.summary;

import java.util.UUID;

/**
 * Generierte Delta-Summary in zwei Audiences (Iteration 14, CVM-33).
 *
 * @param scanId aktueller Scan.
 * @param previousScanId vorheriger freigegebener Scan; {@code null} bei
 *     Initial-Run.
 * @param shortText Slack-Snippet (max ~600 Zeichen).
 * @param longText ausfuehrliche Variante fuer Lenkungsausschuss.
 * @param delta strukturierter Diff (kann leer sein).
 * @param llmAufgerufen ob ein LLM-Call erfolgte (false bei
 *     Initial-Run oder Diff unter Mindestschwelle).
 */
public record ScanDeltaSummary(
        UUID scanId,
        UUID previousScanId,
        String shortText,
        String longText,
        ScanDelta delta,
        boolean llmAufgerufen) {}
