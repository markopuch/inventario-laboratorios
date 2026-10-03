# FE-06 — Usuarios, roles y asignación de laboratorios

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Directorio y administración de usuarios implementados para ADMIN: creación, edición pública, rol, actividad, contraseña y asignaciones.**

## Implementación y verificación actuales

`/usuarios` muestra `Usuarios.jsx`; `/asignaciones` conserva el recorrido independiente por ID y ya no es el destino del alias `/usuarios`. Consume GET/POST `/api/admin/usuarios`, GET/PUT `/{id}`, PATCH `/{id}/rol`, PATCH `/{id}/estado`, PUT `/{id}/password` y GET/PUT `/{id}/laboratorios`. Username es inmutable, la lista incluye inactivos y el último ADMIN activo está protegido con 409. Guardar laboratorios reemplaza la lista del usuario consultado; cambiar ID invalida el resultado.

La adaptación comprobó alta/edición, rol, actividad y asignaciones por UI. El smoke Docker volvió a comprobar esos contratos por HTTP, incluida contraseña; no se automatizó el envío de contraseña nueva desde el diálogo de navegador. Autocambios permitidos refrescan perfil/alcance; desactivar la propia cuenta cierra sesión. Sin registro público/recuperación. Siete pruebas de usuarios y siete de asignaciones se incluyen en las 67 frontend.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes conservan el estado entregado. Las correcciones del
selector y de identidad al guardar se detallan en la auditoría.

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
