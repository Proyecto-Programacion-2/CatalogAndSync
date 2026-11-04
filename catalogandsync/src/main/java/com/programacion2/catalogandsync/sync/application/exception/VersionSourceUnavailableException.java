package com.programacion2.catalogandsync.sync.application.exception;

/**
 * Falla de transporte al leer las versiones de la catedra (conexion, timeout,
 * AUTH). Es reintentable: la politica fija de H1.9 vuelve a intentar.
 * <p>
 * Se distingue de {@link VersionSourceException} a secas (valor ausente, vacio,
 * no numerico o negativo): eso es un problema de datos y reintentar no lo
 * arregla.
 */
public class VersionSourceUnavailableException extends VersionSourceException {

    public VersionSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
