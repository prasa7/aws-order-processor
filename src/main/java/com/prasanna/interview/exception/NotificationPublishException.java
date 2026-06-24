package com.prasanna.interview.exception;

/**
 * Raised when the processed-order notification cannot be serialized or published to SNS.
 */
public class NotificationPublishException extends OrderProcessingException {

    /**
     * Creates a notification publish exception.
     *
     * @param message failure message
     * @param cause serialization or SNS client cause
     */
    public NotificationPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
