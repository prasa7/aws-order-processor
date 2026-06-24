package com.prasanna.interview.exception;

/**
 * Base unchecked exception for order-processing failures.
 */
public class OrderProcessingException extends RuntimeException {

    /**
     * Creates an order-processing exception.
     *
     * @param message failure message
     */
    public OrderProcessingException(String message) {
        super(message);
    }

    /**
     * Creates an order-processing exception with a cause.
     *
     * @param message failure message
     * @param cause root cause
     */
    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
