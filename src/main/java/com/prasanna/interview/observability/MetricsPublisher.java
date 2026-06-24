package com.prasanna.interview.observability;

import com.prasanna.interview.config.OrderProcessingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits CloudWatch Embedded Metric Format JSON through Log4j2.
 */
@Component
public class MetricsPublisher {

    private static final Logger LOGGER = LogManager.getLogger(MetricsPublisher.class);

    /** Count of successfully processed order messages. */
    public static final String ORDER_MESSAGE_PROCESSED = "OrderMessageProcessed";
    /** Count of order messages that failed validation, processing, or notification publishing. */
    public static final String ORDER_MESSAGE_FAILED = "OrderMessageFailed";
    /** Processing duration in milliseconds. */
    public static final String ORDER_PROCESSING_DURATION_MS = "OrderProcessingDurationMs";
    /** Count of processed digital orders. */
    public static final String DIGITAL_ORDER_PROCESSED = "DigitalOrderProcessed";
    /** Count of processed physical orders. */
    public static final String PHYSICAL_ORDER_PROCESSED = "PhysicalOrderProcessed";
    /** Count of processed notifications successfully published to SNS. */
    public static final String ORDER_PROCESSED_NOTIFICATION_PUBLISHED = "OrderProcessedNotificationPublished";
    /** Count of processed notifications that failed to publish to SNS. */
    public static final String ORDER_PROCESSED_NOTIFICATION_FAILED = "OrderProcessedNotificationFailed";
    /** Count of validation and JSON parsing failures. */
    public static final String VALIDATION_FAILED = "ValidationFailed";
    /** Count of duplicate event ids skipped by idempotency. */
    public static final String DUPLICATE_EVENT_SKIPPED = "DuplicateEventSkipped";
    /** Count of reference-data cache hits. */
    public static final String REFERENCE_DATA_CACHE_HIT = "ReferenceDataCacheHit";
    /** Count of reference-data cache misses. */
    public static final String REFERENCE_DATA_CACHE_MISS = "ReferenceDataCacheMiss";
    /** Count of failed reference-data loads. */
    public static final String REFERENCE_DATA_CACHE_LOAD_FAILED = "ReferenceDataCacheLoadFailed";

    private final ObjectMapper objectMapper;
    private final OrderProcessingProperties properties;

    /**
     * Creates the metrics publisher.
     *
     * @param objectMapper JSON mapper used to serialize EMF payloads
     * @param properties service and environment properties used as dimensions
     */
    public MetricsPublisher(ObjectMapper objectMapper, OrderProcessingProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Emits a count metric with value {@code 1}.
     *
     * @param metricName metric name
     */
    public void count(String metricName) {
        metric(metricName, 1, "Count");
    }

    /**
     * Emits a duration metric in milliseconds.
     *
     * @param metricName metric name
     * @param durationMs duration value in milliseconds
     */
    public void duration(String metricName, long durationMs) {
        metric(metricName, durationMs, "Milliseconds");
    }

    private void metric(String metricName, Number value, String unit) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("_aws", Map.of(
                "Timestamp", Instant.now().toEpochMilli(),
                "CloudWatchMetrics", List.of(Map.of(
                        "Namespace", properties.serviceName(),
                        "Dimensions", List.of(List.of("Service", "Environment")),
                        "Metrics", List.of(Map.of("Name", metricName, "Unit", unit))
                ))
        ));
        metric.put("Service", properties.serviceName());
        metric.put("Environment", properties.environment());
        metric.put(metricName, value);

        try {
            LOGGER.info(objectMapper.writeValueAsString(metric));
        } catch (JsonProcessingException e) {
            LOGGER.info(metric);
        }
    }
}
