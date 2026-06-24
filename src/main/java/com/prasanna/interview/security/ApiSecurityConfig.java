package com.prasanna.interview.security;

import com.prasanna.interview.config.OrderProcessingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the optional HTTP API.
 *
 * <p>This configuration is active only for servlet applications with {@code order.api.enabled=true}. It creates a
 * stateless security chain scoped to the configured API base path and inserts {@link ApiTokenAuthenticationFilter}
 * before username/password authentication.</p>
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "order.api", name = "enabled", havingValue = "true")
public class ApiSecurityConfig {

    /**
     * Creates the API security configuration.
     */
    public ApiSecurityConfig() {
    }

    /**
     * Builds the stateless filter chain for the order API.
     *
     * @param http Spring Security HTTP builder
     * @param properties application properties containing API path and token settings
     * @return security filter chain scoped to the configured API base path
     * @throws Exception when Spring Security cannot build the filter chain
     */
    @Bean
    SecurityFilterChain orderApiSecurityFilterChain(HttpSecurity http,
                                                    OrderProcessingProperties properties) throws Exception {
        OrderProcessingProperties.Api api = properties.api();
        requireToken(api);
        return http
                .securityMatcher(securityMatcher(api.basePath()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .addFilterBefore(new ApiTokenAuthenticationFilter(api), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void requireToken(OrderProcessingProperties.Api api) {
        if (api.authToken() == null || api.authToken().isBlank()) {
            throw new IllegalStateException("order.api.auth-token must be configured when order.api.enabled=true");
        }
    }

    private String securityMatcher(String basePath) {
        String normalized = basePath.startsWith("/") ? basePath : "/" + basePath;
        return normalized.endsWith("/") ? normalized + "**" : normalized + "/**";
    }
}
