package com.programacion2.catalogandsync.sync.infrastructure.redis.adapter;

import com.programacion2.catalogandsync.sync.application.exception.VersionSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.ports.out.RemoteVersionPort;
import com.programacion2.catalogandsync.sync.infrastructure.redis.VersionValueParser;
import com.programacion2.catalogandsync.sync.infrastructure.redis.config.CatedraRedisConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador real de {@link RemoteVersionPort} contra Redis (IR §14.2).
 * <p>
 * Lee las dos claves canonicas por separado. No son atomicas entre si, pero el
 * planner tolera ese desfase: si {@code current-version} avanza despues de la
 * lectura, el proximo ciclo lo alcanza; si {@code oldest-available-version}
 * avanza, el planner fuerza snapshot, que es la salida segura.
 * <p>
 * Los errores de transporte se traducen a
 * {@link VersionSourceUnavailableException} (reintentable) para que la capa de
 * aplicacion no dependa de Spring Data.
 */
@Component
public class RedisVersionSourceAdapter implements RemoteVersionPort {

    static final String CURRENT_VERSION_KEY = "catedra:sync:current-version";
    static final String OLDEST_AVAILABLE_VERSION_KEY = "catedra:sync:oldest-available-version";

    private final StringRedisTemplate redisTemplate;

    public RedisVersionSourceAdapter(
            @Qualifier(CatedraRedisConfig.CATEDRA_REDIS_TEMPLATE) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long currentVersion() {
        return readVersion(CURRENT_VERSION_KEY);
    }

    @Override
    public long oldestAvailableVersion() {
        return readVersion(OLDEST_AVAILABLE_VERSION_KEY);
    }

    private long readVersion(String key) {
        String raw;
        try {
            raw = redisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            throw new VersionSourceUnavailableException("Could not read '" + key + "' from Catedra Redis", ex);
        }
        return VersionValueParser.parse(raw, key);
    }
}
