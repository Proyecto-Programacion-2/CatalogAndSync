package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.infrastructure.config.SyncProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Politica de reintentos fija de H1.9: {@code maxAttempts} intentos totales y
 * una espera constante entre intentos (3 intentos y 2 s por defecto).
 * <p>
 * Solo reintenta lo que el predicado declare transitorio. El llamador decide
 * que errores califican; esta clase no conoce HTTP ni Redis. Si se agotan los
 * intentos, relanza la ultima excepcion tal cual, sin envolverla.
 */
@Slf4j
@Component
public class FixedRetryExecutor {

    private final int maxAttempts;
    private final Duration delay;

    @Autowired
    public FixedRetryExecutor(SyncProperties properties) {
        this(properties.retry().maxAttempts(), properties.retry().delay());
    }

    FixedRetryExecutor(int maxAttempts, Duration delay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("delay must be zero or positive");
        }
        this.maxAttempts = maxAttempts;
        this.delay = delay;
    }

    public <T> T execute(String operation, Supplier<T> action, Predicate<RuntimeException> retryable) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                if (!retryable.test(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                log.warn("{} failed on attempt {}/{}; retrying in {}ms: {}",
                        operation, attempt, maxAttempts, delay.toMillis(), ex.getMessage());
                sleep(operation);
            }
        }
        throw new IllegalStateException("Retry loop ended without a result");
    }

    private void sleep(String operation) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SyncException(operation + " was interrupted while waiting to retry", ex);
        }
    }
}
