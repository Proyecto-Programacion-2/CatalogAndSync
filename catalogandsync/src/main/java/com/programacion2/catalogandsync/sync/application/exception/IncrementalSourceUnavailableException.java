package com.programacion2.catalogandsync.sync.application.exception;

/**
 * Falla de transporte al leer el incremental desde Redis (conexion, timeout,
 * AUTH). Es reintentable: la politica fija de H1.9 vuelve a intentar.
 * <p>
 * Se distingue de {@link IncrementalSourceException} a secas (contrato roto,
 * historial incompleto): eso no se arregla esperando.
 */
public class IncrementalSourceUnavailableException extends IncrementalSourceException {

    public IncrementalSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
