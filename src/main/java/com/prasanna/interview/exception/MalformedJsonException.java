package com.prasanna.interview.exception;

/**
 * Raised when an SQS body, SNS envelope message, or direct API payload cannot be parsed as JSON.
 */
public class MalformedJsonException extends OrderProcessingException {

    /**
     * Creates a malformed JSON exception.
     *
     * @param message failure message
     * @param cause JSON parsing cause
     */
    public MalformedJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
