# FE-03 — Equipos e inventario

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: **27 de octubre de 2026**. Docker `e1ce75a` conserva
sus resultados históricos del 3 de octubre.

**Estado vigente: CRUD, filtros, traslado, baja lógica, paginación y búsqueda
de equipos en servidor integrados y publicados.**

## Implementación y verificación actuales

Equipos usa `listarPagina`, con tamaños 20/50/100, anterior/siguiente, totales y
búsqueda `q` en todo el conjunto filtrado/autorizado antes del conteo. Cambiar
filtros, búsqueda o tamaño vuelve a página cero; cancela solicitudes y descarta
respuestas obsoletas. Auditoría también pagina. Los GET conservan arrays sin
`page/size`; dashboard, selectores e importador no cambian de contrato.

Regresión acreditada: **410/410 backend, 107/107 frontend**, build de **1681
módulos** y CI aprobado. `ef31d65` quedó Live tras reconstrucción limpia Render
`dep-dbgfo1mi0phs73dt8020` (4m52s), Vercel success y API cloud **59/59**.
La UI pública verificó tamaños, segunda página y búsqueda fuera de la primera
con 258 equipos; Auditoría se comprobó sobre 351 eventos.
[Guía](../backend-final/paginacion-login.md) e
[informe cloud](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md).
Jason conserva tarjetas/modal; tabla/página de alta, adjuntos y exportación
detallada del mockup continúan fuera del alcance implementado.

## Evidencia histórica del 3 de octubre

Se comprobó código interno inmutable, traslado L201 → L206, historia conservada y Equipo BAJA sin botones de mutación en la interfaz GESTOR. LECTOR recibió 403 al consultar el equipo fuera de alcance. Durante mantenimiento EN_PROCESO, edición/baja/traslado devolvieron 409. La confirmación nativa de baja bloqueó la automatización del navegador; se canceló y la operación se realizó por API. Esto no acredita el clic final de baja por UI en esa ejecución.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

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
