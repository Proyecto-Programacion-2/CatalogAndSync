package com.programacion2.catalogandsync;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifica que el contexto levante con PostgreSQL local arriba (docker compose up -d).
 * Usa el perfil `test` para apuntar a `catalogandsync_test` y no tocar los datos
 * de desarrollo.
 * <p>
 * Hasta que existan los adaptadores reales (REST de la catedra y JPA de escritura,
 * hitos 1.3 y 1.4) los puertos de salida se satisfacen con dobles de nuestras
 * propias interfaces. No se emula a la catedra: no hay respuestas falsas ni datos
 * de catalogo, solo el bean necesario para cablear el contexto.
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogandsyncApplicationTests {

	@MockitoBean
	private SnapshotSourcePort snapshotSourcePort;

	@MockitoBean
	private CatalogWritePort catalogWritePort;

	@Test
	void contextLoads() {
	}

}
