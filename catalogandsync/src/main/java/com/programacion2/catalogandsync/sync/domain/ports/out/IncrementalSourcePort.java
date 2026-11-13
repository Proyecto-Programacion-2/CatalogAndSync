package com.programacion2.catalogandsync.sync.domain.ports.out;

import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;

import java.util.Optional;

/**
 * Fuente de versiones incrementales de la catedra (IR §14.5).
 */
public interface IncrementalSourcePort {

    /**
     * Lee y hidrata la version pedida.
     *
     * @return el lote con las entidades afectadas, o {@link Optional#empty()} si
     *         el historial de esa version ya no esta disponible. El vacio es una
     *         senal de negocio (hay que reconstruir con snapshot), no un error.
     */
    Optional<IncrementalBatch> read(long version);
}
