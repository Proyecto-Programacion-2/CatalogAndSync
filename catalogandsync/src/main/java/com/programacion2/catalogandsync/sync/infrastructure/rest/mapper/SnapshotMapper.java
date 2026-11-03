package com.programacion2.catalogandsync.sync.infrastructure.rest.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.SnapshotMappingException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.SnapshotResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.WeeklyScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Traduce el contrato REST de la catedra al modelo de dominio.
 * <p>
 * Es puro: no hace HTTP ni conoce Spring. Falla rapido ante un contrato
 * incompleto, en particular si falta una coleccion. Convertir {@code null} en
 * lista vacia seria peligroso: la escritura deshabilitaria todo el catalogo
 * por la baja logica en cascada.
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

        return new Snapshot(
                version,
                mapCollection(response.professionalCategories(), "professionalCategories", this::toDomain),
                mapCollection(response.professionals(), "professionals", this::toDomain),
                mapCollection(response.weeklySchedules(), "weeklySchedules", this::toDomain)
        );
    }

    private ProfessionalCategory toDomain(ProfessionalCategoryResponse response) {
        return new ProfessionalCategory(
                required(response.id(), "professionalCategories[].id"),
                required(response.name(), "professionalCategories[].name"),
                response.description(),
                required(response.enabled(), "professionalCategories[].enabled"),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private Professional toDomain(ProfessionalResponse response) {
        return new Professional(
                required(response.id(), "professionals[].id"),
                required(response.categoryId(), "professionals[].categoryId"),
                required(response.firstName(), "professionals[].firstName"),
                required(response.lastName(), "professionals[].lastName"),
                required(response.enabled(), "professionals[].enabled"),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private WeeklySchedule toDomain(WeeklyScheduleResponse response) {
        return new WeeklySchedule(
                required(response.id(), "weeklySchedules[].id"),
                required(response.professionalId(), "weeklySchedules[].professionalId"),
                toDayOfWeek(response.dayOfWeek()),
                required(response.startTime(), "weeklySchedules[].startTime"),
                required(response.endTime(), "weeklySchedules[].endTime"),
                required(response.slotDurationMinutes(), "weeklySchedules[].slotDurationMinutes"),
                required(response.enabled(), "weeklySchedules[].enabled"),
                required(response.createdAt(), "weeklySchedules[].createdAt"),
                response.updatedAt()
        );
    }

    private DayOfWeek toDayOfWeek(String value) {
        String raw = required(value, "weeklySchedules[].dayOfWeek");
        try {
            return DayOfWeek.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new SnapshotMappingException("Unknown dayOfWeek '" + raw + "' in Catedra snapshot", ex);
        }
    }

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
