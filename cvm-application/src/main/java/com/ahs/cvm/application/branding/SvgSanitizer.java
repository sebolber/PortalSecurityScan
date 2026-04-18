package com.ahs.cvm.application.branding;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whitelist-basierte SVG-Pruefung fuer Logo-Uploads (Iteration 27, CVM-61).
 *
 * <p>Absichtlich kein XML-DOM-Parser: das Ziel ist eine harte,
 * schnell lesbare Ablehnung verdaechtiger SVGs. Wenn mindestens eines
 * der unten aufgefuehrten Muster matcht, wird das SVG verworfen.
 * Eine korrekt gebuendelte Admin-Logo-Datei kommt hier ohne weiteres
 * durch.
 *
 * <p>Bis zum Asset-Upload-Endpoint in Iteration 27b wird dieser
 * Sanitizer ueber {@code logoUrl} nicht scharf geschaltet (externe
 * URLs werden nicht geparst). Er ist aber bereits jetzt
 * service- und testgetrieben vorhanden, damit der Upload-Endpunkt
 * in 27b nur noch angebunden werden muss.
 */
public final class SvgSanitizer {

    private static final Pattern SCRIPT_ELEMENT =
            Pattern.compile("<\\s*script\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONEVENT_ATTRIBUTE =
            Pattern.compile("\\s+on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_URI =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_HREF =
            Pattern.compile(
                    "\\b(?:xlink:)?href\\s*=\\s*[\"'](?:https?:|ftp:|file:|data:(?!image/))",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_OBJECT =
            Pattern.compile("<\\s*foreignObject\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern USE_EXTERNAL =
            Pattern.compile(
                    "<\\s*use\\b[^>]*\\b(?:xlink:)?href\\s*=\\s*[\"']https?:",
                    Pattern.CASE_INSENSITIVE);

    private SvgSanitizer() {}

    /**
     * Fuehrt die Pruefung aus. Wirft {@link SvgRejectedException}, wenn
     * das Dokument verdaechtige Inhalte enthaelt.
     */
    public static void ensureSafe(String svgDocument) {
        if (svgDocument == null || svgDocument.isBlank()) {
            throw new SvgRejectedException("SVG-Inhalt ist leer.");
        }
        String normalised = svgDocument.toLowerCase(Locale.ROOT);
        if (!normalised.contains("<svg")) {
            throw new SvgRejectedException("Kein SVG-Root-Element gefunden.");
        }
        checkPattern(svgDocument, SCRIPT_ELEMENT, "eingebettetes script-Element");
        checkPattern(svgDocument, ONEVENT_ATTRIBUTE, "on*-Event-Handler");
        checkPattern(svgDocument, JAVASCRIPT_URI, "javascript:-URI");
        checkPattern(svgDocument, EXTERNAL_HREF, "externer href/xlink:href");
        checkPattern(svgDocument, FOREIGN_OBJECT, "foreignObject-Element");
        checkPattern(svgDocument, USE_EXTERNAL, "externer <use href=...>");
    }

    /**
     * Erwartet gueltiges SVG und gibt das Dokument unveraendert zurueck.
     * Kurzform fuer Aufrufer, die den Return-Wert in einer Kette nutzen.
     */
    public static String sanitize(String svgDocument) {
        ensureSafe(svgDocument);
        return svgDocument;
    }

    private static void checkPattern(String svg, Pattern pattern, String reason) {
        Matcher m = pattern.matcher(svg);
        if (m.find()) {
            throw new SvgRejectedException(
                    "SVG verworfen: " + reason + " entdeckt.");
        }
    }

    /** Wird geworfen, wenn die Whitelist-Pruefung fehlschlaegt. */
    public static final class SvgRejectedException extends RuntimeException {
        public SvgRejectedException(String message) {
            super(message);
        }
    }
}
