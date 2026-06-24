package com.prasanna.interview.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency implementation for tests and local-only scenarios.
 */
public class InMemoryIdempotencyService implements IdempotencyService {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * Creates an empty in-memory idempotency store.
     */
    public InMemoryIdempotencyService() {
    }

    @Override
    public boolean isDuplicate(String eventId) {
        return processedEventIds.contains(eventId);
    }

    @Override
    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }
}
