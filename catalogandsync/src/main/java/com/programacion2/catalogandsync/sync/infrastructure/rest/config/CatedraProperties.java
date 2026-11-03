package com.programacion2.catalogandsync.sync.infrastructure.rest.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracion del cliente REST de la catedra.
 * <p>
 * Se bindea desde el bloque {@code catedra} de application.yaml, que a su vez
 * lee {@code CATEDRA_API_BASE_URL} y {@code CATEDRA_JWT} del .env. El JWT no se
 * loguea ni se expone en ningun DTO.
 */
@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(
        @NotBlank String baseUrl,
        @NotBlank String jwt,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
    /**
     * Evita que el JWT termine en un log si alguien imprime el bean.
     */
    @Override
    public String toString() {
        return "CatedraProperties[baseUrl=" + baseUrl
                + ", jwt=***, connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + "]";
    }
}
