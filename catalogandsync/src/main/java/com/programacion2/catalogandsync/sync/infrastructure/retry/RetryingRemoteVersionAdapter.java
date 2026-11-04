package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.VersionSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.ports.out.RemoteVersionPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Decorador de {@link RemoteVersionPort} con la politica fija de reintentos de
 * H1.9.
 * <p>
 * Solo reintenta {@link VersionSourceUnavailableException} (transporte,
 * timeout, AUTH). Un valor ausente, vacio, no numerico o negativo sigue
 * fallando de inmediato: esperar no cambia el dato publicado por la catedra.
 * <p>
 * Cada clave se reintenta por separado; el planner ya tolera que las dos
 * lecturas no sean atomicas entre si.
 */
@Primary
@Component
public class RetryingRemoteVersionAdapter implements RemoteVersionPort {

    private final RemoteVersionPort delegate;
    private final FixedRetryExecutor retryExecutor;

    public RetryingRemoteVersionAdapter(
            @Qualifier("redisVersionSourceAdapter") RemoteVersionPort delegate,
            FixedRetryExecutor retryExecutor) {
        this.delegate = delegate;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public long currentVersion() {
        return retryExecutor.execute(
                "Catedra current-version read",
                delegate::currentVersion,
                RetryingRemoteVersionAdapter::isRetryable);
    }

    @Override
    public long oldestAvailableVersion() {
        return retryExecutor.execute(
                "Catedra oldest-available-version read",
                delegate::oldestAvailableVersion,
                RetryingRemoteVersionAdapter::isRetryable);
    }

    private static boolean isRetryable(RuntimeException ex) {
        return ex instanceof VersionSourceUnavailableException;
    }
}
