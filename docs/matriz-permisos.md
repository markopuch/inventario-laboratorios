# Matriz de permisos - Sistema de Inventario de Laboratorios

**Proyecto:** API REST de inventario de equipos de laboratorio  
**Versión:** 1.0  
**Estado:** Documento base para el Sprint 0

## 1. Principio de autorización

La autorización se evalúa en dos niveles:

1. **Rol:** determina la clase de operación permitida.
2. **Alcance de laboratorio:** determina sobre qué laboratorios puede ejecutarse la operación.

`ADMIN` tiene alcance global. `GESTOR` y `LECTOR` están limitados por las asignaciones activas de `usuario_laboratorio`.

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

## 3. Matriz por endpoint propuesto

| Método y ruta | Operación | ADMIN | GESTOR | LECTOR | Regla de alcance |
|---|---|---:|---:|---:|---|
| `POST /api/auth/login` | Autenticarse | Sí | Sí | Sí | El usuario debe estar activo |
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
| `GET /api/organizacion/**` | Consultar organización | Sí | Sí | Sí | Datos de apoyo para formularios y filtros |
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
| GESTOR intenta acceder a un laboratorio no asignado | `403 Forbidden` |
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

## 6. Alcance implementado hasta Sprint 4A

Están implementados login JWT, perfil propio (`GET /api/auth/me`), Categoría y
Subcategoría. Los tres roles consultan los catálogos; solo ADMIN los modifica.
La ruta jerárquica de Subcategoría usa la misma política de lectura que Categoría.
Todas estas rutas, salvo el login, requieren un JWT válido: ausencia o token
inválido devuelve 401; rol sin permiso devuelve 403.

Los endpoints de equipos, movimientos, organización y administración completa
de usuarios de la matriz siguen siendo propuestas. El alcance por laboratorio
y UsuarioLaboratorio aún no tienen implementación de servicio. Esto mantiene
la política conceptual del Sprint 0 sin presentar permisos futuros como
funcionalidades terminadas. Consulta la
[guía de Sprint 4A](sprint-4a-subcategorias.md) para comprobar los roles con Postman.
