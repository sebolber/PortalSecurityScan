package com.ahs.cvm.ai.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministischer Text-Chunker. Standard: max 1500 Zeichen pro
 * Chunk, 150 Zeichen Ueberlappung. Werte sind ueber Konstanten
 * konfiguriert; Aenderungen erfordern bewusste Re-Indexierung.
 *
 * <p>Pragmatischer Ansatz: hart auf Zeichengrenzen schneiden, dann
 * den Rand zurueck zum letzten Whitespace verschieben, wenn moeglich.
 * Damit bleiben Token nicht halbiert.
 */
@Component
public class Chunker {

    static final int CHUNK_SIZE = 1500;
    static final int OVERLAP = 150;

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String trimmed = text.trim();
        if (trimmed.length() <= CHUNK_SIZE) {
            return List.of(trimmed);
        }

        List<String> chunks = new ArrayList<>();
        int pos = 0;
        while (pos < trimmed.length()) {
            int end = Math.min(pos + CHUNK_SIZE, trimmed.length());
            int boundary = end;
            if (end < trimmed.length()) {
                int rueck = trimmed.lastIndexOf(' ', end);
                if (rueck > pos + CHUNK_SIZE / 2) {
                    boundary = rueck;
                }
            }
            chunks.add(trimmed.substring(pos, boundary).trim());
            if (boundary >= trimmed.length()) {
                break;
            }
            pos = Math.max(0, boundary - OVERLAP);
        }
        return List.copyOf(chunks);
    }
}
