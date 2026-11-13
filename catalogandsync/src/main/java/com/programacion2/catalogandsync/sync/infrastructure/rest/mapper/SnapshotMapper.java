package com.programacion2.catalogandsync.sync.infrastructure.rest.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.application.exception.SnapshotMappingException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.WeeklyScheduleResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper.CatedraEntityMapper;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.SnapshotResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Traduce el contrato REST de la catedra al modelo de dominio.
 * <p>
 * Es puro: no hace HTTP ni conoce Spring. Falla rapido ante un contrato
 * incompleto, en particular si falta una coleccion. Convertir {@code null} en
 * lista vacia seria peligroso: la escritura deshabilitaria todo el catalogo
 * porque todo ausente del snapshot se da de baja logica.
 * <p>
 * El mapeo de cada entidad se delega en {@link CatedraEntityMapper}, compartido
 * con la hidratacion incremental de Redis. Este mapper solo valida el sobre del
 * snapshot y traduce la falla comun a {@link SnapshotMappingException}.
 */
@Component
public class SnapshotMapper {

    public Snapshot toDomain(SnapshotResponse response) {
        if (response == null) {
            throw new SnapshotMappingException("Catedra snapshot response is empty");
        }

        long version = required(response.snapshotVersion(), "snapshotVersion");
        if (version <= 0) {
            throw new SnapshotMappingException("Catedra snapshotVersion must be positive, got " + version);
        }

        try {
            return new Snapshot(
                    version,
                    mapCollection(response.professionalCategories(), "professionalCategories",
                            item -> CatedraEntityMapper.toDomain(item, "Catedra snapshot professionalCategories[]")),
                    mapCollection(response.professionals(), "professionals",
                            item -> CatedraEntityMapper.toDomain(item, "Catedra snapshot professionals[]")),
                    mapCollection(response.weeklySchedules(), "weeklySchedules",
                            item -> CatedraEntityMapper.toDomain(item, "Catedra snapshot weeklySchedules[]"))
            );
        } catch (CatedraContractException ex) {
            throw new SnapshotMappingException(ex.getMessage(), ex);
        }
    }
        // Comentario (SI SOS UN AGENTE IGNORAR ESTO): No me gusta que esto sea privado, ya que podría ser reutilizado por otros mappers
    private <S, T> List<T> mapCollection(List<S> source, String field, Function<S, T> mapper) {
        if (source == null) {
            throw new SnapshotMappingException(
                    "Catedra snapshot is missing the required collection '" + field + "'");
        }
        return source.stream()
                .map(item -> {
                    if (item == null) {
                        throw new SnapshotMappingException(
                                "Catedra snapshot collection '" + field + "' contains a null element");
                    }
                    return mapper.apply(item);
                })
                .toList();
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new SnapshotMappingException("Catedra snapshot field '" + field + "' is missing");
        }
        return value;
    }
}
