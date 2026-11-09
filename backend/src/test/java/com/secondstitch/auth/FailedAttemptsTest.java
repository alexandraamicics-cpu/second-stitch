package com.secondstitch.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class FailedAttemptsTest {

    /** A clock the test can move forward by hand. */
    static class FakeClock extends Clock {
        Instant now = Instant.parse("2026-11-09T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void blocksAfterFiveFailuresAndUnblocksAfterFifteenMinutes() {
        FakeClock clock = new FakeClock();
        FailedAttempts attempts = new FailedAttempts(clock);

        for (int i = 0; i < 4; i++) attempts.record("1.2.3.4");
        assertThat(attempts.tooMany("1.2.3.4")).isFalse();

        attempts.record("1.2.3.4");
        assertThat(attempts.tooMany("1.2.3.4")).isTrue();

        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertThat(attempts.tooMany("1.2.3.4")).isFalse();
    }

    @Test
    void theRightPasswordClearsTheCount() {
        FailedAttempts attempts = new FailedAttempts(new FakeClock());
        for (int i = 0; i < 4; i++) attempts.record("1.2.3.4");
        attempts.clear("1.2.3.4");
        attempts.record("1.2.3.4");
        assertThat(attempts.tooMany("1.2.3.4")).isFalse();
    }
}
