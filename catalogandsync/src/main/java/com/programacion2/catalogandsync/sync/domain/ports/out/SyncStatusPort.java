package com.programacion2.catalogandsync.sync.domain.ports.out;

import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;

/**
 * Guarda el estado del ultimo ciclo exitoso para consultarlo sin hacer I/O
 * (H1.9). Una implementacion en memoria es valida para una sola instancia.
 * <p>
 * {@link #record} se llama solo cuando el ciclo termino bien; un ciclo fallido
 * conserva el estado anterior para que el consumidor vea cuanto hace que no hay
 * un exito (via {@code stale}).
 */
public interface SyncStatusPort {

    void record(SyncMode mode, long localVersion, Long currentVersion, Long oldestAvailableVersion);

    SyncStatus currentStatus();
}
