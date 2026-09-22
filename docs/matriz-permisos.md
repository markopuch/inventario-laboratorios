# Matriz de permisos - Sistema de Inventario de Laboratorios

**Proyecto:** API REST de inventario de equipos de laboratorio  
**Versión:** 1.0  
**Estado:** Diseño base y permisos implementados hasta Sprint 6

## 1. Principio de autorización

La autorización se evalúa en dos niveles:

1. **Rol:** determina la clase de operación permitida.
2. **Alcance de laboratorio:** determina sobre qué laboratorios puede ejecutarse la operación.

Desde Sprint 4E, `ADMIN` tiene alcance efectivo global a laboratorios activos y
`GESTOR`/`LECTOR` obtienen sus laboratorios activamente asignados. El servicio
se aplica al CRUD y filtros de Equipo desde Sprint 5, y a traslado e historial
desde Sprint 6. ADMIN conserva además consulta histórica
global de Equipos, incluso si su laboratorio fue dado de baja posteriormente. Los catálogos actuales de Categoría,
Subcategoría, Sede, Área y Laboratorio son **globales para lectura de los tres
roles** y permiten escritura únicamente a ADMIN.

> `equipo.id_responsable` representa custodia o responsabilidad. No concede permisos.

## 2. Resumen por operación

| Operación | ADMIN | GESTOR | LECTOR |
|---|---:|---:|---:|
| Iniciar sesión | Sí | Sí | Sí |
| Ver equipos | Todos | Sus laboratorios | Sus laboratorios |
| Ver detalle de equipo | Todos | Sus laboratorios | Sus laboratorios |
| Registrar equipo | Sí | En sus laboratorios | No |
| Editar equipo | Sí | En sus laboratorios | No |
| Dar de baja un equipo | Sí | En sus laboratorios | No |
| Trasladar equipo | Sí | Origen y destino autorizados | No |
| Filtrar equipos | Todos | Dentro de su alcance | Dentro de su alcance |
| Ver movimientos | Todos | Movimientos de su alcance | Movimientos de su alcance |
| Gestionar categorías | Sí | No | No |
| Consultar categorías | Sí | Sí | Sí |
| Gestionar subcategorías | Sí | No | No |
| Consultar subcategorías, incluida la lista por categoría | Sí | Sí | Sí |
| Gestionar sedes, áreas y laboratorios | Sí | No | No |
| Consultar sedes, áreas y laboratorios | Sí | Sí | Sí |
| Crear o editar usuarios | Sí | No | No |
| Activar o desactivar usuarios | Sí | No | No |
| Asignar laboratorios a usuarios | Sí | No | No |
| Consultar asignaciones explícitas de un usuario | Sí | No | No |
| Consultar alcance propio | Global activo | Asignaciones activas a laboratorios activos | Asignaciones activas a laboratorios activos |
| Cambiar roles | Sí | No | No |

## 3. Matriz de endpoints existentes y propuestas futuras

Login, perfil propio, Categoría, Subcategoría, las 17 operaciones de organización,
los tres endpoints de asignaciones/alcance, los seis de Equipo y los tres de
traslado/historial están implementados. La administración general de usuarios
sigue pendiente. Administrar asignaciones no implica crear usuarios ni cambiar roles.
La sección 6 delimita el alcance.

| Método y ruta | Operación | ADMIN | GESTOR | LECTOR | Regla de alcance |
|---|---|---:|---:|---:|---|
| `POST /api/auth/login` | Autenticarse | Sí | Sí | Sí | El usuario debe estar activo |
| `GET /api/auth/me` | Consultar perfil propio | Sí | Sí | Sí | JWT válido |
| `GET /api/auth/me/laboratorios` **IMPLEMENTADO** | Consultar alcance efectivo propio | Sí | Sí | Sí | ADMIN: todos activos; GESTOR/LECTOR: asignación y laboratorio activos; principal del contexto |
| `GET /api/equipos` **IMPLEMENTADO** | Listar equipos | Sí | Sí | Sí | ADMIN incluye históricos BAJA; los demás solo laboratorios activos asignados; filtros en SQL |
| `GET /api/equipos/{id}` **IMPLEMENTADO** | Consultar equipo | Sí | Sí | Sí | El equipo debe pertenecer al alcance del usuario |
| `POST /api/equipos` **IMPLEMENTADO** | Registrar equipo | Sí | Sí | No | GESTOR debe tener asignado el laboratorio recibido |
| `PUT /api/equipos/{id}` **IMPLEMENTADO** | Editar equipo | Sí | Sí | No | GESTOR debe tener acceso al laboratorio actual; no se edita si está en BAJA |
| `DELETE /api/equipos/{id}` **IMPLEMENTADO** | Dar de baja | Sí | Sí | No | Es baja lógica; GESTOR solo en sus laboratorios |
| `POST /api/equipos/{idEquipo}/traslados` **IMPLEMENTADO** | Trasladar equipo | Sí | Sí | No | GESTOR requiere origen Y destino en alcance actual; destino activo para todos |
| `GET /api/equipos/{idEquipo}/movimientos` **IMPLEMENTADO** | Ver historial del equipo | Sí | Sí | Sí | ADMIN global; GESTOR/LECTOR solo movimientos cuyo origen O destino esté en su alcance actual |
| `GET /api/movimientos` **IMPLEMENTADO** | Listar movimientos | Sí | Sí | Sí | Alcance en SQL; filtro idLaboratorio coincide con origen O destino |
| `GET /api/categorias` | Consultar categorías | Sí | Sí | Sí | Sin restricción por laboratorio |
| `GET /api/categorias/{id}` | Consultar categoría | Sí | Sí | Sí | Sin restricción por laboratorio |
| `POST /api/categorias` | Crear categoría | Sí | No | No | Administración global |
| `PUT /api/categorias/{id}` | Editar categoría | Sí | No | No | Administración global |
| `DELETE /api/categorias/{id}` | Desactivar categoría | Sí | No | No | En Sprint 4A: rechaza si tiene subcategorías activas |
| `GET /api/subcategorias` | Listar subcategorías activas | Sí | Sí | Sí | Sin restricción por laboratorio |
| `GET /api/subcategorias/{id}` | Consultar subcategoría activa | Sí | Sí | Sí | Sin restricción por laboratorio |
| `GET /api/categorias/{id}/subcategorias` | Listar hijas activas de categoría activa | Sí | Sí | Sí | Sin restricción por laboratorio |
| `POST /api/subcategorias` | Crear subcategoría | Sí | No | No | Padre existente y activo; administración global |
| `PUT /api/subcategorias/{id}` | Actualizar o reasignar subcategoría | Sí | No | No | Hija activa y padre destino existente y activo |
| `DELETE /api/subcategorias/{id}` | Dar de baja subcategoría | Sí | No | No | Rechaza con 409 si tiene Equipos no BAJA; nombre reservado |
| `GET /api/sedes` | Listar sedes activas | Sí | Sí | Sí | Catálogo global |
| `GET /api/sedes/{id}` | Consultar sede activa | Sí | Sí | Sí | Catálogo global |
| `POST /api/sedes` | Crear sede | Sí | No | No | Administración global |
| `PUT /api/sedes/{id}` | Actualizar sede | Sí | No | No | Sede activa |
| `DELETE /api/sedes/{id}` | Dar de baja sede | Sí | No | No | Rechaza si tiene áreas activas |
| `GET /api/areas` | Listar áreas activas | Sí | Sí | Sí | Catálogo global |
| `GET /api/areas/{id}` | Consultar área activa | Sí | Sí | Sí | Catálogo global |
| `GET /api/sedes/{idSede}/areas` | Listar áreas activas de sede activa | Sí | Sí | Sí | Catálogo global; padre inactivo/ausente → 404 |
| `POST /api/areas` | Crear área | Sí | No | No | Sede existente y activa |
| `PUT /api/areas/{id}` | Actualizar o mover área | Sí | No | No | Área activa; sede destino activa; nombre único en destino |
| `DELETE /api/areas/{id}` | Dar de baja área | Sí | No | No | Rechaza si tiene laboratorios activos |
| `GET /api/laboratorios` | Listar laboratorios activos | Sí | Sí | Sí | Catálogo global |
| `GET /api/laboratorios/{id}` | Consultar laboratorio activo | Sí | Sí | Sí | Catálogo global |
| `GET /api/areas/{idArea}/laboratorios` | Listar laboratorios activos de área activa | Sí | Sí | Sí | Catálogo global; padre inactivo/ausente → 404 |
| `POST /api/laboratorios` | Crear laboratorio | Sí | No | No | Área existente y activa; código global único |
| `PUT /api/laboratorios/{id}` | Actualizar o mover laboratorio | Sí | No | No | Laboratorio activo y área destino activa |
| `DELETE /api/laboratorios/{id}` | Dar de baja laboratorio | Sí | No | No | Rechaza con 409 si hay asignaciones activas o Equipos no BAJA; conserva código reservado |
| `POST /api/admin/usuarios` **FUTURO** | Crear usuario | Sí | No | No | Solo administración |
| `PUT /api/admin/usuarios/{id}` **FUTURO** | Editar usuario | Sí | No | No | Solo administración |
| `PATCH /api/admin/usuarios/{id}/estado` **FUTURO** | Activar/desactivar usuario | Sí | No | No | Solo administración |
| `GET /api/admin/usuarios/{idUsuario}/laboratorios` **IMPLEMENTADO** | Consultar asignaciones explícitas activas | Sí | No | No | Configuración del destinatario, incluso inactivo; no es su alcance efectivo |
| `PUT /api/admin/usuarios/{idUsuario}/laboratorios` **IMPLEMENTADO** | Reemplazar asignaciones explícitas activas | Sí | No | No | Atómico; lista vacía válida; destinatario existente; laboratorios existentes y activos |
| `GET /api/admin/equipos` **IMPLEMENTADO** | Consultar todos los equipos | Sí | No | No | Endpoint explícitamente global |

## 4. Casos esperados de autorización

| Caso | Resultado esperado |
|---|---|
| Solicitud protegida sin token | `401 Unauthorized` |
| Token inválido, alterado o vencido | `401 Unauthorized` |
| Usuario inactivo con token anterior | `401 Unauthorized` o invalidación equivalente documentada |
| Usuario autenticado con rol insuficiente | `403 Forbidden` |
| GESTOR o LECTOR consulta o reemplaza asignaciones administrativas | `403 Forbidden` |
| ADMIN consulta sus asignaciones explícitas | `200`, solo filas activas; pueden ser cero sin limitar su alcance global |
| GESTOR/LECTOR sin asignaciones consulta su alcance | `200`, `alcanceGlobal=false`, `laboratorios=[]` |
| Cualquier rol consulta `GET /api/laboratorios` | `200`, catálogo global activo; no es el endpoint de alcance propio |
| GESTOR intenta acceder a equipos de un laboratorio no asignado | `403 Forbidden` en Equipo; filtro explícito de laboratorio no autorizado también |
| LECTOR intenta crear, editar, trasladar o dar de baja | `403 Forbidden` |
| ADMIN consulta o administra cualquier laboratorio | Operación permitida si la solicitud es válida |
| GESTOR traslada entre dos laboratorios asignados | 200 si Equipo no BAJA, destino activo/diferente y request válido |
| GESTOR solo tiene acceso al origen o solo al destino | `403 Forbidden`, sin cambios ni movimiento |
| ADMIN consulta historia con un laboratorio inactivo | 200; la baja lógica no oculta esos movimientos |
| Equipo actual fuera del alcance, con movimientos visibles relacionados | GET historial 200 con solo los visibles; no habilita GET detalle del Equipo |
| Sin movimientos visibles y Equipo actual dentro del alcance | GET historial `200 []` |
| Sin movimientos visibles y Equipo actual fuera del alcance | GET historial 403 |

## 5. Implementación recomendada

- Spring Security valida autenticación y rol.
- `AlcanceLaboratorioService` calcula el alcance y comprueba acceso a un laboratorio: ADMIN global activo; GESTOR/LECTOR según asignación activa.
- El endpoint de alcance propio obtiene el principal del contexto; no recibe un ID de usuario. Los endpoints ADMIN sí reciben el ID del destinatario después de autorizar al administrador.
- EquipoRepository aplica filtros y laboratorios permitidos en PostgreSQL; no filtra toda la tabla en memoria. El catálogo global de Laboratorio conserva su política.
- MovimientoEquipoRepository aplica origen O destino en alcance actual; ADMIN no filtra historia por estado activo de los laboratorios.
- Las pruebas deben cubrir al menos un caso permitido y uno rechazado por cada rol.

## 6. Alcance implementado hasta Sprint 6

Están implementados login JWT, perfil propio (`GET /api/auth/me`), Categoría y
Subcategoría, Sede, Área y Laboratorio. Los tres roles consultan los catálogos;
solo ADMIN los modifica. Las rutas jerárquicas por Categoría, Sede y Área usan
la misma política de lectura global. No existe una ruta `/api/organizacion/**`;
las rutas reales están enumeradas arriba.
Todas estas rutas, salvo el login, requieren un JWT válido: ausencia o token
inválido devuelve 401; rol sin permiso devuelve 403.

La administración completa de usuarios sigue pendiente. Sprint 6 incorpora
traslado transaccional e historial inmutable desde la API. Equipo tiene CRUD, filtros, baja lógica
BAJA y listado administrativo global. Sprint 4E implementa GET/PUT de
asignaciones, GET del alcance propio y la regla de no desactivar Laboratorio
con asignaciones activas. La asignación de un usuario inactivo también bloquea
la baja; ese usuario continúa sin autenticarse. Las relaciones inactivas no
bloquean y su fecha original se conserva al reactivarlas.

El reemplazo consulta el estado vigente dentro de una transacción. El alcance
se actualiza en la siguiente petición sin renovar un JWT que continúe válido.
ADMIN puede tener asignaciones explícitas, pero no limitan su alcance global.
No se modifican login, BCrypt, firma ni expiración JWT. Sprint 5 agrega el
bloqueo de baja de Subcategoría/Laboratorio si hay Equipos no BAJA, mantiene el
bloqueo por asignaciones y exige ADMIN/GESTOR para escribir Equipos. LECTOR
solo consulta. Un Equipo BAJA sigue visible con autorización; PUT y segundo
DELETE devuelven 409.

El traslado de Sprint 6 solo acepta destino, motivo y ubicación interna destino.
Origen, actor, tipo y fecha proceden del servidor; campos ajenos dan 400. BAJA,
destino inactivo o mismo destino producen 409. MANTENIMIENTO e INOPERATIVO sí
permiten trasladar. No existen POST directo, PUT ni DELETE de movimientos.
Los movimientos históricos no bloquean la baja lógica de Laboratorio por sí
solos. Los nombres del historial provienen de las entidades actuales; no son
una auditoría versionada de sus nombres.

Consulta las guías de [Sprint 4A](sprints/sprint-4a-subcategorias.md),
[organización](sprints/sprint-4b-organizacion.md#paso-16--jwt-y-roles-401-y-403) y
[Sprint 4E](sprints/sprint-4e-usuario-laboratorio.md#28-postman-secuencia-manual)
para comprobar los roles con Postman. La [guía de Sprint 5](sprints/sprint-5-equipos.md)
añade la matriz de casos de Equipo, filtros e inmutabilidad. La
[guía de Sprint 6](sprints/sprint-6-movimientos.md) incorpora traslado, rollback,
alcance del historial y bajas lógicas de laboratorios con historia.
