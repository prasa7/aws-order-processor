package com.prasanna.interview.security;

import com.prasanna.interview.config.OrderProcessingProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTokenAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerTokenAndContinuesFilterChain() throws Exception {
        ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(apiProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders/process");
        request.addHeader("Authorization", "Bearer test-api-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};
        FilterChain chain = (servletRequest, servletResponse) -> {
            chainInvoked[0] = true;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getName()).isEqualTo("partner-service");
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ORDER_API");
        };

        filter.doFilter(request, response, chain);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInvalidTokenBeforeCallingFilterChain() throws Exception {
        ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(apiProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders/process");
        request.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};
        FilterChain chain = (servletRequest, servletResponse) -> chainInvoked[0] = true;

        filter.doFilter(request, response, chain);

        assertThat(chainInvoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
    }

    private OrderProcessingProperties.Api apiProperties() {
        return new OrderProcessingProperties.Api(
                true,
                "/api/v1/orders",
                "Authorization",
                "test-api-token",
                "partner-service"
        );
    }
}
