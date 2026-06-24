package com.prasanna.interview.service;

/**
 * Event-id based deduplication boundary.
 *
 * <p>Production implementations should make {@link #isDuplicate(String)} and {@link #markProcessed(String)}
 * durable enough for the chosen delivery semantics. The provided DynamoDB skeleton is intended to become that
 * production implementation.</p>
 */
public interface IdempotencyService {

    /**
     * Checks whether the event id has already been processed.
     *
     * @param eventId source event id
     * @return {@code true} when the event should be skipped
     */
    boolean isDuplicate(String eventId);

    /**
     * Marks the event id as processed after the processed notification has been published.
     *
     * @param eventId source event id
     */
    void markProcessed(String eventId);
}
