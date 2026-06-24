package com.prasanna.interview.validation;

import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Validates required payload text and maximum UTF-8 byte size.
 */
@Component
public class PayloadSizeValidator {

    private final OrderProcessingProperties properties;

    /**
     * Creates the validator with payload-size configuration.
     *
     * @param properties runtime properties containing the maximum payload size
     */
    public PayloadSizeValidator(OrderProcessingProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates that a payload is present and within the configured byte-size limit.
     *
     * @param payload payload text
     * @param payloadName human-readable payload name used in validation messages
     */
    public void validate(String payload, String payloadName) {
        if (payload == null || payload.isBlank()) {
            throw new ValidationException(payloadName + " is required");
        }
        int byteCount = payload.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount > properties.maxPayloadSizeBytes()) {
            throw new ValidationException(payloadName + " exceeds maximum payload size of "
                    + properties.maxPayloadSizeBytes() + " bytes");
        }
    }
}
