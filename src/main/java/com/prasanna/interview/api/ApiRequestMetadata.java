package com.prasanna.interview.api;

/**
 * Request-scoped metadata captured by HTTP adapters before handing work to the shared order-processing service.
 *
 * @param requestId client supplied or generated request identifier used for structured logs
 * @param principal authenticated caller name resolved by the API security layer
 * @param sourceIp best-effort caller IP address, usually from {@code X-Forwarded-For} or the remote address
 */
public record ApiRequestMetadata(
        String requestId,
        String principal,
        String sourceIp
) {
}
