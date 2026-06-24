package com.prasanna.interview.cache;

import com.prasanna.interview.observability.MetricsPublisher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReferenceDataCacheTest {

    @Test
    void returnsCachedReferenceDataUntilTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-23T10:00:00Z"));
        MetricsPublisher metricsPublisher = mock(MetricsPublisher.class);
        ReferenceDataCache cache = new ReferenceDataCache(Duration.ofSeconds(5), clock, metricsPublisher);
        AtomicInteger loads = new AtomicInteger();

        String first = cache.get("currency:USD", () -> "value-" + loads.incrementAndGet());
        String second = cache.get("currency:USD", () -> "value-" + loads.incrementAndGet());
        clock.advance(Duration.ofSeconds(6));
        String third = cache.get("currency:USD", () -> "value-" + loads.incrementAndGet());

        assertThat(first).isEqualTo("value-1");
        assertThat(second).isEqualTo("value-1");
        assertThat(third).isEqualTo("value-2");
        verify(metricsPublisher).count(MetricsPublisher.REFERENCE_DATA_CACHE_HIT);
        verify(metricsPublisher, org.mockito.Mockito.times(2)).count(MetricsPublisher.REFERENCE_DATA_CACHE_MISS);
    }

    @Test
    void recordsReferenceDataLoadFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-23T10:00:00Z"));
        MetricsPublisher metricsPublisher = mock(MetricsPublisher.class);
        ReferenceDataCache cache = new ReferenceDataCache(Duration.ofSeconds(5), clock, metricsPublisher);

        assertThatThrownBy(() -> cache.get("currency:FAIL", () -> {
            throw new IllegalStateException("reference data unavailable");
        })).isInstanceOf(IllegalStateException.class);

        verify(metricsPublisher).count(MetricsPublisher.REFERENCE_DATA_CACHE_LOAD_FAILED);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
