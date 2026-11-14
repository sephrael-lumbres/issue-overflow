package com.sephrael.issueoverflow.configuration;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowCounterTest {

    private final AtomicLong now = new AtomicLong();
    private final Ticker ticker = now::get;
    private final FixedWindowCounter counter = new FixedWindowCounter(3, Duration.ofMinutes(10), ticker);

    @Test
    void allowsUpToTheLimitThenSaysHowLongToWait() {
        assertThat(counter.secondsToWait("203.0.113.7")).isZero();
        assertThat(counter.secondsToWait("203.0.113.7")).isZero();
        assertThat(counter.secondsToWait("203.0.113.7")).isZero();

        advance(Duration.ofMinutes(4));

        assertThat(counter.secondsToWait("203.0.113.7")).isEqualTo(Duration.ofMinutes(6).toSeconds() + 1);
    }

    @Test
    void aKeyStartsAgainWhenItsWindowEnds() {
        for(int attempt = 0; attempt < 4; attempt++) {
            counter.secondsToWait("203.0.113.7");
        }

        advance(Duration.ofMinutes(10).plusSeconds(1));

        assertThat(counter.secondsToWait("203.0.113.7")).isZero();
    }

    @Test
    void eachKeyHasItsOwnCount() {
        for(int attempt = 0; attempt < 3; attempt++) {
            counter.secondsToWait("203.0.113.7");
        }

        assertThat(counter.secondsToWait("203.0.113.8")).isZero();
        assertThat(counter.secondsToWait("203.0.113.7")).isPositive();
    }

    private void advance(Duration duration) {
        now.addAndGet(duration.toNanos());
    }
}
