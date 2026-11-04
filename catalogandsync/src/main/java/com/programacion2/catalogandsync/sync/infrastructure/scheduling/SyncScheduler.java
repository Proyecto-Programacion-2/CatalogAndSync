package com.programacion2.catalogandsync.sync.infrastructure.scheduling;

import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.service.SyncService;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara un ciclo de sincronizacion cada {@code sync.scheduler.fixed-delay-ms}
 * (15 minutos por defecto) y uno inmediato al arrancar.
 * <p>
 * El scheduler de Spring corre en un solo hilo, pero el lock de
 * {@link SyncService} protege tambien disparadores manuales futuros: si ya hay
 * un ciclo en curso, este se saltea con un {@code WARN}. Una falla no mata el
 * ciclo: se loguea y se reintenta en la proxima ejecucion; la reconciliacion es
 * el mecanismo de recuperacion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncService syncService;

    @Scheduled(
            fixedDelayString = "${sync.scheduler.fixed-delay-ms}",
            initialDelayString = "${sync.scheduler.initial-delay-ms}")
    public void runCycle() {
        long startedAt = System.currentTimeMillis();
        try {
            SyncOutcome outcome = syncService.sync();
            log.info("Sync cycle finished: mode={}, version={}, nothingChanged={}, duration={}ms",
                    outcome.mode(), outcome.appliedVersion(), outcome.nothingChanged(),
                    System.currentTimeMillis() - startedAt);
        } catch (SyncAlreadyRunningException ex) {
            log.warn("Sync cycle skipped after {}ms: {}",
                    System.currentTimeMillis() - startedAt, ex.getMessage());
        } catch (SyncException ex) {
            log.warn("Sync cycle failed after {}ms: {}",
                    System.currentTimeMillis() - startedAt, ex.getMessage());
            log.debug("Sync cycle failure detail", ex);
        }
    }
}
