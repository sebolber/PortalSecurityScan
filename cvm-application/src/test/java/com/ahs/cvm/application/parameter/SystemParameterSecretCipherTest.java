package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterSecretCipherTest {

    private final SystemParameterSecretCipher cipher = new SystemParameterSecretCipher("cvm-test-secret");

    @Test
    @DisplayName("Round-Trip: Klartext wird verschluesselt und wieder entschluesselt")
    void round_trip() {
        String klartext = "sk-ant-api03-deadbeef-1234567890";
        String encoded = cipher.encrypt(klartext);
        assertThat(encoded).startsWith("enc:");
        assertThat(encoded).isNotEqualTo(klartext);
        assertThat(cipher.decrypt(encoded)).isEqualTo(klartext);
    }

    @Test
    @DisplayName("Gleicher Klartext wird zweimal unterschiedlich verschluesselt (IV zufaellig)")
    void nicht_deterministisch() {
        String encoded1 = cipher.encrypt("secret");
        String encoded2 = cipher.encrypt("secret");
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(cipher.decrypt(encoded1)).isEqualTo("secret");
        assertThat(cipher.decrypt(encoded2)).isEqualTo("secret");
    }

    @Test
    @DisplayName("null wird unveraendert weitergereicht")
    void null_unveraendert() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("Leerer String erzeugt nur den Praefix - isEncrypted=true")
    void leer_wird_praefix_only() {
        String encoded = cipher.encrypt("");
        assertThat(encoded).isEqualTo("enc:");
        assertThat(cipher.isEncrypted(encoded)).isTrue();
        assertThat(cipher.decrypt(encoded)).isEmpty();
    }

    @Test
    @DisplayName("decrypt auf Klartext ohne Praefix liefert den Wert zurueck (Abwaertskompatibilitaet)")
    void decrypt_auf_klartext() {
        assertThat(cipher.decrypt("plain-value")).isEqualTo("plain-value");
    }

    @Test
    @DisplayName("Beschaedigter Ciphertext fuehrt zu einer Exception (Tag-Mismatch)")
    void beschaedigter_ciphertext() {
        String encoded = cipher.encrypt("geheim");
        String broken = encoded.substring(0, encoded.length() - 2) + "AA";
        assertThatThrownBy(() -> cipher.decrypt(broken))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("isEncrypted erkennt den Praefix und verneint bei Klartext")
    void is_encrypted_praefix() {
        assertThat(cipher.isEncrypted("enc:xyz")).isTrue();
        assertThat(cipher.isEncrypted("plain")).isFalse();
        assertThat(cipher.isEncrypted(null)).isFalse();
    }
}
