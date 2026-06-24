package com.prasanna.interview.api;

import com.prasanna.interview.observability.ProcessingLogContext;
import com.prasanna.interview.service.OrderProcessingOutcome;
import com.prasanna.interview.service.OrderProcessingService;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the API extension point.
 *
 * <p>This adapter intentionally stays thin: it converts API request metadata into the internal structured logging
 * context and delegates all parsing, validation, idempotency, processing, metrics, and SNS publishing to
 * {@link OrderProcessingService}.</p>
 */
@Service
public class DefaultOrderApiExtensionPoint implements OrderApiExtensionPoint {

    private final OrderProcessingService orderProcessingService;

    /**
     * Creates the extension point using the shared order-processing service.
     *
     * @param orderProcessingService service used by both Lambda and API execution paths
     */
    public DefaultOrderApiExtensionPoint(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @Override
    public OrderApiResponse process(String orderJson, ApiRequestMetadata metadata) {
        ProcessingLogContext logContext = ProcessingLogContext.forApi(
                metadata == null ? null : metadata.requestId(),
                metadata == null ? null : metadata.principal(),
                metadata == null ? null : metadata.sourceIp()
        );
        OrderProcessingOutcome outcome = orderProcessingService.processApiPayload(orderJson, logContext);
        return OrderApiResponse.from(outcome);
    }
}
