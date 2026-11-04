package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedRetryExecutorTest {

    @Test
    void returnsTheFirstSuccessfulResultWithoutRetrying() {
        AtomicInteger calls = new AtomicInteger();
        FixedRetryExecutor executor = new FixedRetryExecutor(3, Duration.ZERO);

        String result = executor.execute(
                "operation",
                () -> {
                    calls.incrementAndGet();
                    return "ok";
                },
                ex -> true);

        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    void retriesUntilAnAttemptSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        FixedRetryExecutor executor = new FixedRetryExecutor(3, Duration.ZERO);

        String result = executor.execute(
                "operation",
                () -> {
                    if (calls.incrementAndGet() < 3) {
                        throw new SyncException("transient");
                    }
                    return "ok";
                },
                ex -> true);

        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void rethrowsTheLastFailureWhenAttemptsAreExhausted() {
        AtomicInteger calls = new AtomicInteger();
        FixedRetryExecutor executor = new FixedRetryExecutor(3, Duration.ZERO);

        SyncException exception = assertThrows(SyncException.class, () -> executor.execute(
                "operation",
                () -> {
                    calls.incrementAndGet();
                    throw new SyncException("still down");
                },
                ex -> true));

        assertEquals(3, calls.get());
        assertEquals("still down", exception.getMessage());
    }

    @Test
    void doesNotRetryWhenTheFailureIsNotRetryable() {
        AtomicInteger calls = new AtomicInteger();
        FixedRetryExecutor executor = new FixedRetryExecutor(3, Duration.ZERO);

        assertThrows(SyncException.class, () -> executor.execute(
                "operation",
                () -> {
                    calls.incrementAndGet();
                    throw new SyncException("contract error");
                },
                ex -> false));

        assertEquals(1, calls.get());
    }

    @Test
    void interruptionDuringTheDelayStopsRetryingAndSurfacesAsSyncException() throws Exception {
        FixedRetryExecutor executor = new FixedRetryExecutor(3, Duration.ofSeconds(5));
        CountDownLatch firstAttempt = new CountDownLatch(1);
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                executor.execute(
                        "operation",
                        () -> {
                            firstAttempt.countDown();
                            throw new SyncException("transient");
                        },
                        ex -> true);
            } catch (Throwable throwable) {
                caught.set(throwable);
            }
        });

        thread.start();
        assertTrue(firstAttempt.await(2, TimeUnit.SECONDS));
        thread.interrupt();
        thread.join(2_000);

        assertFalse(thread.isAlive());
        SyncException exception = assertInstanceOf(SyncException.class, caught.get());
        assertTrue(exception.getMessage().contains("interrupted"));
    }

    @Test
    void rejectsAnAttemptCountBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedRetryExecutor(0, Duration.ZERO));
    }
}
