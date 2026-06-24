package com.prasanna.interview.security;

import com.prasanna.interview.OrderProcessorApplication;
import com.prasanna.interview.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OrderProcessorApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.main.web-application-type=servlet",
                "order.service-name=security-test-service",
                "order.environment=test",
                "order.aws-region=us-east-1",
                "order.order-processed-topic-arn=arn:aws:sns:us-east-1:xxxxxxxxx:OrderProcessedTopic",
                "order.enable-idempotency=false",
                "order.max-retry-count=3",
                "order.structured-logging=true",
                "order.reference-data-cache-ttl-seconds=60",
                "order.max-payload-size-bytes=262144",
                "order.mask-customer-id-in-logs=false",
                "order.enable-strict-json-validation=true",
                "order.enable-virtual-threads=false",
                "order.enable-notification-publishing=false",
                "order.api.enabled=true",
                "order.api.auth-token=test-api-token"
        }
)
@AutoConfigureMockMvc
class ApiSecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithoutConfiguredBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/orders/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.digitalOrderJson("evt-security-missing-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void acceptsRequestsWithConfiguredBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/orders/process")
                        .header("Authorization", "Bearer test-api-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.digitalOrderJson("evt-security-success")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.eventId").value("evt-security-success"));
    }

    @Test
    void normalizesSecurityMatcherBasePath() {
        ApiSecurityConfig config = new ApiSecurityConfig();

        String matcher = ReflectionTestUtils.invokeMethod(config, "securityMatcher", "internal/orders/");

        assertThat(matcher).isEqualTo("/internal/orders/**");
    }

    @Test
    void rejectsBlankApiTokenConfiguration() {
        ApiSecurityConfig config = new ApiSecurityConfig();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                config,
                "requireToken",
                new com.prasanna.interview.config.OrderProcessingProperties.Api(
                        true,
                        "/api/v1/orders",
                        "Authorization",
                        " ",
                        "order-api-client"
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("order.api.auth-token must be configured when order.api.enabled=true");
    }
}
