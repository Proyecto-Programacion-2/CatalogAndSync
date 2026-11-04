package com.programacion2.catalogandsync.sync.application.exception;

/**
 * La fuente remota de versiones (Redis de la catedra) no esta disponible o
 * devolvio un valor que no cumple el contrato.
 * <p>
 * El ciclo termina con error y se reintenta en la proxima ejecucion periodica.
 * No se degrada a una version inventada: decidir con datos falsos podria
 * disparar un snapshot innecesario o, peor, dejar el catalogo atrasado.
 */
public class VersionSourceException extends SyncException {

    public VersionSourceException(String message) {
        super(message);
    }

    public VersionSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
