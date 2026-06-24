package com.prasanna.interview.api.web;

import com.prasanna.interview.exception.MalformedJsonException;
import com.prasanna.interview.exception.NotificationPublishException;
import com.prasanna.interview.exception.OrderProcessingException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.security.SecuritySanitizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Maps domain exceptions from the shared processing service to stable HTTP API responses.
 *
 * <p>Messages are sanitized before they are returned, which keeps malformed payload and downstream errors useful
 * without exposing control characters or unsafe text to clients.</p>
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "order.api", name = "enabled", havingValue = "true")
public class OrderApiExceptionHandler {

    private final SecuritySanitizer sanitizer;

    /**
     * Creates the API exception handler.
     *
     * @param sanitizer sanitizer used before returning error messages to clients
     */
    public OrderApiExceptionHandler(SecuritySanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    /**
     * Converts validation and malformed JSON failures to {@code 400 Bad Request}.
     *
     * @param exception validation or parsing exception
     * @return sanitized API error response
     */
    @ExceptionHandler({ValidationException.class, MalformedJsonException.class})
    public ResponseEntity<OrderApiErrorResponse> badRequest(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception);
    }

    /**
     * Converts processed-notification publish failures to {@code 502 Bad Gateway}.
     *
     * @param exception notification publish exception
     * @return sanitized API error response
     */
    @ExceptionHandler(NotificationPublishException.class)
    public ResponseEntity<OrderApiErrorResponse> badGateway(NotificationPublishException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception);
    }

    /**
     * Converts unexpected processing failures to {@code 500 Internal Server Error}.
     *
     * @param exception processing exception
     * @return sanitized API error response
     */
    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<OrderApiErrorResponse> internalServerError(OrderProcessingException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<OrderApiErrorResponse> error(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(new OrderApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                sanitizer.sanitizeErrorMessage(exception.getMessage())
        ));
    }
}
