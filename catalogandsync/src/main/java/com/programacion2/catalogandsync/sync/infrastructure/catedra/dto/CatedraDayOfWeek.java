package com.programacion2.catalogandsync.sync.infrastructure.catedra.dto;

/**
 * Vocabulario de {@code dayOfWeek} tal como lo publica la catedra (IR §14.4).
 * <p>
 * Es deliberadamente distinto de {@code catalog.domain.model.DayOfWeek}: el
 * cable tiene su vocabulario y el dominio el suyo, y
 * {@code CatedraEntityMapper} es el unico punto de traduccion. Si la catedra
 * agrega un dia, el switch de ese mapper deja de compilar y obliga a revisar la
 * traduccion en vez de aceptar un valor que el dominio no modela.
 * <p>
 * Jackson rechaza en la deserializacion cualquier nombre fuera de esta lista.
 */
public enum CatedraDayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
