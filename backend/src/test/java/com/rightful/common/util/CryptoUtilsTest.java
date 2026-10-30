package com.rightful.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    @Test
    @DisplayName("Ed25519 keypair generation and signing verification")
    void testEd25519SigningAndVerification() {
        KeyPair keyPair = CryptoUtils.generateEd25519KeyPair();
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());

        byte[] message = "rightful_entitlement_payload_2026".getBytes(StandardCharsets.UTF_8);
        byte[] signature = CryptoUtils.signEd25519(message, keyPair.getPrivate());

        assertNotNull(signature);
        assertEquals(64, signature.length, "Ed25519 signatures must be exactly 64 bytes");

        boolean isValid = CryptoUtils.verifyEd25519(message, signature, keyPair.getPublic());
        assertTrue(isValid, "Signature must verify successfully against original public key");

        byte[] corruptedMessage = "tampered_payload".getBytes(StandardCharsets.UTF_8);
        boolean isTamperedValid = CryptoUtils.verifyEd25519(corruptedMessage, signature, keyPair.getPublic());
        assertFalse(isTamperedValid, "Tampered payload must fail signature verification");
    }

    @Test
    @DisplayName("HMAC-SHA-256 license key hashing is deterministic and irreversible")
    void testHmacSha256Hashing() {
        String licenseKey = "RF-PROD-TEST-8A4F-9B2C-1E4D";
        String secret = "super_secure_server_hmac_secret_32_bytes";

        String hash1 = CryptoUtils.hmacSha256(licenseKey, secret);
        String hash2 = CryptoUtils.hmacSha256(licenseKey, secret);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "HMAC-SHA-256 output must be 64 hex characters (256 bits)");
        assertEquals(hash1, hash2, "HMAC hash must be deterministic for identical key and secret");

        String diffKeyHash = CryptoUtils.hmacSha256("RF-PROD-TEST-DIFFERENT-KEY", secret);
        assertNotEquals(hash1, diffKeyHash);

        assertTrue(CryptoUtils.constantTimeEquals(hash1, hash2));
        assertFalse(CryptoUtils.constantTimeEquals(hash1, diffKeyHash));
    }
}
