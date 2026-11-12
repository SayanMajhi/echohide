package com.sayan.echohide.crypto;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

/**
 * Handles AES-256-GCM authenticated encryption and decryption.
 */
public class CryptoManager {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    public static final int SALT_LENGTH_BYTES = 16;
    public static final int IV_LENGTH_BYTES = 12; // 96 bits recommended for GCM
    public static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static class EncryptedResult {
        private final byte[] ciphertext;
        private final byte[] salt;
        private final byte[] iv;

        public EncryptedResult(byte[] ciphertext, byte[] salt, byte[] iv) {
            this.ciphertext = ciphertext;
            this.salt = salt;
            this.iv = iv;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }

        public byte[] getSalt() {
            return salt;
        }

        public byte[] getIv() {
            return iv;
        }
    }

    /**
     * Derives an AES-256 key from a passphrase and salt.
     */
    public static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * @param plaintext Secret text message
     * @param password  Passphrase for key derivation
     * @return EncryptedResult containing ciphertext (with appended auth tag), salt, and IV
     */
    public static EncryptedResult encrypt(String plaintext, String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        SecretKey key = deriveKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] inputBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(inputBytes);

        return new EncryptedResult(ciphertext, salt, iv);
    }

    /**
     * Decrypts ciphertext and verifies GCM authentication tag.
     *
     * @param ciphertext Ciphertext with tag
     * @param password   Passphrase
     * @param salt       Salt used for derivation
     * @param iv         IV used during encryption
     * @return Decrypted plaintext string
     * @throws AuthenticationFailedException if tag validation fails or key is wrong
     */
    public static String decrypt(byte[] ciphertext, String password, byte[] salt, byte[] iv)
            throws AuthenticationFailedException, GeneralSecurityException {
        try {
            SecretKey key = deriveKey(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintextBytes = cipher.doFinal(ciphertext);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new AuthenticationFailedException("Authentication tag mismatch: Wrong password or tampered/corrupted steganographic payload.", e);
        }
    }
}

