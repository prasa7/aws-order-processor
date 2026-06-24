package com.prasanna.interview.service;

import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.OrderProcessedEvent;

/**
 * Result of processing one normalized order event.
 *
 * @param status outcome status for downstream adapters
 * @param sourceEvent parsed source order event
 * @param processedEvent published processed event, absent when processing was skipped as a duplicate
 */
public record OrderProcessingOutcome(
        Status status,
        OrderEvent sourceEvent,
        OrderProcessedEvent processedEvent
) {
    /**
     * Creates an outcome for a successfully processed and published order.
     *
     * @param sourceEvent original source event
     * @param processedEvent processed notification event
     * @return processed outcome
     */
    public static OrderProcessingOutcome processed(OrderEvent sourceEvent, OrderProcessedEvent processedEvent) {
        return new OrderProcessingOutcome(Status.PROCESSED, sourceEvent, processedEvent);
    }

    /**
     * Creates an outcome for an event skipped because the event id was already processed.
     *
     * @param sourceEvent duplicate source event
     * @return duplicate-skip outcome
     */
    public static OrderProcessingOutcome duplicateSkipped(OrderEvent sourceEvent) {
        return new OrderProcessingOutcome(Status.DUPLICATE_SKIPPED, sourceEvent, null);
    }

    /**
     * Processing states exposed to adapters.
     */
    public enum Status {
        /**
         * The order was processed and a processed notification was published.
         */
        PROCESSED,
        /**
         * The event id was already processed and no additional notification was published.
         */
        DUPLICATE_SKIPPED
    }
}
