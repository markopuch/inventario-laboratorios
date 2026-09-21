# Sprint 4 — Subcategorías, organización y alcance

Este documento conserva los cierres históricos de **Sprint 4A: Subcategorías** y
**Sprint 4B–4D: Sede → Área → Laboratorio**, y agrega el cierre vigente de
[Sprint 4E: UsuarioLaboratorio y alcance](#1-resumen-sprint-4e).
Las guías detalladas son [Sprint 4A](sprint-4a-subcategorias.md),
[organización](sprint-4b-organizacion.md) y
[UsuarioLaboratorio](sprint-4e-usuario-laboratorio.md).

**Registro histórico de Sprint 4A (2026-09-20):** las siguientes 15 secciones
describen aquel cierre. Sus 99 pruebas y V7 son la base anterior; el resultado
de aquel incremento está en el reporte 4B–4D. El estado vigente está en el
reporte 4E, con 168 pruebas aprobadas y sin nueva migración.

## 1. Resumen Sprint 4A

Se implementó la vertical Java de Subcategoría: JPA, DTOs, dominio, MapStruct,
servicio transaccional, API REST, baja lógica, JWT y permisos. La tabla existía
desde V1; ahora puede operarse mediante la API. Se conservaron arquitectura,
carpetas, estilo del curso y versiones.

**Cierre verificado: 99 pruebas aprobadas de 99, sin fallos, errores ni omitidas.**
V7 está aplicada en la base habitual. Sus 2 categorías, 4 subcategorías y
3 usuarios conservaron íntegramente sus datos.

## 2. Arquitectura implementada

```text
HTTP / JSON → JWT y roles → Controller → Request + @Valid
    → Mapper → Domain → Service transaccional
    → Entity JPA → Repository → PostgreSQL

Categoria (1) ← Subcategoria (N)
```

La hija referencia `CategoriaEntity` con `@ManyToOne(fetch=LAZY)` y
`@JoinColumn(name="id_categoria", nullable=false)`. El request recibe
`idCategoria`; el Service valida el padre real; la respuesta solo expone su
resumen `{id,nombre}`. El dominio contiene una `Categoria` sin anotaciones JPA.
Es la primera vertical de catálogo relacionado; Usuario → Rol ya existía.

## 3. Archivos creados

Las rutas Java de esta tabla parten de
`backend/inventario/src/main/java/com/utec/inventario/`.

| Archivo | Propósito |
|---|---|
| `entity/SubcategoriaEntity.java` | Columnas y asociación JPA con el padre |
| `domain/Subcategoria.java` | Modelo de negocio sin JPA |
| `dto/request/CreateSubcategoriaRequest.java` | Entrada y validación de creación |
| `dto/request/UpdateSubcategoriaRequest.java` | Entrada y validación de PUT completo |
| `dto/response/SubcategoriaResponse.java` | Datos públicos de la hija |
| `dto/response/CategoriaResumenResponse.java` | ID y nombre del padre |
| `mapper/SubcategoriaMapper.java` | Conversiones y copia de campos editables |
| `repository/SubcategoriaRepository.java` | Consultas, duplicados y bloqueos |
| `service/SubcategoriaService.java` | Reglas y transacciones |
| `controller/SubcategoriaController.java` | CRUD REST |
| `controller/CategoriaSubcategoriaController.java` | GET jerárquico por categoría |

Otros archivos nuevos:

| Ruta | Propósito |
|---|---|
| `backend/inventario/src/main/resources/db/migration/V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql` | Unicidad por padre sin distinguir mayúsculas |
| `backend/inventario/src/test/java/com/utec/inventario/mapper/SubcategoriaMapperTest.java` | Conversiones |
| `backend/inventario/src/test/java/com/utec/inventario/service/SubcategoriaServiceTest.java` | Reglas de servicio |
| `backend/inventario/src/test/java/com/utec/inventario/SubcategoriaIntegrationTests.java` | HTTP, seguridad y persistencia |
| `backend/inventario/src/test/java/com/utec/inventario/SubcategoriaConcurrenciaTests.java` | Concurrencia de hija/padre e índice |
| `docs/sprint-4.md` | Avance de Sprint 4A dentro de Sprint 4 |
| `docs/sprint-4a-subcategorias.md` | Guía pedagógica, Postman y SQL |

## 4. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/service/CategoriaService.java` | Impedir baja de padre con hijas activas |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Permisos para nuevas rutas |
| `backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java` | Duplicados de subcategoría → 409 |
| `backend/inventario/src/test/java/com/utec/inventario/service/CategoriaServiceTest.java` | Nueva dependencia y regla del padre |
| `backend/inventario/src/test/java/com/utec/inventario/exception/GlobalExceptionHandlerTest.java` | Restricciones e idioma PostgreSQL |
| `README.md` | Estado, rutas, permisos y V7 |
| `docs/reglas-negocio.md` | RN-31 y RN-32 sin renumerar anteriores |
| `docs/matriz-permisos.md` | Rutas de Subcategoría y alcance actual |

`CategoriaRepository` y `CategoriaController` no se modificaron. Se reutiliza
la búsqueda bloqueante existente del padre.

## 5. Migración Flyway agregada

V7 añade `uq_subcategoria_categoria_nombre_ignore_case` sobre
`(id_categoria, UPPER(nombre))`, incluyendo inactivas. La restricción anterior
distinguía mayúsculas. El mismo nombre sigue permitido en padres diferentes.

El Service anticipa conflictos; el índice también protege ante concurrencia.
V7 comprueba duplicados y se detiene si encuentra alguno, sin eliminar ni
fusionar filas. La inspección previa no encontró duplicados. V1–V6 permanecen
intactas y Hibernate continúa con `ddl-auto=validate`.

V7 se aplicó correctamente tanto en la base de verificación como en
`inventario_laboratorios`. El historial muestra V1–V7 con `success=true`.

## 6. Explicación archivo por archivo

`SubcategoriaEntity` mapea fila y padre; PostgreSQL genera ID y fecha.
`Subcategoria` representa el negocio con una `Categoria` de dominio.
Los requests aceptan nombre, descripción e ID del padre; los responses evitan
exponer Entities y solo incluyen un resumen de Categoría.

`SubcategoriaMapper` transforma modelos y copia campos editables; no consulta
repositorios. `SubcategoriaRepository` filtra activas y comprueba duplicados
incluyendo inactivas. Sus lecturas usan `@EntityGraph` para cargar el padre y
sus consultas de modificación bloquean la hija.

`SubcategoriaService` normaliza texto, valida padre y nombre, coordina
transacciones y asigna la Entity del padre. El Controller de Subcategoría
gestiona HTTP; el controller jerárquico delega en ese mismo servicio.

`CategoriaService` bloquea al padre y comprueba hijas activas antes de la baja.
`SecurityConfig` permite GET a los tres roles y escrituras solo a ADMIN.
Los tests verifican conversiones, reglas, HTTP, seguridad, SQL y concurrencia.
La [guía](sprint-4a-subcategorias.md#responsabilidad-de-cada-capa) desarrolla las capas
y los flujos completos POST, GET, PUT y DELETE.

## 7. Reglas de negocio implementadas

| Regla | Resultado |
|---|---|
| RN-S4A-01 | Padre inexistente al crear o mover → 404 |
| RN-S4A-02 | Padre inactivo al crear o mover → 409 |
| RN-S4A-03 | Nombre único por padre sin distinguir mayúsculas; duplicado → 409 |
| RN-S4A-04 | La baja lógica mantiene reservado el nombre dentro del padre |
| RN-S4A-05 | GET devuelve activas; detalle inactivo → 404 |
| RN-S4A-06 | DELETE guarda `activo=false` y devuelve 204 |
| RN-S4A-07 | PUT sobre hija inactiva → 404 |
| RN-S4A-08 | PUT excluye el ID propio al buscar duplicados |
| RN-S4A-09 | Cambiar de padre valida existencia, actividad y unicidad en destino |
| RN-S4A-10 | Baja de categoría con hijas activas → 409; conserva el padre activo |
| RN-S4A-11 | GET jerárquico de padre inexistente/inactivo → 404 |

Los bloqueos coordinan crear o mover una hija con desactivar al padre y evitan
que PUT reactive una hija dada de baja. Protegen las operaciones de estos
servicios; las modificaciones SQL externas deben respetar las mismas reglas.

## 8. Regla diferida

**«No desactivar Subcategoría con Equipos activos»** se implementará cuando
exista Equipo. Este sprint no adelanta su vertical ni sus repositorios.

## 9. Seguridad

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET lista, detalle y lista por categoría | Sí | Sí | Sí |
| POST, PUT y DELETE de Subcategoría | Sí | No | No |

Sin token válido: 401. Con rol sin permiso: 403. Se conservan JWT, usuarios JPA
y BCrypt. Los catálogos son globales; el alcance por laboratorio queda pendiente.

## 10. Tests automáticos

**99 ejecutadas, 99 aprobadas, 0 fallidas, 0 errores y 0 omitidas.**
Se agregaron 59 casos a los 40 existentes:

| Clase | Casos nuevos |
|---|---:|
| `SubcategoriaMapperTest` | 4 |
| `SubcategoriaServiceTest` | 17 |
| `SubcategoriaIntegrationTests` | 28 |
| `SubcategoriaConcurrenciaTests` | 6 |
| `CategoriaServiceTest` | 1 |
| `GlobalExceptionHandlerTest` | 3 |

La base fue `inventario_verificacion_s4a_20260920_a93d`. Las pruebas que
modifican datos se ejecutaron allí; **no se utilizaron pruebas destructivas
sobre `inventario_laboratorios`**. La base temporal se eliminó al finalizar,
después de comprobar que no quedaron datos de prueba. `bootJar` también se
construyó correctamente.

Las seis regresiones concurrentes cubren alta y movimiento frente a baja del
padre en ambos órdenes, DELETE frente a PUT de hija y duplicado real rechazado
por PostgreSQL y traducido a 409. El reporte HTML se genera en
`backend/inventario/build/reports/tests/test/index.html`.
La [guía](sprint-4a-subcategorias.md#tests-automáticos) explica cómo repetir la suite.

## 11. Guía Postman

La [guía manual](sprint-4a-subcategorias.md#postman-preparación) explica los tres
tokens, IDs reales y 21 casos secuenciales: CRUD, cambio de categoría, validación,
duplicados, baja lógica, nombres reservados, padre inactivo, baja del padre con
y sin hijas, roles y ausencia de token.

Cada caso tiene método, URL, autorización, cabeceras, body, respuesta y regla.
Usa `corrida` para no reutilizar nombres reservados. El caso 17 crea una hija
activa de apoyo antes de comprobar el rechazo de baja del padre.
Postman queda para tu comprobación manual; no se automatizó su interfaz.

## 12. Verificación PostgreSQL

Las [consultas completas](sprint-4a-subcategorias.md#comprobación-manual-en-postgresql--pgadmin)
permiten contrastar la API con las filas:

| Consulta | Resultado esperado |
|---|---|
| JOIN de hija y padre | Categoría, fecha y estado reales |
| Hijas con `activo=true` | Filas visibles en GET |
| Duplicados por `id_categoria, UPPER(nombre)` | Cero filas |
| Hijas activas bajo padre inactivo | Cero filas tras escrituras de la API |
| `pg_indexes` para subcategoría | Índice UNIQUE de V7 |
| `flyway_schema_history` | V1–V7 con `success=true` |

En la base habitual se comprobó V7 y su índice, y se compararon los datos
completos antes y después: **2 categorías, 4 subcategorías y 3 usuarios sin
cambios**. Solo se añadió la migración y su índice, sin modificar esas filas.

Con la aplicación empaquetada se verificaron los tres logins y los GET de lista,
detalle y lista por categoría para los tres roles: 200. Sin JWT: 401.
La API mostró las cuatro subcategorías iniciales. En esa revisión se dejó una
instancia en `http://localhost:8080`; posteriormente se detuvo para liberar el
puerto. Este registro histórico no afirma que exista un backend activo ahora.

## 13. Cambios de documentación

- [README](../../README.md): estado, rutas, permisos y V7.
- [Reglas de negocio](../reglas-negocio.md): RN-31 padre activo y RN-32 baja de
  padre con hijas activas, sin renumerar RN-01 a RN-30.
- [Matriz de permisos](../matriz-permisos.md): seis rutas nuevas y distinción
  entre API actual y funcionalidades propuestas.
- [Guía Sprint 4A](sprint-4a-subcategorias.md): JPA, capas, flujos, reglas,
  archivos, 21 pruebas Postman, SQL y tests.
- Este archivo: registro de Sprint 4A dentro de Sprint 4.

## 14. Pendientes

Queda disponible la comprobación manual del usuario en Postman. La suite
automática, el arranque y la migración de Sprint 4A están verificados.

Al cierre de Sprint 4A, los CRUD de Sede, Área y Laboratorio estaban pendientes
aunque sus tablas existían; se implementan en el incremento documentado más
abajo. En aquel cierre también faltaban UsuarioLaboratorio, alcance por laboratorio,
administración completa de usuarios, Equipo y MovimientoEquipo.
No se incorporaron frontend, Docker, OpenAPI, mantenimiento ni auditoría general.

## 15. Checklist

- [x] SubcategoriaEntity
- [x] Domain
- [x] Repository
- [x] CreateRequest
- [x] UpdateRequest
- [x] Response
- [x] Mapper
- [x] Service
- [x] Controller
- [x] ManyToOne
- [x] JoinColumn
- [x] Validación de padre existente
- [x] Validación de padre activo
- [x] Duplicados por Categoría
- [x] Duplicados case-insensitive
- [x] Baja lógica
- [x] Bloqueo de Categoría con hijos activos
- [x] GET por Categoría
- [x] ADMIN escritura
- [x] GESTOR lectura
- [x] LECTOR lectura
- [x] 400
- [x] 401
- [x] 403
- [x] 404
- [x] 409
- [x] Nueva migración Flyway aplicada y verificada
- [x] Tests aprobados
- [x] Postman documentado
- [x] README actualizado
- [x] No se implementó Equipo
- [x] No se implementó Sprint 4B

---

**Registro histórico de Sprint 4B–4D:** las siguientes 20 secciones conservan
sus 140 pruebas y sus pendientes de aquel cierre. UsuarioLaboratorio y alcance
se completan después, en el reporte Sprint 4E al final; los valores históricos
no se reescriben como si ya hubieran existido.

# 1. Resumen del Sprint 4B–4D

Se agregan tres verticales completas: Sede (4B), Área (4C) y Laboratorio (4D).
Incluyen JPA, dominio separado, DTOs, MapStruct, Lombok, servicios transaccionales,
17 endpoints, baja lógica, JWT/roles, reglas padre-hijo, unicidad y concurrencia.
Se mantienen los paquetes, estilo del curso y versiones existentes.
La [guía pedagógica](sprint-4b-organizacion.md) incluye los 26 temas solicitados,
Postman secuencial, consultas de pgAdmin y comandos de verificación.

**Suite verificada: 140 pruebas aprobadas, sin fallos, errores ni omitidas.**
Compilación, unitarias y `bootJar` correctos. V8/V9 aplicadas después de completar
las verificaciones; V1–V9 con `success=true`. Las seis tablas comparadas conservaron
sus filas. La base temporal se eliminó y la instancia de comprobación se detuvo.

# 2. Estado inicial encontrado

La última migración aplicada era V7 y la suite anterior tenía 99 pruebas.
V1 y el esquema real ya incluían Sede, Área y Laboratorio con PK `SERIAL`,
`activo NOT NULL DEFAULT TRUE` y `fecha_creacion TIMESTAMPTZ` generada por BD.
No fue necesario agregar columnas de baja lógica ni fecha.

| Tabla | Campos editables encontrados | Unicidad inicial |
|---|---|---|
| `sede` | nombre(100), dirección(200), distrito(100), departamento(100) | No impone nombre único |
| `area` | nombre(100), descripción(255), FK sede | `UNIQUE(nombre,id_sede)`, sensible a mayúsculas |
| `laboratorio` | nombre(100), código(30), ubicación(200), FK área | Código único global, sensible a mayúsculas |

La RN-13 histórica describía código por Área, en contradicción con V1 y README.
Se corrigió RN-13 manteniendo código global único; no se cambió el modelo a
unicidad por Área. Las tablas existentes de Equipo, MovimientoEquipo y
UsuarioLaboratorio siguen sin equivaler a una implementación Java.

# 3. Arquitectura implementada

```text
Sede
  ↓ 1:N
Área
  ↓ 1:N
Laboratorio

HTTP / JSON → JWT y rol → Controller → Request + @Valid
    → Mapper → Domain → Service → Entity JPA → Repository → PostgreSQL
```

Área tiene `@ManyToOne(fetch=LAZY)` hacia Sede y Laboratorio hacia Área. El
request recibe el ID padre; el Service valida y asigna la Entity real; el
Response muestra su resumen `{id,nombre}`. Los dominios no tienen JPA y los
padres no agregan colecciones `@OneToMany` bidireccionales.

# 4. Archivos creados

Las rutas Java parten de `backend/inventario/src/main/java/com/utec/inventario/`.

| Ruta | Propósito |
|---|---|
| `entity/SedeEntity.java` | Columnas reales y campos generados de Sede |
| `entity/AreaEntity.java` | Área y relación JPA con Sede |
| `entity/LaboratorioEntity.java` | Laboratorio y relación JPA con Área |
| `domain/Sede.java` | Modelo de Sede sin persistencia |
| `domain/Area.java` | Modelo de Área con Sede de dominio |
| `domain/Laboratorio.java` | Modelo de Laboratorio con Área de dominio |
| `dto/request/CreateSedeRequest.java` | Validación del alta de Sede |
| `dto/request/UpdateSedeRequest.java` | Campos editables de PUT Sede |
| `dto/request/CreateAreaRequest.java` | Campos de Área e ID de Sede |
| `dto/request/UpdateAreaRequest.java` | PUT Área, incluido cambio de Sede |
| `dto/request/CreateLaboratorioRequest.java` | Campos de Laboratorio e ID de Área |
| `dto/request/UpdateLaboratorioRequest.java` | PUT Laboratorio, incluido cambio de Área |
| `dto/response/SedeResponse.java` | Respuesta de Sede |
| `dto/response/AreaResponse.java` | Respuesta de Área con padre resumido |
| `dto/response/LaboratorioResponse.java` | Respuesta de Laboratorio con padre resumido |
| `dto/response/SedeResumenResponse.java` | ID/nombre de Sede |
| `dto/response/AreaResumenResponse.java` | ID/nombre de Área |
| `mapper/SedeMapper.java` | Conversiones y copia de campos editables |
| `mapper/AreaMapper.java` | Conversiones de hija y padre resumido |
| `mapper/LaboratorioMapper.java` | Conversiones de hija y padre resumido |
| `repository/SedeRepository.java` | Activas y bloqueo de Sede |
| `repository/AreaRepository.java` | Activas, sede, duplicados e hijos activos |
| `repository/LaboratorioRepository.java` | Activos, área, código e hijos activos |
| `service/SedeService.java` | CRUD y bloqueo de baja con áreas activas |
| `service/AreaService.java` | CRUD, Sede activa, unicidad y bloqueo por laboratorios |
| `service/LaboratorioService.java` | CRUD, Área activa y código global |
| `controller/SedeController.java` | Cinco endpoints CRUD |
| `controller/AreaController.java` | Cinco endpoints CRUD |
| `controller/LaboratorioController.java` | Cinco endpoints CRUD |
| `controller/SedeAreaController.java` | GET de áreas por Sede |
| `controller/AreaLaboratorioController.java` | GET de laboratorios por Área |

Otros archivos, con rutas relativas al repositorio:

| Ruta | Propósito |
|---|---|
| `backend/inventario/src/main/resources/db/migration/V8__area_nombre_unico_por_sede_sin_mayusculas.sql` | Unicidad de Área por Sede ignorando caja |
| `backend/inventario/src/main/resources/db/migration/V9__laboratorio_codigo_unico_sin_mayusculas.sql` | Código global ignorando caja |
| `backend/inventario/src/test/java/com/utec/inventario/mapper/SedeMapperTest.java` | Conversiones de Sede y campos protegidos |
| `backend/inventario/src/test/java/com/utec/inventario/mapper/AreaMapperTest.java` | Conversiones de Área y relación |
| `backend/inventario/src/test/java/com/utec/inventario/mapper/LaboratorioMapperTest.java` | Conversiones de Laboratorio y relación |
| `backend/inventario/src/test/java/com/utec/inventario/service/SedeServiceTest.java` | Reglas de Sede |
| `backend/inventario/src/test/java/com/utec/inventario/service/AreaServiceTest.java` | Reglas de Área |
| `backend/inventario/src/test/java/com/utec/inventario/service/LaboratorioServiceTest.java` | Reglas de Laboratorio |
| `backend/inventario/src/test/java/com/utec/inventario/OrganizacionIntegrationTests.java` | HTTP, persistencia y seguridad |
| `backend/inventario/src/test/java/com/utec/inventario/OrganizacionConcurrenciaTests.java` | Carreras reales y protección de índices |
| `docs/sprint-4b-organizacion.md` | Guía pedagógica, Postman y SQL |

# 5. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Política de lectura/escritura de los tres catálogos |
| `backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java` | Duplicados conocidos de Área/Laboratorio → 409 |
| `backend/inventario/src/test/java/com/utec/inventario/exception/GlobalExceptionHandlerTest.java` | Regresión de restricciones y errores sin SQL |
| `README.md` | Estado, APIs, permisos y V8/V9 |
| `docs/reglas-negocio.md` | Corregir RN-13; extender RN-31/RN-32 sin renumerar |
| `docs/matriz-permisos.md` | Enumerar 17 rutas globales vigentes |
| `docs/sprint-4.md` | Mantener cierre 4A y añadir este reporte |

# 6. Migraciones nuevas

| Migración | Qué hace y por qué | Datos afectados |
|---|---|---|
| `V8__area_nombre_unico_por_sede_sin_mayusculas.sql` | Crea `uq_area_sede_nombre_ignore_case` sobre `(id_sede,UPPER(nombre))`; cierra duplicados que solo cambian mayúsculas | Indexa las filas existentes, incluidas inactivas; no modifica ni elimina filas |
| `V9__laboratorio_codigo_unico_sin_mayusculas.sql` | Crea `uq_laboratorio_codigo_ignore_case` sobre `UPPER(codigo)`; refuerza unicidad global | Indexa las filas existentes, incluidas inactivas; no modifica ni elimina filas |

Ambas comprueban previamente duplicados y se detienen ante conflictos, sin
repararlos automáticamente. Las restricciones antiguas se mantienen junto a los
índices nuevos. V1–V7 no se modifican; Hibernate conserva `ddl-auto=validate`.
Las columnas `activo` ya existían: no se crea una migración redundante.
La comparación SHA-256 confirmó que V1–V7 permanecieron intactas.

# 7. Sede

`SedeEntity` mapea la raíz con IDENTITY para SERIAL y fecha generada por la BD.
`Sede` separa negocio de JPA. Los DTOs permiten nombre, dirección, distrito y
departamento con tamaños reales; no permiten controlar ID, estado ni fecha.
`SedeMapper` transforma/copia; `SedeRepository` consulta activos y bloquea
escrituras. `SedeService` normaliza, conserva campos protegidos y rechaza su baja
si `AreaRepository` encuentra hijas activas. `SedeController` expone el CRUD y
`SedeAreaController` delega la consulta jerárquica a `AreaService`.
No existe una nueva regla de nombre único para Sede.

# 8. Area

`AreaEntity` usa `@ManyToOne` con `SedeEntity` y `id_sede` obligatorio. El dominio
contiene una Sede sin JPA. Los requests reciben nombre, descripción e `idSede`;
`AreaResponse` devuelve `SedeResumenResponse`. `AreaMapper` convierte sin buscar
padres. `AreaRepository` consulta activos, hijos por Sede y duplicados por
padre, incluyendo inactivos. `AreaService` valida y bloquea la sede, permite
mover el área y rechaza su baja con Laboratorios activos. `AreaController`
gestiona CRUD y `AreaLaboratorioController` delega la consulta de hijos a
`LaboratorioService`.

# 9. Laboratorio

`LaboratorioEntity` usa `@ManyToOne` con `AreaEntity` y `id_area` obligatorio.
El dominio contiene un Área separada. Requests incluyen nombre, código,
ubicación e `idArea`; la respuesta contiene `AreaResumenResponse`.
`LaboratorioMapper` copia los campos editables; el Service asigna el Área real.
`LaboratorioRepository` filtra activos, busca por Área y valida código global.
`LaboratorioService` permite reasignación a Área activa y baja lógica; el
Controller expone sus cinco operaciones. No se incorpora EquipoRepository ni
se anticipan restricciones de equipos/usuarios asignados.

# 10. Reglas padre-hijo

RN-31 exige padre existente y activo al crear o mover Subcategoría, Área y
Laboratorio. Ausencia devuelve 404; inactividad devuelve 409. GET jerárquico de
padre ausente/inactivo devuelve 404; padre activo sin hijos devuelve `200 []`.

RN-32 impide desactivar Categoría con Subcategorías activas, Sede con Áreas
activas y Área con Laboratorios activos. Devuelve 409 y mantiene al padre activo.
Las comprobaciones pertenecen a los Services y comparten bloqueos con altas y
movimientos de hijas. Laboratorio → Equipo sigue pendiente.

# 11. Unicidad

Área usa **Sede + nombre sin distinguir mayúsculas**. El mismo nombre puede
existir en otra sede. Laboratorio usa **código global sin distinguir mayúsculas**:
cambiar de Área o Sede no permite reutilizarlo. Los inactivos conservan su
reserva; PUT excluye el propio ID. Los índices de BD protegen concurrencia.
Sede no incorpora unicidad de nombre.

# 12. Seguridad

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET Sede, Área, Laboratorio y sus rutas jerárquicas | Sí | Sí | Sí |
| POST, PUT, DELETE | Sí | No | No |

Catálogos globales; no se aplica UsuarioLaboratorio. JWT ausente/inválido/vencido
devuelve 401. Rol insuficiente devuelve 403. Se conserva login, firma JWT y
comprobación de usuario/rol vigentes. La API reutiliza el error JSON controlado.

# 13. Concurrencia

Se bloquea el padre destino en altas y movimientos de hijas y en la baja del
padre. Si la hija confirma primero, la baja ve hijos activos; si confirma
primero el padre, la hija encuentra el destino inactivo. En ambos órdenes se
preserva la regla. PUT/DELETE de una misma entidad bloquean su fila y no permiten
reactivar una baja mediante una actualización que estaba esperando.

Las regresiones concurrentes cubren alta y movimiento de Área frente a baja de
Sede, alta y movimiento de Laboratorio frente a baja de Área, DELETE frente a
PUT de Sede/Área/Laboratorio, y dos escrituras con el mismo nombre/código.
Los índices únicos producen un conflicto 409 incluso cuando una comprobación
previa no pudo ver otra transacción sin confirmar. No se agrega infraestructura
nueva ni se promete que SQL externo pueda ignorar las reglas de los servicios.

# 14. Tests

| Dato | Resultado de este incremento |
|---|---|
| Total anterior | 99 |
| Tests nuevos | 41 |
| Total final | 140 |
| Aprobados | 140 |
| Fallidos / errores / omitidos | 0 / 0 / 0 |
| Base de verificación | `inventario_verificacion_s4b_org_20260921_c8f2` |
| Compilación | Correcta |
| Pruebas unitarias previas a integración | 65 aprobadas, 0 fallos/errores/omitidas |
| bootJar | Construido correctamente |

Los 41 casos nuevos se distribuyen así, sin contar dos veces los escenarios
que una misma prueba comprueba en varios pasos:

| Grupo | Casos nuevos |
|---|---:|
| Mappers de Sede, Área y Laboratorio | 6 |
| Services de Sede, Área y Laboratorio | 11 |
| `GlobalExceptionHandlerTest` | 1 |
| `OrganizacionIntegrationTests` | 14 |
| `OrganizacionConcurrenciaTests` | 9 |

Las pruebas de integración y concurrencia usan exclusivamente esa base temporal,
con prefijo `inventario_verificacion_`. No se usan pruebas destructivas sobre
`inventario_laboratorios`. Los fixtures de integración se limpiaron: quedaron
los conteos iniciales 2 categorías, 4 subcategorías, 3 usuarios, 1 sede, 2 áreas
y 2 laboratorios. Se comprobó que no quedaran conexiones a la base temporal,
se eliminó y se confirmó su ausencia al finalizar.
Comandos para repetirlas en la [guía](sprint-4b-organizacion.md#23-tests-y-verificación-reproducible).

# 15. Verificación Flyway

En la base de verificación, **V1–V9 tienen `success=true`**. Se comprobaron los
índices V8/V9 y la conservación de las restricciones anteriores. Las consultas
de duplicados y de hijas activas bajo padres inactivos confirmaron cero casos.
La comparación SHA-256 confirmó V1–V7 sin cambios.

En `inventario_laboratorios` también se verificaron **V1–V9 con `success=true`**.
V8/V9 se aplicaron solo después de compilación, unitarias, suite completa,
bootJar y SQL. Se conservaron tanto los dos índices nuevos como los dos índices
de las restricciones UNIQUE anteriores. Ninguna migración borró ni cambió filas.

# 16. Verificación base habitual

La inspección inicial de `inventario_laboratorios` registró:

| Tabla | Antes | Después |
|---|---:|---:|
| `categoria` | 2 | 2 |
| `subcategoria` | 4 | 4 |
| `usuario` | 3 | 3 |
| `sede` | 1 | 1 |
| `area` | 2 | 2 |
| `laboratorio` | 2 | 2 |

Además de los conteos, una comparación interna del contenido completo confirmó
que **las filas de las seis tablas no cambiaron**. No se imprimieron contraseñas,
hashes de contraseñas, tokens ni credenciales. Las consultas de duplicados y de
hijos activos bajo padres inactivos confirmaron cero casos.

El JAR arrancó para comprobar migraciones en el puerto temporal 8081. Los GET
anónimos de `/api/sedes`, `/api/areas` y `/api/laboratorios` devolvieron 401. Luego
se detuvo esa instancia y se verificó que **8080 y 8081 quedaron libres**.
No se deja un backend oculto ejecutándose: el usuario puede iniciarlo con el
script habitual y detenerlo con Ctrl+C.

# 17. Postman documentado

La [secuencia manual](sprint-4b-organizacion.md#21-postman-preparación-y-secuencia-manual)
incluye los 17 endpoints, autenticación de los tres roles, IDs reales guardados
como variables, cuerpos compatibles con V1, cambios de padre, bajas en orden,
duplicados y reserva después de baja. Explica 400, 401, 403, 404 y 409, rutas
jerárquicas y persistencia tras reinicio. No usa IDs supuestos ni credenciales
versionadas. La comprobación manual queda disponible para el usuario.

# 18. Documentación actualizada

- [README](../../README.md): estado real, APIs, permisos y V8/V9.
- [Reglas de negocio](../reglas-negocio.md): RN-13 global y RN-31/RN-32 en tres jerarquías.
- [Matriz de permisos](../matriz-permisos.md): 17 rutas organizacionales y tres roles.
- [Sprint 4](sprint-4.md): cierre 4A histórico más reporte de este incremento.
- [Sprint 4B–4D organización](sprint-4b-organizacion.md): guía pedagógica de 26 temas.

# 19. Pendientes

UsuarioLaboratorio, alcance por laboratorio, Equipo y MovimientoEquipo quedan
explícitamente pendientes. También administración completa de usuarios y frontend.
Las reglas de impedir baja de Laboratorio/Subcategoría con Equipos activos y
las restricciones por UsuarioLaboratorio se incorporarán con esas verticales.
No se adelantan Docker, mantenimiento ni auditoría general.

# 20. Checklist

- [x] SedeEntity
- [x] Sede Domain
- [x] Sede DTOs
- [x] Sede Mapper
- [x] Sede Repository
- [x] Sede Service
- [x] Sede Controller
- [x] CRUD Sede
- [x] Baja lógica Sede
- [x] AreaEntity
- [x] Area Domain
- [x] Area DTOs
- [x] Area Mapper
- [x] Area Repository
- [x] Area Service
- [x] Area Controller
- [x] ManyToOne Area → Sede
- [x] GET por Sede
- [x] Duplicado por Sede
- [x] Baja lógica Area
- [x] Bloquear baja Sede con Areas activas
- [x] LaboratorioEntity
- [x] Laboratorio Domain
- [x] Laboratorio DTOs
- [x] Laboratorio Mapper
- [x] Laboratorio Repository
- [x] Laboratorio Service
- [x] Laboratorio Controller
- [x] ManyToOne Laboratorio → Area
- [x] GET por Area
- [x] Código global único
- [x] Baja lógica Laboratorio
- [x] Bloquear baja Area con Laboratorios activos
- [x] ADMIN escritura
- [x] GESTOR lectura
- [x] LECTOR lectura
- [x] 400
- [x] 401
- [x] 403
- [x] 404
- [x] 409
- [x] Concurrencia implementada y cubierta por regresiones
- [x] Flyway verificado en ambas bases: V1–V9
- [x] Tests aprobados: 140/140
- [x] bootJar correcto
- [x] README actualizado
- [x] Documentación actualizada
- [x] No se implementó UsuarioLaboratorio
- [x] No se implementó Equipo
- [x] No se implementó MovimientoEquipo


---

# 1. Resumen Sprint 4E

Se implementó UsuarioLaboratorio sobre la tabla de V2: asignaciones explícitas
administradas por ADMIN, reemplazo atómico, baja lógica y reactivación, alcance
efectivo propio y servicio reutilizable para Equipo. Laboratorio rechaza su baja
si tiene asignaciones activas. El catálogo global conserva su comportamiento.

**168 pruebas aprobadas de 168**: 140 anteriores y 28 nuevas; cero fallos, errores
y omitidas. No se necesita V10. Equipo y MovimientoEquipo siguen pendientes.

# 2. Estado inicial encontrado

El backend tenía JPA/JWT/BCrypt y tres roles, Categoría, Subcategoría y
Sede → Área → Laboratorio. Flyway llegaba a V9 y la regresión era de 140 pruebas.
UsuarioLaboratorio existía solo como tabla; ahora incorpora vertical Java/API.

Esquema real de `usuario_laboratorio`, conservado íntegro:

| Columna | Tipo | Restricción/default |
|---|---|---|
| `id_usuario` | INTEGER | NOT NULL, componente de PK y FK Usuario |
| `id_laboratorio` | INTEGER | NOT NULL, componente de PK y FK Laboratorio |
| `activo` | BOOLEAN | NOT NULL, DEFAULT TRUE |
| `fecha_asignacion` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

PK `(id_usuario,id_laboratorio)`; ambas FK con UPDATE/DELETE RESTRICT. La PK
cubre búsquedas por usuario y existe índice adicional por laboratorio. La
inspección de la base habitual coincidió con el esquema definido por V1–V9.

# 3. Arquitectura

```text
Rol (1)
  ↓ (0..N)
Usuario
  ↓ (1 usuario por asignación; 0..N asignaciones)
UsuarioLaboratorio
  ↓ (1 laboratorio por asignación; 0..N asignaciones)
Laboratorio

Usuario ↔ Laboratorio: N:M mediante la Entity puente.
Rol = qué. Alcance = dónde.
HTTP/JWT → Controller → DTO + Mapper → Domain → Service → Repository → PostgreSQL
```

Se conservan los paquetes y convenciones del curso. El dominio no tiene
anotaciones JPA; los responses solo contienen datos públicos. El servicio de
alcance se reutilizará en Equipo sin anticipar esa vertical.

# 4. Archivos creados

Las primeras quince rutas parten de
`backend/inventario/src/main/java/com/utec/inventario/`.

| Ruta | Propósito |
|---|---|
| `entity/UsuarioLaboratorioId.java` | PK compuesta serializable e igualdad |
| `entity/UsuarioLaboratorioEntity.java` | Puente con EmbeddedId, MapsId, estado y fecha |
| `domain/UsuarioLaboratorio.java` | Dominio de relación sin JPA |
| `domain/UsuarioLaboratorios.java` | Usuario y conjunto explícito de laboratorios |
| `domain/AlcanceLaboratorios.java` | Dominio del alcance global o asignado |
| `dto/request/ActualizarLaboratoriosUsuarioRequest.java` | Lista obligatoria y elementos positivos no nulos |
| `dto/response/UsuarioResumenResponse.java` | Cinco campos públicos de usuario |
| `dto/response/LaboratorioAlcanceResponse.java` | ID, código y nombre |
| `dto/response/UsuarioLaboratoriosResponse.java` | Asignaciones explícitas |
| `dto/response/AlcanceLaboratoriosResponse.java` | Alcance efectivo |
| `mapper/UsuarioLaboratorioMapper.java` | Mapeos públicos con MapStruct |
| `repository/UsuarioLaboratorioRepository.java` | Consultas de asignaciones y existencia activa |
| `service/UsuarioLaboratorioService.java` | Reemplazo transaccional |
| `service/AlcanceLaboratorioService.java` | Obtener, comprobar y validar alcance |
| `controller/UsuarioLaboratorioController.java` | GET/PUT exclusivos ADMIN |

Pruebas, con base `backend/inventario/src/test/java/com/utec/inventario/`:

| Ruta | Propósito |
|---|---|
| `entity/UsuarioLaboratorioIdTest.java` | Igualdad y hash de la clave compuesta |
| `mapper/UsuarioLaboratorioMapperTest.java` | Dominio y responses sin datos sensibles |
| `service/UsuarioLaboratorioServiceTest.java` | Reemplazo y reglas |
| `service/AlcanceLaboratorioServiceTest.java` | Política ADMIN/GESTOR/LECTOR y comprobación de acceso |
| `UsuarioLaboratorioIntegrationTests.java` | HTTP, SQL, estados, seguridad y rollback |
| `UsuarioLaboratorioConcurrenciaTests.java` | Asignación frente a DELETE y dos PUT |

Nueva documentación:
`docs/sprints/sprint-4e-usuario-laboratorio.md`, guía pedagógica de 33 temas,
Postman, SQL y verificación.

# 5. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/repository/UsuarioRepository.java` | Bloquear ID del usuario y consultar proyecciones públicas sin contraseña |
| `backend/inventario/src/main/java/com/utec/inventario/repository/LaboratorioRepository.java` | Comprobar existencia activa para alcance |
| `backend/inventario/src/main/java/com/utec/inventario/service/LaboratorioService.java` | Bloquear baja con asignaciones activas |
| `backend/inventario/src/main/java/com/utec/inventario/controller/AuthController.java` | GET del alcance del principal |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Reglas de rol para tres endpoints |
| `backend/inventario/src/test/java/com/utec/inventario/service/LaboratorioServiceTest.java` | Regresión de la nueva restricción de baja |
| `README.md` | Estado, endpoints y enlaces a las carpetas actuales |
| `docs/reglas-negocio.md` | RN-33 a RN-36 |
| `docs/matriz-permisos.md` | Separar asignaciones/alcance vigentes de CRUD de usuarios futuro |
| `docs/sprints/sprint-4.md` | Agregar este reporte conservando cierres históricos |
| `docs/Erd_actual/erd-logico-v2.md` | UsuarioLaboratorio pasa a implementado, sin relaciones nuevas |
| `docs/Erd_actual/erd-logico-v2.svg` | Reflejar el estado visual actualizado de UsuarioLaboratorio |
| `docs/Erd_actual/erd-fisico-v2.md` | Actualizar solo estado de implementación y enlaces |
| `docs/Erd_actual/erd-v2-cambios.md` | Ocho entidades implementadas, dos futuras; esquema idéntico |

No se modifica `DemoUsuariosConfig`, JWT, BCrypt ni V1–V9.

# 6. Flyway

**No fue necesaria una nueva migración; no se creó V10.** V2 ya contiene la
PK compuesta, las cuatro columnas, las dos FK y el índice necesario. Mapear una
tabla existente en JPA no justifica aumentar la versión de Flyway.

V1–V9 conservaron sus huellas SHA-256. La base temporal aplicó las nueve
migraciones con éxito. La base habitual no necesita ninguna modificación para
este sprint. Compilación, suite completa y generación del JAR con `bootJar`
finalizaron correctamente.

# 7. Clave compuesta JPA

`UsuarioLaboratorioId` implementa Serializable, `@Embeddable` e igualdad por
ambos componentes. La Entity utiliza `@EmbeddedId`. Dos `@MapsId` enlazan
cada componente con su asociación `@ManyToOne(fetch=LAZY)`: Usuario y Laboratorio.
No se agregan colecciones bidireccionales ni un identificador artificial.

# 8. Semántica de asignaciones

PUT reemplaza el conjunto explícito activo. Normaliza IDs repetidos, permite
`[]`, mantiene las relaciones solicitadas, desactiva las retiradas y reactiva
las existentes inactivas. Solo crea filas para pares nuevos. Nunca borra las
relaciones ni cambia su fecha original al reactivar. Es idempotente.

Usuario inexistente da 404; laboratorio inexistente da 404; inactivo da 409.
Todos los destinos se validan antes de modificar y el conjunto se confirma en
una transacción. ADMIN puede administrar la configuración de usuarios inactivos,
que siguen sin poder autenticarse.

# 9. Alcance efectivo

ADMIN devuelve `alcanceGlobal=true` y todos los laboratorios activos,
independientemente de sus asignaciones explícitas. GESTOR/LECTOR devuelven
`false` y solo laboratorios activos con una asignación activa. Una lista vacía
es válida. Obtener, comprobar y validar alcance utilizan estado vigente.

El endpoint propio obtiene el usuario del contexto autenticado. Cambiar
asignaciones no requiere renovar un JWT aún válido. `GET /api/laboratorios`
sigue siendo el catálogo global; el alcance se consulta por la ruta específica.
`id_responsable` de Equipo no concede permisos ni alcance.

# 10. LaboratorioService

Después de bloquear el laboratorio, el Service consulta si existe una asignación
activa. En tal caso devuelve 409 y conserva el laboratorio activo, incluso si
el usuario asignado está inactivo. Con solo asignaciones inactivas la baja puede
continuar y devuelve 204. La comprobación por Equipos sigue pendiente.

# 11. Seguridad

| Endpoint | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET `/api/admin/usuarios/{idUsuario}/laboratorios` | 200 | 403 | 403 |
| PUT `/api/admin/usuarios/{idUsuario}/laboratorios` | 200 | 403 | 403 |
| GET `/api/auth/me/laboratorios` | 200 global | 200 asignado | 200 asignado |

Los códigos de éxito presuponen petición válida y recursos adecuados.
Sin token válido las tres rutas devuelven 401. Login, firma, expiración y BCrypt
se conservan. No existe nuevo CRUD general de usuarios.

# 12. Concurrencia

Cada PUT bloquea primero al Usuario y después los laboratorios destino en orden
ascendente. Dos reemplazos del mismo usuario quedan serializados y el estado
final corresponde a un conjunto completo. Asignar/reactivar y DELETE comparten
el bloqueo de Laboratorio: si gana la asignación, DELETE recibe 409; si gana
DELETE, la asignación recibe 409. Las regresiones coordinan ambos órdenes y
verifican que no sobreviva asignación activa a laboratorio inactivo.

# 13. Tests

| Medida | Resultado |
|---|---:|
| Total anterior | 140 |
| Nuevas invocaciones | 28 |
| Total final | 168 |
| Aprobadas | 168 |
| Fallidas | 0 |
| Errores | 0 |
| Omitidas | 0 |

Desglose nuevo: ID compuesto 1, Mapper 2, Service de asignaciones 5, Service de
alcance 3, integración 12, concurrencia 4 y regresión de LaboratorioService 1.
Los escenarios HTTP/SQL se agrupan por flujo; no se cuenta cada solicitud como test.

Base usada: `inventario_verificacion_s4e_scope_20260921_a7d9`.
Tras la suite quedaron solo datos iniciales/demo propios de esa base: 3 roles,
3 usuarios, 2 categorías, 4 subcategorías, 1 sede, 2 áreas, 2 laboratorios y
0 asignaciones. Cero pares duplicados y cero asignaciones activas a laboratorios
inactivos. Tras comprobar cero conexiones se eliminó la base temporal y se
confirmó su ausencia en el catálogo PostgreSQL.

# 14. Base habitual

No se agregaron asignaciones demo ni se modificó `DemoUsuariosConfig`. Al no
necesitar migración, no se escribió en la base habitual.

| Tabla | Antes | Después |
|---|---:|---:|
| usuario | 3 | 3 |
| laboratorio | 2 | 2 |
| usuario_laboratorio | 0 | 0 |

Las huellas de los datos públicos comparados también permanecieron idénticas.
No se imprimieron contraseñas, hashes, JWT ni credenciales. Las pruebas con
escritura utilizaron exclusivamente la base temporal indicada.

# 15. Documentación actualizada

[README](../../README.md), [reglas de negocio](../reglas-negocio.md),
[matriz de permisos](../matriz-permisos.md), este reporte y la
[guía Sprint 4E](sprint-4e-usuario-laboratorio.md). La guía incluye 33 temas,
las tres rutas, JSON, 13 casos manuales y 14 bloques de consultas de pgAdmin.

Se respeta la organización actual `docs/sprints/` y `docs/Erd_actual/`.
Los ERD existentes solo cambian el estado documental de UsuarioLaboratorio:
ocho entidades implementadas y dos futuras. El esquema, sus diez tablas,
76 columnas y trece relaciones se conserva.

# 16. Pendientes

Equipo, MovimientoEquipo, aplicar el alcance a Equipo y movimientos,
administración completa de usuarios y frontend. También quedan fuera
traslados, mantenimiento, auditoría general y Docker. Sprint 4E termina aquí.

# 17. Checklist

- [x] UsuarioLaboratorioId
- [x] UsuarioLaboratorioEntity
- [x] EmbeddedId
- [x] MapsId Usuario
- [x] MapsId Laboratorio
- [x] Domain
- [x] Repository
- [x] Request
- [x] Responses
- [x] Service de asignaciones
- [x] Service de alcance
- [x] GET admin asignaciones
- [x] PUT admin asignaciones
- [x] GET alcance propio
- [x] ADMIN global
- [x] GESTOR asignaciones
- [x] LECTOR asignaciones
- [x] Baja lógica de asignación
- [x] Reactivación
- [x] PUT atómico
- [x] Request vacío válido
- [x] IDs repetidos normalizados
- [x] Laboratorio inexistente 404
- [x] Laboratorio inactivo 409
- [x] Usuario inexistente 404
- [x] Impedir baja laboratorio con asignaciones activas
- [x] 401
- [x] 403
- [x] Concurrencia
- [x] Tests aprobados: 168/168
- [x] Documentación
- [x] No se implementó Equipo
- [x] No se implementó MovimientoEquipo
