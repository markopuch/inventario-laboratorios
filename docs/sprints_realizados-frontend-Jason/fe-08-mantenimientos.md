# FE-08 — Mantenimientos

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Mantenimiento de Equipo implementado e integrado; programación, edición y seguimiento usan API real y alcance.**

## Implementación y verificación actuales

`Mantenimientos.jsx` reemplaza el placeholder. ADMIN y GESTOR autorizados crean/gestionan; LECTOR consulta dentro del alcance. Consume GET/POST `/api/mantenimientos`, GET/PUT `/{id}` y PATCH `/{id}/estado`. Filtros: Equipo, Laboratorio, Estado, Tipo y fechas. Tipos: PREVENTIVO, CORRECTIVO, CALIBRACION y OTRO.

Estados: PROGRAMADO → EN_PROCESO → COMPLETADO; PROGRAMADO o EN_PROCESO pueden pasar a CANCELADO. El formulario ofrece transiciones válidas; edición mientras está PROGRAMADO. El servidor conserva fechas/observaciones; no existe DELETE de mantenimiento. Al iniciar, Equipo pasa a MANTENIMIENTO y edición/baja/traslado reciben 409; completar/cancelar restaura el estado previo.

En Docker actual se programó/inició/completó mediante interfaz y se verificó otra cancelación por HTTP. Ocho pruebas de mantenimiento se incluyen en las 67 frontend. El calendario del mockup sigue fuera de la interfaz actual; el módulo funcional ya no es placeholder. Véase la [captura de mantenimiento](../../frontend/version-jason/frontend/evidencias/mantenimientos-2026-10-03.png).

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes conservan la inspección inicial del 3 de octubre de 2026, anterior a la adaptación V10–V13. Sus pendientes no sustituyen el estado vigente indicado arriba.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| Ruta `/mantenimientos` | IMPLEMENTADA |
| `src/pantallas/ModuloVisual.jsx` | IMPLEMENTADO como componente visual reutilizable |
| Título y subtítulo | IMPLEMENTADOS |
| Iconografía del módulo | IMPLEMENTADA |
| CRUD/programación | PENDIENTE |
| Calendario | PENDIENTE |
| Historial de mantenimiento | PENDIENTE |
| Endpoints específicos | NO IMPLEMENTADOS en el cliente actual |

## Alcance confirmado

- Entrada de navegación.
- Presentación visual del módulo.
- Mensaje que identifica la preparación de la estructura.

No se debe documentar como implementado el calendario, programación o seguimiento
real del mantenimiento.

## Referencia visual revisada

La referencia de mantenimiento contempla programación y calendario. La implementación
actual deja preparada la superficie visual, sin inventar datos.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Ruta y navegación | Implementado |
| 2 | Encabezado | Implementado |
| 3 | Placeholder visual | Implementado |
| 4 | Programación | Pendiente |
| 5 | Calendario | Pendiente |
| 6 | Persistencia/API | Pendiente |
| 7 | Pruebas | Pendiente |

## Evidencia de implementación

`ModuloVisual.jsx` selecciona el icono de Mantenimientos y muestra que el módulo
puede conectarse cuando existan endpoints disponibles.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/ModuloVisual.jsx`.
- `src/App.jsx`.
- Mockup de Mantenimientos.
