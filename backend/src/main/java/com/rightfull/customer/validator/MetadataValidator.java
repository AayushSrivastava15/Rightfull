package com.rightfull.customer.validator;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates customer metadata JSON payload for safety and size limits.
 */
@Component
public class MetadataValidator {

    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{1,64}$");
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile("(?i)(<script|javascript:|eval\\(|exec\\(|\\$\\{|<\\?php)");
    private static final int MAX_ENTRIES = 50;
    private static final int MAX_STRING_LENGTH = 1000;

    public void validate(Map<String, Object> metadata) {
        if (metadata == null) {
            return;
        }

        if (metadata.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Customer metadata exceeds maximum allowed entries (" + MAX_ENTRIES + ")");
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank() || !SAFE_KEY_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid customer metadata key: '" + key + "'");
            }

            Object val = entry.getValue();
            if (val instanceof String str) {
                if (str.length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("Metadata value for key '" + key + "' exceeds max length of " + MAX_STRING_LENGTH);
                }
                if (DANGEROUS_PATTERN.matcher(str).find()) {
                    throw new IllegalArgumentException("Dangerous expression in metadata for key '" + key + "'");
                }
            }
        }
    }
}

