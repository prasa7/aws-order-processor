package com.prasanna.interview.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Runtime configuration for Lambda processing, optional API exposure, validation, logging, idempotency, and caching.
 *
 * @param serviceName logical service name used in logs and metrics
 * @param environment deployment environment name
 * @param awsRegion AWS region used by SDK clients
 * @param orderProcessedTopicArn SNS topic ARN that receives processed notifications
 * @param enableIdempotency enables event-id deduplication when a production implementation is wired
 * @param maxRetryCount reserved retry-count setting for downstream retry policies
 * @param structuredLogging emits JSON logs when enabled
 * @param referenceDataCacheTtlSeconds reference-data cache TTL in seconds
 * @param maxPayloadSizeBytes maximum accepted SQS, SNS message, or API body size
 * @param maskCustomerIdInLogs masks customer ids in structured logs when enabled
 * @param enableStrictJsonValidation rejects unknown JSON fields when enabled
 * @param enableVirtualThreads processes SQS batch records with virtual threads when enabled
 * @param enableNotificationPublishing publishes processed notifications to SNS when enabled
 * @param api optional secured HTTP API configuration
 */
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "order")
public record OrderProcessingProperties(
        @NotBlank String serviceName,
        @NotBlank String environment,
        @NotBlank String awsRegion,
        @NotBlank String orderProcessedTopicArn,
        boolean enableIdempotency,
        @Min(0) @Max(100) int maxRetryCount,
        boolean structuredLogging,
        @Min(1) long referenceDataCacheTtlSeconds,
        @Min(1024) int maxPayloadSizeBytes,
        boolean maskCustomerIdInLogs,
        boolean enableStrictJsonValidation,
        boolean enableVirtualThreads,
        boolean enableNotificationPublishing,
        Api api
) {
    /**
     * Applies defaults for nested API configuration when the configuration block is absent.
     */
    public OrderProcessingProperties {
        if (api == null) {
            api = new Api(false, "/api/v1/orders", "Authorization", "", "order-api-client");
        }
    }

    /**
     * Configuration for the optional servlet HTTP API.
     *
     * @param enabled creates the API controller and security chain when true
     * @param basePath base URL path for the order API
     * @param authHeaderName header used by the API token filter
     * @param authToken expected token value, supplied from a secret-backed environment variable
     * @param principalName authenticated principal name used in structured logs
     */
    public record Api(
            boolean enabled,
            @NotBlank String basePath,
            @NotBlank String authHeaderName,
            String authToken,
            @NotBlank String principalName
    ) {
    }
}
