package com.prasanna.interview.observability;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.SnsEnvelope;
import com.prasanna.interview.security.SecuritySanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructuredLoggerTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void writesStructuredJsonLogWithSanitizedContextFields() throws Exception {
        OrderProcessingProperties properties = TestFixtures.properties(false, false, 262_144, true, true);
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        StructuredLogger logger = new StructuredLogger(objectMapper, properties, new SecuritySanitizer(properties));
        ProcessingLogContext context = contextWithOrderDetails();

        logger.info("processed\u0000message", context);

        Map<String, Object> fields = objectMapper.readValue(stdout(), new TypeReference<>() {
        });
        assertThat(fields)
                .containsEntry("level", "INFO")
                .containsEntry("service", "order-processor-test")
                .containsEntry("environment", "test")
                .containsEntry("message", "processedmessage")
                .containsEntry("awsRequestId", "aws-request-001")
                .containsEntry("sqsMessageId", "sqs-structured")
                .containsEntry("snsMessageId", "sns-structured")
                .containsEntry("orderId", "order-001")
                .containsEntry("customerId", "**********2345")
                .containsEntry("durationMs", 42);
    }

    @Test
    void writesErrorFieldsFromThrowable() throws Exception {
        OrderProcessingProperties properties = TestFixtures.properties();
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        StructuredLogger logger = new StructuredLogger(objectMapper, properties, new SecuritySanitizer(properties));

        logger.error("failed", null, new IllegalStateException("bad\u0000state"));

        Map<String, Object> fields = objectMapper.readValue(stdout(), new TypeReference<>() {
        });
        assertThat(fields)
                .containsEntry("level", "ERROR")
                .containsEntry("errorType", "IllegalStateException")
                .containsEntry("errorMessage", "badstate");
    }

    @Test
    void writesUnstructuredLogWhenStructuredLoggingIsDisabled() {
        OrderProcessingProperties properties = propertiesWithStructuredLogging(false);
        StructuredLogger logger = new StructuredLogger(
                TestFixtures.objectMapper(true),
                properties,
                new SecuritySanitizer(properties)
        );

        logger.warn("plain", null);

        assertThat(stdout())
                .startsWith("WARN {")
                .contains("message=plain")
                .contains("service=order-processor-test");
    }

    @Test
    void fallsBackToMapOutputWhenLogJsonSerializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        OrderProcessingProperties properties = TestFixtures.properties();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });
        StructuredLogger logger = new StructuredLogger(objectMapper, properties, new SecuritySanitizer(properties));

        logger.info("fallback", null);

        assertThat(stdout())
                .startsWith("INFO {")
                .contains("message=fallback");
    }

    private ProcessingLogContext contextWithOrderDetails() throws Exception {
        SQSEvent.SQSMessage sqsMessage = TestFixtures.sqsMessage("sqs-structured", "{}");
        ProcessingLogContext context = new ProcessingLogContext(sqsMessage, TestFixtures.lambdaContext());
        context.withEnvelope(new SnsEnvelope(
                "Notification",
                "sns-structured",
                "arn:aws:sns:us-east-1:xxxxxxxxx:OrderCreatedTopic",
                TestFixtures.digitalOrderJson("evt-structured"),
                Instant.parse("2026-06-23T10:21:30Z")
        ));
        context.withOrder(TestFixtures.objectMapper(true)
                .readValue(TestFixtures.digitalOrderJson("evt-structured"), DigitalOrder.class));
        context.withDuration(42);
        context.withError(new IllegalArgumentException("bad"), "bad");
        return context;
    }

    private OrderProcessingProperties propertiesWithStructuredLogging(boolean structuredLogging) {
        return new OrderProcessingProperties(
                "order-processor-test",
                "test",
                "us-east-1",
                "arn:aws:sns:us-east-1:xxxxxxxxx:OrderProcessedTopic",
                false,
                3,
                structuredLogging,
                60,
                262_144,
                false,
                true,
                false,
                true,
                new OrderProcessingProperties.Api(false, "/api/v1/orders", "Authorization", "", "order-api-client")
        );
    }

    private String stdout() {
        return output.toString(StandardCharsets.UTF_8).trim();
    }
}
