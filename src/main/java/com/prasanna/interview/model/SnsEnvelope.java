package com.prasanna.interview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Minimal SNS notification envelope extracted from an SQS message body.
 *
 * @param type SNS envelope type
 * @param messageId SNS message id
 * @param topicArn SNS topic ARN that produced the message
 * @param message inner JSON order event payload
 * @param timestamp SNS publish timestamp
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnsEnvelope(
        @JsonProperty("Type") String type,
        @JsonProperty("MessageId") String messageId,
        @JsonProperty("TopicArn") String topicArn,
        @JsonProperty("Message") String message,
        @JsonProperty("Timestamp") Instant timestamp
) {
}
