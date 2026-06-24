package com.prasanna.interview.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.cache.ReferenceDataCache;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.exception.MalformedJsonException;
import com.prasanna.interview.exception.NotificationPublishException;
import com.prasanna.interview.exception.OrderProcessingException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.OrderProcessedEvent;
import com.prasanna.interview.model.PhysicalOrder;
import com.prasanna.interview.model.SnsEnvelope;
import com.prasanna.interview.observability.MetricsPublisher;
import com.prasanna.interview.observability.ProcessingLogContext;
import com.prasanna.interview.observability.StructuredLogger;
import com.prasanna.interview.security.SecuritySanitizer;
import com.prasanna.interview.validation.OrderEventParser;
import com.prasanna.interview.validation.OrderEventValidator;
import com.prasanna.interview.validation.PayloadSizeValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared application service for processing orders from both Lambda and optional HTTP API entry points.
 *
 * <p>The service owns the core workflow: payload-size validation, SNS envelope extraction for Lambda records,
 * order-event parsing, required-field validation, idempotency checks, type-specific processing via pattern matching,
 * SNS notification publishing, structured logging, and CloudWatch EMF metrics.</p>
 */
@Service
public class OrderProcessingService {

    private final ObjectMapper objectMapper;
    private final PayloadSizeValidator payloadSizeValidator;
    private final OrderEventParser orderEventParser;
    private final OrderEventValidator orderEventValidator;
    private final OrderNotificationPublisher notificationPublisher;
    private final IdempotencyService idempotencyService;
    private final ReferenceDataCache referenceDataCache;
    private final MetricsPublisher metricsPublisher;
    private final StructuredLogger structuredLogger;
    private final SecuritySanitizer sanitizer;
    private final OrderProcessingProperties properties;

    /**
     * Creates the order-processing service with all production collaborators.
     *
     * @param objectMapper JSON mapper configured for Java time and strict-property handling
     * @param payloadSizeValidator validator for SQS, SNS, and API payload size limits
     * @param orderEventParser parser that selects concrete order records from the {@code orderType} discriminator
     * @param orderEventValidator required-field validator for parsed orders
     * @param notificationPublisher publisher for processed-order SNS notifications
     * @param idempotencyService event-id based duplicate detector
     * @param referenceDataCache TTL cache for reference data only
     * @param metricsPublisher CloudWatch Embedded Metric Format publisher
     * @param structuredLogger structured JSON logger
     * @param sanitizer sanitizer for safe error and customer identifiers in logs
     * @param properties runtime configuration
     */
    public OrderProcessingService(ObjectMapper objectMapper,
                                  PayloadSizeValidator payloadSizeValidator,
                                  OrderEventParser orderEventParser,
                                  OrderEventValidator orderEventValidator,
                                  OrderNotificationPublisher notificationPublisher,
                                  IdempotencyService idempotencyService,
                                  ReferenceDataCache referenceDataCache,
                                  MetricsPublisher metricsPublisher,
                                  StructuredLogger structuredLogger,
                                  SecuritySanitizer sanitizer,
                                  OrderProcessingProperties properties) {
        this.objectMapper = objectMapper;
        this.payloadSizeValidator = payloadSizeValidator;
        this.orderEventParser = orderEventParser;
        this.orderEventValidator = orderEventValidator;
        this.notificationPublisher = notificationPublisher;
        this.idempotencyService = idempotencyService;
        this.referenceDataCache = referenceDataCache;
        this.metricsPublisher = metricsPublisher;
        this.structuredLogger = structuredLogger;
        this.sanitizer = sanitizer;
        this.properties = properties;
    }

    /**
     * Processes one SNS-wrapped SQS record from the Lambda event source mapping.
     *
     * @param sqsMessage SQS message whose body contains an SNS notification envelope
     * @param lambdaContext Lambda invocation context used for request id logging
     * @throws ValidationException when required fields, order type, or payload size validation fails
     * @throws MalformedJsonException when the SNS envelope or inner message cannot be parsed as JSON
     * @throws NotificationPublishException when publishing the processed notification fails
     * @throws OrderProcessingException for unexpected runtime failures
     */
    public void processRecord(SQSEvent.SQSMessage sqsMessage, Context lambdaContext) {
        long started = System.nanoTime();
        ProcessingLogContext logContext = new ProcessingLogContext(sqsMessage, lambdaContext);
        try {
            payloadSizeValidator.validate(sqsMessage.getBody(), "SQS body");
            SnsEnvelope snsEnvelope = readSnsEnvelope(sqsMessage.getBody());
            logContext.withEnvelope(snsEnvelope);
            validateSnsEnvelope(snsEnvelope);
            payloadSizeValidator.validate(snsEnvelope.message(), "SNS Message");

            OrderEvent orderEvent = orderEventParser.parse(snsEnvelope.message());
            processParsedOrderEvent(orderEvent, logContext, started);
        } catch (ValidationException | MalformedJsonException e) {
            metricsPublisher.count(MetricsPublisher.VALIDATION_FAILED);
            logContext.withDuration(elapsedMs(started));
            recordFailure(logContext, e);
            throw e;
        } catch (NotificationPublishException e) {
            metricsPublisher.count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_FAILED);
            logContext.withDuration(elapsedMs(started));
            recordFailure(logContext, e);
            throw e;
        } catch (RuntimeException e) {
            logContext.withDuration(elapsedMs(started));
            recordFailure(logContext, e);
            throw new OrderProcessingException("Unexpected order processing failure", e);
        } finally {
            long durationMs = elapsedMs(started);
            logContext.withDuration(durationMs);
            metricsPublisher.duration(MetricsPublisher.ORDER_PROCESSING_DURATION_MS, durationMs);
        }
    }

    /**
     * Processes a direct order-event JSON payload from the optional HTTP API extension.
     *
     * <p>The payload is intentionally not an SNS envelope. This keeps API clients simple while reusing the same
     * validation, idempotency, processing, metrics, logging, and notification-publishing workflow as Lambda.</p>
     *
     * @param requestBody direct {@code DigitalOrder} or {@code PhysicalOrder} JSON
     * @param logContext pre-populated context containing API request metadata
     * @return processing outcome used to build the API response
     * @throws ValidationException when required fields, order type, or payload size validation fails
     * @throws MalformedJsonException when the request body cannot be parsed as JSON
     * @throws NotificationPublishException when publishing the processed notification fails
     * @throws OrderProcessingException for unexpected runtime failures
     */
    public OrderProcessingOutcome processApiPayload(String requestBody, ProcessingLogContext logContext) {
        long started = System.nanoTime();
        ProcessingLogContext context = logContext == null ? new ProcessingLogContext((String) null, null) : logContext;
        try {
            payloadSizeValidator.validate(requestBody, "API request body");
            OrderEvent orderEvent = orderEventParser.parse(requestBody);
            return processParsedOrderEvent(orderEvent, context, started);
        } catch (ValidationException | MalformedJsonException e) {
            metricsPublisher.count(MetricsPublisher.VALIDATION_FAILED);
            context.withDuration(elapsedMs(started));
            recordFailure(context, e);
            throw e;
        } catch (NotificationPublishException e) {
            metricsPublisher.count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_FAILED);
            context.withDuration(elapsedMs(started));
            recordFailure(context, e);
            throw e;
        } catch (RuntimeException e) {
            context.withDuration(elapsedMs(started));
            recordFailure(context, e);
            throw new OrderProcessingException("Unexpected order processing failure", e);
        } finally {
            long durationMs = elapsedMs(started);
            context.withDuration(durationMs);
            metricsPublisher.duration(MetricsPublisher.ORDER_PROCESSING_DURATION_MS, durationMs);
        }
    }

    private SnsEnvelope readSnsEnvelope(String body) {
        try {
            return objectMapper.readValue(body, SnsEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new MalformedJsonException("Malformed SNS envelope JSON in SQS body", e);
        }
    }

    private void validateSnsEnvelope(SnsEnvelope snsEnvelope) {
        if (snsEnvelope.messageId() == null || snsEnvelope.messageId().isBlank()) {
            throw new ValidationException("SNS MessageId is required");
        }
        if (snsEnvelope.topicArn() == null || snsEnvelope.topicArn().isBlank()) {
            throw new ValidationException("SNS TopicArn is required");
        }
        if (snsEnvelope.message() == null || snsEnvelope.message().isBlank()) {
            throw new ValidationException("SNS Message is required");
        }
    }

    private OrderProcessingOutcome processParsedOrderEvent(OrderEvent orderEvent,
                                                           ProcessingLogContext logContext,
                                                           long started) {
        orderEventValidator.validate(orderEvent);
        logContext.withOrder(orderEvent);

        if (idempotencyService.isDuplicate(orderEvent.eventId())) {
            metricsPublisher.count(MetricsPublisher.DUPLICATE_EVENT_SKIPPED);
            logContext.withDuration(elapsedMs(started));
            structuredLogger.warn("Duplicate order event skipped", logContext);
            return OrderProcessingOutcome.duplicateSkipped(orderEvent);
        }

        OrderProcessedEvent processedEvent = processOrder(orderEvent);
        boolean notificationPublished = notificationPublisher.publish(processedEvent);
        idempotencyService.markProcessed(orderEvent.eventId());
        if (notificationPublished) {
            metricsPublisher.count(MetricsPublisher.ORDER_PROCESSED_NOTIFICATION_PUBLISHED);
        }
        metricsPublisher.count(MetricsPublisher.ORDER_MESSAGE_PROCESSED);
        logContext.withDuration(elapsedMs(started));
        structuredLogger.info("Order message processed", logContext);
        return OrderProcessingOutcome.processed(orderEvent, processedEvent);
    }

    private OrderProcessedEvent processOrder(OrderEvent orderEvent) {
        return switch (orderEvent) {
            case DigitalOrder digitalOrder -> processDigitalOrder(digitalOrder);
            case PhysicalOrder physicalOrder -> processPhysicalOrder(physicalOrder);
        };
    }

    private OrderProcessedEvent processDigitalOrder(DigitalOrder order) {
        referenceDataCache.get("order-type:DIGITAL", () -> "digital-reference-data");
        metricsPublisher.count(MetricsPublisher.DIGITAL_ORDER_PROCESSED);
        return toProcessedEvent(order);
    }

    private OrderProcessedEvent processPhysicalOrder(PhysicalOrder order) {
        referenceDataCache.get("order-type:PHYSICAL", () -> "physical-reference-data");
        metricsPublisher.count(MetricsPublisher.PHYSICAL_ORDER_PROCESSED);
        return toProcessedEvent(order);
    }

    private OrderProcessedEvent toProcessedEvent(OrderEvent order) {
        return new OrderProcessedEvent(
                UUID.randomUUID().toString(),
                order.eventId(),
                order.correlationId(),
                order.orderId(),
                order.customerId(),
                order.orderType(),
                "PROCESSED",
                Instant.now(),
                properties.serviceName(),
                properties.environment()
        );
    }

    private void recordFailure(ProcessingLogContext logContext, RuntimeException e) {
        logContext.withError(e, sanitizer.sanitizeErrorMessage(e.getMessage()));
        metricsPublisher.count(MetricsPublisher.ORDER_MESSAGE_FAILED);
        structuredLogger.error("Order message failed", logContext, e);
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
