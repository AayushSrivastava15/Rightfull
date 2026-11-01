package com.rightfull.customer.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Customer Metadata Validator Tests")
class MetadataValidatorTest {

    private MetadataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MetadataValidator();
    }

    @Test
    @DisplayName("Valid metadata map passes validation")
    void testValidMetadataPasses() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("country", "US");
        meta.put("tier", "enterprise");
        meta.put("account_id", 12345);

        assertThatCode(() -> validator.validate(meta)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null metadata passes without error")
    void testNullMetadataPasses() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dangerous expression in metadata value is rejected")
    void testDangerousStringRejected() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("note", "<script>alert('pwned')</script>");

        assertThatThrownBy(() -> validator.validate(meta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dangerous expression");
    }

    @Test
    @DisplayName("Invalid metadata key format is rejected")
    void testInvalidKeyRejected() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("invalid key with space", "value");

        assertThatThrownBy(() -> validator.validate(meta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid customer metadata key");
    }
}

