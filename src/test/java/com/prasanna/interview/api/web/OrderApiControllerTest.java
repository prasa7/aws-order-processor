package com.prasanna.interview.api.web;

import com.prasanna.interview.api.ApiRequestMetadata;
import com.prasanna.interview.api.OrderApiExtensionPoint;
import com.prasanna.interview.api.OrderApiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApiControllerTest {

    @Test
    void returnsAcceptedResponseAndPassesRequestMetadata() {
        OrderApiExtensionPoint extensionPoint = mock(OrderApiExtensionPoint.class);
        OrderApiResponse apiResponse = new OrderApiResponse(
                "PROCESSED",
                "evt-api",
                "processed-api",
                "corr-api",
                "order-api",
                "DIGITAL",
                Instant.parse("2026-06-23T10:30:00Z")
        );
        when(extensionPoint.process(eq("{}"), org.mockito.ArgumentMatchers.any(ApiRequestMetadata.class)))
                .thenReturn(apiResponse);
        OrderApiController controller = new OrderApiController(extensionPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders/process");
        request.addHeader("X-Request-Id", "api-request-001");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.5");

        ResponseEntity<OrderApiResponse> response = controller.process(
                "{}",
                request,
                new TestingAuthenticationToken("partner-service", "N/A")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(apiResponse);
        ArgumentCaptor<ApiRequestMetadata> metadataCaptor = ArgumentCaptor.forClass(ApiRequestMetadata.class);
        verify(extensionPoint).process(eq("{}"), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue())
                .isEqualTo(new ApiRequestMetadata("api-request-001", "partner-service", "203.0.113.10"));
    }
}
