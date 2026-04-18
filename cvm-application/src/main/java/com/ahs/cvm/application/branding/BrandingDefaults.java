package com.ahs.cvm.application.branding;

/**
 * Default-Branding, wenn ein Mandant noch keine Zeile in
 * {@code branding_config} hat. Spiegelt die adesso-CI-Vorgabe
 * (Styleguide Maerz 2026): adesso-Blau als Primaerfarbe,
 * Fira Sans als Schrift, adesso-Grau als Akzent.
 */
public final class BrandingDefaults {

    public static final String PRIMARY = "#006ec7";
    public static final String PRIMARY_CONTRAST = "#ffffff";
    public static final String ACCENT = "#887d75";
    public static final String FONT = "Fira Sans";
    public static final String FONT_MONO = "Fira Code";
    public static final String APP_TITLE = "CVE-Relevance-Manager";
    public static final String LOGO_ALT = "adesso health solutions";

    private BrandingDefaults() {}

    public static BrandingView view() {
        return new BrandingView(
                PRIMARY,
                PRIMARY_CONTRAST,
                ACCENT,
                FONT,
                FONT_MONO,
                APP_TITLE,
                null,
                LOGO_ALT,
                null,
                null,
                1);
    }
}
