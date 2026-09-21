# Matriz de permisos - Sistema de Inventario de Laboratorios

**Proyecto:** API REST de inventario de equipos de laboratorio  
**Versión:** 1.0  
**Estado:** Diseño base y permisos implementados hasta Sprint 4B–4D

## 1. Principio de autorización

La autorización se evalúa en dos niveles:

1. **Rol:** determina la clase de operación permitida.
2. **Alcance de laboratorio:** determina sobre qué laboratorios puede ejecutarse la operación.

En el diseño futuro de equipos y movimientos, `ADMIN` tiene alcance global y
`GESTOR`/`LECTOR` estarán limitados por asignaciones de `usuario_laboratorio`.
Ese alcance aún no está implementado. Los catálogos actuales de Categoría,
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
| Cambiar roles | Sí | No | No |

## 3. Matriz de endpoints existentes y propuestas futuras

Login, perfil propio, Categoría, Subcategoría y las 17 operaciones de organización
están implementados. Las filas de equipos, movimientos y administración de
usuarios son propuestas, sujetas a sus sprints. La sección 6 delimita el alcance.

| Método y ruta | Operación | ADMIN | GESTOR | LECTOR | Regla de alcance |
|---|---|---:|---:|---:|---|
| `POST /api/auth/login` | Autenticarse | Sí | Sí | Sí | El usuario debe estar activo |
| `GET /api/auth/me` | Consultar perfil propio | Sí | Sí | Sí | JWT válido |
| `GET /api/equipos` | Listar equipos | Sí | Sí | Sí | ADMIN ve todos; los demás solo sus laboratorios |
| `GET /api/equipos/{id}` | Consultar equipo | Sí | Sí | Sí | El equipo debe pertenecer al alcance del usuario |
| `POST /api/equipos` | Registrar equipo | Sí | Sí | No | GESTOR debe tener asignado el laboratorio recibido |
| `PUT /api/equipos/{id}` | Editar equipo | Sí | Sí | No | GESTOR debe tener acceso al laboratorio actual; no se edita si está en BAJA |
| `DELETE /api/equipos/{id}` | Dar de baja | Sí | Sí | No | Es baja lógica; GESTOR solo en sus laboratorios |
| `POST /api/equipos/{id}/traslados` | Trasladar equipo | Sí | Sí | No | GESTOR requiere acceso al origen y al destino |
| `GET /api/equipos/{id}/movimientos` | Ver historial del equipo | Sí | Sí | Sí | El equipo o sus movimientos deben estar dentro del alcance |
| `GET /api/movimientos` | Listar movimientos | Sí | Sí | Sí | ADMIN ve todos; los demás solo los relacionados con su alcance |
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
| `DELETE /api/subcategorias/{id}` | Dar de baja subcategoría | Sí | No | No | Baja lógica; nombre reservado dentro de su categoría |
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
| `DELETE /api/laboratorios/{id}` | Dar de baja laboratorio | Sí | No | No | Baja lógica; conserva código reservado |
| `POST /api/admin/usuarios` | Crear usuario | Sí | No | No | Solo administración |
| `PUT /api/admin/usuarios/{id}` | Editar usuario | Sí | No | No | Solo administración |
| `PATCH /api/admin/usuarios/{id}/estado` | Activar/desactivar usuario | Sí | No | No | Solo administración |
| `PUT /api/admin/usuarios/{id}/laboratorios` | Asignar laboratorios | Sí | No | No | Solo administración |
| `GET /api/admin/equipos` | Consultar todos los equipos | Sí | No | No | Endpoint explícitamente global |

## 4. Casos esperados de autorización

| Caso | Resultado esperado |
|---|---|
| Solicitud protegida sin token | `401 Unauthorized` |
| Token inválido, alterado o vencido | `401 Unauthorized` |
| Usuario inactivo con token anterior | `401 Unauthorized` o invalidación equivalente documentada |
| Usuario autenticado con rol insuficiente | `403 Forbidden` |
| GESTOR intenta acceder a equipos de un laboratorio no asignado | `403 Forbidden` en el futuro módulo de alcance |
| LECTOR intenta crear, editar, trasladar o dar de baja | `403 Forbidden` |
| ADMIN consulta o administra cualquier laboratorio | Operación permitida si la solicitud es válida |
| GESTOR traslada entre dos laboratorios asignados | Operación permitida si se cumplen las reglas de negocio |
| GESTOR tiene acceso al origen, pero no al destino | `403 Forbidden` |

## 5. Implementación recomendada

- Spring Security valida autenticación y rol.
- El servicio de negocio valida el alcance mediante `usuario_laboratorio`.
- El backend obtiene el usuario desde el JWT o contexto de seguridad; no confía en un `idUsuario` enviado por el cliente.
- Los repositorios y filtros deben incluir el alcance permitido para evitar exposición accidental de datos.
- Las pruebas deben cubrir al menos un caso permitido y uno rechazado por cada rol.

## 6. Alcance implementado hasta Sprint 4B–4D

Están implementados login JWT, perfil propio (`GET /api/auth/me`), Categoría y
Subcategoría, Sede, Área y Laboratorio. Los tres roles consultan los catálogos;
solo ADMIN los modifica. Las rutas jerárquicas por Categoría, Sede y Área usan
la misma política de lectura global. No existe una ruta `/api/organizacion/**`;
las rutas reales están enumeradas arriba.
Todas estas rutas, salvo el login, requieren un JWT válido: ausencia o token
inválido devuelve 401; rol sin permiso devuelve 403.

Los endpoints de equipos, movimientos y administración completa de usuarios
de la matriz siguen siendo propuestas. El alcance por laboratorio
y UsuarioLaboratorio aún no tienen implementación de servicio. Esto mantiene
la política conceptual del Sprint 0 sin presentar permisos futuros como
funcionalidades terminadas. Consulta la
[guía de Sprint 4A](sprint-4a-subcategorias.md) y la
[guía de organización](sprint-4b-organizacion.md#paso-16--jwt-y-roles-401-y-403)
para comprobar los roles con Postman. Las restricciones de baja de Laboratorio
por Equipos o UsuarioLaboratorio quedan pendientes de esas verticales.
