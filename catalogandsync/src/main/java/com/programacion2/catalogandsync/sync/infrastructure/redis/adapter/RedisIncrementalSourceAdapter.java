package com.programacion2.catalogandsync.sync.infrastructure.redis.adapter;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceException;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;
import com.programacion2.catalogandsync.sync.domain.ports.out.IncrementalSourcePort;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.IncrementalChangesResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.WeeklyScheduleResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper.CatedraEntityMapper;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper.IncrementalChangeMapper;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper.IncrementalChangeMapper.IncrementalChangeIds;
import com.programacion2.catalogandsync.sync.infrastructure.redis.config.CatedraRedisConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Adaptador real de {@link IncrementalSourcePort} contra Redis (IR §14.4 y
 * §14.5).
 * <p>
 * Flujo por version:
 * <ol>
 *   <li>{@code GET catedra:sync:changes:{version}};</li>
 *   <li>si la clave no existe, devuelve {@link Optional#empty()} (historial
 *       perdido, hay que reconstruir con snapshot);</li>
 *   <li>valida el sobre del delta con {@link IncrementalChangeMapper};</li>
 *   <li>hidrata cada ID con {@code HGET} en el hash de su tipo y mapea la
 *       entidad con {@link CatedraEntityMapper}.</li>
 * </ol>
 * Los errores de transporte se traducen a
 * {@link IncrementalSourceUnavailableException} (reintentable); un contrato
 * roto o una entidad ausente a {@link IncrementalSourceException} (no
 * reintentable). No se inventan entidades ni versiones.
 */
@Component
public class RedisIncrementalSourceAdapter implements IncrementalSourcePort {

    static final String CHANGES_KEY_PREFIX = "catedra:sync:changes:";
    static final String PROFESSIONAL_CATEGORIES_HASH = "catedra:sync:professional-categories";
    static final String PROFESSIONALS_HASH = "catedra:sync:professionals";
    static final String WEEKLY_SCHEDULES_HASH = "catedra:sync:weekly-schedules";

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public RedisIncrementalSourceAdapter(
            @Qualifier(CatedraRedisConfig.CATEDRA_REDIS_TEMPLATE) StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<IncrementalBatch> read(long version) {
        String key = CHANGES_KEY_PREFIX + version;
        String raw = readString(key);
        if (raw == null) {
            return Optional.empty();
        }

        IncrementalChangeIds ids = readChanges(raw, key, version);

        return Optional.of(new IncrementalBatch(
                version,
                hydrate(ids.categoryIds(), PROFESSIONAL_CATEGORIES_HASH,
                        ProfessionalCategoryResponse.class, "professional-categories",
                        (dto, context) -> CatedraEntityMapper.toDomain(dto, context)),
                hydrate(ids.professionalIds(), PROFESSIONALS_HASH,
                        ProfessionalResponse.class, "professionals",
                        (dto, context) -> CatedraEntityMapper.toDomain(dto, context)),
                hydrate(ids.scheduleIds(), WEEKLY_SCHEDULES_HASH,
                        WeeklyScheduleResponse.class, "weekly-schedules",
                        (dto, context) -> CatedraEntityMapper.toDomain(dto, context))
        ));
    }

    private String readString(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            throw new IncrementalSourceUnavailableException(
                    "Could not read '" + key + "' from Catedra Redis", ex);
        }
    }

    private IncrementalChangeIds readChanges(String raw, String key, long version) {
        IncrementalChangesResponse response;
        try {
            response = jsonMapper.readValue(raw, IncrementalChangesResponse.class);
        } catch (JacksonException ex) {
            throw new IncrementalSourceException("Catedra Redis key '" + key + "' is not valid JSON", ex);
        }

        try {
            return IncrementalChangeMapper.toIds(response, version);
        } catch (CatedraContractException ex) {
            throw new IncrementalSourceException(ex.getMessage(), ex);
        }
    }

    private <D, R> List<R> hydrate(List<Long> ids, String hashKey, Class<D> dtoType,
                                   String entityName, BiFunction<D, String, R> mapper) {
        if (ids.isEmpty()) {
            return List.of();
        }

        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return ids.stream()
                .map(id -> {
                    String context = "Catedra Redis " + entityName + "[" + id + "]";
                    String json;
                    try {
                        json = hashOperations.get(hashKey, String.valueOf(id));
                    } catch (DataAccessException ex) {
                        throw new IncrementalSourceUnavailableException(
                                "Could not read " + context + " from Catedra Redis", ex);
                    }
                    if (json == null) {
                        throw new IncrementalSourceException(
                                context + " is missing but the incremental delta references it");
                    }
                    try {
                        D dto = jsonMapper.readValue(json, dtoType);
                        return mapper.apply(dto, context);
                    } catch (JacksonException ex) {
                        throw new IncrementalSourceException(context + " is not valid JSON", ex);
                    } catch (CatedraContractException ex) {
                        throw new IncrementalSourceException(ex.getMessage(), ex);
                    }
                })
                .toList();
    }
}
