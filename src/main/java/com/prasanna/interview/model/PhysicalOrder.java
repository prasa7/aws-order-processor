package com.prasanna.interview.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order-created event for a physical shipment.
 *
 * @param eventId unique event id used for idempotency
 * @param correlationId correlation id propagated through logs and notifications
 * @param orderId business order id
 * @param customerId customer id
 * @param customerEmail customer email address
 * @param orderType discriminator value, expected to be {@code PHYSICAL}
 * @param amount order amount
 * @param currency ISO currency code
 * @param occurredAt source event timestamp
 * @param shippingAddress destination address for fulfillment
 * @param shippingMethod requested shipping method
 */
public record PhysicalOrder(
        String eventId,
        String correlationId,
        String orderId,
        String customerId,
        String customerEmail,
        String orderType,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String shippingAddress,
        String shippingMethod
) implements OrderEvent {
}
