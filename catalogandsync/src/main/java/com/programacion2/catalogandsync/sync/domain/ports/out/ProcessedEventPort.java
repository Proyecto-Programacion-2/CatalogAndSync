package com.programacion2.catalogandsync.sync.domain.ports.out;

/**
 * Registro de eventos Kafka ya procesados (R1 de SYNC_RESPONSABILIDADES.md).
 * <p>
 * {@code eventId} es la clave de idempotencia funcional del contrato (IR
 * seccion 15.1). La tabla es una <b>optimizacion</b>, no el mecanismo de
 * correccion: el ledger de versiones ya hace idempotente cualquier ciclo, asi
 * que un evento registrado de mas solo evita repetir trabajo; uno registrado de
 * menos provoca un ciclo redundante que termina en {@code NOTHING}.
 */
public interface ProcessedEventPort {

    /**
     * @return {@code true} si el evento ya se registro en un ciclo exitoso
     */
    boolean exists(String eventId);

    /**
     * Registra el evento y la version local que quedo aplicada. Es idempotente:
     * si el {@code eventId} ya existe, conserva el primer registro.
     */
    void record(String eventId, long appliedVersion);
}
