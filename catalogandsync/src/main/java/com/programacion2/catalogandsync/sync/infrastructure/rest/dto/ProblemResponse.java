package com.programacion2.catalogandsync.sync.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Solo lo que necesitamos de un {@code application/problem+json} (IR §13).
 * El campo {@code detail} es texto humano: nunca se usa para decidir.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemResponse(
        String code
) {
}
