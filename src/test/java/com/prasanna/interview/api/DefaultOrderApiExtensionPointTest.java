package com.prasanna.interview.api;

import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.OrderProcessedEvent;
import com.prasanna.interview.observability.ProcessingLogContext;
import com.prasanna.interview.service.OrderProcessingOutcome;
import com.prasanna.interview.service.OrderProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultOrderApiExtensionPointTest {

    @Test
    void delegatesApiPayloadToSharedProcessorAndMapsProcessedResponse() throws Exception {
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        String orderJson = TestFixtures.digitalOrderJson("evt-api");
        DigitalOrder order = objectMapper.readValue(orderJson, DigitalOrder.class);
        OrderProcessedEvent processedEvent = new OrderProcessedEvent(
                "processed-api",
                order.eventId(),
                order.correlationId(),
                order.orderId(),
                order.customerId(),
                order.orderType(),
                "PROCESSED",
                Instant.parse("2026-06-23T10:30:00Z"),
                "order-processor-test",
                "test"
        );
        OrderProcessingService service = mock(OrderProcessingService.class);
        when(service.processApiPayload(eq(orderJson), org.mockito.ArgumentMatchers.any(ProcessingLogContext.class)))
                .thenReturn(OrderProcessingOutcome.processed(order, processedEvent));
        DefaultOrderApiExtensionPoint extensionPoint = new DefaultOrderApiExtensionPoint(service);

        OrderApiResponse response = extensionPoint.process(
                orderJson,
                new ApiRequestMetadata("api-request-001", "partner-service", "203.0.113.10")
        );

        assertThat(response.status()).isEqualTo("PROCESSED");
        assertThat(response.eventId()).isEqualTo("evt-api");
        assertThat(response.processedEventId()).isEqualTo("processed-api");
        assertThat(response.orderId()).isEqualTo("order-001");

        ArgumentCaptor<ProcessingLogContext> contextCaptor = ArgumentCaptor.forClass(ProcessingLogContext.class);
        verify(service).processApiPayload(eq(orderJson), contextCaptor.capture());
        assertThat(contextCaptor.getValue().asFields())
                .containsEntry("apiRequestId", "api-request-001")
                .containsEntry("apiPrincipal", "partner-service")
                .containsEntry("sourceIp", "203.0.113.10");
    }

    @Test
    void mapsDuplicateOutcomeWithoutProcessedEventId() throws Exception {
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        String orderJson = TestFixtures.digitalOrderJson("evt-duplicate-api");
        DigitalOrder order = objectMapper.readValue(orderJson, DigitalOrder.class);
        OrderProcessingService service = mock(OrderProcessingService.class);
        when(service.processApiPayload(eq(orderJson), org.mockito.ArgumentMatchers.any(ProcessingLogContext.class)))
                .thenReturn(OrderProcessingOutcome.duplicateSkipped(order));
        DefaultOrderApiExtensionPoint extensionPoint = new DefaultOrderApiExtensionPoint(service);

        OrderApiResponse response = extensionPoint.process(orderJson, null);

        assertThat(response.status()).isEqualTo("DUPLICATE_SKIPPED");
        assertThat(response.eventId()).isEqualTo("evt-duplicate-api");
        assertThat(response.processedEventId()).isNull();
        assertThat(response.processedAt()).isNull();
    }
}
