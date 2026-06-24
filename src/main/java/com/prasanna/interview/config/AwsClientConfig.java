package com.prasanna.interview.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Spring bean configuration for AWS SDK clients and JSON serialization.
 */
@Configuration
public class AwsClientConfig {

    /**
     * Creates the AWS client configuration.
     */
    public AwsClientConfig() {
    }

    @Bean
    SnsClient snsClient(OrderProcessingProperties properties) {
        return SnsClient.builder()
                .region(Region.of(properties.awsRegion()))
                .build();
    }

    @Bean
    DynamoDbClient dynamoDbClient(OrderProcessingProperties properties) {
        return DynamoDbClient.builder()
                .region(Region.of(properties.awsRegion()))
                .build();
    }

    @Bean
    ObjectMapper objectMapper(OrderProcessingProperties properties) {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, properties.enableStrictJsonValidation())
                .build();
    }
}
