# CatalogAndSync — Servicio de catalogo y sincronizacion

Backend Java con Spring Boot (JHipster opcional). Arquitectura hexagonal, principios SOLID. Repositorio: `github.com/Proyecto-Programacion-2/CatalogAndSync`.

## Rol
- Unica fuente local vigente del catalogo: categorias, profesionales y horarios semanales.
- Sincronizacion completa (snapshot REST) e incremental (Kafka + Redis) contra el servicio de catedra.
- Conserva la version local aplicada; ante discontinuidad o version fuera de la ventana, reconstruye con snapshot.
- Busquedas y filtros (categoria, nombre, estado habilitado y disponibilidad) resueltos SOLO sobre la copia local; nunca se consulta al servicio central por busqueda.

## Documentacion fuente de verdad (copiadas en este repo)
- `PROJECT_STATEMENT-v1.md` — responsabilidades del servicio (secciones 4.1, 5 y 6).
- `INTEGRATION_REFERENCE-v2.md` — contratos reales REST/Redis/Kafka (secciones 7, 14, 15.2-15.3 y 16).

Si este repositorio y los otros dos desincronizan estos documentos, la referencia de la raiz manda: `/home/franco/Facultad/Programacion-2/PROJECT_STATEMENT-v1.md` y `INTEGRATION_REFERENCE-v2.md`.

Cada repositorio replica tambien `ARQUITECTURA.md`. Al modificar cualquiera de estos documentos, propagar el cambio a los 3 repositorios y a la raiz.

## Docs/
- `Docs/CU_2.md` — caso de uso 2 (esquema BD local + filtros). Fuente de verdad del esquema local, subordinada a la catedra (PS §6).
- `Docs/EJEMPLOARQ.md` — ejemplo ilustrativo de arquitectura hexagonal (contextos `catalog`/`sync`). NO es fuente de verdad de decisiones; las decisiones viven en `ARQUITECTURA.md`.

## Pautas
- Agente cooperador, no generador de codigo: escribir codigo solo cuando se pida; consultar antes de modificar archivos.
- CRITICO: cualquier decision de arquitectura interna (motor de BD, estrategia transaccional de sincronizacion, idempotencia) se consulta al usuario. NUNCA asumir.
- Persistencia principal en gestor de BD servidor (prohibido H2/SQLite/embebidas). Infraestructura del backend y dependencias: Docker Compose.