package com.ahs.cvm.application.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.stereotype.Component;

/**
 * Erzeugt aus einem HTML-String einen deterministischen PDF-Bytestrom
 * via openhtmltopdf + Apache PDFBox.
 *
 * <p>Determinismus-Strategie:
 * <ul>
 *   <li>{@code Producer} wird fix gesetzt.</li>
 *   <li>{@code CreationDate} und {@code ModificationDate} werden aus
 *       dem uebergebenen {@link Instant} abgeleitet.</li>
 *   <li>{@code /ID}-Array im Trailer wird durch einen deterministischen
 *       SHA-Wert ersetzt, sonst waere es zufaellig.</li>
 * </ul>
 * Dadurch sind zwei Aufrufe mit identischer Eingabe (HTML, Instant,
 * DocumentId) byte-gleich.
 */
@Component
public class HardeningReportPdfRenderer {

    private static final String PRODUCER = "CVM Report 1.0";

    /**
     * Rendert das HTML in ein PDF und setzt Dokumentmetadaten
     * deterministisch.
     *
     * @param html XHTML-konformer String (muss openhtmltopdf-kompatibel sein).
     * @param creationTime Zeitpunkt, der als CreationDate/ModDate
     *     eingetragen wird.
     * @param documentId Deterministischer Identifier (typ. SHA-256 ueber
     *     Eingabe). Wird zweifach ins {@code /ID}-Array uebernommen.
     * @return PDF-Bytestrom.
     */
    public byte[] render(String html, Instant creationTime, String documentId) {
        byte[] rohes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.withProducer(PRODUCER);
            builder.toStream(out);
            builder.run();
            rohes = out.toByteArray();
        } catch (IOException ex) {
            throw new PdfRenderingException(
                    "PDF-Erzeugung fehlgeschlagen: " + ex.getMessage(), ex);
        }

        // PDF erneut oeffnen, Metadaten deterministisch setzen, speichern.
        try (PDDocument doc = PDDocument.load(rohes);
                ByteArrayOutputStream finalOut = new ByteArrayOutputStream()) {
            setzeMetadatenDeterministisch(doc, creationTime, documentId);
            doc.save(finalOut);
            return finalOut.toByteArray();
        } catch (IOException ex) {
            throw new PdfRenderingException(
                    "PDF-Nachbearbeitung fehlgeschlagen: " + ex.getMessage(), ex);
        }
    }

    /**
     * Setzt {@code Info}-Dictionary und {@code /ID}-Trailer im
     * frisch gerenderten PDDocument auf feste, eingabegebundene Werte.
     */
    private void setzeMetadatenDeterministisch(
            PDDocument doc, Instant creationTime, String documentId) {
        PDDocumentInformation info = doc.getDocumentInformation();
        info.setProducer(PRODUCER);
        info.setCreator("CVM");
        Calendar cal = GregorianCalendar.from(
                creationTime.atZone(TimeZone.getTimeZone("UTC").toZoneId()));
        info.setCreationDate(cal);
        info.setModificationDate(cal);

        COSArray ids = new COSArray();
        COSString idString = new COSString(documentId);
        idString.setForceHexForm(true);
        ids.add(idString);
        ids.add(idString);
        doc.getDocument().getTrailer().setItem(COSName.ID, ids);
    }

    /**
     * Geworfen, wenn openhtmltopdf das PDF nicht bauen kann (HTML
     * invalid, Font fehlt, I/O-Fehler).
     */
    public static class PdfRenderingException extends RuntimeException {
        public PdfRenderingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
