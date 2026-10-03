# FE-07 — Movimientos e historial

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Traslado e historial reales comprobados; LECTOR conserva historia por origen después del traslado y de la baja.**

## Implementación y verificación actuales

El traslado Docker actual se hizo desde interfaz. Origen y actor los determina el servidor: `idUsuarioActor` enviado por cliente recibió 400 y origen=destino 409. Historial devuelve fecha, tipo TRASLADO, actor, origen, destino y motivo; permanece tras BAJA. LECTOR consultó la historia por el origen autorizado aun sin poder leer el equipo en su ubicación actual. La pantalla mantiene búsqueda y filtro de laboratorio. No tiene estados Pendiente/Completado; rango de fechas/tipo y exportación de esta pantalla siguen sin implementarse, aunque Reportes ya ofrece filtros propios.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

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
