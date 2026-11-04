package com.programacion2.catalogandsync.sync.application.exception;

/**
 * Ya hay un ciclo de sincronizacion en curso y el disparador actual no debe
 * esperar ni solaparse (lock {@code tryLock} de H1.9).
 * <p>
 * No es una falla del ciclo: el disparador que la recibe simplemente lo saltea.
 */
public class SyncAlreadyRunningException extends RuntimeException {

    public SyncAlreadyRunningException(String message) {
        super(message);
    }
}
