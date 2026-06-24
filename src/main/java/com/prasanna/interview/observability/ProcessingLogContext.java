package com.prasanna.interview.observability;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.SnsEnvelope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable structured-log context for one order-processing attempt.
 *
 * <p>The context is populated incrementally as the processor unwraps SNS, parses the order, measures duration,
 * and handles failures. Null fields are filtered by {@link StructuredLogger} before logging.</p>
 */
public class ProcessingLogContext {

    private final String awsRequestId;
    private final String sqsMessageId;
    private String apiRequestId;
    private String apiPrincipal;
    private String sourceIp;
    private String eventId;
    private String correlationId;
    private String orderId;
    private String customerId;
    private String orderType;
    private String snsMessageId;
    private String snsTopicArn;
    private Long durationMs;
    private String errorType;
    private String errorMessage;

    /**
     * Creates a context for an SNS-wrapped SQS record processed by Lambda.
     *
     * @param sqsMessage SQS message being processed
     * @param lambdaContext Lambda invocation context
     */
    public ProcessingLogContext(SQSEvent.SQSMessage sqsMessage, Context lambdaContext) {
        this(lambdaContext == null ? null : lambdaContext.getAwsRequestId(),
                sqsMessage == null ? null : sqsMessage.getMessageId());
    }

    /**
     * Creates a context from explicit Lambda identifiers.
     *
     * @param awsRequestId Lambda request id
     * @param sqsMessageId SQS message id
     */
    public ProcessingLogContext(String awsRequestId, String sqsMessageId) {
        this.awsRequestId = awsRequestId;
        this.sqsMessageId = sqsMessageId;
    }

    /**
     * Creates a context for an API-triggered order-processing request.
     *
     * @param requestId API request id
     * @param principal authenticated API principal
     * @param sourceIp caller source IP
     * @return API log context
     */
    public static ProcessingLogContext forApi(String requestId, String principal, String sourceIp) {
        ProcessingLogContext context = new ProcessingLogContext((String) null, null);
        context.apiRequestId = requestId;
        context.apiPrincipal = principal;
        context.sourceIp = sourceIp;
        return context;
    }

    /**
     * Adds SNS envelope fields to the context.
     *
     * @param envelope SNS envelope extracted from SQS
     */
    public void withEnvelope(SnsEnvelope envelope) {
        if (envelope != null) {
            this.snsMessageId = envelope.messageId();
            this.snsTopicArn = envelope.topicArn();
        }
    }

    /**
     * Adds parsed order fields to the context.
     *
     * @param orderEvent parsed order event
     */
    public void withOrder(OrderEvent orderEvent) {
        if (orderEvent != null) {
            this.eventId = orderEvent.eventId();
            this.correlationId = orderEvent.correlationId();
            this.orderId = orderEvent.orderId();
            this.customerId = orderEvent.customerId();
            this.orderType = orderEvent.orderType();
        }
    }

    /**
     * Adds elapsed processing duration.
     *
     * @param durationMs duration in milliseconds
     */
    public void withDuration(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Adds sanitized error fields to the context.
     *
     * @param throwable failure cause
     * @param sanitizedMessage sanitized error message
     */
    public void withError(Throwable throwable, String sanitizedMessage) {
        if (throwable != null) {
            this.errorType = throwable.getClass().getSimpleName();
            this.errorMessage = sanitizedMessage;
        }
    }

    /**
     * Returns fields ready to merge into the structured log payload.
     *
     * @return ordered field map
     */
    public Map<String, Object> asFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("awsRequestId", awsRequestId);
        fields.put("apiRequestId", apiRequestId);
        fields.put("apiPrincipal", apiPrincipal);
        fields.put("sourceIp", sourceIp);
        fields.put("eventId", eventId);
        fields.put("correlationId", correlationId);
        fields.put("orderId", orderId);
        fields.put("customerId", customerId);
        fields.put("orderType", orderType);
        fields.put("sqsMessageId", sqsMessageId);
        fields.put("snsMessageId", snsMessageId);
        fields.put("snsTopicArn", snsTopicArn);
        fields.put("durationMs", durationMs);
        fields.put("errorType", errorType);
        fields.put("errorMessage", errorMessage);
        return fields;
    }
}
