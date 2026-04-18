package com.ahs.cvm.application.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContrastValidatorTest {

    @Test
    @DisplayName("Schwarz auf Weiss liefert maximales Kontrastverhaeltnis (~21)")
    void extremFall() {
        double ratio = ContrastValidator.ratio("#000000", "#ffffff");
        assertThat(ratio).isCloseTo(21.0, within(0.5));
    }

    @Test
    @DisplayName("Gleiche Farbe liefert Verhaeltnis 1.0")
    void gleicheFarbe() {
        assertThat(ContrastValidator.ratio("#006ec7", "#006ec7"))
                .isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("adesso-Blau auf Weiss erfuellt AA")
    void adessoBlauAuftWeiss() {
        assertThat(ContrastValidator.meetsAa("#006ec7", "#ffffff")).isTrue();
    }

    @Test
    @DisplayName("Hellgrau auf Weiss erfuellt AA nicht")
    void hellgrau() {
        assertThat(ContrastValidator.meetsAa("#cccccc", "#ffffff")).isFalse();
    }

    @Test
    @DisplayName("3-Zeichen-Kurzform wird akzeptiert")
    void kurzform() {
        double longFormRatio = ContrastValidator.ratio("#000000", "#ffffff");
        double shortFormRatio = ContrastValidator.ratio("#000", "#fff");
        assertThat(shortFormRatio).isCloseTo(longFormRatio, within(0.001));
    }

    @Test
    @DisplayName("Ungueltiger Hex-Wert wirft IllegalArgumentException")
    void ungueltig() {
        assertThat(
                        org.assertj.core.api.Assertions.catchThrowable(
                                () -> ContrastValidator.ratio("nichtHex", "#fff")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
