# 1. Resumen Sprint 5

Se implementó la vertical Equipo: JPA, dominio, DTOs, MapStruct, Repository,
Service, CRUD REST, filtros y alcance. ADMIN tiene acceso global; GESTOR opera
dentro de sus laboratorios; LECTOR solo consulta. La baja usa estado BAJA y
conserva el recurso consultable. Código interno y Laboratorio son inmutables.

Flyway permanece en V9, sin nueva migración. MovimientoEquipo, traslado e
historial siguen pendientes. Resultado final de verificación:
**203 pruebas aprobadas de 203: 35 nuevas, cero fallos, errores y omitidas**.

# 2. Estado inicial

La base era Sprint 4E con 168 pruebas aprobadas: catálogos, usuarios JPA/JWT,
roles, asignaciones y AlcanceLaboratorioService reutilizable. Equipo existía
desde V3 como tabla, sin vertical Java/API. El catálogo PostgreSQL coincidió
con Flyway y sus nueve migraciones exitosas.

Esquema real de Equipo, conservado:

| Columna | Tipo | Nullability / restricción / default |
|---|---|---|
| id_equipo | SERIAL → INTEGER | PK, NOT NULL, secuencia |
| codigo_interno | VARCHAR(50) | NOT NULL, UNIQUE |
| serie_utec | VARCHAR(100) | NULL, UNIQUE |
| numero_serie | VARCHAR(100) | NULL, UNIQUE |
| nombre | VARCHAR(150) | NOT NULL |
| marca | VARCHAR(100) | NULL |
| modelo | VARCHAR(100) | NULL |
| estado | VARCHAR(30) | NOT NULL, DEFAULT OPERATIVO, CHECK de cuatro estados |
| anio | INTEGER | NULL, CHECK 1900–2100 o NULL |
| orden_compra | VARCHAR(50) | NULL |
| ubicacion_interna | VARCHAR(200) | NULL |
| comentario | TEXT | NULL, sin límite SQL adicional |
| requiere_mantenimiento | BOOLEAN | NOT NULL, DEFAULT FALSE |
| id_subcategoria | INTEGER | NOT NULL, FK |
| id_laboratorio | INTEGER | NOT NULL, FK |
| id_responsable | INTEGER | NULL, FK |
| fecha_creacion | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| fecha_actualizacion | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Dieciocho columnas, tres UNIQUE sensibles a mayúsculas, dos CHECK y tres FK
`ON UPDATE RESTRICT ON DELETE RESTRICT`. Las tres FK tienen índices adicionales.
No hay `activo` ni trigger de actualización.

# 3. Arquitectura

```text
Categoría
   ↓
Subcategoría
   ↓
Equipo ← Laboratorio ← Área ← Sede
   ↑
Usuario responsable (opcional)

Usuario
   ↓
UsuarioLaboratorio
   ↓
Laboratorio
   ↓
alcance sobre Equipo
```

HTTP/JWT → Controller → Request y Mapper → Domain → Service transaccional →
Repository/Entity → PostgreSQL. Se conserva el estilo del proyecto, sin
colecciones bidireccionales. **Rol = qué; alcance = dónde**. Custodia no es permiso.

# 4. Archivos creados

Las rutas Java parten de
`backend/inventario/src/main/java/com/utec/inventario/`.

| Ruta | Propósito |
|---|---|
| `entity/EquipoEntity.java` | Persistencia de las columnas reales y asociaciones |
| `domain/Equipo.java` | Dominio sin JPA |
| `domain/EstadoEquipo.java` | Enum con los cuatro estados de V3 |
| `dto/request/CreateEquipoRequest.java` | Entrada validada de creación |
| `dto/request/UpdateEquipoRequest.java` | Entrada de reemplazo editable e inmutabilidad |
| `dto/response/EquipoResponse.java` | Datos públicos del recurso |
| `dto/response/SubcategoriaEquipoResponse.java` | Resumen de clasificación |
| `dto/response/LaboratorioEquipoResponse.java` | Resumen de ubicación |
| `dto/response/ResponsableEquipoResponse.java` | Resumen público del custodio |
| `mapper/EquipoMapper.java` | Conversiones y copia controlada |
| `repository/EquipoRepository.java` | Filtros JPQL, duplicados, existencia y bloqueos |
| `service/EquipoService.java` | Reglas, alcance, transacciones y fechas |
| `controller/EquipoController.java` | Cinco operaciones del recurso |
| `controller/AdminEquipoController.java` | Listado global ADMIN reutilizable |
| `exception/CampoEquipoNoEditableException.java` | Validación específica del PUT |

Pruebas nuevas, relativas a `backend/inventario/src/test/java/com/utec/inventario/`:

| Ruta | Propósito |
|---|---|
| `mapper/EquipoMapperTest.java` | Requests, copia y resúmenes públicos |
| `service/EquipoServiceTest.java` | Reglas y alcance del servicio |
| `EquipoIntegrationTests.java` | HTTP, filtros, estados, roles y persistencia |
| `EquipoConcurrenciaTests.java` | Integridad bajo operaciones simultáneas |

Documentación nueva:

- `docs/sprints/sprint-5-equipos.md`: guía de 42 temas, Postman y SQL.
- `docs/sprints/sprint-5.md`: este reporte de 23 secciones.

# 5. Archivos modificados

| Ruta principal | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/service/SubcategoriaService.java` | Bloquear baja si hay Equipos no BAJA |
| `backend/inventario/src/main/java/com/utec/inventario/service/LaboratorioService.java` | Agregar bloqueo por Equipos, conservando asignaciones |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Roles en las seis rutas nuevas |
| `backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java` | UNIQUE de Equipo y error seguro de campo no editable |
| `backend/inventario/src/main/java/com/utec/inventario/repository/UsuarioRepository.java` | Proyecciones públicas y bloqueo del actor |
| `backend/inventario/src/main/java/com/utec/inventario/repository/LaboratorioRepository.java` | Bloquear laboratorio actual al editar Equipo, sin trasladarlo |
| `backend/inventario/src/test/java/com/utec/inventario/service/SubcategoriaServiceTest.java` | Regresión del bloqueo por Equipo no BAJA |
| `backend/inventario/src/test/java/com/utec/inventario/service/LaboratorioServiceTest.java` | Conservar asignaciones y agregar bloqueo por Equipo no BAJA |
| `README.md` | Estado, contratos, pruebas y enlaces de Sprint 5 |
| `docs/reglas-negocio.md` | RN-37–41 y precisiones de RN-14–20, conservando numeración |
| `docs/matriz-permisos.md` | Equipo implementado; movimientos/traslados futuros |
| `docs/Erd_actual/erd-logico-v2.md` | Equipo pasa a implementado, sin cambiar relaciones |
| `docs/Erd_actual/erd-logico-v2.svg` | Representación visual del estado actualizado |
| `docs/Erd_actual/erd-fisico-v2.md` | Solo estado de implementación; DDL intacto |
| `docs/Erd_actual/erd-v2-cambios.md` | Nueve entidades implementadas y una futura |

Las guías de Sprint 4 conservan sus cierres históricos. No se modificaron
migraciones ni DemoUsuariosConfig para cerrar Sprint 5.

# 6. Flyway

**No se creó una nueva migración. No fue necesaria V10. La versión final es V9.**
V3 ya contiene las columnas, claves, checks e índices requeridos. Las reglas de
roles, referencias activas, baja e inmutabilidad se aplican en Java; no justifican
reescribir migraciones existentes. Se mantienen UNIQUE sensibles a mayúsculas.

Verificación de huellas V1–V9, compilación y JAR:
**V1–V9 con SHA-256 idénticos; compileJava, suite completa y bootJar correctos**.

# 7. EquipoEntity

Mapea las 18 columnas, ID compatible con SERIAL, enum de estado y fechas
OffsetDateTime. Las referencias a Subcategoría y Laboratorio son obligatorias;
Usuario responsable es opcional. Las tres utilizan ManyToOne LAZY. No se añade
`activo` ni colecciones a los padres y la Entity no usa `@Data`.

# 8. DTOs y Response

Create exige código, nombre, estado, indicador de mantenimiento, Subcategoría
y Laboratorio. Responsable, series y demás campos opcionales admiten null.
Update reemplaza los campos editables y permite limpiar opcionales; no incluye
código, laboratorio, ID ni fechas. Campos desconocidos/inmutables en PUT dan 400.

EquipoResponse contiene todos los datos públicos y tres resúmenes: Subcategoría
`id/nombre`, Laboratorio `id/codigo/nombre`, responsable
`id/userName/nombre/apellido` o null. No expone contraseñas, hashes ni Entities.

# 9. Mapper

MapStruct usa componente Spring y `ReportingPolicy.ERROR`. Convierte requests,
dominio, Entity, response y listas; copia solo los campos editables. El Service
resuelve relaciones y responsables públicos. El Mapper no consulta repositorios
ni decide permisos, duplicados o alcance.

# 10. Repository y filtros

Dos consultas JPQL comparten los cuatro filtros opcionales y el orden por ID:
global y restringida por laboratorios permitidos. Estado, laboratorio,
Subcategoría y mantenimiento se combinan mediante AND en PostgreSQL.
No se carga toda la tabla con `findAll()` para filtrar después.

GESTOR/LECTOR sin alcance reciben lista vacía. Si solicitan un laboratorio fuera
de alcance explícitamente, reciben 403. ADMIN puede filtrar un laboratorio
existente aunque esté inactivo para consultar historia. El GET administrativo
reutiliza el mismo Service global y filtros.

# 11. EquipoService

Normaliza strings opcionales: trim y blanco → null. Valida referencias,
duplicados y estados, aplica alcance, preserva campos inmutables y controla la
transacción. POST con BAJA, PUT hacia BAJA y editar/eliminar nuevamente un
Equipo BAJA producen 409. Referencia ausente produce 404 e inactiva 409.

Un responsable nuevo debe estar activo; conservar el mismo responsable que
se desactivó después se permite, igual que retirarlo. No exige asignación del
custodio al laboratorio. Las tres unicidades conservan la semántica de V3.

# 12. Alcance

ADMIN consulta cualquier Equipo existente, incluidos BAJA e historia en
laboratorios posteriormente inactivos. Crear exige laboratorio activo.
GESTOR consulta y escribe dentro de sus asignaciones activas a laboratorios
activos; LECTOR solamente consulta ese conjunto.

AlcanceLaboratorioService aporta la política vigente de Sprint 4E y el Repository
la aplica al listado. Los cambios de asignaciones afectan las siguientes
peticiones sin renovar un JWT válido. El catálogo `GET /api/laboratorios` sigue
siendo global; el alcance propio sigue devolviendo laboratorios activos.

# 13. Código interno y Laboratorio inmutables

Se fijan en POST y nunca cambian mediante PUT. La validación del UpdateRequest
rechaza su presencia con 400 y explica que el cambio de laboratorio necesita
el flujo de traslado. Ese flujo todavía no existe; editar no debe eludir la
futura creación transaccional de MovimientoEquipo.

# 14. Baja lógica

DELETE cambia el estado a BAJA y conserva la fila, código, series y referencias.
GET puede seguir mostrando el Equipo a usuarios autorizados porque BAJA es un
estado del bien, no un filtro de ocultamiento. Segundo DELETE y PUT sobre BAJA
dan 409. No se implementa reactivación.

Las fechas iniciales proceden de los DEFAULT de PostgreSQL. `fechaCreacion` se
preserva; EquipoService actualiza `fechaActualizacion` con reloj Java en UTC en
cada PUT correcto y DELETE lógico. No se agregó trigger.

# 15. Reglas agregadas a SubcategoriaService y LaboratorioService

Ambos bloquean al padre y consultan EquipoRepository: cualquier Equipo con
estado distinto de BAJA impide la baja y produce 409. Con todos los Equipos en
BAJA, Subcategoría puede darse de baja. Laboratorio debe cumplir además la regla
previa de no tener asignaciones activas de UsuarioLaboratorio, aun de usuarios
inactivos. Las FK conservan la historia y no se eliminan filas.

# 16. Seguridad

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET lista/detalle | Global | Dentro de alcance | Dentro de alcance |
| POST/PUT/DELETE | Permitido bajo reglas de negocio | Dentro de alcance | 403 |
| GET `/api/admin/equipos` | Permitido | 403 | 403 |

Las seis rutas requieren JWT válido; sin él devuelven 401. Un detalle existente
fuera del alcance devuelve 403; inexistente, 404. Login, firma, expiración y
BCrypt se conservan. Ser responsable no concede permisos.

# 17. Concurrencia

Las escrituras coordinan el usuario actor, Equipo cuando corresponde y los padres
en orden consistente Subcategoría → Laboratorio. Crear frente a baja de padre
no puede dejar un Equipo no BAJA bajo un padre inactivo: la operación que llega
después encuentra el conflicto y da 409.

PUT/DELETE bloquean el mismo Equipo: si DELETE confirma primero, PUT detecta
BAJA y no lo reactiva. El bloqueo de lectura del actor coordina las escrituras
con el reemplazo de sus asignaciones. El UNIQUE de PostgreSQL resuelve la carrera
de código interno duplicado. Cobertura ejecutada: **9 invocaciones aprobadas**: CREATE frente a baja de Subcategoría/Laboratorio
en ambos órdenes (4), PUT frente a DELETE (2), código interno duplicado (1) y
revocación de alcance frente a POST de GESTOR en ambos órdenes (2).

# 18. Tests

| Medida | Resultado |
|---|---|
| Total anterior | 168 |
| Tests nuevos | 35 |
| Total final | 203 |
| Aprobados | 203 |
| Fallidos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Base utilizada | `inventario_verificacion_s5_equipo_20260921_b8f4` |

Las 35 invocaciones nuevas se distribuyen en Mapper 3, Service 7, integración 14,
concurrencia 9 y dos regresiones de padres (una en SubcategoriaServiceTest y otra
en LaboratorioServiceTest). El reporte JUnit final contiene 29 suites.

Se verifican mappers, contratos, alcance, cuatro filtros, referencias, duplicados,
fechas, BAJA consultable, padres y carreras coordinadas. Las escrituras de prueba
utilizan solo `inventario_verificacion_*` con fixtures propios. El conteo final
corresponde a invocaciones JUnit, no a cada petición HTTP de un flujo.
Limpieza y eliminación segura: **fixtures verificados, cero conexiones y base temporal eliminada**;
se confirmó su ausencia en el catálogo PostgreSQL.

Después de limpiar fixtures quedaron 0 Equipos, 0 movimientos, 0 asignaciones,
3 usuarios, 3 roles, 2 laboratorios, 4 subcategorías, 2 categorías, 1 sede y
2 áreas. La comprobación de Equipos no BAJA bajo padres inactivos devolvió cero.

# 19. Base habitual

Al no necesitar migración, no se crean Equipos ni se modifican asignaciones demo
en `inventario_laboratorios` para cerrar el sprint. Se comparan conteos y huellas
de datos públicos sin imprimir información sensible.

| Tabla | Antes | Después |
|---|---:|---|
| equipo | 0 | 0 |
| usuario | 3 | 3 |
| usuario_laboratorio | 0 | 0 |
| laboratorio | 2 | 2 |
| subcategoria | 4 | 4 |

Preservación del contenido comparado: **huellas de datos públicos idénticas en las cinco tablas**.

# 20. Postman

La [guía de 42 temas](sprint-5-equipos.md#37-postman-preparación-y-secuencia-manual)
incluye preparación con IDs reales, tokens locales, padres de prueba aislados,
JSON exacto de POST/PUT y 17 casos de comprobación. Cubre roles, alcance vacío,
filtros, duplicados, referencias, campos inmutables, BAJA, fechas y bajas de padres.

Se guardan/restauran asignaciones anteriores y se dan de baja solamente los
recursos de prueba. La guía explica que las bajas lógicas conservan filas y que
la limpieza manual no equivale a borrar historia. Incluye 15 grupos de consultas
SQL de lectura para corroborar persistencia y esquema.

# 21. Documentación

- [README](../../README.md): Equipo implementado y pendientes reales.
- [Reglas de negocio](../reglas-negocio.md): RN-37–41 y precisiones sin renumerar.
- [Matriz de permisos](../matriz-permisos.md): seis endpoints de Equipo vigentes.
- [Guía Sprint 5](sprint-5-equipos.md): 42 temas, Postman y SQL.
- Este reporte: 23 secciones y evidencia de ejecución.
- [ERD lógico](../Erd_actual/erd-logico-v2.md),
  [físico](../Erd_actual/erd-fisico-v2.md) y
  [cambios](../Erd_actual/erd-v2-cambios.md): nueve entidades implementadas,
  MovimientoEquipo futuro. El físico conserva sus diez tablas y 76 columnas.

# 22. Pendientes

MovimientoEquipo, traslado de equipos, historial de movimientos, administración
completa de usuarios y frontend. También mantenimiento como entidad independiente,
auditoría general, Docker y permisos dinámicos. No se implementan en este sprint.

# 23. Checklist

- [x] EquipoEntity
- [x] Domain Equipo
- [x] EstadoEquipo
- [x] CreateRequest
- [x] UpdateRequest
- [x] EquipoResponse
- [x] Mapper
- [x] Repository
- [x] Service
- [x] Controller
- [x] ManyToOne Subcategoria
- [x] ManyToOne Laboratorio
- [x] ManyToOne Responsable
- [x] CRUD
- [x] Filtros
- [x] Filtro estado
- [x] Filtro laboratorio
- [x] Filtro subcategoria
- [x] Filtro mantenimiento
- [x] codigoInterno único
- [x] serieUtec única
- [x] numeroSerie único
- [x] Opcionales vacíos → null
- [x] codigoInterno inmutable
- [x] Laboratorio inmutable mediante PUT
- [x] Subcategoria activa
- [x] Laboratorio activo
- [x] Responsable nuevo activo
- [x] ADMIN global
- [x] GESTOR alcance
- [x] LECTOR alcance lectura
- [x] GET detalle fuera alcance → 403
- [x] GET lista restringida
- [x] Baja lógica
- [x] BAJA consultable
- [x] BAJA no editable
- [x] Segundo DELETE → 409
- [x] fechaCreacion preservada
- [x] fechaActualizacion actualizada
- [x] Bloquear baja Subcategoria con Equipos no BAJA
- [x] Bloquear baja Laboratorio con Equipos no BAJA
- [x] Conservar bloqueo por UsuarioLaboratorio
- [x] 400
- [x] 401
- [x] 403
- [x] 404
- [x] 409
- [x] Concurrencia: 9 invocaciones aprobadas
- [x] Tests: 203/203 aprobados
- [x] Postman documentado
- [x] SQL documentado
- [x] README
- [x] ERD estado actualizado
- [x] No se implementó MovimientoEquipo
- [x] No se implementó traslado
