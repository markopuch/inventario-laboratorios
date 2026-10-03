# FE-07 — Movimientos e historial

Fecha de inicio: 1 de octubre de 2026.

**Estado actualizado: traslado e historial comprobados en Docker, con fecha, tipo, origen, destino, actor y motivo reales. Lectura histórica por origen y conservación tras baja comprobadas. No existen estados Pendiente/Completado en el contrato actual.**

## Evidencia vigente al 3 de octubre de 2026

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
comprobó traslado 201 → 206 e historial antes/después de la baja. LECTOR obtuvo
403 para el equipo fuera del alcance actual y 200 para el historial autorizado
por origen. Cinco consultas de inconsistencia devolvieron 0, incluidos movimientos
huérfanos y traslados con origen igual a destino.

La [auditoría de integración](auditoria-integracion.md) documenta las correcciones
del filtro de laboratorio y los KPIs. `tipoMovimiento` y `fechaMovimiento` son los
campos reales; Pendiente/Completado se retiraron. Actions aprobó las 27 pruebas de
Jason, incluidas 5 de fechas/orden/búsqueda de movimientos. Permanecen pendientes
rango de fechas, filtro por tipo, exportación y panel lateral del mockup.

## Registro de entrega original (histórico)

Los indicadores Pendientes/Completados mencionados abajo fueron un defecto de
interpretación del contrato en la entrega inicial; no describen el estado vigente.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/pantallas/Movimientos.jsx` | IMPLEMENTADO |
| Movimientos hoy | IMPLEMENTADO |
| Pendientes | IMPLEMENTADO |
| Completados | IMPLEMENTADO |
| Búsqueda | IMPLEMENTADA |
| Filtro por laboratorio | IMPLEMENTADO |
| Tabla de historial | IMPLEMENTADA |
| Traslado desde Equipos | IMPLEMENTADO mediante `movimientosApi.trasladar()` |

## Alcance confirmado

- Consulta de movimientos.
- Conteo de movimientos del día según fecha disponible.
- Conteo de pendientes y completados según estado recibido.
- Búsqueda sobre los datos cargados.
- Filtro por laboratorio.
- Tabla con fecha, equipo, origen, destino, actor y motivo.

## Referencia visual revisada

Se conserva el enfoque del mockup de historial y actividad, priorizando trazabilidad,
filtros y lectura tabular.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/movimientos` | historial |
| `GET /api/equipos/{id}/movimientos` | API disponible en cliente para historial por equipo |
| `POST /api/equipos/{id}/traslados` | traslado desde Equipos |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Historial | Implementado |
| 2 | KPIs | Implementado |
| 3 | Búsqueda | Implementado |
| 4 | Filtro por laboratorio | Implementado |
| 5 | Tabla | Implementado |
| 6 | Traslado | Implementado en Equipos |
| 7 | Pruebas de consistencia de estados | Pendiente |
| 8 | Verificación con backend | Pendiente de evidencia completa |

## Evidencia de implementación

`Movimientos.jsx` obtiene laboratorios para el filtro y movimientos para la tabla.
La fila se delega a `FilaMovimiento.jsx`.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Movimientos.jsx`.
- `src/componentes/FilaMovimiento.jsx`.
- `src/servicios/api.js`.
- Mockup de Movimientos, historial y actividad.
