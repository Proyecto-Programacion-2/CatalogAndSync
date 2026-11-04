package com.programacion2.catalogandsync.sync.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el ciclo periodico solo con {@code sync.scheduler.enabled=true}.
 * <p>
 * En el perfil {@code test} queda apagado: ningun test dispara una request real
 * a la catedra ni abre una conexion a su Redis.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "sync.scheduler", name = "enabled", havingValue = "true")
public class SyncSchedulingConfig {
}
