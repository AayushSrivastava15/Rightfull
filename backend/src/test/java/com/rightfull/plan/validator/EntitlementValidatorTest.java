package com.rightfull.plan.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entitlement Validator Security & Typing Unit Tests")
class EntitlementValidatorTest {

    private EntitlementValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EntitlementValidator();
    }

    @Test
    @DisplayName("Valid flat entitlements pass validation cleanly")
    void testValidFlatEntitlementsPass() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("export_pdf", true);
        entitlements.put("max_projects", 10);
        entitlements.put("api_rate_limit", 5000L);
        entitlements.put("tier", "enterprise");
        entitlements.put("modules", List.of("analytics", "reporting", "sso"));

        assertThatCode(() -> validator.validate(entitlements)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Complex structured entitlements matching prompt example pass cleanly")
    void testPromptExampleEntitlementsPass() {
        Map<String, Object> entitlements = Map.of(
                "features", Map.of(
                        "pdf_export", true,
                        "team_collaboration", true,
                        "advanced_analytics", false
                ),
                "limits", Map.of(
                        "max_projects", 50,
                        "max_users", 5,
                        "api_requests_per_day", 10000
                ),
                "modules", Map.of(
                        "reporting", true,
                        "automation", false
                )
        );

        assertThatCode(() -> validator.validate(entitlements)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Legitimate strings containing SELECT, DROP, script, eval pass as valid data")
    void testLegitimateTechnicalStringsPass() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("default_query", "SELECT id, name FROM users");
        entitlements.put("action_on_expire", "DROP_CACHE");
        entitlements.put("script_engine", "lua_script_v2");
        entitlements.put("evaluation_mode", "lazy_eval");
        entitlements.put("query_filter", "SELECT * FROM metrics WHERE error = true");
        entitlements.put("lifecycle_hook", "pre_drop_hook");
        entitlements.put("runner", List.of("bash_script", "eval_worker"));

        assertThatCode(() -> validator.validate(entitlements)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null entitlements map passes without error")
    void testNullEntitlementsPasses() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Blank or null entitlement key is rejected")
    void testBlankKeyRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("", true);

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("Key with invalid characters (uppercase or spaces) is rejected")
    void testInvalidKeyFormatRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("Invalid Key With Spaces!", true);

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid entitlement key format");
    }

    @Test
    @DisplayName("Null entitlement value is rejected")
    void testNullValueRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("null_feature", null);

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Negative quota value is rejected")
    void testNegativeQuotaRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("max_projects", -5);

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Infinite or NaN quota is rejected")
    void testInfiniteQuotaRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("max_projects", Double.POSITIVE_INFINITY);

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be finite");
    }

    @Test
    @DisplayName("String exceeding maximum length of 256 is rejected")
    void testStringLengthExceededRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("long_text", "a".repeat(300));

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds max length");
    }

    @Test
    @DisplayName("List containing non-string items is rejected")
    void testNonStringListRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("roles", List.of(1, 2, 3));

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must contain only strings");
    }

    @Test
    @DisplayName("List element exceeding max length of 64 is rejected")
    void testListElementLengthExceededRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("roles", List.of("a".repeat(70)));

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds max length");
    }

    @Test
    @DisplayName("Nesting beyond maximum allowed depth (3) is rejected")
    void testExcessiveNestingRejected() {
        Map<String, Object> level4 = Map.of("deep_flag", true);
        Map<String, Object> level3 = Map.of("level3", level4);
        Map<String, Object> level2 = Map.of("level2", level3);
        Map<String, Object> level1 = Map.of("level1", level2);

        assertThatThrownBy(() -> validator.validate(level1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nesting depth exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("Unsupported arbitrary Java objects are rejected")
    void testUnsupportedObjectRejected() {
        Map<String, Object> entitlements = new HashMap<>();
        entitlements.put("custom_object", new java.util.Date());

        assertThatThrownBy(() -> validator.validate(entitlements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported entitlement value type");
    }
}
