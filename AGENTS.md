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
- `MAP.md` (este repo) — plan de desarrollo unicamente de CatalogAndSync. Mapa global del proyecto: `MAP.md` en la raiz del workspace (no se propaga).

## Build y comandos
- El modulo Maven esta en `catalogandsync/` (un nivel mas adentro). Compilar o testear desde la raiz de este repo falla.
- `./mvnw ...` — no hay `mvn` en el PATH (wrapper con Maven 3.9.16, Java 25 Temurin).
- Spring Boot **4.1.1** con starters renombrados: `spring-boot-starter-webmvc` (no `-web`) y tests con `spring-boot-starter-data-jpa-test` / `-data-redis-test` / `-kafka-test` / `-webmvc-test` (no existe `spring-boot-starter-test`). Verificar el nombre real en `pom.xml` antes de agregar dependencias.
- Configuracion en `catalogandsync/src/main/resources/application.yaml` (yaml, no yml).
- El scaffold de JHipster esta casi por completo sin commitear y hay un `catalogandsync.zip` suelto en este repo: revisar `git status` y nunca commitear el zip.

## Pautas
- Agente cooperador, no generador de codigo: escribir codigo solo cuando se pida; consultar antes de modificar archivos.
- CRITICO: cualquier decision de arquitectura interna (motor de BD, estrategia transaccional de sincronizacion, idempotencia) se consulta al usuario. NUNCA asumir.
- Persistencia principal en gestor de BD servidor (prohibido H2/SQLite/embebidas). PostgreSQL es el motor ya decidido.
- SIN STUB DE CATEDRA: no crear ningun stub, mock, fake, emulador ni copia local del servicio de la catedra (REST, Redis ni Kafka), ni como servicio propio ni en Docker Compose (IR §3 lo prohibe). En las pruebas tampoco se emula la catedra — nada de fakes de `CatalogDataSourcePort`, del cliente REST a catedra ni de listeners Kafka; solo dobles de capas propias (persistencia, seguridad) y verificacion de contrato manual contra la instancia real.
- El `docker-compose.yml` local es DECIDIDO y AUN NO EXISTE; cuando se implemente levantara SOLO PostgreSQL. Hosts, topics y credenciales de Kafka/Redis/API vienen del bloque `integration` de `POST /api/student/register` o `GET /api/student/integration` (IR §5.1 / §5.3) y se externalizan por variable de entorno; jamas en el repo ni en codigo. El JWT tecnico dura 1 ano; si se pierde o se expone hay que dar de alta otra cuenta tecnica (IR §5.4).
- PENDIENTE DE CONSULTAR: `CatalogandsyncApplicationTests.contextLoads()` es un `@SpringBootTest` sin datasource configurado, asi que no puede pasar hasta que exista el Compose con PostgreSQL y se decida como aislar la BD de pruebas.