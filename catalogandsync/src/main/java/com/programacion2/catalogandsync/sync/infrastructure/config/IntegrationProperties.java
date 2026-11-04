package com.programacion2.catalogandsync.sync.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Datos que la catedra entrega en el bloque {@code integration} (IR §5.1).
 * <p>
 * Por ahora se bindea solo el subconjunto Redis; Kafka se agrega cuando exista
 * su consumidor (hito 1.7). El password nunca se expone en {@link #toString()}.
 */
@Validated
@ConfigurationProperties(prefix = "integration")
public record IntegrationProperties(
        @NotBlank String redisHost,
        @Min(1) @Max(65535) int redisPort,
        String redisUsername,
        @NotBlank String redisPassword
) {

    @Override
    public String toString() {
        return "IntegrationProperties[redisHost=" + redisHost
                + ", redisPort=" + redisPort
                + ", redisUsername=" + redisUsername
                + ", redisPassword=***]";
    }
}
