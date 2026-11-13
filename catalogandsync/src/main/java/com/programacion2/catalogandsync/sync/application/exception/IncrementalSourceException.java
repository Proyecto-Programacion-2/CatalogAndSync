package com.programacion2.catalogandsync.sync.application.exception;

/**
 * La fuente incremental de la catedra respondio pero no se puede aplicar: el
 * contrato de {@code changes:{version}} esta roto, falta una entidad referida
 * por el delta o el JSON no se puede interpretar.
 * <p>
 * No es reintentable: esperar no cambia un dato mal publicado. El executor
 * incremental la trata como senal para reconstruir con snapshot.
 */
public class IncrementalSourceException extends SyncException {

    public IncrementalSourceException(String message) {
        super(message);
    }

    public IncrementalSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
