package com.programacion2.catalogandsync.sync.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Ajustes propios del slice {@code sync}: timeouts del cliente Redis de la
 * catedra (decididos 2026-11-02), politica de reintentos, cadencia del ciclo
 * periodico y umbral de estado obsoleto.
 */
@Validated
@ConfigurationProperties(prefix = "sync")
public record SyncProperties(
        @Valid @NotNull Redis redis,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Scheduler scheduler,
        @Valid @NotNull Status status
) {

    public record Redis(
            @NotNull Duration connectTimeout,
            @NotNull Duration commandTimeout
    ) {
    }

    /**
     * Reintentos fijos (decididos 2026-11-02): {@code maxAttempts} cuenta el
     * intento original, no solo los reintentos. Con 3 y 2 s hay como maximo 2
     * esperas entre intentos.
     */
    public record Retry(
            @Min(1) int maxAttempts,
            @NotNull Duration delay
    ) {
    }

    /**
     * El scheduler usa milisegundos para no depender del parseo de duraciones
     * en {@code @Scheduled}. 900000 ms = 15 minutos.
     */
    public record Scheduler(
            boolean enabled,
            long fixedDelayMs,
            long initialDelayMs
    ) {
    }

    /**
     * Un ciclo se considera obsoleto si el ultimo exitoso es mas viejo que este
     * umbral. Por defecto 2x la cadencia (30 min con el ciclo de 15 min).
     */
    public record Status(
            @Positive long staleAfterMs
    ) {
    }
}
