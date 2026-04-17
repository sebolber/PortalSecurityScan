package com.ahs.cvm.application.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SbomEncryptionTest {

    private final SbomEncryption encryption = new SbomEncryption("test-secret");

    @Test
    @DisplayName("SbomEncryption: Round-Trip ergibt den Ausgangstext")
    void roundTrip() {
        byte[] klartext = "{\"bomFormat\":\"CycloneDX\"}".getBytes(StandardCharsets.UTF_8);

        byte[] verschluesselt = encryption.encrypt(klartext);
        byte[] entschluesselt = encryption.decrypt(verschluesselt);

        assertThat(verschluesselt).isNotEqualTo(klartext);
        assertThat(entschluesselt).isEqualTo(klartext);
    }

    @Test
    @DisplayName("SbomEncryption: zwei Encrypt-Aufrufe erzeugen unterschiedliche Ciphertexts (IV)")
    void encryptIvIstZufaellig() {
        byte[] klartext = "test".getBytes(StandardCharsets.UTF_8);

        byte[] a = encryption.encrypt(klartext);
        byte[] b = encryption.encrypt(klartext);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("SbomEncryption: sha256Hex ist deterministisch und 64 Zeichen lang")
    void sha256IstDeterministisch() {
        byte[] input = "content".getBytes(StandardCharsets.UTF_8);

        String a = encryption.sha256Hex(input);
        String b = encryption.sha256Hex(input);

        assertThat(a).isEqualTo(b).hasSize(64);
    }
}
