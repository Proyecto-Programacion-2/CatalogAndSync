package com.programacion2.catalogandsync.sync.infrastructure.status;

import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.domain.ports.out.SyncStatusPort;
import com.programacion2.catalogandsync.sync.infrastructure.config.SyncProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementacion en memoria de {@link SyncStatusPort} (H1.9).
 * <p>
 * Es valida para una sola instancia y se pierde al reiniciar: el estado se
 * reconstruye con el primer ciclo (que corre al arrancar). No justifica una
 * tabla propia porque es observabilidad operativa, no dato del negocio.
 */
@Component
public class InMemorySyncStatusAdapter implements SyncStatusPort {

    private final AtomicReference<Entry> lastSuccessful = new AtomicReference<>();
    private final Duration staleAfter;

    public InMemorySyncStatusAdapter(SyncProperties properties) {
        this.staleAfter = Duration.ofMillis(properties.status().staleAfterMs());
    }

    @Override
    public void record(SyncMode mode, long localVersion, Long currentVersion, Long oldestAvailableVersion) {
        lastSuccessful.set(new Entry(
                mode, localVersion, currentVersion, oldestAvailableVersion, Instant.now()));
    }

    @Override
    public SyncStatus currentStatus() {
        Entry entry = lastSuccessful.get();
        if (entry == null) {
            return new SyncStatus(null, null, null, null, null, true);
        }
        boolean stale = Instant.now().isAfter(entry.syncedAt().plus(staleAfter));
        return new SyncStatus(
                entry.localVersion(),
                entry.currentVersion(),
                entry.oldestAvailableVersion(),
                entry.mode(),
                entry.syncedAt(),
                stale);
    }

    private record Entry(
            SyncMode mode,
            long localVersion,
            Long currentVersion,
            Long oldestAvailableVersion,
            Instant syncedAt
    ) {
    }
}
