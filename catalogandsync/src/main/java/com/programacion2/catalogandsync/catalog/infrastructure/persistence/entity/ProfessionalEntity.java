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
 * Copia local de un profesional. La relacion con la categoria se modela como
 * columna escalar: los IDs son de la catedra y la escritura se hace en orden
 * dentro de una unica transaccion.
 */
@Entity
@Table(name = "professional", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class ProfessionalEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private long categoryId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
