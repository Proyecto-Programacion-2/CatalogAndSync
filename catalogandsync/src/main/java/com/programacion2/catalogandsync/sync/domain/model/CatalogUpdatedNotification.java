package com.programacion2.catalogandsync.sync.domain.model;

import java.time.Instant;

/**
 * Notificacion {@code CatalogUpdated} ya validada (IR seccion 15.3).
 * <p>
 * Es solo un disparador: {@code newVersion} no decide el rango a aplicar. El
 * planner lee las versiones reales de Redis y el ledger local, asi que un evento
 * atrasado termina en {@code NOTHING} y uno perdido lo recupera el ciclo
 * periodico.
 * <p>
 * Los invariantes viven aca, no en el mapper: valen con independencia del
 * transporte. El mapper traduce la falla a {@code CatedraContractException} con
 * el contexto del canal.
 */
public record CatalogUpdatedNotification(
        String eventId,
        long newVersion,
        Instant occurredAt,
        String source
) {

    public CatalogUpdatedNotification {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("CatalogUpdated eventId is required");
        }
        if (newVersion <= 0) {
            throw new IllegalArgumentException("CatalogUpdated newVersion must be positive, got " + newVersion);
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("CatalogUpdated occurredAt is required");
        }
    }
}
