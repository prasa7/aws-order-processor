package com.prasanna.interview.model;

import java.time.Instant;

/**
 * Notification published to the processed-order SNS topic after a successful order-processing workflow.
 *
 * @param eventId generated processed-event id
 * @param sourceEventId original order-created event id
 * @param correlationId correlation id copied from the source event
 * @param orderId business order id
 * @param customerId customer id copied from the source event
 * @param orderType processed order type
 * @param processingStatus processing status, currently {@code PROCESSED}
 * @param processedAt timestamp when processing completed
 * @param serviceName service that produced the notification
 * @param environment environment that produced the notification
 */
public record OrderProcessedEvent(
        String eventId,
        String sourceEventId,
        String correlationId,
        String orderId,
        String customerId,
        String orderType,
        String processingStatus,
        Instant processedAt,
        String serviceName,
        String environment
) {
}
