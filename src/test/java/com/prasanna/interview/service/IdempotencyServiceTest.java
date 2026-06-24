package com.prasanna.interview.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    @Test
    void noOpIdempotencyServiceNeverTreatsEventsAsDuplicates() {
        NoOpIdempotencyService service = new NoOpIdempotencyService();

        service.markProcessed("evt-1");

        assertThat(service.isDuplicate("evt-1")).isFalse();
    }

    @Test
    void dynamoDbIdempotencyServiceChecksEventIdKey() {
        DynamoDbClient dynamoDbClient = mock(DynamoDbClient.class);
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("eventId", AttributeValue.fromS("evt-1")))
                .build());
        DynamoDbIdempotencyService service = new DynamoDbIdempotencyService(dynamoDbClient, "OrderIdempotency");

        boolean duplicate = service.isDuplicate("evt-1");

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(dynamoDbClient).getItem(captor.capture());
        assertThat(duplicate).isTrue();
        assertThat(captor.getValue().tableName()).isEqualTo("OrderIdempotency");
        assertThat(captor.getValue().consistentRead()).isTrue();
        assertThat(captor.getValue().key().get("eventId").s()).isEqualTo("evt-1");
    }

    @Test
    void dynamoDbIdempotencyServiceWritesProcessedMarkerConditionally() {
        DynamoDbClient dynamoDbClient = mock(DynamoDbClient.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-23T10:15:30Z"), ZoneOffset.UTC);
        DynamoDbIdempotencyService service = new DynamoDbIdempotencyService(dynamoDbClient, "OrderIdempotency", clock);

        service.markProcessed("evt-2");

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("OrderIdempotency");
        assertThat(request.item().get("eventId").s()).isEqualTo("evt-2");
        assertThat(request.item().get("processedAt").s()).isEqualTo("2026-06-23T10:15:30Z");
        assertThat(request.conditionExpression()).isEqualTo("attribute_not_exists(eventId)");
    }
}
