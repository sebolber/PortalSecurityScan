package com.ahs.cvm.application.parameter;

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
 * AES-GCM-Verschluesselung fuer sensible System-Parameter
 * (Vorbild: {@code com.ahs.cvm.application.scan.SbomEncryption}).
 *
 * <p>Gespeichert wird: {@code "enc:" || Base64(<12-Byte-IV> || <Ciphertext+Tag>)}.
 * Der Praefix ermoeglicht die Unterscheidung zwischen verschluesselten und
 * Klartext-Werten (z.B. seeded Boot-Defaults ohne Verschluesselung).
 *
 * <p>Der Master-Key wird aus {@code cvm.encryption.parameter-secret} per
 * SHA-256 abgeleitet. In Produktion kommt der Wert aus Vault/OpenShift-
 * Secret; der Dev-Default reicht ausschliesslich fuer lokale Tests.
 */
@Component
public class SystemParameterSecretCipher {

    public static final String ENC_PREFIX = "enc:";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SystemParameterSecretCipher(
            @Value("${cvm.encryption.parameter-secret:cvm-dev-default-parameter-secret}") String secret) {
        this.key = ableiteSchluessel(secret);
    }

    public String encrypt(String klartext) {
        if (klartext == null) {
            return null;
        }
        if (klartext.isEmpty()) {
            return ENC_PREFIX;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE, key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(klartext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Parameter-Verschluesselung fehlgeschlagen", e);
        }
    }

    public String decrypt(String gespeichert) {
        if (gespeichert == null) {
            return null;
        }
        if (!gespeichert.startsWith(ENC_PREFIX)) {
            return gespeichert;
        }
        String body = gespeichert.substring(ENC_PREFIX.length());
        if (body.isEmpty()) {
            return "";
        }
        try {
            byte[] combined = Base64.getDecoder().decode(body);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Parameter-Entschluesselung fehlgeschlagen", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }

    private static SecretKeySpec ableiteSchluessel(String secret) {
        try {
            byte[] material = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(material, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Parameter-Schluesselableitung fehlgeschlagen", e);
        }
    }
}
