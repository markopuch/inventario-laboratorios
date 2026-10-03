# FE-09 — Reportes, indicadores y exportación

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Reportes implementados con cinco agregados reales y filtros; exportación y gráficos completos del mockup quedan fuera del alcance actual.**

## Implementación y verificación actuales

`Reportes.jsx` reemplaza el placeholder. Consume GET `/api/reportes/resumen`, `/equipos/por-estado`, `/equipos/por-laboratorio`, `/movimientos` y `/mantenimientos`. Muestra totales y tablas de agregados; no simula gráficos/exportación. ADMIN tiene alcance global; GESTOR/LECTOR reciben resultados según laboratorios autorizados.

Filtros: sede, área, laboratorio, estado de Equipo, estado/tipo de Mantenimiento y fechas. Rango invertido se rechaza localmente; el backend también valida. Equipos filtra fecha de registro, Movimientos fecha del movimiento con días UTC y Mantenimiento fechaProgramada. Estado/tipo de mantenimiento afecta solo sus indicadores. Errores/reintento se distinguen de respuestas válidas vacías.

El smoke Docker verificó los cinco endpoints filtrados. GESTOR vio por UI 1 Equipo, 1 Movimiento y 2 Mantenimientos de fixtures; LECTOR recibió 403 para laboratorio ajeno. Cinco pruebas de reportes se incluyen en las 67 frontend. Véase la [captura de Reportes Docker](../../frontend/version-jason/frontend/evidencias/docker-actual-reportes-2026-10-03.png).

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes conservan la inspección inicial del 3 de octubre de 2026, anterior a la adaptación V10–V13. Sus pendientes no sustituyen el estado vigente indicado arriba.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| Ruta `/reportes` | IMPLEMENTADA |
| Componente visual | IMPLEMENTADO mediante `ModuloVisual.jsx` |
| Encabezado | IMPLEMENTADO |
| Indicadores de reportes | PENDIENTES |
| Filtros de reportes | PENDIENTES |
| Exportación | PENDIENTE |
| Integración con endpoints específicos | PENDIENTE |

## Alcance confirmado

- Entrada de navegación.
- Título «Reportes».
- Subtítulo sobre indicadores y exportación.
- Superficie visual preparada para futura integración.

No se afirma que exista exportación PDF/Excel/CSV ni un motor de reportes.

## Referencia visual revisada

La referencia contempla indicadores y exportación. La versión actual mantiene la
estructura visual sin simular resultados.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Ruta y navegación | Implementado |
| 2 | Encabezado | Implementado |
| 3 | Placeholder visual | Implementado |
| 4 | Indicadores | Pendiente |
| 5 | Filtros | Pendiente |
| 6 | Exportación | Pendiente |
| 7 | Integración backend | Pendiente |
| 8 | Pruebas | Pendiente |

## Evidencia de implementación

`ModuloVisual.jsx` proporciona la superficie de Reportes y su icono correspondiente.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/ModuloVisual.jsx`.
- `src/App.jsx`.
- Mockup de Reportes, indicadores y exportación.
