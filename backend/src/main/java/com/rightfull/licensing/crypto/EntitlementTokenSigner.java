package com.rightfull.licensing.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.product.crypto.Ed25519KeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mints and verifies RFC 7515 / RFC 8037 JWS EdDSA compact tokens for offline license entitlement verification.
 * Format: {@code BASE64URL(Header) . BASE64URL(Payload) . BASE64URL(Signature)}
 */
@Component
public class EntitlementTokenSigner {

    private static final Logger log = LoggerFactory.getLogger(EntitlementTokenSigner.class);

    private final Ed25519KeyService ed25519KeyService;
    private final ObjectMapper objectMapper;

    public EntitlementTokenSigner(Ed25519KeyService ed25519KeyService) {
        this.ed25519KeyService = ed25519KeyService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public record EntitlementTokenClaims(
            String issuer,
            UUID licenseId,
            UUID productId,
            Instant issuedAt,
            Instant notBefore,
            Instant expiresAt,
            Instant validationTime,
            String status,
            String deviceIdentifierHash,
            Map<String, Object> entitlements,
            int offlineGracePeriodSeconds,
            int revalidationIntervalSeconds
    ) {}

    /**
     * Mints a cryptographically signed JWS EdDSA token using the product's Ed25519 private key.
     */
    public String mintToken(EntitlementTokenClaims claims, String keyId, PrivateKey privateKey) {
        try {
            // 1. Build JWS Header (RFC 8037 EdDSA)
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "EdDSA");
            header.put("typ", "JWT");
            header.put("kid", keyId);

            String headerJson = objectMapper.writeValueAsString(header);
            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

            // 2. Build JWS Payload
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", claims.issuer() != null ? claims.issuer() : "rightfull.com");
            payload.put("sub", claims.licenseId().toString());
            payload.put("aud", claims.productId().toString());
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("iat", claims.issuedAt().getEpochSecond());
            payload.put("nbf", claims.notBefore().getEpochSecond());
            if (claims.expiresAt() != null) {
                payload.put("exp", claims.expiresAt().getEpochSecond());
            }
            payload.put("val_ts", claims.validationTime().getEpochSecond());
            payload.put("status", claims.status());
            if (claims.deviceIdentifierHash() != null) {
                payload.put("device_id", claims.deviceIdentifierHash());
            }
            payload.put("offline_grace_seconds", claims.offlineGracePeriodSeconds());
            payload.put("revalidation_interval_seconds", claims.revalidationIntervalSeconds());
            payload.put("entitlements", claims.entitlements() != null ? claims.entitlements() : Map.of());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            // 3. Sign header.payload using Ed25519
            String signingInput = encodedHeader + "." + encodedPayload;
            byte[] signature = ed25519KeyService.sign(signingInput.getBytes(StandardCharsets.UTF_8), privateKey);
            String encodedSignature = base64UrlEncode(signature);

            return signingInput + "." + encodedSignature;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JWS claims to JSON", e);
        }
    }

    /**
     * Verifies the authenticity and cryptographic signature of a JWS EdDSA token using the public key.
     */
    public boolean verifyTokenSignature(String token, PublicKey publicKey) {
        if (token == null || publicKey == null) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] signatureBytes = base64UrlDecode(parts[2]);
            return ed25519KeyService.verify(signingInput.getBytes(StandardCharsets.UTF_8), signatureBytes, publicKey);
        } catch (Exception e) {
            log.debug("Token signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts unverified claims payload from a token (e.g. to inspect kid or aud prior to key lookup).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWS compact serialization");
        }
        try {
            byte[] payloadBytes = base64UrlDecode(parts[1]);
            return objectMapper.readValue(payloadBytes, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWS token payload", e);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }
}
