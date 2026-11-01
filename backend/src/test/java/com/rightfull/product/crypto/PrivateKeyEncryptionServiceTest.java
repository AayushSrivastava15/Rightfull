package com.rightfull.product.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Private Key Encryption Service Tests (AES-256-GCM + AAD Binding)")
class PrivateKeyEncryptionServiceTest {

    private static final String VALID_32_BYTE_KEY = "12345678901234567890123456789012";
    private PrivateKeyEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new PrivateKeyEncryptionService(VALID_32_BYTE_KEY);
        encryptionService.validateAndInitializeMasterKey();
    }

    @Test
    @DisplayName("Encrypts and decrypts private key bytes successfully with valid AAD")
    void testEncryptionDecryptionRoundTrip() {
        byte[] plaintext = "mock-pkcs8-private-key-bytes-for-ed25519".getBytes(StandardCharsets.UTF_8);
        UUID productId = UUID.randomUUID();
        String keyId = "kid_test_123456";

        var encrypted = encryptionService.encrypt(plaintext, productId, keyId, "v1");
        assertNotNull(encrypted);
        assertNotNull(encrypted.ciphertext());
        assertEquals(12, encrypted.nonce().length, "GCM nonce must be 12 bytes");
        assertEquals("v1", encrypted.version());

        byte[] decrypted = encryptionService.decrypt(
                encrypted.ciphertext(),
                encrypted.nonce(),
                encrypted.version(),
                productId,
                keyId
        );

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Generates distinct random nonces for identical plaintext")
    void testUniqueNonceGeneration() {
        byte[] plaintext = "identical-key-data".getBytes(StandardCharsets.UTF_8);
        UUID productId = UUID.randomUUID();
        String keyId = "kid_same_key";

        var payload1 = encryptionService.encrypt(plaintext, productId, keyId, "v1");
        var payload2 = encryptionService.encrypt(plaintext, productId, keyId, "v1");

        assertNotEquals(
                new String(payload1.nonce(), StandardCharsets.ISO_8859_1),
                new String(payload2.nonce(), StandardCharsets.ISO_8859_1)
        );
        assertNotEquals(
                new String(payload1.ciphertext(), StandardCharsets.ISO_8859_1),
                new String(payload2.ciphertext(), StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("Rejects decryption when Product ID in AAD does not match")
    void testAadRejectsMismatchedProductId() {
        byte[] plaintext = "secret-private-key".getBytes(StandardCharsets.UTF_8);
        UUID realProductId = UUID.randomUUID();
        UUID attackerProductId = UUID.randomUUID();
        String keyId = "kid_test_token";

        var encrypted = encryptionService.encrypt(plaintext, realProductId, keyId, "v1");

        assertThrows(SecurityException.class, () ->
                encryptionService.decrypt(
                        encrypted.ciphertext(),
                        encrypted.nonce(),
                        encrypted.version(),
                        attackerProductId,
                        keyId
                )
        );
    }

    @Test
    @DisplayName("Rejects decryption when Key ID in AAD does not match")
    void testAadRejectsMismatchedKeyId() {
        byte[] plaintext = "secret-private-key".getBytes(StandardCharsets.UTF_8);
        UUID productId = UUID.randomUUID();

        var encrypted = encryptionService.encrypt(plaintext, productId, "kid_valid", "v1");

        assertThrows(SecurityException.class, () ->
                encryptionService.decrypt(
                        encrypted.ciphertext(),
                        encrypted.nonce(),
                        encrypted.version(),
                        productId,
                        "kid_attacker_spliced"
                )
        );
    }

    @Test
    @DisplayName("Rejects decryption when ciphertext has been tampered with")
    void testRejectsTamperedCiphertext() {
        byte[] plaintext = "secret-private-key".getBytes(StandardCharsets.UTF_8);
        UUID productId = UUID.randomUUID();
        String keyId = "kid_test";

        var encrypted = encryptionService.encrypt(plaintext, productId, keyId, "v1");
        encrypted.ciphertext()[0] ^= 0xFF; // Flip bits in ciphertext

        assertThrows(SecurityException.class, () ->
                encryptionService.decrypt(
                        encrypted.ciphertext(),
                        encrypted.nonce(),
                        encrypted.version(),
                        productId,
                        keyId
                )
        );
    }

    @Test
    @DisplayName("Fails startup validation when master key configuration is missing or invalid")
    void testStartupValidationFailsOnInvalidKey() {
        PrivateKeyEncryptionService missingKey = new PrivateKeyEncryptionService("");
        assertThrows(IllegalStateException.class, missingKey::validateAndInitializeMasterKey);

        PrivateKeyEncryptionService shortKey = new PrivateKeyEncryptionService("short_key_16_byte");
        assertThrows(IllegalStateException.class, shortKey::validateAndInitializeMasterKey);
    }
}

