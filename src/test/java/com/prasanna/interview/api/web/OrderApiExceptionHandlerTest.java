package com.prasanna.interview.api.web;

import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.exception.NotificationPublishException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.security.SecuritySanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApiExceptionHandlerTest {

    private final OrderApiExceptionHandler handler = new OrderApiExceptionHandler(
            new SecuritySanitizer(TestFixtures.properties())
    );

    @Test
    void mapsValidationFailuresToBadRequest() {
        ResponseEntity<OrderApiErrorResponse> response = handler.badRequest(
                new ValidationException("bad\u0000payload")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("badpayload");
    }

    @Test
    void mapsNotificationFailuresToBadGateway() {
        ResponseEntity<OrderApiErrorResponse> response = handler.badGateway(
                new NotificationPublishException("sns unavailable", new RuntimeException("downstream"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
    }
}
