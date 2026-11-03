package com.programacion2.catalogandsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto levante con PostgreSQL local arriba (docker compose up -d).
 * Usa el perfil `test` para apuntar a `catalogandsync_test` y no tocar los datos
 * de desarrollo.
 * <p>
 * El adaptador REST de la catedra ya es un bean real (hito 1.3). El perfil `test`
 * le da una URL y un JWT placeholder para que se construya; este test no dispara
 * ninguna request ni emula a la catedra.
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogandsyncApplicationTests {

	@Test
	void contextLoads() {
	}

}
