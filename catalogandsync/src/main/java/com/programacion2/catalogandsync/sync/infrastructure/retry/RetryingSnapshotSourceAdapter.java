package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.SnapshotFetchException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Decorador de {@link SnapshotSourcePort} que aplica la politica fija de
 * reintentos de H1.9 sin tocar el adaptador REST.
 * <p>
 * Reintenta solo errores transitorios: transporte/timeout ({@code status ==
 * null}) y {@code 5xx}. Un {@code 4xx} o un contrato invalido
 * ({@code SnapshotMappingException}) no se reintentan porque la respuesta no
 * va a cambiar.
 * <p>
 * Es {@link Primary} para que el caso de uso reciba el decorador y no el
 * adaptador crudo; el adaptador real se inyecta por nombre
 * ({@code catedraSnapshotAdapter}).
 */
@Primary
@Component
public class RetryingSnapshotSourceAdapter implements SnapshotSourcePort {

    private final SnapshotSourcePort delegate;
    private final FixedRetryExecutor retryExecutor;

    public RetryingSnapshotSourceAdapter(
            @Qualifier("catedraSnapshotAdapter") SnapshotSourcePort delegate,
            FixedRetryExecutor retryExecutor) {
        this.delegate = delegate;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public Snapshot fetchSnapshot() {
        return retryExecutor.execute(
                "Catedra snapshot request",
                delegate::fetchSnapshot,
                RetryingSnapshotSourceAdapter::isRetryable);
    }

    private static boolean isRetryable(RuntimeException ex) {
        if (!(ex instanceof SnapshotFetchException fetchException)) {
            return false;
        }
        Integer status = fetchException.status();
        return status == null || status >= 500;
    }
}
