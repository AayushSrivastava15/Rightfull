package com.rightfull.product.crypto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Encrypts and decrypts product private signing keys using AES-256-GCM
 * with Authenticated Additional Data (AAD) binding to prevent ciphertext splicing across records.
 */
@Service
public class PrivateKeyEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(PrivateKeyEncryptionService.class);

    public static final String AES_GCM_CIPHER = "AES/GCM/NoPadding";
    public static final int GCM_NONCE_LENGTH_BYTES = 12;
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final String DEFAULT_ENCRYPTION_VERSION = "v1";

    private final String masterKeyConfig;
    private SecretKey keyEncryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public record EncryptedPrivateKeyPayload(
            byte[] ciphertext,
            byte[] nonce,
            String version
    ) {}

    public PrivateKeyEncryptionService(
            @Value("${rightfull.crypto.master-encryption-key:}") String masterKeyConfig) {
        this.masterKeyConfig = masterKeyConfig;
    }

    @PostConstruct
    public void validateAndInitializeMasterKey() {
        if (masterKeyConfig == null || masterKeyConfig.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Production master encryption key 'rightfull.crypto.master-encryption-key' must be configured. Plaintext storage is forbidden."
            );
        }

        byte[] keyBytes;
        String rawKey = masterKeyConfig.trim();
        try {
            // Attempt Base64 decoding first if standard 44-char base64 string
            byte[] decoded = Base64.getDecoder().decode(rawKey);
            if (decoded.length == 32) {
                keyBytes = decoded;
            } else {
                keyBytes = deriveOrValidateUtf8Key(rawKey);
            }
        } catch (IllegalArgumentException e) {
            keyBytes = deriveOrValidateUtf8Key(rawKey);
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Master key-encryption key (KEK) must be exactly 32 bytes (256 bits). Current length: " + keyBytes.length
            );
        }

        this.keyEncryptionKey = new SecretKeySpec(keyBytes, "AES");
        log.info("Initialized PrivateKeyEncryptionService with AES-256-GCM key-encryption key.");
    }

    private byte[] deriveOrValidateUtf8Key(String raw) {
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 32) {
            return bytes;
        }
        throw new IllegalStateException("Raw UTF-8 master encryption key must be exactly 32 bytes (256-bit). Length: " + bytes.length);
    }

    /**
     * Encrypts private key bytes using AES-256-GCM and binds to record attributes via AAD.
     *
     * @param plaintextPrivateKey PKCS#8 private key bytes
     * @param productId Owning product UUID
     * @param keyId Public key identifier (kid)
     * @param encryptionVersion Encryption format version (e.g. "v1")
     * @return Encrypted payload containing ciphertext and unique 12-byte nonce
     */
    public EncryptedPrivateKeyPayload encrypt(
            byte[] plaintextPrivateKey,
            UUID productId,
            String keyId,
            String encryptionVersion) {
        if (plaintextPrivateKey == null || plaintextPrivateKey.length == 0) {
            throw new IllegalArgumentException("Plaintext private key cannot be null or empty");
        }
        if (productId == null || keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Product ID and Key ID are required for AAD binding");
        }

        String version = (encryptionVersion != null && !encryptionVersion.isBlank())
                ? encryptionVersion : DEFAULT_ENCRYPTION_VERSION;

        byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keyEncryptionKey, spec);

            byte[] aad = buildAad(productId, keyId, version);
            cipher.updateAAD(aad);

            byte[] ciphertext = cipher.doFinal(plaintextPrivateKey);
            return new EncryptedPrivateKeyPayload(ciphertext, nonce, version);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt private signing key with AES-256-GCM", e);
        }
    }

    /**
     * Decrypts ciphertext and verifies AES-GCM authentication tag and AAD binding.
     *
     * @param ciphertext AES-GCM ciphertext including 128-bit authentication tag
     * @param nonce 12-byte initialization vector
     * @param encryptionVersion Format version
     * @param productId Expected product UUID
     * @param keyId Expected Key ID
     * @return Decrypted PKCS#8 private key bytes
     */
    public byte[] decrypt(
            byte[] ciphertext,
            byte[] nonce,
            String encryptionVersion,
            UUID productId,
            String keyId) {
        if (ciphertext == null || ciphertext.length == 0 || nonce == null || nonce.length != GCM_NONCE_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid ciphertext or GCM nonce supplied for decryption");
        }
        if (productId == null || keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Product ID and Key ID are required for AAD verification");
        }

        String version = (encryptionVersion != null && !encryptionVersion.isBlank())
                ? encryptionVersion : DEFAULT_ENCRYPTION_VERSION;

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.DECRYPT_MODE, keyEncryptionKey, spec);

            byte[] aad = buildAad(productId, keyId, version);
            cipher.updateAAD(aad);

            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Failed to decrypt signing key: AEAD authentication verification failed or key corrupted", e);
        }
    }

    private byte[] buildAad(UUID productId, String keyId, String version) {
        String binding = productId.toString() + ":" + keyId.trim() + ":" + version.trim();
        return binding.getBytes(StandardCharsets.UTF_8);
    }
}

