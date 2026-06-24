package com.prasanna.interview.service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * DynamoDB-backed idempotency service skeleton for production event-id deduplication.
 *
 * <p>The table is expected to use {@code eventId} as its string partition key. The implementation performs a
 * strongly consistent read for duplicate checks and conditional writes when marking an event as processed.</p>
 */
public class DynamoDbIdempotencyService implements IdempotencyService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final Clock clock;

    /**
     * Creates the DynamoDB idempotency service using the system UTC clock.
     *
     * @param dynamoDbClient AWS SDK v2 DynamoDB client
     * @param tableName DynamoDB table name
     */
    public DynamoDbIdempotencyService(DynamoDbClient dynamoDbClient, String tableName) {
        this(dynamoDbClient, tableName, Clock.systemUTC());
    }

    DynamoDbIdempotencyService(DynamoDbClient dynamoDbClient, String tableName, Clock clock) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.clock = clock;
    }

    @Override
    public boolean isDuplicate(String eventId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .consistentRead(true)
                .key(Map.of("eventId", AttributeValue.fromS(eventId)))
                .build();
        return dynamoDbClient.getItem(request).hasItem();
    }

    @Override
    public void markProcessed(String eventId) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "eventId", AttributeValue.fromS(eventId),
                        "processedAt", AttributeValue.fromS(Instant.now(clock).toString())
                ))
                .conditionExpression("attribute_not_exists(eventId)")
                .build();
        dynamoDbClient.putItem(request);
    }
}
