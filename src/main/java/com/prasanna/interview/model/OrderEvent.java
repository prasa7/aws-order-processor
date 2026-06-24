package com.prasanna.interview.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Common contract for supported order-created events.
 *
 * <p>The sealed hierarchy is intentionally closed to {@link DigitalOrder} and {@link PhysicalOrder}, allowing the
 * processing service to use an exhaustive Java pattern matching switch for order-type specific behavior.</p>
 */
public sealed interface OrderEvent permits DigitalOrder, PhysicalOrder {

    /**
     * Unique event id used for idempotency.
     *
     * @return event id
     */
    String eventId();

    /**
     * Correlation id propagated through logs and SNS message attributes.
     *
     * @return correlation id
     */
    String correlationId();

    /**
     * Business order id.
     *
     * @return order id
     */
    String orderId();

    /**
     * Customer id, optionally masked in logs.
     *
     * @return customer id
     */
    String customerId();

    /**
     * Customer email address.
     *
     * @return customer email
     */
    String customerEmail();

    /**
     * Type discriminator used during JSON parsing.
     *
     * @return {@code DIGITAL} or {@code PHYSICAL}
     */
    String orderType();

    /**
     * Order amount.
     *
     * @return monetary amount
     */
    BigDecimal amount();

    /**
     * ISO currency code.
     *
     * @return currency code
     */
    String currency();

    /**
     * Source event timestamp.
     *
     * @return occurrence timestamp
     */
    Instant occurredAt();
}
