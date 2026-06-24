package com.prasanna.interview.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.OrderProcessorApplication;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.service.OrderProcessingService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * AWS Lambda entry point for the SNS-to-SQS order-processing flow.
 *
 * <p>The handler consumes {@link SQSEvent} batches and returns {@link SQSBatchResponse} so Lambda can use partial
 * batch failure handling. Each failed SQS record is returned as a {@code BatchItemFailure}; successful records are
 * omitted from the response and are not retried by the event source mapping.</p>
 */
public class OrderProcessorLambda implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final OrderProcessingService orderProcessingService;
    private final OrderProcessingProperties properties;

    /**
     * Creates a Lambda handler backed by a lazily initialized Spring context.
     */
    public OrderProcessorLambda() {
        this(springContext().getBean(OrderProcessingService.class),
                springContext().getBean(OrderProcessingProperties.class));
    }

    /**
     * Creates a handler with explicit collaborators, primarily for tests and custom runtime wiring.
     *
     * @param orderProcessingService shared service used to process each SQS record
     * @param properties runtime configuration, including optional virtual-thread processing
     */
    public OrderProcessorLambda(OrderProcessingService orderProcessingService,
                                OrderProcessingProperties properties) {
        this.orderProcessingService = orderProcessingService;
        this.properties = properties;
    }

    /**
     * Processes a batch of SQS messages and returns only failed message ids.
     *
     * @param event SQS batch event supplied by Lambda
     * @param context Lambda invocation context used for structured logging
     * @return batch response containing failed item identifiers
     */
    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> records = event == null ? List.of() : event.getRecords();
        if (records == null || records.isEmpty()) {
            return new SQSBatchResponse(List.of());
        }

        List<SQSBatchResponse.BatchItemFailure> failures = properties.enableVirtualThreads()
                ? processWithVirtualThreads(records, context)
                : processSequentially(records, context);

        return new SQSBatchResponse(failures);
    }

    private List<SQSBatchResponse.BatchItemFailure> processSequentially(List<SQSEvent.SQSMessage> records,
                                                                        Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        for (SQSEvent.SQSMessage record : records) {
            processOne(record, context).ifPresent(failures::add);
        }
        return failures;
    }

    private List<SQSBatchResponse.BatchItemFailure> processWithVirtualThreads(List<SQSEvent.SQSMessage> records,
                                                                              Context context) {
        List<SubmittedRecord> submittedRecords = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SQSEvent.SQSMessage record : records) {
                submittedRecords.add(new SubmittedRecord(record, executor.submit(() -> processOne(record, context))));
            }
            List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
            for (SubmittedRecord submittedRecord : submittedRecords) {
                try {
                    submittedRecord.future().get().ifPresent(failures::add);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.add(batchFailure(submittedRecord.message()));
                } catch (ExecutionException e) {
                    failures.add(batchFailure(submittedRecord.message()));
                }
            }
            return failures;
        }
    }

    private Optional<SQSBatchResponse.BatchItemFailure> processOne(SQSEvent.SQSMessage record, Context context) {
        try {
            orderProcessingService.processRecord(record, context);
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(batchFailure(record));
        }
    }

    private SQSBatchResponse.BatchItemFailure batchFailure(SQSEvent.SQSMessage record) {
        return new SQSBatchResponse.BatchItemFailure(record.getMessageId());
    }

    private static ConfigurableApplicationContext springContext() {
        return LazySpringContext.INSTANCE;
    }

    private static final class LazySpringContext {
        private static final ConfigurableApplicationContext INSTANCE = new SpringApplicationBuilder(OrderProcessorApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    private record SubmittedRecord(SQSEvent.SQSMessage message,
                                   Future<Optional<SQSBatchResponse.BatchItemFailure>> future) {
    }
}
