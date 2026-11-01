package com.rightfull.product.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ed25519 Cryptographic Service Tests")
class Ed25519KeyServiceTest {

    private Ed25519KeyService keyService;

    @BeforeEach
    void setUp() {
        keyService = new Ed25519KeyService();
    }

    @Test
    @DisplayName("Generates valid and distinct Ed25519 keypairs")
    void testKeyPairGeneration() {
        KeyPair kp1 = keyService.generateKeyPair();
        KeyPair kp2 = keyService.generateKeyPair();

        assertNotNull(kp1.getPublic());
        assertNotNull(kp1.getPrivate());
        assertNotEquals(kp1.getPublic(), kp2.getPublic());
        assertNotEquals(kp1.getPrivate(), kp2.getPrivate());
        assertTrue(
                "Ed25519".equalsIgnoreCase(kp1.getPublic().getAlgorithm()) || "EdDSA".equalsIgnoreCase(kp1.getPublic().getAlgorithm()),
                "Public key algorithm should be Ed25519 or EdDSA"
        );
    }

    @Test
    @DisplayName("Public key Base64 X.509 encoding and decoding round-trip")
    void testPublicKeyRoundTrip() {
        KeyPair keyPair = keyService.generateKeyPair();
        String encoded = keyService.encodePublicKey(keyPair.getPublic());
        assertNotNull(encoded);
        assertFalse(encoded.isBlank());

        PublicKey reconstructed = keyService.decodePublicKey(encoded);
        assertEquals(keyPair.getPublic(), reconstructed);
    }

    @Test
    @DisplayName("Private key PKCS#8 byte encoding and decoding round-trip")
    void testPrivateKeyRoundTrip() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] encoded = keyService.encodePrivateKey(keyPair.getPrivate());
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        PrivateKey reconstructed = keyService.decodePrivateKey(encoded);
        assertEquals(keyPair.getPrivate(), reconstructed);
    }

    @Test
    @DisplayName("Digital signature creation and verification succeeds")
    void testSigningAndVerification() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] payload = "rightfull-entitlement-claim-payload-001".getBytes(StandardCharsets.UTF_8);

        byte[] signature = keyService.sign(payload, keyPair.getPrivate());
        assertNotNull(signature);
        assertEquals(64, signature.length, "Ed25519 signatures must be exactly 64 bytes");

        boolean verified = keyService.verify(payload, signature, keyPair.getPublic());
        assertTrue(verified, "Valid signature must verify successfully");
    }

    @Test
    @DisplayName("Digital signature rejects tampered message payload")
    void testSignatureRejectsTamperedPayload() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] original = "license-valid-until-2030".getBytes(StandardCharsets.UTF_8);
        byte[] tampered = "license-valid-until-2099".getBytes(StandardCharsets.UTF_8);

        byte[] signature = keyService.sign(original, keyPair.getPrivate());
        boolean verified = keyService.verify(tampered, signature, keyPair.getPublic());
        assertFalse(verified, "Signature must fail verification on tampered data");
    }

    @Test
    @DisplayName("Digital signature rejects corrupted signature bytes")
    void testSignatureRejectsCorruptedSignature() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] payload = "client-hardware-fingerprint".getBytes(StandardCharsets.UTF_8);

        byte[] signature = keyService.sign(payload, keyPair.getPrivate());
        signature[0] ^= 0xFF; // Corrupt first byte

        boolean verified = keyService.verify(payload, signature, keyPair.getPublic());
        assertFalse(verified, "Corrupted signature must fail verification");
    }
}
