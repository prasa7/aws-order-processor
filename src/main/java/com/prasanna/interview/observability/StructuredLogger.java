package com.prasanna.interview.observability;

import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.security.SecuritySanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Emits structured order-processing logs through Log4j2.
 *
 * <p>When structured logging is enabled, log entries are single-line JSON objects suitable for CloudWatch Logs
 * Insights. Sensitive fields are sanitized or masked before output.</p>
 */
@Component
public class StructuredLogger {

    private static final Logger LOGGER = LogManager.getLogger(StructuredLogger.class);

    private final ObjectMapper objectMapper;
    private final OrderProcessingProperties properties;
    private final SecuritySanitizer sanitizer;

    /**
     * Creates the structured logger.
     *
     * @param objectMapper JSON mapper used to serialize structured fields
     * @param properties service, environment, and logging configuration
     * @param sanitizer sanitizer for messages and customer identifiers
     */
    public StructuredLogger(ObjectMapper objectMapper,
                            OrderProcessingProperties properties,
                            SecuritySanitizer sanitizer) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.sanitizer = sanitizer;
    }

    /**
     * Writes an informational structured log event.
     *
     * @param message event message
     * @param context processing context fields
     */
    public void info(String message, ProcessingLogContext context) {
        log("INFO", message, context, null);
    }

    /**
     * Writes a warning structured log event.
     *
     * @param message event message
     * @param context processing context fields
     */
    public void warn(String message, ProcessingLogContext context) {
        log("WARN", message, context, null);
    }

    /**
     * Writes an error structured log event.
     *
     * @param message event message
     * @param context processing context fields
     * @param throwable failure cause
     */
    public void error(String message, ProcessingLogContext context, Throwable throwable) {
        log("ERROR", message, context, throwable);
    }

    private void log(String level, String message, ProcessingLogContext context, Throwable throwable) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", Instant.now().toString());
        fields.put("level", level);
        fields.put("service", properties.serviceName());
        fields.put("environment", properties.environment());
        fields.put("message", sanitizer.stripControlCharacters(message));
        if (context != null) {
            fields.putAll(context.asFields());
        }
        if (throwable != null) {
            fields.put("errorType", throwable.getClass().getSimpleName());
            fields.put("errorMessage", sanitizer.sanitizeErrorMessage(throwable.getMessage()));
        }
        fields.computeIfPresent("customerId", (key, value) -> sanitizer.sanitizeCustomerId(Objects.toString(value, null)));
        fields.values().removeIf(Objects::isNull);

        if (!properties.structuredLogging()) {
            logAtLevel(level, level + " " + fields);
            return;
        }

        try {
            logAtLevel(level, objectMapper.writeValueAsString(fields));
        } catch (JsonProcessingException e) {
            logAtLevel(level, level + " " + fields);
        }
    }

    private void logAtLevel(String level, String logLine) {
        switch (level) {
            case "ERROR" -> LOGGER.error(logLine);
            case "WARN" -> LOGGER.warn(logLine);
            default -> LOGGER.info(logLine);
        }
    }
}
