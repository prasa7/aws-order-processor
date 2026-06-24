package com.prasanna.interview.config;

import com.prasanna.interview.service.DynamoDbIdempotencyService;
import com.prasanna.interview.service.IdempotencyService;
import com.prasanna.interview.service.NoOpIdempotencyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Selects the idempotency implementation from runtime configuration.
 */
@Configuration
public class IdempotencyConfig {

    /**
     * Creates the idempotency configuration.
     */
    public IdempotencyConfig() {
    }

    @Bean
    IdempotencyService idempotencyService(OrderProcessingProperties properties, DynamoDbClient dynamoDbClient) {
        if (!properties.enableIdempotency()) {
            return new NoOpIdempotencyService();
        }
        String tableName = System.getenv("IDEMPOTENCY_TABLE_NAME");
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("IDEMPOTENCY_TABLE_NAME is required when order.enable-idempotency=true");
        }
        return new DynamoDbIdempotencyService(dynamoDbClient, tableName);
    }
}
