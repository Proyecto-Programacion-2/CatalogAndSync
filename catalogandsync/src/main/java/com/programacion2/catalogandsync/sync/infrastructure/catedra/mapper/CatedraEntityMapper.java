package com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.WeeklyScheduleResponse;

/**
 * Traduce una entidad cruda de la catedra (REST o Redis) al modelo de dominio.
 * <p>
 * Es puro y estatico a proposito: el snapshot REST y la hidratacion por
 * {@code HGET} comparten exactamente las mismas reglas, sin depender de Spring.
 * Falla con {@link CatedraContractException} ante un contrato incompleto; cada
 * adaptador decide como traducir esa falla a su propio canal.
 */
public final class CatedraEntityMapper {

    private CatedraEntityMapper() {
    }

    public static ProfessionalCategory toDomain(ProfessionalCategoryResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return new ProfessionalCategory(
                required(response.id(), context, "id"),
                required(response.name(), context, "name"),
                response.description(),
                required(response.enabled(), context, "enabled"),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static Professional toDomain(ProfessionalResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return new Professional(
                required(response.id(), context, "id"),
                required(response.categoryId(), context, "categoryId"),
                required(response.firstName(), context, "firstName"),
                required(response.lastName(), context, "lastName"),
                required(response.enabled(), context, "enabled"),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static WeeklySchedule toDomain(WeeklyScheduleResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return new WeeklySchedule(
                required(response.id(), context, "id"),
                required(response.professionalId(), context, "professionalId"),
                toDayOfWeek(response.dayOfWeek(), context),
                required(response.startTime(), context, "startTime"),
                required(response.endTime(), context, "endTime"),
                required(response.slotDurationMinutes(), context, "slotDurationMinutes"),
                required(response.enabled(), context, "enabled"),
                required(response.createdAt(), context, "createdAt"),
                response.updatedAt()
        );
    }

    private static DayOfWeek toDayOfWeek(String value, String context) {
        String raw = required(value, context, "dayOfWeek");
        try {
            return DayOfWeek.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new CatedraContractException(context + " has an unknown dayOfWeek '" + raw + "'", ex);
        }
    }

    private static <T> T required(T value, String context, String field) {
        if (value == null) {
            throw new CatedraContractException(context + " field '" + field + "' is missing");
        }
        return value;
    }
}
