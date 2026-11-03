package com.programacion2.catalogandsync.sync.application.exception;

/**
 * El snapshot llego pero no cumple el contrato: falta una coleccion, falta un
 * campo obligatorio o un {@code dayOfWeek} es desconocido.
 * <p>
 * Es distinto de {@link SnapshotFetchException}: aca la catedra respondio, pero
 * aplicar esos datos podria corromper el catalogo (por ejemplo, una coleccion
 * nula interpretada como vacia deshabilitaria todo).
 */
public class SnapshotMappingException extends SyncException {

    public SnapshotMappingException(String message) {
        super(message);
    }

    public SnapshotMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
