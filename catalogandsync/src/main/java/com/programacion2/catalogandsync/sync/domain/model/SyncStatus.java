package com.programacion2.catalogandsync.sync.domain.model;

import java.time.Instant;

/**
 * Estado observable del ultimo ciclo exitoso de sincronizacion.
 * <p>
 * Los valores desconocidos son {@code null}: nunca se sincronizo, la version
 * local no estaba disponible o la lectura de la catedra fallo. {@code stale}
 * indica que ese ultimo exito quedo mas viejo que el umbral configurado.
 */
public record SyncStatus(
        Long localVersion,
        Long currentVersion,
        Long oldestAvailableVersion,
        SyncMode lastSyncMode,
        Instant lastSyncAt,
        boolean stale
) {
}
