# Caso de Uso 2
## Servicio de Catálogo
El servicio de catálogo tiene 2 responsabilidades, la primera es de almacenar la copia local del catálogo, y la segunda habilitar la búsqueda sobre los datos locales

### 2.1 Copia Local del catálogo
El servicio debe almacenar localmente los datos del servicio de la cátedra
El servicio debe utilizar una base de datos no efímera
El servicio debe permitir la escritura de los datos
El servicio solo permite la escritura de los procesos de sincronización que poseen el JWT técnico
El servicio no debe permitir la escritura por usuarios no autorizados (En este caso son los usuarios de la app KMP)
El servicio debe permitir la lectura de los datos tanto por usuarios autorizados como por usuarios de KMP
La fuente de verdad de la base de datos será toda la información proveniente de la cátedra. Los datos locales no tienen prioridad NUNCA por encima de la cátedra.
El servicio debe almacenar información de categorias, profesionales y horarios semanales
La base de datos tendrá las siguientes tablas:
professional_category:
    - id; tipo: bigint PK; id de la catedra
    - name; tipo: varchar; not null
    - description; tipo: varchar(255); nullable
    - enabled; tipo: boolean; not null
    - created_at; timestamptz
    - updated_at; timestamptz

professional:
    - id; tipo: bigint PK; id de la catedra
    - category_id; bigint FK -> professional_category.id ; not null
    - first_name; varchar; not null
    - last_name; varchar; not null
    - enabled; boolean; not null
    - created_at; timestamptz
    - updated_at; timestamptz
    - nota: búsqueda por nombre sobre first_name + last_name (criterio e índice según decisión de BD)

weekly_schedule:
    - id; bigint PK; id de la catedra
    - professional_id; bigint FK; not null, indice ( professional_id, day_of_week, enabled)
    - day_of_week; varchar(9); not null, MONDAY .. SUNDAY (enum vs varchar: decisión de BD)
    - start_time; time; not null
    - end_time; time; not null
    - slot_duration_minutes; int; not null
    - enabled; boolean; not null
    - created_at; timestamptz; not null
    - updated_at; timestamptz

catalog_version:
    - local_version; bigint; not null (unica fila garantizada por PK/CHECK, solo avanza cuando la unidad de trabajo terminó)
    - last_sync_mode; varchar; SNAPSHOT / INCREMENTAL (diagnóstico)
    - updated_at; timestamptz

### 2.2 Filtros de búsqueda de catálogo
El servicio debe proveer los filtros que van a ser utilizados por los usuarios de KMP app.
Los filtros se aplican sobre la base de datos local.
Los filtros básicos necesitados son: Categoria, nombre, estado habilitado y disponibilidad.
Filtros adicionales podrán ser creados, pero siempre serán aplicados a la base de datos local.
El filtro disponibilidad se resuelve consultando a TurnosYReservas mediante un contrato interno.
