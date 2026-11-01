package com.rightfull.plan.validator;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Validates structured license configuration entitlements as pure JSON data.
 * <p>
 * Supported Types and Documented Limits:
 * <ul>
 *   <li><b>Boolean:</b> {@code true} or {@code false} (feature flags).</li>
 *   <li><b>Number:</b> Finite, non-negative integers or decimals (quotas, limits, rate thresholds).</li>
 *   <li><b>String:</b> Static text data up to 256 characters. Treated strictly as data; technical strings
 *       containing SQL keywords (e.g. SELECT, DROP) or script tokens (e.g. script, eval) are valid data.</li>
 *   <li><b>List&lt;String&gt;:</b> Up to 50 string elements, each max 64 characters (e.g. enabled modules, roles).</li>
 *   <li><b>Map&lt;String, Object&gt;:</b> Nested JSON sections (e.g. 'features', 'limits', 'modules') up to max depth of 3.</li>
 * </ul>
 * <p>
 * Security & Structural Limits:
 * <ul>
 *   <li>Total Entries: Max 100 entries across the entire JSON tree.</li>
 *   <li>Nesting Depth: Max 3 levels (root level = 1).</li>
 *   <li>Key Format: 1 to 64 characters, lowercase alphanumeric with hyphens, underscores, or dots ({@code ^[a-z0-9][a-z0-9_.-]{0,63}$}).</li>
 *   <li>Execution Safety: Entitlement data is never evaluated, compiled, or executed by the server.</li>
 * </ul>
 */
@Component
public class EntitlementValidator {

    public static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_.-]{0,63}$");
    public static final int MAX_NESTING_DEPTH = 3;
    public static final int MAX_ENTITLEMENT_ENTRIES = 100;
    public static final int MAX_STRING_VALUE_LENGTH = 256;
    public static final int MAX_LIST_ELEMENTS = 50;
    public static final int MAX_LIST_STRING_LENGTH = 64;

    public void validate(Map<String, Object> entitlements) {
        if (entitlements == null) {
            return;
        }

        AtomicInteger entryCount = new AtomicInteger(0);
        validateMap(entitlements, 1, entryCount);
    }

    @SuppressWarnings("unchecked")
    private void validateMap(Map<?, ?> map, int currentDepth, AtomicInteger totalEntries) {
        if (currentDepth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("Entitlement nesting depth exceeds maximum allowed limit of " + MAX_NESTING_DEPTH);
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (totalEntries.incrementAndGet() > MAX_ENTITLEMENT_ENTRIES) {
                throw new IllegalArgumentException("Entitlements map exceeds maximum allowed entries (" + MAX_ENTITLEMENT_ENTRIES + ")");
            }

            Object rawKey = entry.getKey();
            if (!(rawKey instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException("Entitlement key cannot be null or blank");
            }

            if (!SAFE_KEY_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid entitlement key format: '" + key + "'. Keys must be lowercase alphanumeric, with underscores, hyphens, or dots, length 1-64.");
            }

            Object value = entry.getValue();
            if (value == null) {
                throw new IllegalArgumentException("Entitlement value for key '" + key + "' cannot be null");
            }

            validateValue(key, value, currentDepth, totalEntries);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateValue(String key, Object value, int currentDepth, AtomicInteger totalEntries) {
        if (value instanceof Boolean) {
            return;
        }

        if (value instanceof Number number) {
            double doubleVal = number.doubleValue();
            if (Double.isNaN(doubleVal) || Double.isInfinite(doubleVal)) {
                throw new IllegalArgumentException("Entitlement numeric value for key '" + key + "' must be finite");
            }
            if (number.longValue() < 0) {
                throw new IllegalArgumentException("Entitlement numeric quota for key '" + key + "' cannot be negative");
            }
            return;
        }

        if (value instanceof String str) {
            if (str.length() > MAX_STRING_VALUE_LENGTH) {
                throw new IllegalArgumentException("Entitlement string value for key '" + key + "' exceeds max length of " + MAX_STRING_VALUE_LENGTH);
            }
            // Strings are treated strictly as JSON data values without code execution or naive keyword blocking
            return;
        }

        if (value instanceof Collection<?> collection) {
            if (collection.size() > MAX_LIST_ELEMENTS) {
                throw new IllegalArgumentException("Entitlement list for key '" + key + "' exceeds max elements (" + MAX_LIST_ELEMENTS + ")");
            }
            for (Object item : collection) {
                if (!(item instanceof String itemStr)) {
                    throw new IllegalArgumentException("Entitlement list for key '" + key + "' must contain only strings");
                }
                if (itemStr.length() > MAX_LIST_STRING_LENGTH) {
                    throw new IllegalArgumentException("List element in '" + key + "' exceeds max length of " + MAX_LIST_STRING_LENGTH);
                }
            }
            return;
        }

        if (value instanceof Map<?, ?> nestedMap) {
            validateMap(nestedMap, currentDepth + 1, totalEntries);
            return;
        }

        throw new IllegalArgumentException("Unsupported entitlement value type for key '" + key + "': " + value.getClass().getSimpleName() + ". Allowed types: Boolean, Number, String, List<String>, Map<String, Object>.");
    }
}
