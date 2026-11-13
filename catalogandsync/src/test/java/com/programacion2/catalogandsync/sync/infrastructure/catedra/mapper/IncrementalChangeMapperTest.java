package com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.IncrementalChangesResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper.IncrementalChangeMapper.IncrementalChangeIds;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test puro del contrato {@code changes:{version}}. No emula Redis: verifica la
 * validacion y la extraccion de IDs; la hidratacion real se comprueba
 * manualmente contra la instancia de la catedra.
 */
class IncrementalChangeMapperTest {

    @Test
    void mapsAllAffectedIds() {
        IncrementalChangesResponse response = response(7L,
                List.of(1L, 2L), List.of(15L), List.of(21L, 22L));

        IncrementalChangeIds ids = IncrementalChangeMapper.toIds(response, 7L);

        assertEquals(List.of(1L, 2L), ids.categoryIds());
        assertEquals(List.of(15L), ids.professionalIds());
        assertEquals(List.of(21L, 22L), ids.scheduleIds());
    }

    @Test
    void acceptsEmptyCollections() {
        IncrementalChangeIds ids = IncrementalChangeMapper.toIds(response(7L, List.of(), List.of(), List.of()), 7L);

        assertTrue(ids.categoryIds().isEmpty());
        assertTrue(ids.professionalIds().isEmpty());
        assertTrue(ids.scheduleIds().isEmpty());
    }

    @Test
    void rejectsNullResponse() {
        assertThrows(CatedraContractException.class, () -> IncrementalChangeMapper.toIds(null, 7L));
    }

    @Test
    void rejectsMissingVersion() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(new IncrementalChangesResponse(null, changes(List.of(), List.of(), List.of())), 7L));
    }

    @Test
    void rejectsVersionDifferentFromTheRequestedOne() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(8L, List.of(), List.of(), List.of()), 7L));
    }

    @Test
    void rejectsMissingChangesObject() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(new IncrementalChangesResponse(7L, null), 7L));
    }

    @Test
    void rejectsNullCollectionsInsteadOfTreatingThemAsEmpty() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, null, List.of(), List.of()), 7L));
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, List.of(), null, List.of()), 7L));
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, List.of(), List.of(), null), 7L));
    }

    @Test
    void rejectsNullAndNonPositiveIds() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, Arrays.asList(1L, null), List.of(), List.of()), 7L));
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, List.of(), List.of(0L), List.of()), 7L));
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(response(7L, List.of(), List.of(), List.of(-3L)), 7L));
    }

    @Test
    void collapsesDuplicateIdsPreservingFirstAppearance() {
        IncrementalChangeIds ids = IncrementalChangeMapper.toIds(
                response(7L, List.of(1L, 2L, 1L), List.of(15L, 15L), List.of(21L)), 7L);

        assertEquals(List.of(1L, 2L), ids.categoryIds());
        assertEquals(List.of(15L), ids.professionalIds());
        assertEquals(List.of(21L), ids.scheduleIds());
    }

    @Test
    void duplicatesDoNotBypassValidation() {
        assertThrows(CatedraContractException.class,
                () -> IncrementalChangeMapper.toIds(
                        response(7L, Arrays.asList(1L, null, 1L), List.of(), List.of()), 7L));
    }

    private static IncrementalChangesResponse response(
            Long version, List<Long> categories, List<Long> professionals, List<Long> schedules) {
        return new IncrementalChangesResponse(version, changes(categories, professionals, schedules));
    }

    private static IncrementalChangesResponse.Changes changes(
            List<Long> categories, List<Long> professionals, List<Long> schedules) {
        return new IncrementalChangesResponse.Changes(categories, professionals, schedules);
    }
}
