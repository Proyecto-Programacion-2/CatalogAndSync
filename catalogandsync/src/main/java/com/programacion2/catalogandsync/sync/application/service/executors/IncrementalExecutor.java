package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceException;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalResult;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.IncrementalSourcePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aplica una secuencia incremental desde {@code fromVersion} hasta
 * {@code toVersion}, en orden y con una transaccion por version.
 * <p>
 * La lectura ocurre fuera de la transaccion de escritura: el adaptador Redis
 * hace I/O (un {@code GET} y varios {@code HGET}) y no debe retener una
 * conexion a PostgreSQL mientras espera a la catedra. Cada
 * {@link CatalogWritePort#applyChanges} es una invocacion proxied distinta, asi
 * que cada version se confirma (o se revierte) por separado. Si el proceso se
 * corta en la version N, las versiones menores ya aplicadas son consistentes y
 * el proximo ciclo retoma desde la version local.
 * <p>
 * Que hace ante cada falla:
 * <ul>
 *   <li>historial perdido ({@code Optional.empty()}) -> {@link IncrementalResult.NeedsSnapshot};</li>
 *   <li>contrato roto ({@link IncrementalSourceException}) -> {@link IncrementalResult.NeedsSnapshot};</li>
 *   <li>transporte agotado ({@link IncrementalSourceUnavailableException}) -> se propaga,
 *       porque esperar y reintentar en el proximo ciclo puede resolverlo sin pagar un snapshot;</li>
 *   <li>falla de persistencia -> se propaga; lo ya confirmado queda aplicado.</li>
 * </ul>
 * El executor no decide tomar el snapshot: solo informa que hace falta. La
 * orquestacion vive en el caso de uso, que es quien conoce ambos caminos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalExecutor {

    private final IncrementalSourcePort incrementalSourcePort;
    private final CatalogWritePort catalogWritePort;

    public IncrementalResult execute(SyncDecision.ApplyIncremental decision) {
        long fromVersion = decision.fromVersion();
        long toVersion = decision.toVersion();
        long appliedVersions = 0;

        for (long version = fromVersion; version <= toVersion; version++) {
            Optional<IncrementalBatch> batch;
            try {
                batch = incrementalSourcePort.read(version);
            } catch (IncrementalSourceUnavailableException ex) {
                log.warn("INCREMENTAL_TRANSPORT_FAILED at version {} after retries " +
                        "(fromVersion={}, toVersion={}, appliedVersions={}). The cycle will retry later.",
                        version, fromVersion, toVersion, appliedVersions);
                throw ex;
            } catch (IncrementalSourceException ex) {
                log.warn("INCREMENTAL_CONTRACT_BROKEN at version {}: {}. Falling back to snapshot " +
                                "(fromVersion={}, toVersion={}, appliedVersions={})",
                        version, ex.getMessage(), fromVersion, toVersion, appliedVersions);
                return new IncrementalResult.NeedsSnapshot(
                        "INCREMENTAL_CONTRACT_BROKEN at version " + version + ": " + ex.getMessage());
            }

            if (batch.isEmpty()) {
                log.warn("INCREMENTAL_HISTORY_MISSING at version {}. Falling back to snapshot " +
                                "(fromVersion={}, toVersion={}, appliedVersions={})",
                        version, fromVersion, toVersion, appliedVersions);
                return new IncrementalResult.NeedsSnapshot(
                        "INCREMENTAL_HISTORY_MISSING at version " + version);
            }

            apply(batch.get());
            appliedVersions++;
        }

        log.info("Incremental sync applied {} versions ({} -> {})", appliedVersions, fromVersion, toVersion);
        return new IncrementalResult.Applied(new SyncOutcome(SyncMode.INCREMENTAL, toVersion, false));
    }

    private void apply(IncrementalBatch batch) {
        catalogWritePort.applyChanges(
                batch.version(),
                SyncMode.INCREMENTAL,
                batch.categories(),
                batch.professionals(),
                batch.schedules());

        log.info("Applied incremental version {} (categories={}, professionals={}, schedules={})",
                batch.version(),
                batch.categories().size(),
                batch.professionals().size(),
                batch.schedules().size());
    }
}
