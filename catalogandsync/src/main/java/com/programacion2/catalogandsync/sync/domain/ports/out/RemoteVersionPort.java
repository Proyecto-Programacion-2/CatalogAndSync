package com.programacion2.catalogandsync.sync.domain.ports.out;

/**
 * Versiones publicadas por la catedra en Redis (IR §14.2).
 * <p>
 * Es una fuente remota de solo lectura. Si no responde o devuelve un valor que
 * no cumple el contrato, el ciclo falla: nunca se inventa una version.
 */
public interface RemoteVersionPort {

    long currentVersion();

    long oldestAvailableVersion();
}
