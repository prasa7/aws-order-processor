package com.prasanna.interview.validation;

import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.PhysicalOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Performs required-field validation for parsed order events.
 *
 * <p>JSON shape and unknown-field behavior are handled by {@link OrderEventParser}; this validator checks the
 * business-level fields common to all orders and the additional fields required by each concrete order type.</p>
 */
@Component
public class OrderEventValidator {

    /**
     * Creates the order event validator.
     */
    public OrderEventValidator() {
    }

    /**
     * Validates a parsed order event.
     *
     * @param orderEvent parsed order event
     * @throws ValidationException when a required field is missing or invalid
     */
    public void validate(OrderEvent orderEvent) {
        requireText(orderEvent.eventId(), "eventId");
        requireText(orderEvent.correlationId(), "correlationId");
        requireText(orderEvent.orderId(), "orderId");
        requireText(orderEvent.customerId(), "customerId");
        requireText(orderEvent.customerEmail(), "customerEmail");
        requireText(orderEvent.orderType(), "orderType");
        requireText(orderEvent.currency(), "currency");
        if (orderEvent.occurredAt() == null) {
            throw new ValidationException("occurredAt is required");
        }
        if (orderEvent.amount() == null || orderEvent.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than zero");
        }

        switch (orderEvent) {
            case DigitalOrder digitalOrder -> validateDigitalOrder(digitalOrder);
            case PhysicalOrder physicalOrder -> validatePhysicalOrder(physicalOrder);
        }
    }

    private void validateDigitalOrder(DigitalOrder order) {
        if (!"DIGITAL".equals(order.orderType())) {
            throw new ValidationException("DigitalOrder must use orderType DIGITAL");
        }
        requireText(order.productCode(), "productCode");
        requireText(order.downloadUrl(), "downloadUrl");
    }

    private void validatePhysicalOrder(PhysicalOrder order) {
        if (!"PHYSICAL".equals(order.orderType())) {
            throw new ValidationException("PhysicalOrder must use orderType PHYSICAL");
        }
        requireText(order.shippingAddress(), "shippingAddress");
        requireText(order.shippingMethod(), "shippingMethod");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
    }
}
