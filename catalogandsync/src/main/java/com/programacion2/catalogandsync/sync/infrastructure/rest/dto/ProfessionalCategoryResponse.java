package com.programacion2.catalogandsync.sync.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfessionalCategoryResponse(
        Long id,
        String name,
        String description,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
