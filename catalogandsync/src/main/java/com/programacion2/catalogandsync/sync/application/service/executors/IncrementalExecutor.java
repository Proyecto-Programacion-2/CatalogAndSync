package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Camino incremental (stub hasta H1.6).
 * <p>
 * El planner puede elegir {@link SyncDecision.ApplyIncremental} en cuanto la
 * version local esta dentro de la ventana y atrasada respecto de la catedra.
 * Devolver {@code null} haria que el ciclo terminara en un
 * {@link NullPointerException} al leer el {@link SyncOutcome}; fallar con
 * {@link SyncException} y el codigo {@code INCREMENTAL_UNAVAILABLE} deja el
 * motivo en el log y el scheduler reintenta en el proximo ciclo.
 */
@Slf4j
@Component
public class IncrementalExecutor {

    public SyncOutcome execute(SyncDecision.ApplyIncremental decision) {
        log.warn("INCREMENTAL_UNAVAILABLE: incremental sync is not implemented yet (H1.6): fromVersion={}, toVersion={}",
                decision.fromVersion(), decision.toVersion());
        throw new SyncException("INCREMENTAL_UNAVAILABLE: incremental sync is not implemented yet (H1.6): fromVersion="
                + decision.fromVersion() + ", toVersion=" + decision.toVersion());
    }
}
