package com.rightfull.licensing.crypto;

import com.rightfull.common.util.CryptoUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * High-entropy cryptographic license key generator and HMAC-SHA-256 hasher.
 * <p>
 * Key Format: {@code RF-<PREFIX>-<CHUNK1>-<CHUNK2>-<CHUNK3>-<LAST4>}
 * (e.g. {@code RF-7K2M-9X42-M7PQ-K28D-W9A1})
 * Total entropy exceeds 80 bits. Plaintext is never persisted to PostgreSQL.
 */
@Component
public class LicenseKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(LicenseKeyGenerator.class);

    // Crockford Base32-inspired alphabet (uppercase, omitting 0, O, 1, I to prevent user ambiguity)
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^RF-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}$");

    private final String hmacSecret;
    private final SecureRandom secureRandom = new SecureRandom();

    public record GeneratedLicenseKey(
            String rawKey,
            String keyHash,
            String keyPrefix,
            String lastFour,
            String maskedKey
    ) {}

    public LicenseKeyGenerator(@Value("${rightfull.security.hmac-secret:}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (hmacSecret == null || hmacSecret.trim().length() < 32) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY CONFIGURATION ERROR: 'rightfull.security.hmac-secret' must be at least 32 characters long. License key digest security depends on high HMAC entropy."
            );
        }
        log.info("LicenseKeyGenerator initialized with secure HMAC-SHA-256 secret verification passed.");
    }

    /**
     * Generates a new cryptographically secure license key.
     */
    public GeneratedLicenseKey generate() {
        String prefixPart = generateChunk(4);
        String chunk1 = generateChunk(4);
        String chunk2 = generateChunk(4);
        String chunk3 = generateChunk(4);
        String lastFour = generateChunk(4);

        String fullPrefix = "RF-" + prefixPart;
        String rawKey = fullPrefix + "-" + chunk1 + "-" + chunk2 + "-" + chunk3 + "-" + lastFour;
        String keyHash = hashKey(rawKey);
        String maskedKey = fullPrefix + "-••••-••••-••••-" + lastFour;

        return new GeneratedLicenseKey(rawKey, keyHash, fullPrefix, lastFour, maskedKey);
    }

    /**
     * Normalizes a user-provided license key (trimming and converting to uppercase).
     */
    public String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.trim().toUpperCase();
    }

    /**
     * Computes the HMAC-SHA-256 hash of a normalized license key using the server secret.
     */
    public String hashKey(String rawKey) {
        String normalized = normalizeKey(rawKey);
        return CryptoUtils.hmacSha256(normalized, hmacSecret);
    }

    /**
     * Verifies whether a candidate raw key matches the expected HMAC-SHA-256 hash in constant time.
     */
    public boolean verifyKey(String candidateRawKey, String expectedHash) {
        if (candidateRawKey == null || expectedHash == null) {
            return false;
        }
        String computedHash = hashKey(candidateRawKey);
        return CryptoUtils.constantTimeEquals(computedHash, expectedHash);
    }

    /**
     * Produces a masked key string given the key prefix and last four characters.
     */
    public String mask(String prefix, String lastFour) {
        if (prefix == null || lastFour == null) {
            return "RF-••••-••••-••••-••••";
        }
        return prefix + "-••••-••••-••••-" + lastFour;
    }

    private String generateChunk(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}

