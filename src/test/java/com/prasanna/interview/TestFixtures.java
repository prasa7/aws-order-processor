package com.prasanna.interview;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.model.SnsEnvelope;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.List;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static OrderProcessingProperties properties() {
        return properties(false, false, 262_144, true, false);
    }

    public static OrderProcessingProperties properties(boolean enableIdempotency,
                                                       boolean enableVirtualThreads,
                                                       int maxPayloadSizeBytes,
                                                       boolean strictJson,
                                                       boolean maskCustomerId) {
        return properties(
                enableIdempotency,
                enableVirtualThreads,
                maxPayloadSizeBytes,
                strictJson,
                maskCustomerId,
                true
        );
    }

    public static OrderProcessingProperties properties(boolean enableIdempotency,
                                                       boolean enableVirtualThreads,
                                                       int maxPayloadSizeBytes,
                                                       boolean strictJson,
                                                       boolean maskCustomerId,
                                                       boolean enableNotificationPublishing) {
        return new OrderProcessingProperties(
                "order-processor-test",
                "test",
                "us-east-1",
                "arn:aws:sns:us-east-1:xxxxxxxxx:OrderProcessedTopic",
                enableIdempotency,
                3,
                true,
                60,
                maxPayloadSizeBytes,
                maskCustomerId,
                strictJson,
                enableVirtualThreads,
                enableNotificationPublishing,
                new OrderProcessingProperties.Api(false, "/api/v1/orders", "Authorization", "", "order-api-client")
        );
    }

    public static ObjectMapper objectMapper(boolean strictJson) {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, strictJson)
                .build();
    }

    public static String digitalOrderJson(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "correlationId": "corr-001",
                  "orderId": "order-001",
                  "customerId": "customer-12345",
                  "customerEmail": "ada@example.com",
                  "orderType": "DIGITAL",
                  "amount": 49.99,
                  "currency": "USD",
                  "occurredAt": "2026-06-23T10:15:30Z",
                  "productCode": "EBOOK-001",
                  "downloadUrl": "https://downloads.example.com/order-001"
                }
                """.formatted(eventId);
    }

    public static String physicalOrderJson(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "correlationId": "corr-002",
                  "orderId": "order-002",
                  "customerId": "customer-67890",
                  "customerEmail": "grace@example.com",
                  "orderType": "PHYSICAL",
                  "amount": 89.99,
                  "currency": "USD",
                  "occurredAt": "2026-06-23T10:20:30Z",
                  "shippingAddress": "1 Lambda Way, Seattle, WA",
                  "shippingMethod": "EXPRESS"
                }
                """.formatted(eventId);
    }

    public static String snsEnvelopeJson(ObjectMapper objectMapper, String snsMessageId, String message) throws Exception {
        return objectMapper.writeValueAsString(new SnsEnvelope(
                "Notification",
                snsMessageId,
                "arn:aws:sns:us-east-1:xxxxxxxxx:OrderCreatedTopic",
                message,
                Instant.parse("2026-06-23T10:21:30Z")
        ));
    }

    public static SQSEvent.SQSMessage sqsMessage(String messageId, String body) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody(body);
        return message;
    }

    public static SQSEvent sqsEvent(SQSEvent.SQSMessage... records) {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(records));
        return event;
    }

    public static Context lambdaContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "aws-request-001";
            }

            @Override
            public String getLogGroupName() {
                return "/aws/lambda/order-processor";
            }

            @Override
            public String getLogStreamName() {
                return "2026/06/23/[$LATEST]abc";
            }

            @Override
            public String getFunctionName() {
                return "order-processor";
            }

            @Override
            public String getFunctionVersion() {
                return "$LATEST";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "arn:aws:lambda:us-east-1:xxxxxxxxx:function:order-processor";
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 30_000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 512;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {
                    @Override
                    public void log(String message) {
                    }

                    @Override
                    public void log(byte[] message) {
                    }
                };
            }
        };
    }
}
