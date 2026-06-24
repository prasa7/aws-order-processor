package com.prasanna.interview.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.cache.ReferenceDataCache;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.exception.MalformedJsonException;
import com.prasanna.interview.exception.NotificationPublishException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.observability.MetricsPublisher;
import com.prasanna.interview.observability.ProcessingLogContext;
import com.prasanna.interview.observability.StructuredLogger;
import com.prasanna.interview.security.SecuritySanitizer;
import com.prasanna.interview.validation.OrderEventParser;
import com.prasanna.interview.validation.OrderEventValidator;
import com.prasanna.interview.validation.PayloadSizeValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProcessingServiceTest {

    private ObjectMapper objectMapper;
    private SnsClient snsClient;
    private MetricsPublisher metricsPublisher;
    private StructuredLogger structuredLogger;
    private OrderProcessingService service;
    private Context lambdaContext;

    @BeforeEach
    void setUp() {
        OrderProcessingProperties properties = TestFixtures.properties();
        objectMapper = TestFixtures.objectMapper(true);
        snsClient = mock(SnsClient.class);
        metricsPublisher = mock(MetricsPublisher.class);
        structuredLogger = mock(StructuredLogger.class);
        SecuritySanitizer sanitizer = new SecuritySanitizer(properties);
        service = new OrderProcessingService(
                objectMapper,
                new PayloadSizeValidator(properties),
                new OrderEventParser(objectMapper),
                new OrderEventValidator(),
                new OrderNotificationPublisher(snsClient, objectMapper, properties),
                new InMemoryIdempotencyService(),
                new ReferenceDataCache(properties, metricsPublisher),
                metricsPublisher,
                structuredLogger,
                sanitizer,
                properties
        );
        lambdaContext = TestFixtures.lambdaContext();
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("published").build());
    }

    @Test
    void processesDirectApiPayloadThroughSharedProcessingPath() {
        OrderProcessingOutcome outcome = service.processApiPayload(
                TestFixtures.digitalOrderJson("evt-api-direct"),
                ProcessingLogContext.forApi("api-request-001", "partner-service", "203.0.113.10")
        );

        assertThat(outcome.status()).isEqualTo(OrderProcessingOutcome.Status.PROCESSED);
        assertThat(outcome.sourceEvent().eventId()).isEqualTo("evt-api-direct");
        assertThat(outcome.processedEvent()).isNotNull();
        assertThat(outcome.processedEvent().sourceEventId()).isEqualTo("evt-api-direct");
        verify(snsClient).publish(any(PublishRequest.class));
        verify(metricsPublisher).count(MetricsPublisher.ORDER_MESSAGE_PROCESSED);
    }

    @Test
    void processesDirectApiPayloadWithoutSnsWhenNotificationPublishingDisabled() {
        OrderProcessingProperties localProperties = TestFixtures.properties(false, false, 262_144, true, false, false);
        OrderProcessingService localService = new OrderProcessingService(
                objectMapper,
                new PayloadSizeValidator(localProperties),
                new OrderEventParser(objectMapper),
                new OrderEventValidator(),
                new OrderNotificationPublisher(snsClient, objectMapper, localProperties),
                new InMemoryIdempotencyService(),
                new ReferenceDataCache(localProperties, metricsPublisher),
                metricsPublisher,
                structuredLogger,
                new SecuritySanitizer(localProperties),
                localProperties
        );

        OrderProcessingOutcome outcome = localService.processApiPayload(
                TestFixtures.digitalOrderJson("evt-api-no-sns"),
                ProcessingLogContext.forApi("api-request-001", "partner-service", "203.0.113.10")
        );

        assertThat(outcome.status()).isEqualTo(OrderProcessingOutcome.Status.PROCESSED);
        assertThat(outcome.processedEvent()).isNotNull();
        verify(snsClient, never()).publish(any(PublishRequest.class));
        verify(metricsPublisher, never()).count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_PUBLISHED);
        verify(metricsPublisher).count(MetricsPublisher.ORDER_MESSAGE_PROCESSED);
    }

    @Test
    void processesDigitalOrderAndPublishesProcessedNotification() throws Exception {
        SQSEvent.SQSMessage message = TestFixtures.sqsMessage(
                "sqs-1",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-1", TestFixtures.digitalOrderJson("evt-1"))
        );

        service.processRecord(message, lambdaContext);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();
        assertThat(request.topicArn()).isEqualTo("arn:aws:sns:us-east-1:xxxxxxxxx:OrderProcessedTopic");
        assertThat(request.messageAttributes()).containsKeys("eventId", "correlationId", "orderId", "customerId", "orderType");
        assertThat(request.messageAttributes().get("orderType").stringValue()).isEqualTo("DIGITAL");
        assertThat(request.message()).contains("\"sourceEventId\":\"evt-1\"");
        verify(metricsPublisher).count(MetricsPublisher.DIGITAL_ORDER_PROCESSED);
        verify(metricsPublisher).count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_PUBLISHED);
        verify(metricsPublisher).count(MetricsPublisher.ORDER_MESSAGE_PROCESSED);
    }

    @Test
    void skipsDuplicateEventsWithoutPublishingAgain() throws Exception {
        SQSEvent.SQSMessage first = TestFixtures.sqsMessage(
                "sqs-1",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-1", TestFixtures.digitalOrderJson("evt-duplicate"))
        );
        SQSEvent.SQSMessage duplicate = TestFixtures.sqsMessage(
                "sqs-2",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-2", TestFixtures.digitalOrderJson("evt-duplicate"))
        );

        service.processRecord(first, lambdaContext);
        service.processRecord(duplicate, lambdaContext);

        verify(snsClient).publish(any(PublishRequest.class));
        verify(metricsPublisher).count(MetricsPublisher.DUPLICATE_EVENT_SKIPPED);
    }

    @Test
    void rejectsUnknownOrderType() throws Exception {
        String unsupported = """
                {
                  "eventId": "evt-bad",
                  "correlationId": "corr-bad",
                  "orderId": "order-bad",
                  "customerId": "customer-bad",
                  "customerEmail": "bad@example.com",
                  "orderType": "SUBSCRIPTION",
                  "amount": 9.99,
                  "currency": "USD",
                  "occurredAt": "2026-06-23T10:15:30Z"
                }
                """;
        SQSEvent.SQSMessage message = TestFixtures.sqsMessage(
                "sqs-bad",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-bad", unsupported)
        );

        assertThatThrownBy(() -> service.processRecord(message, lambdaContext))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported orderType");
        verify(snsClient, never()).publish(any(PublishRequest.class));
        verify(metricsPublisher).count(MetricsPublisher.VALIDATION_FAILED);
    }

    @Test
    void rejectsMalformedSqsBodyJson() {
        SQSEvent.SQSMessage message = TestFixtures.sqsMessage("sqs-bad-json", "{not-json");

        assertThatThrownBy(() -> service.processRecord(message, lambdaContext))
                .isInstanceOf(MalformedJsonException.class)
                .hasMessageContaining("Malformed SNS envelope JSON");
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void failsRecordWhenSnsPublishFails() throws Exception {
        SQSEvent.SQSMessage message = TestFixtures.sqsMessage(
                "sqs-1",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-1", TestFixtures.physicalOrderJson("evt-physical"))
        );
        doThrow(new RuntimeException("sns unavailable")).when(snsClient).publish(any(PublishRequest.class));

        assertThatThrownBy(() -> service.processRecord(message, lambdaContext))
                .isInstanceOf(NotificationPublishException.class)
                .hasMessageContaining("Unable to publish");
        verify(metricsPublisher).count(MetricsPublisher.PHYSICAL_ORDER_PROCESSED);
        verify(metricsPublisher).count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_FAILED);
    }

    @Test
    void enforcesPayloadSizeLimit() throws Exception {
        OrderProcessingProperties smallPayloadProperties = TestFixtures.properties(false, false, 64, true, false);
        OrderProcessingService smallPayloadService = new OrderProcessingService(
                objectMapper,
                new PayloadSizeValidator(smallPayloadProperties),
                new OrderEventParser(objectMapper),
                new OrderEventValidator(),
                new OrderNotificationPublisher(snsClient, objectMapper, smallPayloadProperties),
                new InMemoryIdempotencyService(),
                new ReferenceDataCache(smallPayloadProperties, metricsPublisher),
                metricsPublisher,
                structuredLogger,
                new SecuritySanitizer(smallPayloadProperties),
                smallPayloadProperties
        );
        SQSEvent.SQSMessage message = TestFixtures.sqsMessage(
                "sqs-too-large",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-1", TestFixtures.digitalOrderJson("evt-large"))
        );

        assertThatThrownBy(() -> smallPayloadService.processRecord(message, lambdaContext))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds maximum payload size");
    }
}
