package com.programacion2.catalogandsync.sync.domain.model;

/**
 * Resultado de ejecutar el camino incremental.
 * <p>
 * El incremental puede descubrir a mitad de camino que ya no podra completar
 * la secuencia: la ventana de {@code changes:{v}} se reciclo
 * ({@code Optional.empty()}) o un delta no cumple contrato. En esos casos el
 * executor no inventa estado ni escribe una version parcial como si fuera
 * completa: devuelve {@link NeedsSnapshot} y el orquestador decide reconstruir
 * con un snapshot.
 * <p>
 * Una falla de transporte no llega aca: se propaga para que el ciclo falle y se
 * reintente en la proxima ejecucion periodica.
 */
public sealed interface IncrementalResult {

    /**
     * La secuencia completa se aplico version por version.
     */
    record Applied(SyncOutcome outcome) implements IncrementalResult {
    }

    /**
     * El incremental no puede continuar y hay que reconstruir con snapshot.
     *
     * @param reason codigo y detalle para el log y el estado del ciclo
     */
    record NeedsSnapshot(String reason) implements IncrementalResult {
    }
}
