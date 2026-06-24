package com.prasanna.interview.api.web;

import com.prasanna.interview.api.ApiRequestMetadata;
import com.prasanna.interview.api.OrderApiExtensionPoint;
import com.prasanna.interview.api.OrderApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Conditional REST adapter for the optional secured order-processing API.
 *
 * <p>The controller is only created for servlet applications when {@code order.api.enabled=true}. It accepts direct
 * order JSON, extracts operational metadata from HTTP headers and Spring Security, and delegates to
 * {@link OrderApiExtensionPoint}.</p>
 */
@RestController
@RequestMapping("${order.api.base-path:/api/v1/orders}")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "order.api", name = "enabled", havingValue = "true")
public class OrderApiController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final OrderApiExtensionPoint extensionPoint;

    /**
     * Creates the REST controller.
     *
     * @param extensionPoint API extension point used to process direct order JSON
     */
    public OrderApiController(OrderApiExtensionPoint extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    /**
     * Processes a direct {@code DigitalOrder} or {@code PhysicalOrder} JSON payload.
     *
     * @param orderJson direct order-event JSON request body
     * @param request servlet request used for request id and source IP extraction
     * @param authentication authenticated principal established by the API token filter
     * @return {@code 202 Accepted} with the order-processing result
     */
    @PostMapping("/process")
    public ResponseEntity<OrderApiResponse> process(@RequestBody String orderJson,
                                                    HttpServletRequest request,
                                                    Authentication authentication) {
        ApiRequestMetadata metadata = new ApiRequestMetadata(
                requestId(request),
                principal(authentication),
                sourceIp(request)
        );
        return ResponseEntity.accepted().body(extensionPoint.process(orderJson, metadata));
    }

    private String requestId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private String principal(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String sourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
