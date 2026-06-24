package com.prasanna.interview.api;

import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.OrderProcessedEvent;
import com.prasanna.interview.service.OrderProcessingOutcome;

import java.time.Instant;

/**
 * Response body returned by the optional HTTP API after processing a direct order event.
 *
 * @param status processing result, currently {@code PROCESSED} or {@code DUPLICATE_SKIPPED}
 * @param eventId original source event id
 * @param processedEventId generated processed event id, absent when the event is skipped as a duplicate
 * @param correlationId caller supplied correlation id
 * @param orderId order identifier
 * @param orderType order type discriminator
 * @param processedAt timestamp for the processed notification, absent for duplicate skips
 */
public record OrderApiResponse(
        String status,
        String eventId,
        String processedEventId,
        String correlationId,
        String orderId,
        String orderType,
        Instant processedAt
) {
    /**
     * Maps an internal processing outcome into the stable API response contract.
     *
     * @param outcome shared service outcome
     * @return HTTP response body
     */
    public static OrderApiResponse from(OrderProcessingOutcome outcome) {
        OrderEvent sourceEvent = outcome.sourceEvent();
        OrderProcessedEvent processedEvent = outcome.processedEvent();
        return new OrderApiResponse(
                outcome.status().name(),
                sourceEvent.eventId(),
                processedEvent == null ? null : processedEvent.eventId(),
                sourceEvent.correlationId(),
                sourceEvent.orderId(),
                sourceEvent.orderType(),
                processedEvent == null ? null : processedEvent.processedAt()
        );
    }
}
