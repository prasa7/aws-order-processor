package com.prasanna.interview.api.web;

import java.time.Instant;

/**
 * Error body returned by the optional HTTP API.
 *
 * @param timestamp server timestamp for the error response
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message sanitized error message safe for API clients and logs
 */
public record OrderApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
