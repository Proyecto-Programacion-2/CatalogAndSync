package com.programacion2.catalogandsync.sync.application.exception;

/**
 * Falla al obtener el snapshot desde la catedra: timeout, error de conexion,
 * respuesta no exitosa o un contrato que no se pudo interpretar.
 * <p>
 * {@code status} y {@code code} quedan disponibles para el manejo de robustez
 * de H1.9 (reintentos y observabilidad), sin acoplar la capa de aplicacion a
 * Spring.
 */
public class SnapshotFetchException extends SyncException {

    private final Integer status;
    private final String code;

    public SnapshotFetchException(String message, Throwable cause, Integer status, String code) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public Integer status() {
        return status;
    }

    public String code() {
        return code;
    }
}
