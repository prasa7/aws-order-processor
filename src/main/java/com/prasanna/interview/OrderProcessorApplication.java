package com.prasanna.interview;

import com.prasanna.interview.config.OrderProcessingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot bootstrap class for local tooling, tests, and optional servlet API mode.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(OrderProcessingProperties.class)
public class OrderProcessorApplication {

    /**
     * Creates the application bootstrap instance.
     */
    public OrderProcessorApplication() {
    }

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderProcessorApplication.class, args);
    }
}
