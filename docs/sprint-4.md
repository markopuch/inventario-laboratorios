# Sprint 4 — Avance de Sprint 4A

Este documento reúne el cierre de **Sprint 4A: Subcategorías** dentro del Sprint 4.
La [guía completa](sprint-4a-subcategorias.md) contiene las explicaciones y las
21 pruebas manuales. Sprint 4B no está implementado.

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
La API mostró las cuatro subcategorías iniciales. El backend quedó iniciado
en `http://localhost:8080` al cerrar esta revisión.

## 13. Cambios de documentación

- [README](../README.md): estado, rutas, permisos y V7.
- [Reglas de negocio](reglas-negocio.md): RN-31 padre activo y RN-32 baja de
  padre con hijas activas, sin renumerar RN-01 a RN-30.
- [Matriz de permisos](matriz-permisos.md): seis rutas nuevas y distinción
  entre API actual y funcionalidades propuestas.
- [Guía Sprint 4A](sprint-4a-subcategorias.md): JPA, capas, flujos, reglas,
  archivos, 21 pruebas Postman, SQL y tests.
- Este archivo: registro de Sprint 4A dentro de Sprint 4.

## 14. Pendientes

Queda disponible la comprobación manual del usuario en Postman. La suite
automática, el arranque y la migración de Sprint 4A están verificados.

Sprint 4B no está implementado; se definirá su alcance antes de continuar.
Los CRUD de Sede, Área y Laboratorio siguen pendientes aunque sus tablas
existen. También faltan UsuarioLaboratorio, alcance por laboratorio,
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
