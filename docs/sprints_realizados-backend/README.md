# Estado de los sprints del backend

Revisión documental: **3 de octubre de 2026**.
Versión desplegada en Docker local: **`e1ce75a`**, Flyway **V1–V13**.

Este índice distingue los cierres históricos de las ampliaciones posteriores.
La numeración de frontend Jason es independiente; no se asigna un nuevo número
de sprint a la ampliación del backend mediante esta actualización documental.

## Sprints documentados

| Registro | Alcance | Estado |
|---|---|---|
| [Sprint 3](sprint-3-categorias.md) | Categorías y validaciones | Implementado; conserva su evidencia histórica |
| [Sprint 4](sprint-4.md) | Consolidación de catálogos, organización y alcance | Implementado; detalles en [4A](sprint-4a-subcategorias.md), [4B](sprint-4b-organizacion.md) y [4E](sprint-4e-usuario-laboratorio.md) |
| [Sprint 5](sprint-5.md) | Equipo, filtros, alcance y baja lógica | Implementado; [guía de Equipo](sprint-5-equipos.md) |
| [Sprint 6](sprint-6.md) | Traslado transaccional, historial y concurrencia | Implementado; [guía de movimientos](sprint-6-movimientos.md) |
| [Sprint 7](sprint-7.md) | Auditoría y cierre del alcance original | Cerrado históricamente con V9, 10 entidades, 42 operaciones y 233 pruebas |
| [Sprint 8](sprint-8.md) | Docker, GitHub Actions y GHCR | PostgreSQL/backend/Jason integrados; ambas imágenes publicadas y actualización local comprobada |
| [Ampliación posterior](../../backend/inventario/ACTUALIZACION-BACKEND.md) | Catálogos administrativos, estado operativo, usuarios, mantenimiento, reportes y auditoría | Implementada después de Sprint 7; integrada con Jason y desplegada con V13 |

Los contratos y restricciones antiguas de cada cierre describen aquel momento.
El [catálogo vigente](../backend-final/endpoints.md) contiene **70 operaciones**;
el [modelo V13](../Erd_actual/modelo-vigente-v13.md), **12 entidades y 16 FK**.
Usuarios administrativos, mantenimiento y auditoría dejaron de ser backlog
mediante la ampliación posterior; no se atribuyen retroactivamente a Sprint 7.

## Última evidencia

| Evidencia | Resultado y alcance |
|---|---|
| [Actions `e1ce75a`](../despliegue/evidencias/actions-ghcr-e1ce75a-2026-10-03.json) | Backend y frontend Jason terminados con success; imágenes publicadas |
| [Reportes backend previos](../despliegue/evidencias/reportes-backend-2026-10-03.json) | 319 pruebas en 40 XML existentes, 0 fallos/errores/omitidas; su lectura no acredita una nueva ejecución ni el commit exacto |
| [Adaptación Jason](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) | 67 pruebas frontend y build aprobados en la adaptación previa |
| [Flujo Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) | 94 comprobaciones HTTP, 30 aserciones funcionales y siete controles SQL sin inconsistencias |

Los contenedores habituales quedaron saludables. La actualización aplicó V10–V13,
conservó los checksums V1–V9, el contenedor/volumen de PostgreSQL y los registros
anteriores, incluidas cuatro asignaciones. Se guardó un respaldo privado.
Las escrituras del smoke utilizaron únicamente una base temporal: se eliminaron
esa base, sus contenedores, volumen y red, y se liberaron sus puertos.

Las cifras de JUnit, frontend y HTTP son ejecuciones o comprobaciones distintas;
no se suman. El job backend construye con `bootJar -x test`.
Los límites UI/API y el detalle de preservación están en el
[reporte consolidado de Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md).

## Pendientes vigentes

El despliegue en la nube no está acreditado. Configuración persistida, interfaz
de Auditoría, exportación de reportes, refresh tokens y otras ampliaciones
permanecen en el [backlog](../backend-final/backlog.md).
Esta actualización no inicia otro sprint, no modifica funcionalidades y no
repite pruebas ni despliegues.
