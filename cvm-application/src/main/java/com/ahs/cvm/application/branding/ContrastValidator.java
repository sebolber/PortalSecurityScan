package com.ahs.cvm.application.branding;

/**
 * WCAG-2.1-Kontrast-Pruefung fuer Farbpaare (Iteration 27, CVM-61).
 *
 * <p>Implementiert die sRGB-Luminanzformel aus WCAG 2.1 (G18).
 * Schwelle fuer AA-Fliesstext: 4.5:1. Wir nutzen 4.5 als Minimum
 * fuer Primaer-/Kontrastfarbe; eine Large-Text-Variante (3:1) ist
 * derzeit nicht noetig, weil die Primaerfarbe meist fuer Buttons
 * und kleine Texte eingesetzt wird.
 */
public final class ContrastValidator {

    /** WCAG-AA-Schwelle fuer normalen Fliesstext. */
    public static final double AA_THRESHOLD = 4.5;

    private ContrastValidator() {}

    /**
     * Prueft, ob das Farbpaar die AA-Schwelle 4.5:1 erfuellt.
     *
     * @throws IllegalArgumentException bei ungueltigen Hex-Farben.
     */
    public static boolean meetsAa(String foregroundHex, String backgroundHex) {
        return ratio(foregroundHex, backgroundHex) >= AA_THRESHOLD;
    }

    /**
     * Berechnet das Kontrastverhaeltnis zweier Farben im Format
     * {@code #rrggbb} bzw. {@code #rgb}. Ergebnis liegt zwischen
     * 1.0 (identische Farbe) und 21.0 (schwarz/weiss).
     */
    public static double ratio(String colorA, String colorB) {
        double la = relativeLuminance(parseHex(colorA));
        double lb = relativeLuminance(parseHex(colorB));
        double bright = Math.max(la, lb);
        double dark = Math.min(la, lb);
        return (bright + 0.05) / (dark + 0.05);
    }

    private static double relativeLuminance(int[] rgb) {
        double r = channel(rgb[0] / 255.0);
        double g = channel(rgb[1] / 255.0);
        double b = channel(rgb[2] / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(double srgb) {
        return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    private static int[] parseHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Farbwert darf nicht null sein.");
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 3) {
            StringBuilder expanded = new StringBuilder(6);
            for (int i = 0; i < 3; i++) {
                char c = value.charAt(i);
                expanded.append(c).append(c);
            }
            value = expanded.toString();
        }
        if (value.length() != 6) {
            throw new IllegalArgumentException(
                    "Farbwert muss 3 oder 6 Hex-Zeichen haben: " + hex);
        }
        try {
            int r = Integer.parseInt(value.substring(0, 2), 16);
            int g = Integer.parseInt(value.substring(2, 4), 16);
            int b = Integer.parseInt(value.substring(4, 6), 16);
            return new int[] {r, g, b};
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ungueltiger Hex-Wert: " + hex, ex);
        }
    }
}
