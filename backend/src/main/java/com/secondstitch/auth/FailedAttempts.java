package com.secondstitch.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers wrong-password attempts, so nobody can guess the password by
 * trying thousands of times. After 5 wrong tries in 15 minutes, that
 * address has to wait.
 *
 * Kept in memory: it resets when the server restarts, which is fine for this app.
 */
@Component
public class FailedAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new HashMap<>();
    private final Clock clock;

    public FailedAttempts() {
        this(Clock.systemUTC());
    }

    FailedAttempts(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean tooMany(String who) {
        return recent(who).size() >= MAX_FAILURES;
    }

    public synchronized void record(String who) {
        recent(who).addLast(clock.instant());
    }

    public synchronized void clear(String who) {
        failures.remove(who);
    }

    /** This address's failures from the last 15 minutes (older ones are dropped). */
    private Deque<Instant> recent(String who) {
        Deque<Instant> list = failures.computeIfAbsent(who, k -> new ArrayDeque<>());
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!list.isEmpty() && list.peekFirst().isBefore(cutoff)) {
            list.removeFirst();
        }
        return list;
    }
}
