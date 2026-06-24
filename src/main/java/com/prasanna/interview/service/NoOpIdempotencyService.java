package com.prasanna.interview.service;

/**
 * Idempotency implementation that never treats events as duplicates.
 *
 * <p>This is the default when idempotency is disabled.</p>
 */
public class NoOpIdempotencyService implements IdempotencyService {

    /**
     * Creates the no-op idempotency service.
     */
    public NoOpIdempotencyService() {
    }

    @Override
    public boolean isDuplicate(String eventId) {
        return false;
    }

    @Override
    public void markProcessed(String eventId) {
        // Intentionally empty. Use DynamoDbIdempotencyService when idempotency is enabled.
    }
}
