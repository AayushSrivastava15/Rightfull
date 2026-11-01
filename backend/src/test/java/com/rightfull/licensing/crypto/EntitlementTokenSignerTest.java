package com.rightfull.licensing.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.product.crypto.Ed25519KeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entitlement Token Signer (JWS EdDSA) Tests")
class EntitlementTokenSignerTest {

    private Ed25519KeyService ed25519KeyService;
    private EntitlementTokenSigner tokenSigner;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() {
        ed25519KeyService = new Ed25519KeyService();
        tokenSigner = new EntitlementTokenSigner(ed25519KeyService);
        keyPair = ed25519KeyService.generateKeyPair();
    }

    @Test
    @DisplayName("Should mint and verify valid JWS EdDSA compact entitlement token")
    void shouldMintAndVerifyToken() {
        UUID licenseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expires = now.plus(30, ChronoUnit.DAYS);

        EntitlementTokenSigner.EntitlementTokenClaims claims = new EntitlementTokenSigner.EntitlementTokenClaims(
                "rightfull.com",
                licenseId,
                productId,
                now,
                now,
                expires,
                now,
                "VALID",
                "device_sha256_hash_123",
                Map.of("seats", 5, "features", Map.of("pro_export", true)),
                25200,
                300
        );

        String token = tokenSigner.mintToken(claims, "kid_test_123", keyPair.getPrivate());
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);

        boolean verified = tokenSigner.verifyTokenSignature(token, keyPair.getPublic());
        assertThat(verified).isTrue();

        Map<String, Object> parsed = tokenSigner.parsePayload(token);
        assertThat(parsed.get("iss")).isEqualTo("rightfull.com");
        assertThat(parsed.get("sub")).isEqualTo(licenseId.toString());
        assertThat(parsed.get("aud")).isEqualTo(productId.toString());
        assertThat(parsed.get("status")).isEqualTo("VALID");
        assertThat(parsed.get("entitlements")).isNotNull();
    }

    @Test
    @DisplayName("Should reject tampered JWS token payload or header")
    void shouldRejectTamperedToken() {
        UUID licenseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        EntitlementTokenSigner.EntitlementTokenClaims claims = new EntitlementTokenSigner.EntitlementTokenClaims(
                "rightfull.com",
                licenseId,
                productId,
                now,
                now,
                null, // Perpetual
                now,
                "VALID",
                null,
                Map.of("seats", 1),
                25200,
                300
        );

        String token = tokenSigner.mintToken(claims, "kid_test_123", keyPair.getPrivate());
        String[] parts = token.split("\\.");

        // Tamper payload
        String tamperedPayload = parts[1] + "tamper";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        boolean verified = tokenSigner.verifyTokenSignature(tamperedToken, keyPair.getPublic());
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Should reject token when verified against a different public key")
    void shouldRejectTokenWithWrongPublicKey() {
        KeyPair anotherKeyPair = ed25519KeyService.generateKeyPair();

        EntitlementTokenSigner.EntitlementTokenClaims claims = new EntitlementTokenSigner.EntitlementTokenClaims(
                "rightfull.com",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                null,
                Instant.now(),
                "VALID",
                null,
                Map.of(),
                25200,
                300
        );

        String token = tokenSigner.mintToken(claims, "kid_test_123", keyPair.getPrivate());

        boolean verified = tokenSigner.verifyTokenSignature(token, anotherKeyPair.getPublic());
        assertThat(verified).isFalse();
    }
}
