package com.ahs.cvm.application.scan;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-GCM-Verschluesselung der rohen SBOM. Der Schluessel wird aus einer
 * Konfigurationsvariable (Vault-Platzhalter via {@code application.yaml})
 * abgeleitet. "Jasypt-Strategie" gemaess CLAUDE.md, Abschnitt 9.
 *
 * <p>Format der Ciphertexts: {@code <12-Byte-IV> || <Ciphertext+Tag>}.
 */
@Component
public class SbomEncryption {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SbomEncryption(@Value("${cvm.encryption.sbom-secret:cvm-dev-default-secret}") String secret) {
        this.key = ableiteSchluessel(secret);
    }

    public byte[] encrypt(byte[] klartext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(klartext);
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return combined;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SBOM-Verschluesselung fehlgeschlagen", e);
        }
    }

    public byte[] decrypt(byte[] combined) {
        try {
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SBOM-Entschluesselung fehlgeschlagen", e);
        }
    }

    public String sha256Hex(byte[] raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }

    private static SecretKeySpec ableiteSchluessel(String secret) {
        try {
            byte[] material = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(material, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Schluessel-Ableitung fehlgeschlagen", e);
        }
    }

    public static String base64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
