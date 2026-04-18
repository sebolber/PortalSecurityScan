package com.ahs.cvm.application.branding;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.application.branding.SvgSanitizer.SvgRejectedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SvgSanitizerTest {

    private static final String CLEAN_LOGO =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 40 40\">"
                    + "<circle cx=\"20\" cy=\"20\" r=\"18\" fill=\"#006ec7\"/>"
                    + "</svg>";

    @Test
    @DisplayName("Gueltiges SVG wird akzeptiert")
    void sauber() {
        assertThatCode(() -> SvgSanitizer.ensureSafe(CLEAN_LOGO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Eingebettetes script-Element fuehrt zur Ablehnung")
    void script() {
        String malicious = "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<script>alert('x')</script></svg>";
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(malicious))
                .isInstanceOf(SvgRejectedException.class)
                .hasMessageContaining("script");
    }

    @Test
    @DisplayName("onload-Attribut fuehrt zur Ablehnung")
    void onEvent() {
        String malicious =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>";
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(malicious))
                .isInstanceOf(SvgRejectedException.class)
                .hasMessageContaining("on*");
    }

    @Test
    @DisplayName("javascript:-URI fuehrt zur Ablehnung")
    void javascriptUri() {
        String malicious =
                "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                        + "<a xlink:href=\"javascript:alert(1)\">x</a></svg>";
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(malicious))
                .isInstanceOf(SvgRejectedException.class);
    }

    @Test
    @DisplayName("externer xlink:href auf https-URL fuehrt zur Ablehnung")
    void externerHref() {
        String malicious =
                "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                        + "<image xlink:href=\"https://evil.example/x.png\"/></svg>";
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(malicious))
                .isInstanceOf(SvgRejectedException.class);
    }

    @Test
    @DisplayName("foreignObject-Element fuehrt zur Ablehnung")
    void foreignObject() {
        String malicious =
                "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                        + "<foreignObject><div>xss</div></foreignObject></svg>";
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(malicious))
                .isInstanceOf(SvgRejectedException.class);
    }

    @Test
    @DisplayName("Leerer Inhalt wird abgelehnt")
    void leer() {
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe(""))
                .isInstanceOf(SvgRejectedException.class);
    }

    @Test
    @DisplayName("Eingabe ohne SVG-Root wird abgelehnt")
    void ohneRoot() {
        assertThatThrownBy(() -> SvgSanitizer.ensureSafe("<html><body>nope</body></html>"))
                .isInstanceOf(SvgRejectedException.class)
                .hasMessageContaining("SVG-Root");
    }
}
