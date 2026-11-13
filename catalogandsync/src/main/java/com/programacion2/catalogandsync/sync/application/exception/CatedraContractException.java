package com.programacion2.catalogandsync.sync.application.exception;

/**
 * Los datos de la catedra llegaron pero no cumplen el contrato compartido por
 * las fuentes REST (snapshot) y Redis (entidades e incremental): falta un campo
 * obligatorio, un ID no es positivo o una coleccion requerida vino {@code null}.
 * <p>
 * Es la excepcion de bajo nivel del mapeo comun. Cada adaptador la traduce a la
 * excepcion de su propio canal ({@link SnapshotMappingException} en REST,
 * {@code IncrementalSourceException} en Redis) para que la capa de aplicacion
 * distinga el origen.
 */
public class CatedraContractException extends SyncException {

    public CatedraContractException(String message) {
        super(message);
    }

    public CatedraContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
