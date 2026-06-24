package com.prasanna.interview.config;

import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.service.NoOpIdempotencyService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdempotencyConfigTest {

    @Test
    void createsNoOpServiceWhenIdempotencyIsDisabled() {
        IdempotencyConfig config = new IdempotencyConfig();

        assertThat(config.idempotencyService(TestFixtures.properties(), mock(DynamoDbClient.class)))
                .isInstanceOf(NoOpIdempotencyService.class);
    }

    @Test
    void requiresTableNameWhenIdempotencyIsEnabled() {
        IdempotencyConfig config = new IdempotencyConfig();
        OrderProcessingProperties properties = TestFixtures.properties(true, false, 262_144, true, false);

        assertThatThrownBy(() -> config.idempotencyService(properties, mock(DynamoDbClient.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IDEMPOTENCY_TABLE_NAME is required when order.enable-idempotency=true");
    }
}
