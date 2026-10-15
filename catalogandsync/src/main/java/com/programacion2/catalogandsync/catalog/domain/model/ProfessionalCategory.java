package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;

public record ProfessionalCategory(
        long id,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
