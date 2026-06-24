package com.prasanna.interview.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order-created event for a digital product.
 *
 * @param eventId unique event id used for idempotency
 * @param correlationId correlation id propagated through logs and notifications
 * @param orderId business order id
 * @param customerId customer id
 * @param customerEmail customer email address
 * @param orderType discriminator value, expected to be {@code DIGITAL}
 * @param amount order amount
 * @param currency ISO currency code
 * @param occurredAt source event timestamp
 * @param productCode purchased digital product code
 * @param downloadUrl fulfillment URL for the digital product
 */
public record DigitalOrder(
        String eventId,
        String correlationId,
        String orderId,
        String customerId,
        String customerEmail,
        String orderType,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String productCode,
        String downloadUrl
) implements OrderEvent {
}
