package net.danakube.danarent.plot;

import java.util.Map;

public class ValidationResult {

    private final boolean valid;
    private final String messageKey;
    private final Map<String, String> placeholders;

    private ValidationResult(boolean valid, String messageKey, Map<String, String> placeholders) {
        this.valid = valid;
        this.messageKey = messageKey;
        this.placeholders = placeholders != null ? placeholders : Map.of();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null, Map.of());
    }

    public static ValidationResult error(String messageKey) {
        return new ValidationResult(false, messageKey, Map.of());
    }

    public static ValidationResult error(String messageKey, Map<String, String> placeholders) {
        return new ValidationResult(false, messageKey, placeholders);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }
}
