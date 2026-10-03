# FE-07 — Movimientos e historial

Fecha de inicio: 1 de octubre de 2026.

**Estado: implementación frontend realizada. Historial conectado al backend, con KPIs, búsqueda y filtro por laboratorio; el traslado se ejecuta desde Equipos mediante el mismo cliente de movimientos.**

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
