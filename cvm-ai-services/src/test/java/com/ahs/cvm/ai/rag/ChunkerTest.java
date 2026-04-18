package com.ahs.cvm.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChunkerTest {

    private final Chunker chunker = new Chunker();

    @Test
    @DisplayName("Chunker: leerer Text liefert leere Liste")
    void leer() {
        assertThat(chunker.split("")).isEmpty();
        assertThat(chunker.split(null)).isEmpty();
        assertThat(chunker.split("   ")).isEmpty();
    }

    @Test
    @DisplayName("Chunker: kurzer Text bleibt ein Chunk")
    void kurz() {
        List<String> chunks = chunker.split("CVE-2025-48924 ist nicht relevant.");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).startsWith("CVE-2025-48924");
    }

    @Test
    @DisplayName("Chunker: text > 1500 Zeichen wird in mehrere Chunks zerlegt")
    void langerText() {
        String wort = "abcdef ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append(wort);
        }
        List<String> chunks = chunker.split(sb.toString());
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        for (String c : chunks) {
            assertThat(c.length()).isLessThanOrEqualTo(Chunker.CHUNK_SIZE);
        }
    }

    @Test
    @DisplayName("Chunker: gleiche Eingabe liefert gleiche Chunks (deterministisch)")
    void deterministisch() {
        String text = "Ein langer Text ".repeat(200);
        assertThat(chunker.split(text)).isEqualTo(chunker.split(text));
    }
}
