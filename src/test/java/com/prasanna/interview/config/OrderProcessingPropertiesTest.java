package com.prasanna.interview.config;

import com.prasanna.interview.OrderProcessorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OrderProcessorApplication.class,
        properties = {
                "order.service-name=config-test-service",
                "order.environment=integration-test",
                "order.aws-region=us-east-1",
                "order.order-processed-topic-arn=arn:aws:sns:us-east-1:xxxxxxxxx:OrderProcessedTopic",
                "order.enable-idempotency=false",
                "order.max-retry-count=5",
                "order.structured-logging=true",
                "order.reference-data-cache-ttl-seconds=120",
                "order.max-payload-size-bytes=131072",
                "order.mask-customer-id-in-logs=true",
                "order.enable-strict-json-validation=true",
                "order.enable-virtual-threads=true",
                "order.enable-notification-publishing=false",
                "order.api.enabled=true",
                "order.api.base-path=/internal/orders",
                "order.api.auth-header-name=X-Order-Api-Token",
                "order.api.auth-token=test-token",
                "order.api.principal-name=integration-client"
        }
)
class OrderProcessingPropertiesTest {

    @Autowired
    private OrderProcessingProperties properties;

    @Test
    void bindsOrderConfigurationProperties() {
        assertThat(properties.serviceName()).isEqualTo("config-test-service");
        assertThat(properties.environment()).isEqualTo("integration-test");
        assertThat(properties.maxRetryCount()).isEqualTo(5);
        assertThat(properties.referenceDataCacheTtlSeconds()).isEqualTo(120);
        assertThat(properties.maxPayloadSizeBytes()).isEqualTo(131_072);
        assertThat(properties.maskCustomerIdInLogs()).isTrue();
        assertThat(properties.enableVirtualThreads()).isTrue();
        assertThat(properties.enableNotificationPublishing()).isFalse();
        assertThat(properties.api().enabled()).isTrue();
        assertThat(properties.api().basePath()).isEqualTo("/internal/orders");
        assertThat(properties.api().authHeaderName()).isEqualTo("X-Order-Api-Token");
        assertThat(properties.api().authToken()).isEqualTo("test-token");
        assertThat(properties.api().principalName()).isEqualTo("integration-client");
    }
}
