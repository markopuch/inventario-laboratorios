# FE-03 — Equipos e inventario

Fecha de inicio: 1 de octubre de 2026.

**Estado actualizado: alta, consulta, edición, filtros, traslado y baja lógica comprobados mediante UI y HTTP en Docker. Se mantiene la autorización del backend y el alcance por laboratorio; quedan diferencias visuales y revisión exhaustiva de accesibilidad.**

## Evidencia vigente al 3 de octubre de 2026

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
ejecutó crear/leer/editar un equipo conservando código y laboratorio, traslado
201 → 206, consulta de historial y baja sin acciones disponibles. Comprobó filtros,
roles y alcance: LECTOR recibió 403 para el equipo fuera de su laboratorio actual.
Se usó una base temporal eliminada; no se alteraron equipos del inventario habitual.

La ejecución aprobó 33/33 HTTP adicionales y 24/24 aserciones del recorrido completo,
sin presentarlas como una nueva suite de Equipos. Actions aprobó las 27 pruebas
automatizadas de Jason antes de publicar. La comparación visual conserva una
diferencia real: Jason usa tarjetas/modal mientras el mockup propone tabla/página;
faltan ficha completa, adjuntos, paginación y exportación.

## Registro de entrega original (histórico)

La inspección inicial siguiente conserva sus afirmaciones. La
[auditoría de integración](auditoria-integracion.md) precisa los defectos corregidos
de permisos, campos inmutables y KPI de estado, y las diferencias con la referencia.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/pantallas/Equipos.jsx` | IMPLEMENTADO |
| Listado de equipos | IMPLEMENTADO mediante `equiposApi.listar()` |
| KPIs | IMPLEMENTADOS: Total equipos, Operativos, En mantenimiento |
| Búsqueda | IMPLEMENTADA por código, nombre, marca, modelo y laboratorio |
| Filtros | IMPLEMENTADOS por estado, laboratorio, subcategoría y mantenimiento |
| Alta/edición | IMPLEMENTADAS mediante modal |
| Baja | IMPLEMENTADA mediante `DELETE` |
| Traslado | IMPLEMENTADO mediante endpoint de movimientos |
| Permisos | Integrados con `puedeGestionar` del contexto de autenticación |

## Alcance confirmado

- Consulta de equipos según alcance del usuario.
- Alta y edición para usuarios con capacidad de gestión.
- Estado del equipo.
- Marca, modelo, series, año, orden de compra y ubicación interna.
- Subcategoría y laboratorio.
- Indicador «Requiere mantenimiento».
- Comentario.
- Baja de equipo.
- Traslado a otro laboratorio mediante motivo.

No se documenta como implementada una regla de negocio que solo pueda garantizar el
backend.

## Referencia visual revisada

Se conserva el enfoque del mockup de Equipos: encabezado, indicadores superiores,
barra de búsqueda/filtros y tarjetas de equipos.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/equipos` | listado y filtros |
| `POST /api/equipos` | alta |
| `PUT /api/equipos/{id}` | edición |
| `DELETE /api/equipos/{id}` | baja |
| `GET /api/laboratorios` | selector de laboratorio |
| `GET /api/subcategorias` | selector de subcategoría |
| `POST /api/equipos/{id}/traslados` | traslado |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Listado y KPIs | Implementado |
| 2 | Búsqueda y filtros | Implementado |
| 3 | Alta | Implementado |
| 4 | Edición | Implementado |
| 5 | Baja | Implementado |
| 6 | Traslado | Implementado |
| 7 | Permisos visuales | Implementado |
| 8 | Pruebas con backend | Pendiente de evidencia completa |
| 9 | Revisión responsive/teclado | Pendiente de comprobación |

## Evidencia de implementación

`Equipos.jsx` consume los servicios definidos en `src/servicios/api.js` y convierte
los identificadores seleccionados en valores numéricos antes de enviar alta/edición.

El traslado solicita laboratorio destino y motivo y utiliza `movimientosApi.trasladar()`.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Equipos.jsx`.
- `src/componentes/TarjetaEquipo.jsx`.
- `src/componentes/FiltroEquipo.jsx`.
- `src/servicios/api.js`.
- Mockup de Equipos del proyecto.
