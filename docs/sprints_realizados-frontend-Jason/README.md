# Sprints realizados — Frontend Jason

Estado funcional actualizado el **26 de octubre de 2026**. La evidencia Docker
del 3 de octubre corresponde a `e1ce75a` y se conserva como antecedente.

Esta carpeta registra el frontend de `frontend/version-jason/frontend/`. Cada FE presenta primero su estado vigente y después la entrega original como historia. Los resultados de Marko no certifican Jason. Los sprints organizan trabajo por módulos; no equivalen por sí solos a aprobación completa de cada mockup o escenario.

**Organización desde el 26 de octubre de 2026:** la variante de tutoría Marko y
sus sprints se conservan solo en el equipo local, excluidos del seguimiento de Git
y de nuevos clones. Las referencias a ese material son antecedentes históricos;
esta carpeta continúa documentando únicamente Jason.

## Estado actual

| Bloque | Estado y evidencia |
|---|---|
| [FE-01 · Login/sesión](fe-01-login-sesion.md) | Integrado; tres roles, JWT en memoria, alcance, 401 y refresco de perfil ante autocambios administrativos. |
| [FE-02 · Dashboard](fe-02-dashboard.md) | Equipos, laboratorios autorizados, movimientos y resumen real de mantenimiento; errores diferenciados de ceros. |
| [FE-03 · Equipos](fe-03-equipos.md) | CRUD, filtros, baja lógica y traslado. Docker actual: alta/edición/traslado por UI y baja por HTTP; GESTOR vio BAJA sin acciones. |
| [FE-04 · Catálogos](fe-04-categorias-subcategorias.md) | Categorías/subcategorías; activos/inactivos administrativos, inactivación y reactivación reales. |
| [FE-05 · Organización](fe-05-laboratorios-sedes-areas.md) | Sedes/áreas/laboratorios; jerarquía, actividad y estadoOperativo independiente de activo. |
| [FE-06 · Usuarios/roles/asignaciones](fe-06-usuarios-roles-asignacion.md) | Administración ADMIN: directorio, creación, edición, rol, estado, contraseña y laboratorios. Último ADMIN protegido; acceso propio a Asignaciones en menú ADMIN. |
| [FE-07 · Movimientos](fe-07-movimientos.md) | Traslado confirmado e historial con actor/origen/destino; lectura por origen tras traslado/baja. |
| [FE-08 · Mantenimientos](fe-08-mantenimientos.md) | Programación, edición, estados, filtros y alcance implementados. Calendario fuera del alcance actual. |
| [FE-09 · Reportes](fe-09-reportes.md) | Cinco agregados y filtros reales; cinco descargas CSV con filtros aplicados y alcance. Gráficos completos y otros formatos pendientes. |
| [FE-10 · Configuración](fe-10-configuracion.md) | Menú y ruta retirados. Preferencias persistentes y seguridad configurable futuras. |
| [FE-11 · Arquitectura](fe-11-arquitectura-frontend.md) | React/Vite/Axios, routing, sesión, proxy y Nginx integrados; navegación ADMIN de Usuarios, Asignaciones y Auditoría. Fidelidad visual y accesibilidad exhaustivas no certificadas. |
| [Auditoría ADMIN](../frontend/guia-reportes-auditoria.md) | Pantalla de consulta con filtros, fechas UTC y visualización en Lima; reutiliza API existente, solo lectura. |

## Verificación del cierre del 26 de octubre

**90/90 tests frontend** aprobados, build de **1679 módulos** y diez
comprobaciones de navegador local con **API simulada**, sin errores de página.
Incluyen menú/rutas de los tres roles, Auditoría/filtros/errores, cinco descargas
CSV y sus estados, y móvil de 390 px. No se probaron backend/BD reales ni se
publicaron los cambios. Ver [detalle y límites](../frontend/guia-reportes-auditoria.md#verificación-local-del-26-de-octubre)
y [registro de verificación](../memory/verification-2026-10-26.md).

## Evidencia histórica: 3 de octubre

| Comprobación | Resultado documentado |
|---|---|
| Adaptación frontend | 67/67 pruebas y build aprobado con 1677 módulos; evidencia anterior al reemplazo Docker. |
| GitHub Actions | Ambos trabajos exitosos en [37158784428](https://github.com/markopuch/inventario-laboratorios/actions/runs/37158784428), commit e1ce75a. |
| Imágenes | Backend y frontend-jason publicados con sha-e1ce75a y desplegados en Docker local. |
| Habitual | Tres contenedores saludables; frontend 3000, backend 8080. Flyway V1–V13; checksums V1–V9 conservados. |
| Smoke actual | 94/94 HTTP, 30 aserciones funcionales y siete controles SQL sin inconsistencias, aislado con las mismas imágenes. |
| Preservación | Nueve tablas anteriores sin cambios de filas/conteos, incluidas cuatro asignaciones. Contenedor/volumen PostgreSQL conservados y respaldo privado previo. |
| Limpieza | Base inventario_verificacion_docker_actual_5149b3ad y sus contenedores/volumen/red eliminados; puertos temporales libres. |
| Alcance | Login y escrituras en entorno aislado. Habitual: salud, protección HTTP, Flyway y preservación. npm/Gradle no se repitieron en el despliegue. |
| Límite UI | Confirmación nativa de baja bloqueó automatización; se canceló y baja se comprobó por API. LECTOR vio historia después del traslado/baja. |
| Nube | GHCR y Docker local no acreditan una URL pública desplegada. |

HTTP/aserciones/SQL no son endpoints distintos ni se suman a frontend o JUnit. Esta actualización Markdown no ejecuta de nuevo las pruebas.

## Documentos y evidencia

- [Guía de ejecución y pruebas manuales](../../frontend/version-jason/frontend/README.md).
- [Guía de Reportes CSV, Auditoría y Asignaciones](../frontend/guia-reportes-auditoria.md).
- [Roles y alcance: matriz y gráfico](../matriz-permisos.md).
- [Auditoría de integración](auditoria-integracion.md): estado vigente, hallazgos iniciales y límites.
- [Informe Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md): publicación y actualización local.
- [Adaptación V13 y 67 pruebas](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json).
- [Smoke Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json).
- [Reportes Docker](../../frontend/version-jason/frontend/evidencias/docker-actual-reportes-2026-10-03.png) e [historial LECTOR](../../frontend/version-jason/frontend/evidencias/docker-actual-historial-2026-10-03.png).
- [Referencias visuales](../frontend/mockup/): objetivos de diseño sin declarar reproducción 1:1.

## Antecedentes del 3 de octubre de 2026

Antes de la ampliación V10–V13 se registraron:

1. Auditoría Vite inicial: 27/27 pruebas frontend, build de 1670 módulos y 42 HTTP; base inventario_verificacion_jason_91d7ac79, eliminada.
2. Primer Docker V9: 33/33 HTTP y 24/24 aserciones de flujo/consistencia; base inventario_verificacion_docker_26e35749, eliminada. Cinco controles SQL en cero y habitual preservada.
3. Publicación anterior: Actions [37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900), commit 9956939; 27/27 frontend antes de publicar ambas imágenes.

En esa entrega Usuarios solo ofrecía asignaciones y Mantenimientos/Reportes eran placeholders. Estos límites fueron superados por la adaptación; Configuración permanece futura. Las 27/42/33 se conservan como ejecuciones diferentes, sin sumarlas como suite nueva ni a las 233 pruebas históricas del backend.

## Criterio documental

IMPLEMENTADO requiere código; COMPROBADO POR EJECUCIÓN requiere escenario,
entorno y resultado. FUTURO indica funcionalidad sin recorrido actual.
Exportación CSV de agregados se incorpora en el cierre del 26 de octubre;
otros formatos, calendario y accesibilidad exhaustiva no se presentan como
terminados. No se modifican los resultados históricos al documentar el código nuevo.
