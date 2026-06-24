package com.prasanna.interview.exception;

/**
 * Raised when required fields, supported order types, or payload-size constraints are violated.
 */
public class ValidationException extends OrderProcessingException {

    /**
     * Creates a validation exception.
     *
     * @param message validation failure message
     */
    public ValidationException(String message) {
        super(message);
    }
}
