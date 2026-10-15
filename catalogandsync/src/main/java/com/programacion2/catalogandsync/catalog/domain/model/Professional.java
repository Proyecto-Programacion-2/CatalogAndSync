package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;

public record Professional(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
