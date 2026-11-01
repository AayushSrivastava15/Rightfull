package com.rightfull.licensing.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("License Key Generator Cryptographic Tests")
class LicenseKeyGeneratorTest {

    private static final String TEST_SECRET = "super_secure_hmac_secret_for_licensing_tests_32_chars";
    private LicenseKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new LicenseKeyGenerator(TEST_SECRET);
        generator.validateConfiguration();
    }

    @Test
    @DisplayName("Should generate high-entropy license keys with documented format")
    void shouldGenerateFormattedLicenseKey() {
        LicenseKeyGenerator.GeneratedLicenseKey key = generator.generate();

        assertThat(key).isNotNull();
        assertThat(key.rawKey()).isNotNull();
        assertThat(key.rawKey()).startsWith("RF-");
        // Format: RF-XXXX-XXXX-XXXX-XXXX-XXXX (27 chars)
        assertThat(key.rawKey()).matches("^RF-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}-[2-9A-Z]{4}$");
        assertThat(key.keyPrefix()).startsWith("RF-");
        assertThat(key.lastFour()).hasSize(4);
        assertThat(key.maskedKey()).isEqualTo(key.keyPrefix() + "-••••-••••-••••-" + key.lastFour());
        assertThat(key.keyHash()).hasSize(64); // SHA-256 hex digest
    }

    @Test
    @DisplayName("Generated keys should be distinct and unpredictable")
    void shouldGenerateUniqueKeys() {
        LicenseKeyGenerator.GeneratedLicenseKey key1 = generator.generate();
        LicenseKeyGenerator.GeneratedLicenseKey key2 = generator.generate();

        assertThat(key1.rawKey()).isNotEqualTo(key2.rawKey());
        assertThat(key1.keyHash()).isNotEqualTo(key2.keyHash());
    }

    @Test
    @DisplayName("Should verify raw key against computed HMAC-SHA-256 digest in constant time")
    void shouldVerifyKeyDigest() {
        LicenseKeyGenerator.GeneratedLicenseKey key = generator.generate();

        boolean matches = generator.verifyKey(key.rawKey(), key.keyHash());
        assertThat(matches).isTrue();

        // Tampered key should fail verification
        String tamperedKey = key.rawKey().substring(0, key.rawKey().length() - 1) + "X";
        assertThat(generator.verifyKey(tamperedKey, key.keyHash())).isFalse();

        // Null / blank cases
        assertThat(generator.verifyKey(null, key.keyHash())).isFalse();
        assertThat(generator.verifyKey(key.rawKey(), null)).isFalse();
    }

    @Test
    @DisplayName("Normalizes license key inputs (trims and converts to uppercase)")
    void shouldNormalizeKeyInputs() {
        String inputWithSpaces = "  rf-2k4p-88xm-t7qq-v88a-92z1  ";
        String normalized = generator.normalizeKey(inputWithSpaces);

        assertThat(normalized).isEqualTo("RF-2K4P-88XM-T7QQ-V88A-92Z1");
        assertThat(generator.hashKey(inputWithSpaces)).isEqualTo(generator.hashKey(normalized));
    }

    @Test
    @DisplayName("Rejects startup if HMAC secret is shorter than 32 characters")
    void shouldFailOnShortHmacSecret() {
        LicenseKeyGenerator weakGenerator = new LicenseKeyGenerator("too_short_secret");
        assertThatThrownBy(weakGenerator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters long");
    }
}

