package com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.CatedraDayOfWeek;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.WeeklyScheduleResponse;

import java.util.function.Supplier;

/**
 * Traduce una entidad cruda de la catedra (REST o Redis) al modelo de dominio.
 * <p>
 * Es puro y estatico a proposito: el snapshot REST y la hidratacion por
 * {@code HGET} comparten exactamente las mismas reglas, sin depender de Spring.
 * <p>
 * Reparto de responsabilidades:
 * <ul>
 *   <li>este mapper valida el contrato de cable (presencia de campos y
 *       vocabulario de {@code dayOfWeek});</li>
 *   <li>los records de dominio validan sus invariantes (IDs positivos, ventana
 *       horaria coherente, duracion positiva);</li>
 *   <li>{@link #build(String, Supplier)} traduce esa falla de dominio a
 *       {@link CatedraContractException} con el contexto del canal, para que
 *       cada adaptador elija su propia excepcion.</li>
 * </ul>
 * Sin esa traduccion, un invariante roto en el dominio escaparia como
 * {@link IllegalArgumentException} y el incremental perderia el fallback a
 * snapshot.
 */
public final class CatedraEntityMapper {

    private CatedraEntityMapper() {
    }

    public static ProfessionalCategory toDomain(ProfessionalCategoryResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return build(context, () -> new ProfessionalCategory(
                required(response.id(), context, "id"),
                required(response.name(), context, "name"),
                response.description(),
                required(response.enabled(), context, "enabled"),
                response.createdAt(),
                response.updatedAt()
        ));
    }

    public static Professional toDomain(ProfessionalResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return build(context, () -> new Professional(
                required(response.id(), context, "id"),
                required(response.categoryId(), context, "categoryId"),
                required(response.firstName(), context, "firstName"),
                required(response.lastName(), context, "lastName"),
                required(response.enabled(), context, "enabled"),
                response.createdAt(),
                response.updatedAt()
        ));
    }

    public static WeeklySchedule toDomain(WeeklyScheduleResponse response, String context) {
        if (response == null) {
            throw new CatedraContractException(context + " is null");
        }
        return build(context, () -> new WeeklySchedule(
                required(response.id(), context, "id"),
                required(response.professionalId(), context, "professionalId"),
                toDayOfWeek(response.dayOfWeek(), context),
                required(response.startTime(), context, "startTime"),
                required(response.endTime(), context, "endTime"),
                required(response.slotDurationMinutes(), context, "slotDurationMinutes"),
                required(response.enabled(), context, "enabled"),
                required(response.createdAt(), context, "createdAt"),
                response.updatedAt()
        ));
    }

    /**
     * Traduce el vocabulario del cable al del dominio. El switch no tiene
     * {@code default}: si la catedra agrega un dia, esto deja de compilar.
     */
    private static DayOfWeek toDayOfWeek(CatedraDayOfWeek value, String context) {
        CatedraDayOfWeek day = required(value, context, "dayOfWeek");
        return switch (day) {
            case MONDAY -> DayOfWeek.MONDAY;
            case TUESDAY -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY -> DayOfWeek.THURSDAY;
            case FRIDAY -> DayOfWeek.FRIDAY;
            case SATURDAY -> DayOfWeek.SATURDAY;
            case SUNDAY -> DayOfWeek.SUNDAY;
        };
    }

    private static <T> T build(String context, Supplier<T> factory) {
        try {
            return factory.get();
        } catch (IllegalArgumentException ex) {
            throw new CatedraContractException(context + " is invalid: " + ex.getMessage(), ex);
        }
    }

    private static <T> T required(T value, String context, String field) {
        if (value == null) {
            throw new CatedraContractException(context + " field '" + field + "' is missing");
        }
        return value;
    }
}
