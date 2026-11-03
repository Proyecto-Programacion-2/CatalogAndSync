package com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Fila unica ({@code id} siempre 1) con la version local aplicada. No es
 * {@code @Version} de JPA: es la version del catalogo de la catedra.
 */
@Entity
@Table(name = "catalog_version", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class CatalogVersionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    @Column(name = "local_version", nullable = false)
    private long localVersion;

    @Column(name = "last_sync_mode", nullable = false)
    private String lastSyncMode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
