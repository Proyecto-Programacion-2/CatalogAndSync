package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;
import com.programacion2.catalogandsync.sync.domain.ports.out.IncrementalSourcePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decorador de {@link IncrementalSourcePort} con la politica fija de reintentos
 * de H1.9.
 * <p>
 * Solo reintenta {@link IncrementalSourceUnavailableException} (transporte,
 * timeout, AUTH). Un contrato roto ({@code IncrementalSourceException}) no se
 * reintenta: esperar no cambia el dato publicado. Un
 * {@link Optional#empty()} es una respuesta valida (historial perdido) y
 * tampoco se reintenta.
 * <p>
 * Es {@link Primary} para que el caso de uso reciba el decorador y no el
 * adaptador crudo; el adaptador real se inyecta por nombre
 * ({@code redisIncrementalSourceAdapter}).
 */
@Primary
@Component
public class RetryingIncrementalSourceAdapter implements IncrementalSourcePort {

    private final IncrementalSourcePort delegate;
    private final FixedRetryExecutor retryExecutor;

    public RetryingIncrementalSourceAdapter(
            @Qualifier("redisIncrementalSourceAdapter") IncrementalSourcePort delegate,
            FixedRetryExecutor retryExecutor) {
        this.delegate = delegate;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public Optional<IncrementalBatch> read(long version) {
        return retryExecutor.execute(
                "Catedra incremental read for version " + version,
                () -> delegate.read(version),
                RetryingIncrementalSourceAdapter::isRetryable);
    }

    private static boolean isRetryable(RuntimeException ex) {
        return ex instanceof IncrementalSourceUnavailableException;
    }
}
