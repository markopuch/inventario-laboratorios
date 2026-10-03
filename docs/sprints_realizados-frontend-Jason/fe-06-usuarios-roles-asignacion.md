# FE-06 — Usuarios, roles y asignación de laboratorios

Fecha de inicio: 1 de octubre de 2026.

**Estado: frontend parcial implementado. La ruta Usuarios presenta actualmente la gestión de asignaciones de laboratorios por ID de usuario; no existe un CRUD completo de usuarios en la versión entregada.**

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| Ruta `/usuarios` | IMPLEMENTADA |
| `src/pantallas/Asignaciones.jsx` | IMPLEMENTADO |
| Consulta de laboratorios de un usuario | IMPLEMENTADA |
| Visualización de usuario/rol | IMPLEMENTADA |
| Actualización de asignaciones | IMPLEMENTADA |
| CRUD de usuarios | NO IMPLEMENTADO |
| Alta/edición de usuarios | NO IMPLEMENTADO |
| Cambio de rol | NO IMPLEMENTADO en esta pantalla |
| Gestión completa de cuentas | NO IMPLEMENTADA |

## Alcance confirmado

- Introducción de ID de usuario.
- Consulta de datos del usuario y laboratorios asignados.
- Visualización de nombre, apellido, usuario y rol.
- Actualización de los IDs de laboratorios asignados.
- Mensajes básicos de error.

El nombre de la pantalla visible es «Asignaciones», aunque la navegación principal
la expone bajo «Usuarios». Esto documenta el estado real y no equivale a un CRUD
de usuarios.

## Referencia visual revisada

La referencia contempla usuarios, roles y asignaciones. El frontend entregado
implementa únicamente el recorrido de asignación que tiene endpoints disponibles
en el cliente.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/admin/usuarios/{id}/laboratorios` | consultar asignaciones |
| `PUT /api/admin/usuarios/{id}/laboratorios` | reemplazar asignaciones |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Ruta Usuarios | Implementado |
| 2 | Consulta por ID | Implementado |
| 3 | Resumen de usuario/rol | Implementado |
| 4 | Consulta de laboratorios | Implementado |
| 5 | Guardado de asignaciones | Implementado |
| 6 | CRUD de usuarios | Pendiente |
| 7 | Administración de roles | Pendiente |
| 8 | Pruebas con permisos ADMIN | Pendiente de evidencia |

## Evidencia de implementación

`Asignaciones.jsx` utiliza `usuariosApi.laboratorios()` y
`usuariosApi.actualizarLaboratorios()`.

No se debe presentar esta pantalla como administración completa de usuarios.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Asignaciones.jsx`.
- `src/servicios/api.js`.
- Mockup de Usuarios, roles y asignación.
