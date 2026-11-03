package com.programacion2.catalogandsync;

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
 * Hasta que exista el adaptador REST de la catedra (hito 1.3), `SnapshotSourcePort`
 * se satisface con un doble de nuestra propia interfaz. No se emula a la catedra:
 * no hay respuestas falsas ni datos de catalogo, solo el bean necesario para
 * cablear el contexto. `CatalogWritePort` ya lo implementa `JpaCatalogWriteAdapter`.
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogandsyncApplicationTests {

	@MockitoBean
	private SnapshotSourcePort snapshotSourcePort;

	@Test
	void contextLoads() {
	}

}
