package com.prasanna.interview.api;

/**
 * API-facing port for exposing order processing outside the SNS/SQS Lambda flow.
 *
 * <p>Adapters such as REST controllers, API Gateway handlers, or future GraphQL endpoints should depend on this
 * interface instead of calling the Lambda handler. The implementation accepts direct order JSON, reuses the same
 * validation and idempotency rules as the Lambda path, and publishes the same processed notification to SNS.</p>
 */
public interface OrderApiExtensionPoint {

    /**
     * Processes a direct order-event JSON payload.
     *
     * @param orderJson direct {@code DigitalOrder} or {@code PhysicalOrder} JSON, not an SNS envelope
     * @param metadata request metadata for logging and traceability
     * @return API response describing whether the event was processed or skipped as a duplicate
     */
    OrderApiResponse process(String orderJson, ApiRequestMetadata metadata);
}
