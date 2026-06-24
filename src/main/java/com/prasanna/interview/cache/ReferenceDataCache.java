package com.prasanna.interview.cache;

import com.prasanna.interview.config.OrderProcessingProperties;
import com.prasanna.interview.observability.MetricsPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * In-memory TTL cache for reference data only.
 *
 * <p>Order messages and order events are never cached. This cache is intended for stable lookup/reference values that
 * are safe to reuse for a short configured period.</p>
 */
@Component
public class ReferenceDataCache {

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;
    private final MetricsPublisher metricsPublisher;

    /**
     * Creates a cache using the configured TTL and system UTC clock.
     *
     * @param properties cache TTL configuration
     * @param metricsPublisher cache hit, miss, and load-failure metrics publisher
     */
    @Autowired
    public ReferenceDataCache(OrderProcessingProperties properties, MetricsPublisher metricsPublisher) {
        this(Duration.ofSeconds(properties.referenceDataCacheTtlSeconds()), Clock.systemUTC(), metricsPublisher);
    }

    ReferenceDataCache(Duration ttl, Clock clock, MetricsPublisher metricsPublisher) {
        this.ttl = ttl;
        this.clock = clock;
        this.metricsPublisher = metricsPublisher;
    }

    /**
     * Returns a cached reference-data value or loads and stores a new value.
     *
     * @param key cache key
     * @param loader loader invoked on cache miss or expired entry
     * @return cached or loaded value
     */
    public String get(String key, Supplier<String> loader) {
        CacheEntry existing = entries.get(key);
        Instant now = Instant.now(clock);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            metricsPublisher.count(MetricsPublisher.REFERENCE_DATA_CACHE_HIT);
            return existing.value();
        }

        metricsPublisher.count(MetricsPublisher.REFERENCE_DATA_CACHE_MISS);
        try {
            String value = loader.get();
            entries.put(key, new CacheEntry(value, now.plus(ttl)));
            return value;
        } catch (RuntimeException e) {
            metricsPublisher.count(MetricsPublisher.REFERENCE_DATA_CACHE_LOAD_FAILED);
            throw e;
        }
    }

    /**
     * Clears all cached reference-data values.
     */
    public void clear() {
        entries.clear();
    }

    private record CacheEntry(String value, Instant expiresAt) {
    }
}
