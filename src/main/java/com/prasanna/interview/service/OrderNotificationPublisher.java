package com.prasanna.interview.service;

import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.exception.NotificationPublishException;
import com.prasanna.interview.model.OrderProcessedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Publishes processed-order notifications to SNS.
 *
 * <p>The publisher serializes {@link OrderProcessedEvent} as JSON and attaches message attributes that make SNS
 * subscriptions and CloudWatch logs easier to filter.</p>
 */
@Component
public class OrderNotificationPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final OrderProcessingProperties properties;

    /**
     * Creates the SNS publisher.
     *
     * @param snsClient AWS SDK v2 SNS client
     * @param objectMapper JSON mapper
     * @param properties topic ARN and runtime configuration
     */
    public OrderNotificationPublisher(SnsClient snsClient, ObjectMapper objectMapper, OrderProcessingProperties properties) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Publishes a processed-order event to the configured SNS topic when notification publishing is enabled.
     *
     * @param event processed-order notification payload
     * @return {@code true} when SNS publish was attempted, or {@code false} when publishing is disabled
     * @throws NotificationPublishException when serialization or SNS publish fails
     */
    public boolean publish(OrderProcessedEvent event) {
        if (!properties.enableNotificationPublishing()) {
            return false;
        }
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(properties.orderProcessedTopicArn())
                    .message(objectMapper.writeValueAsString(event))
                    .messageAttributes(Map.of(
                            "eventId", stringAttribute(event.eventId()),
                            "correlationId", stringAttribute(event.correlationId()),
                            "orderId", stringAttribute(event.orderId()),
                            "customerId", stringAttribute(event.customerId()),
                            "orderType", stringAttribute(event.orderType())
                    ))
                    .build();
            snsClient.publish(request);
            return true;
        } catch (JsonProcessingException e) {
            throw new NotificationPublishException("Unable to serialize OrderProcessedEvent", e);
        } catch (RuntimeException e) {
            throw new NotificationPublishException("Unable to publish OrderProcessedEvent to SNS", e);
        }
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value == null ? "" : value)
                .build();
    }
}
