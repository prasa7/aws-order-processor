package com.prasanna.interview.security;

import com.prasanna.interview.config.OrderProcessingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Stateless bearer-token filter for the optional HTTP API.
 *
 * <p>The filter accepts either {@code Bearer <token>} or the raw token value in the configured header. Token
 * comparison uses {@link MessageDigest#isEqual(byte[], byte[])} to avoid early-exit string comparison behavior.</p>
 */
class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String headerName;
    private final String expectedToken;
    private final String principalName;

    /**
     * Creates the filter from API configuration.
     *
     * @param properties API security properties
     */
    ApiTokenAuthenticationFilter(OrderProcessingProperties.Api properties) {
        this.headerName = properties.authHeaderName();
        this.expectedToken = properties.authToken();
        this.principalName = properties.principalName();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedToken = extractToken(request.getHeader(headerName));
        if (!tokensMatch(providedToken, expectedToken)) {
            unauthorized(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principalName,
                "N/A",
                AuthorityUtils.createAuthorityList("ROLE_ORDER_API")
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return "";
        }
        if (headerValue.startsWith(BEARER_PREFIX)) {
            return headerValue.substring(BEARER_PREFIX.length()).trim();
        }
        return headerValue.trim();
    }

    private boolean tokensMatch(String providedToken, String expectedToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return false;
        }
        byte[] provided = providedToken.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Unauthorized\"}");
    }
}
