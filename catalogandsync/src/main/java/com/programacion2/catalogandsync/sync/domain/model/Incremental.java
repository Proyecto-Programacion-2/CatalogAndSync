package com.programacion2.catalogandsync.sync.domain.model;

import java.util.List;

public record Incremental(
        long version,
        List<Long> categoryIds,
        List<Long> professionalIds,
        List<Long> scheduleIds
) {
}
