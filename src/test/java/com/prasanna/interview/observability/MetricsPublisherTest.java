package com.prasanna.interview.observability;

import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.config.OrderProcessingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsPublisherTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void writesCountMetricInCloudWatchEmbeddedMetricFormat() throws Exception {
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        MetricsPublisher publisher = new MetricsPublisher(objectMapper, TestFixtures.properties());

        publisher.count(MetricsPublisher.ORDER_MESSAGE_PROCESSED);

        Map<String, Object> metric = objectMapper.readValue(stdout(), new TypeReference<>() {
        });
        assertThat(metric)
                .containsEntry("Service", "order-processor-test")
                .containsEntry("Environment", "test")
                .containsEntry(MetricsPublisher.ORDER_MESSAGE_PROCESSED, 1);
        assertThat(metric.get("_aws")).isInstanceOf(Map.class);
    }

    @Test
    void writesDurationMetricWithDurationValue() throws Exception {
        ObjectMapper objectMapper = TestFixtures.objectMapper(true);
        MetricsPublisher publisher = new MetricsPublisher(objectMapper, TestFixtures.properties());

        publisher.duration(MetricsPublisher.ORDER_PROCESSING_DURATION_MS, 123);

        Map<String, Object> metric = objectMapper.readValue(stdout(), new TypeReference<>() {
        });
        assertThat(metric).containsEntry(MetricsPublisher.ORDER_PROCESSING_DURATION_MS, 123);
        assertThat(stdout()).contains("Milliseconds");
    }

    @Test
    void fallsBackToMapOutputWhenMetricJsonSerializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        OrderProcessingProperties properties = TestFixtures.properties();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });
        MetricsPublisher publisher = new MetricsPublisher(objectMapper, properties);

        publisher.count(MetricsPublisher.VALIDATION_FAILED);

        assertThat(stdout())
                .contains("ValidationFailed=1")
                .contains("Service=order-processor-test");
    }

    private String stdout() {
        return output.toString(StandardCharsets.UTF_8).trim();
    }
}
