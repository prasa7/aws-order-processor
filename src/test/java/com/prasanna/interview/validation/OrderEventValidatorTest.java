package com.prasanna.interview.validation;

import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.model.DigitalOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEventValidatorTest {

    @Test
    void rejectsMissingRequiredFields() {
        OrderEventValidator validator = new OrderEventValidator();
        DigitalOrder order = new DigitalOrder(
                "evt-1",
                "corr-1",
                "",
                "customer-1",
                "ada@example.com",
                "DIGITAL",
                BigDecimal.TEN,
                "USD",
                Instant.parse("2026-06-23T10:15:30Z"),
                "EBOOK-001",
                "https://downloads.example.com/order-001"
        );

        assertThatThrownBy(() -> validator.validate(order))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("orderId is required");
    }

    @Test
    void rejectsInvalidDigitalOrderType() {
        OrderEventValidator validator = new OrderEventValidator();
        DigitalOrder order = new DigitalOrder(
                "evt-1",
                "corr-1",
                "order-1",
                "customer-1",
                "ada@example.com",
                "PHYSICAL",
                BigDecimal.TEN,
                "USD",
                Instant.parse("2026-06-23T10:15:30Z"),
                "EBOOK-001",
                "https://downloads.example.com/order-001"
        );

        assertThatThrownBy(() -> validator.validate(order))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DigitalOrder must use orderType DIGITAL");
    }
}
