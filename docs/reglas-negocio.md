# Reglas de negocio - Sistema de Inventario de Laboratorios

**Proyecto:** API REST de inventario de equipos de laboratorio  
**Versión:** 1.0  
**Estado:** Documento base para el Sprint 0  
**Fuente de verdad futura:** migraciones Flyway y código del backend

## 1. Alcance

Estas reglas definen cómo se registran, consultan, modifican, trasladan y dan de baja los equipos. También establecen cómo se controlan los usuarios, roles y laboratorios autorizados.

El modelo considera tres roles:

- `ADMIN`: acceso global y administración del sistema.
- `GESTOR`: gestión de equipos dentro de los laboratorios que tiene asignados.
- `LECTOR`: consulta de equipos y movimientos dentro de los laboratorios que tiene asignados.

La pertenencia de un usuario a uno o más laboratorios se administra mediante `usuario_laboratorio`. El campo `equipo.id_responsable` identifica al custodio del equipo, pero no concede permisos de acceso.

## 2. Reglas de identidad, usuarios y seguridad

### RN-01. Correo único

Cada usuario debe registrarse con un correo electrónico único. No se permite crear dos usuarios con el mismo correo.

### RN-02. Contraseña protegida

La contraseña debe almacenarse únicamente como hash seguro, por ejemplo BCrypt. Ni la contraseña original ni `password_hash` deben aparecer en respuestas de la API, registros de aplicación o mensajes de error.

### RN-03. Usuario activo

Solo los usuarios activos pueden autenticarse y utilizar los endpoints protegidos. Un usuario inactivo conserva sus registros históricos, pero no puede operar en el sistema.

### RN-04. Rol obligatorio

Todo usuario debe tener exactamente un rol activo: `ADMIN`, `GESTOR` o `LECTOR`.

### RN-05. Asignación de laboratorios

Un usuario puede estar asignado a uno o más laboratorios mediante `usuario_laboratorio`. La combinación `id_usuario + id_laboratorio` no puede repetirse.

## 3. Reglas de autorización

### RN-06. Acceso global del administrador

Un usuario con rol `ADMIN` puede consultar y administrar equipos de cualquier laboratorio.

### RN-07. Alcance del gestor

Un usuario con rol `GESTOR` solo puede crear, editar, trasladar o dar de baja equipos pertenecientes a laboratorios que tenga asignados.

### RN-08. Alcance del lector

Un usuario con rol `LECTOR` solo puede consultar equipos y movimientos pertenecientes a sus laboratorios. No puede crear, editar, trasladar, dar de baja ni gestionar usuarios.

### RN-09. Separación entre rol y pertenencia

El rol define qué tipo de operación puede ejecutar el usuario. `usuario_laboratorio` define sobre qué laboratorios puede ejecutarla. Ambas validaciones deben cumplirse.

### RN-10. El responsable no concede permisos

Que un usuario figure como `id_responsable` de un equipo no le concede acceso automático al laboratorio ni privilegios de edición.

## 4. Reglas de organización y catálogos

### RN-11. Jerarquía organizacional

Toda área debe pertenecer a una sede y todo laboratorio debe pertenecer a un área.

### RN-12. Jerarquía de categorías

Toda subcategoría debe pertenecer a una categoría.

### RN-13. Catálogos sin duplicados lógicos

No se deben repetir nombres de áreas dentro de la misma sede, códigos de laboratorios dentro de la misma área ni nombres de subcategorías dentro de la misma categoría.

## 5. Reglas de equipos

### RN-14. Código interno único

`codigo_interno` identifica al equipo dentro del sistema y no puede repetirse.

### RN-15. Series únicas cuando se informan

`serie_utec` y `numero_serie` no pueden repetirse cuando contienen un valor. Pueden quedar vacíos solo cuando el equipo todavía no dispone de esa información.

### RN-16. Relaciones obligatorias

Todo equipo debe estar asociado a:

- un laboratorio existente;
- una subcategoría existente.

El responsable puede ser opcional durante el registro inicial.

### RN-17. Estados permitidos

El estado del equipo debe ser uno de los siguientes:

- `OPERATIVO`
- `MANTENIMIENTO`
- `INOPERATIVO`
- `BAJA`

### RN-18. Baja lógica

La operación HTTP `DELETE` no elimina físicamente el registro. Cambia el estado del equipo a `BAJA` y conserva la información para trazabilidad.

### RN-19. Restricciones de un equipo dado de baja

Un equipo con estado `BAJA` no puede editarse ni trasladarse. Su información solo puede consultarse por usuarios autorizados.

### RN-20. Consistencia de mantenimiento

Cuando `requiere_mantenimiento = true`, el equipo debe poder identificarse mediante filtros de mantenimiento. Esta marca no reemplaza al estado del equipo.

## 6. Reglas de traslado y trazabilidad

### RN-21. Destino diferente al origen

Un equipo no puede trasladarse al mismo laboratorio en el que ya se encuentra.

### RN-22. Autorización sobre origen y destino

Para trasladar un equipo, un `GESTOR` debe estar autorizado tanto en el laboratorio de origen como en el laboratorio de destino. `ADMIN` está exonerado de esta limitación por tener alcance global.

### RN-23. Movimiento obligatorio

Todo cambio de laboratorio debe generar un registro en `movimiento_equipo` con:

- equipo trasladado;
- laboratorio de origen;
- laboratorio de destino;
- usuario que ejecutó la operación;
- tipo de movimiento;
- motivo;
- fecha y hora.

### RN-24. Operación transaccional

La actualización del laboratorio del equipo y la creación del movimiento deben ejecutarse dentro de una misma transacción. Si una parte falla, ninguna modificación debe persistir.

### RN-25. Motivo obligatorio

Todo traslado debe incluir un motivo no vacío y suficientemente descriptivo.

## 7. Reglas de API y errores

### RN-26. Validación de solicitudes

La API debe rechazar solicitudes con campos obligatorios vacíos, identificadores inválidos, correos mal formados o valores fuera del dominio permitido.

### RN-27. Códigos HTTP consistentes

| Situación | Código esperado |
|---|---:|
| Operación correcta de consulta o actualización | `200 OK` |
| Creación correcta | `201 Created` |
| Baja lógica correcta sin cuerpo | `204 No Content` |
| Solicitud inválida | `400 Bad Request` |
| Falta de autenticación o token inválido | `401 Unauthorized` |
| Usuario autenticado sin permiso | `403 Forbidden` |
| Recurso inexistente | `404 Not Found` |
| Duplicado o regla de negocio incompatible | `409 Conflict` |

### RN-28. Errores sin información sensible

Los errores no deben devolver contraseñas, hashes, tokens, credenciales de PostgreSQL, consultas SQL completas ni trazas internas.

## 8. Reglas para filtros y consultas

### RN-29. Los filtros respetan el alcance

Los filtros por estado, laboratorio, subcategoría o mantenimiento nunca deben permitir que un usuario acceda a datos fuera de sus laboratorios autorizados.

### RN-30. Historial visible según alcance

`ADMIN` puede consultar todos los movimientos. `GESTOR` y `LECTOR` solo pueden consultar movimientos relacionados con laboratorios de su alcance.

## 9. Criterios de aceptación del documento

Las reglas quedan correctamente implementadas cuando se demuestra que:

- Flyway crea todas las restricciones estructurales posibles.
- El servicio valida las reglas que dependen del usuario autenticado o del estado actual del equipo.
- Los controladores solo coordinan HTTP y delegan la lógica al servicio.
- Las pruebas automatizadas cubren los casos permitidos y rechazados.
- Postman evidencia respuestas `200`, `201`, `204`, `400`, `401`, `403`, `404` y `409`.
