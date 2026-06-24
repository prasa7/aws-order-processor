package com.prasanna.interview.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.cache.ReferenceDataCache;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.handler.OrderProcessorLambda;
import com.prasanna.interview.observability.MetricsPublisher;
import com.prasanna.interview.observability.StructuredLogger;
import com.prasanna.interview.security.SecuritySanitizer;
import com.prasanna.interview.service.InMemoryIdempotencyService;
import com.prasanna.interview.service.OrderNotificationPublisher;
import com.prasanna.interview.service.OrderProcessingService;
import com.prasanna.interview.validation.OrderEventParser;
import com.prasanna.interview.validation.OrderEventValidator;
import com.prasanna.interview.validation.PayloadSizeValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProcessingIntegrationTest {

    @Test
    void processesMultipleSnsWrappedSqsRecordsAndReturnsPartialFailures() throws Exception {
        OrderProcessingProperties properties = TestFixtures.properties();
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        SnsClient snsClient = mock(SnsClient.class);
        MetricsPublisher metricsPublisher = mock(MetricsPublisher.class);
        StructuredLogger structuredLogger = mock(StructuredLogger.class);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("published").build());

        OrderProcessingService service = new OrderProcessingService(
                objectMapper,
                new PayloadSizeValidator(properties),
                new OrderEventParser(objectMapper),
                new OrderEventValidator(),
                new OrderNotificationPublisher(snsClient, objectMapper, properties),
                new InMemoryIdempotencyService(),
                new ReferenceDataCache(properties, metricsPublisher),
                metricsPublisher,
                structuredLogger,
                new SecuritySanitizer(properties),
                properties
        );
        OrderProcessorLambda lambda = new OrderProcessorLambda(service, properties);
        Context context = TestFixtures.lambdaContext();

        SQSEvent.SQSMessage digital = TestFixtures.sqsMessage(
                "sqs-digital",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-digital", TestFixtures.digitalOrderJson("evt-digital"))
        );
        SQSEvent.SQSMessage physical = TestFixtures.sqsMessage(
                "sqs-physical",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-physical", TestFixtures.physicalOrderJson("evt-physical"))
        );
        SQSEvent.SQSMessage invalid = TestFixtures.sqsMessage(
                "sqs-invalid",
                TestFixtures.snsEnvelopeJson(objectMapper, "sns-invalid", "{\"orderType\":\"DIGITAL\"}")
        );

        SQSBatchResponse response = lambda.handleRequest(TestFixtures.sqsEvent(digital, invalid, physical), context);

        assertThat(response.getBatchItemFailures())
                .extracting(SQSBatchResponse.BatchItemFailure::getItemIdentifier)
                .containsExactly("sqs-invalid");
        verify(snsClient, times(2)).publish(any(PublishRequest.class));
    }
}
