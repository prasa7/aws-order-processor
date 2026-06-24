package com.prasanna.interview.security;

import com.prasanna.interview.config.OrderProcessingProperties;
import org.springframework.stereotype.Component;

/**
 * Sanitizes values before they are written to logs or returned by API error responses.
 */
@Component
public class SecuritySanitizer {

    private final OrderProcessingProperties properties;

    /**
     * Creates a sanitizer with masking behavior from runtime configuration.
     *
     * @param properties runtime properties
     */
    public SecuritySanitizer(OrderProcessingProperties properties) {
        this.properties = properties;
    }

    /**
     * Sanitizes and optionally masks a customer id.
     *
     * @param customerId customer id
     * @return sanitized customer id
     */
    public String sanitizeCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return customerId;
        }
        if (!properties.maskCustomerIdInLogs()) {
            return stripControlCharacters(customerId);
        }
        String clean = stripControlCharacters(customerId);
        if (clean.length() <= 4) {
            return "****";
        }
        return "*".repeat(clean.length() - 4) + clean.substring(clean.length() - 4);
    }

    /**
     * Masks an email address while preserving its domain.
     *
     * @param email email address
     * @return masked email address
     */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String clean = stripControlCharacters(email);
        int at = clean.indexOf('@');
        if (at <= 0 || at == clean.length() - 1) {
            return "***";
        }
        String local = clean.substring(0, at);
        String domain = clean.substring(at + 1);
        return local.charAt(0) + "***@" + domain;
    }

    /**
     * Sanitizes and truncates an error message.
     *
     * @param message raw error message
     * @return sanitized message limited to 500 characters
     */
    public String sanitizeErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        String clean = stripControlCharacters(message);
        return clean.length() > 500 ? clean.substring(0, 500) : clean;
    }

    /**
     * Removes control characters other than newline, carriage return, and tab.
     *
     * @param value raw value
     * @return value without unsafe control characters
     */
    public String stripControlCharacters(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    }
}
