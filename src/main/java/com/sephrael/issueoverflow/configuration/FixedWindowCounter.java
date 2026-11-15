package com.sephrael.issueoverflow.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts attempts per key in fixed windows: a key's window starts with its first attempt and lasts the given time,
 * after which the key starts again from zero. Windows are kept in memory; past 100,000 keys, Caffeine drops some.
 * The scheduler removes a window when it ends, even when no more requests come.
 */
final class FixedWindowCounter {

    private static final long MAX_KEYS = 100_000;

    private final int maxAttempts;
    private final long windowNanos;
    private final Ticker ticker;
    private final Cache<String, Window> windows;

    FixedWindowCounter(int maxAttempts, Duration window, Ticker ticker) {
        this.maxAttempts = maxAttempts;
        this.windowNanos = window.toNanos();
        this.ticker = ticker;
        this.windows = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(MAX_KEYS)
                .ticker(ticker)
                .scheduler(Scheduler.systemScheduler())
                .build();
    }

    // Counts one attempt for the key: 0 if it is within the limit, otherwise the seconds until the key's window ends (at least 1).
    long secondsToWait(String key) {
        Window window = windows.get(key, ignored -> new Window(ticker.read(), new AtomicInteger()));
        if(window.attempts().incrementAndGet() <= maxAttempts)
            return 0;

        long remainingNanos = window.startedAt() + windowNanos - ticker.read();
        return Math.max(1, Duration.ofNanos(remainingNanos).toSeconds() + 1);
    }

    private record Window(long startedAt, AtomicInteger attempts) {
    }
}
