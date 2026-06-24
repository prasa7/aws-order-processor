package com.prasanna.interview.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.service.OrderProcessingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class OrderProcessorLambdaTest {

    @Test
    void returnsEmptyBatchFailuresWhenAllRecordsSucceed() {
        OrderProcessingService service = mock(OrderProcessingService.class);
        OrderProcessorLambda lambda = new OrderProcessorLambda(service, TestFixtures.properties());
        SQSEvent.SQSMessage record = TestFixtures.sqsMessage("sqs-1", "{}");

        SQSBatchResponse response = lambda.handleRequest(TestFixtures.sqsEvent(record), TestFixtures.lambdaContext());

        assertThat(response.getBatchItemFailures()).isEmpty();
    }

    @Test
    void defaultConstructorCanHandleEmptyEvents() {
        OrderProcessorLambda lambda = new OrderProcessorLambda();

        SQSBatchResponse response = lambda.handleRequest(new SQSEvent(), TestFixtures.lambdaContext());

        assertThat(response.getBatchItemFailures()).isEmpty();
    }

    @Test
    void returnsOnlyFailedRecordIdsForPartialBatchFailure() {
        OrderProcessingService service = mock(OrderProcessingService.class);
        Context context = TestFixtures.lambdaContext();
        SQSEvent.SQSMessage success = TestFixtures.sqsMessage("sqs-success", "{}");
        SQSEvent.SQSMessage failure = TestFixtures.sqsMessage("sqs-failure", "{}");
        doThrow(new ValidationException("bad record")).when(service).processRecord(failure, context);
        OrderProcessorLambda lambda = new OrderProcessorLambda(service, TestFixtures.properties());

        SQSBatchResponse response = lambda.handleRequest(TestFixtures.sqsEvent(success, failure), context);

        assertThat(response.getBatchItemFailures())
                .extracting(SQSBatchResponse.BatchItemFailure::getItemIdentifier)
                .containsExactly("sqs-failure");
    }

    @Test
    void supportsVirtualThreadProcessingMode() {
        OrderProcessingService service = mock(OrderProcessingService.class);
        Context context = TestFixtures.lambdaContext();
        SQSEvent.SQSMessage success = TestFixtures.sqsMessage("sqs-success", "{}");
        SQSEvent.SQSMessage failure = TestFixtures.sqsMessage("sqs-failure", "{}");
        doThrow(new ValidationException("bad record")).when(service).processRecord(failure, context);
        OrderProcessorLambda lambda = new OrderProcessorLambda(
                service,
                TestFixtures.properties(false, true, 262_144, true, false)
        );

        SQSBatchResponse response = lambda.handleRequest(TestFixtures.sqsEvent(success, failure), context);

        assertThat(response.getBatchItemFailures())
                .extracting(SQSBatchResponse.BatchItemFailure::getItemIdentifier)
                .containsExactly("sqs-failure");
    }
}
