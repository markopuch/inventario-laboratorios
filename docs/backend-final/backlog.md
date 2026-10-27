# Backlog y alcance vigente

Base funcional documentada al **3 de octubre de 2026**, commit `e1ce75a`.
Actualización documental del **27 de octubre**: se contrastó el estado de
publicación con la [memoria de despliegue](../memory/docker-deployment.md).
El [plan de cierre web y Markdown](../plan-cierre-web.md) recoge la prioridad
actual del usuario; las ampliaciones de este backlog requieren priorización.
El [cierre de Sprint 7](../sprints_realizados-backend/sprint-7.md) conserva su
alcance histórico V1–V9. Las ampliaciones posteriores ya forman parte de la
aplicación y no deben seguir presentándose como funciones pendientes.
La [evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
distingue publicación, despliegue y comprobaciones de funcionamiento.

## Funciones ya implementadas

| Tema | Estado vigente y evidencia |
|---|---|
| Paginación y límite de login | Publicados el 27 en 34d8033/7412831 y corrección ef31d65. Arrays compatibles sin page/size, q y conteos por alcance; cuota de login configurable con 429/Retry-After y espera UI. Regresión vigente y CI 410/107/28 aprobados, build 1681. Render Live, Vercel success y paginación/búsqueda/Auditoría públicas sobre 258 equipos/351 eventos comprobados. Cinco 401 directos a Render y sexto intento desde Vercel acreditan el contador agregado y espera 868→839 s con botón deshabilitado. [Guía](paginacion-login.md) e [informe cloud](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md). |
| OpenAPI y Postman V13 | Añadidos el 27 de octubre: Springdoc 3.2.0, 70 operaciones, JWT, DTO/filtros/errores y colección completa. Publicados en d8f2123, Actions 5/5 y Render Live; Swagger y Authorize/perfil 200 comprobados. [Guía](openapi.md) e [informe cloud](../despliegue/verificacion-swagger-render-2026-10-27.md). |
| Usuarios administrativos | ADMIN lista, consulta, crea y edita usuarios; cambia actividad, rol y contraseña; configura asignaciones. `AdminUsuarioController` y `AdminUsuarioService` protegen al último ADMIN activo. No hay registro público ni CRUD del catálogo Rol. |
| Mantenimiento | Entity/tabla desde V12; consulta, programación, edición y ciclo PROGRAMADO → EN_PROCESO → COMPLETADO/CANCELADO. Un mantenimiento en proceso bloquea edición, baja y traslado de Equipo. |
| Auditoría | Tabla V13, registro de acciones de los servicios y consulta/filtros exclusivamente ADMIN mediante `GET /api/admin/auditoria`. No es un versionado completo de cada entidad. |
| Auditoría en interfaz | Pantalla Jason ADMIN de solo lectura, seis filtros y fechas UTC; reutiliza la API existente. Verificada en diez comprobaciones simuladas y recorrido posterior real de ocho comprobaciones con API/BD, con participación de los cinco actores de la carga. |
| Reportes | Cinco consultas reales con filtros/alcance y CSV de agregados, sin endpoint nuevo. Cinco descargas comprobadas con API simulada; tres con datos reales contrastados con API. Movimientos/Mantenimientos vacíos mantuvieron descarga deshabilitada. |
| Importación local | 257 equipos de Excel importados; 31 estados vacíos pendientes. Base total 258 al preservar un registro previo BAJA. Cuentas demo y cinco actores en auditoría; guía y manifiesto privado de procedencia. |
| Contraseñas | Desde el 27 de octubre: al menos 15 caracteres Unicode y máximo 72 bytes UTF-8 para alta/restablecimiento/demo ausentes. Sin recortar claves, blancos solos rechazados. BCrypt y acceso con claves anteriores se conservan; sin migraciones. |
| Catálogos | Consulta administrativa de activos/inactivos y PATCH de actividad, además del CRUD y consultas globales activas. |
| Frontend | Cliente activo de Jason en React/Vite, adaptado a los contratos vigentes y servido por Nginx. La base de tutoría Marko y sus sprints son antecedentes conservados solo en el equipo local y en el historial; desde el 26 de octubre de 2026 sus carpetas quedan excluidas del seguimiento de Git y de nuevos clones. |
| Docker y Actions | Workflow actual con JUnit/PostgreSQL, frontend e importador antes de publicar; cierre 5/5 aprobado e imágenes en GHCR. Las imágenes 58ac8f6 aprobaron después 22 comprobaciones Docker aisladas, incluidas siete de UI. El stack principal anterior se conserva saludable con su base independiente; no se actualizó ni recibió la carga nativa. La prueba e1ce75a del 3 de octubre sigue histórica. |

Estas funciones se implementaron **después** del cierre Sprint 7. Reconocerlas
en documentación no modifica los resultados ni las restricciones de aquel sprint.

## Operación y ampliaciones por priorizar

| Tema | Estado y trabajo por definir |
|---|---|
| Nube | Bloque publicado y comprobado: ef31d65 con CI 410/107/28, Vercel success y Render Live tras reconstrucción limpia dep-dbgfo1mi0phs73dt8020, 4m52s y startup 171.8 s. API cloud final 59/59: cinco 401 y sexto 429, Retry-After 893 s/no-store/ApiError y login ADMIN posterior 200. Páginas/búsqueda/Auditoría/alcance/proxy correctos; 258 equipos, seis usuarios y 351 auditorías conservados. La discrepancia inicial quedó resuelta sin causa exacta demostrada. Mantener respaldo/retención y verificar nuevamente cuando se opere. [Informe actual](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md). |
| Operación del límite de login | Corrección ef31d65 implementada y comprobada entre Render/Vercel: cinco fallos/900s por username normalizado, independiente del peer; cuota 60/60s por peer conservada. 410 pruebas backend locales y CI aprobados. Un ataque al username puede imponer espera al dueño. Contadores por proceso/reinicio, sin bloqueo BD; distribución entre réplicas requiere infraestructura adicional si se adopta ese despliegue. |
| Refresh token | No existe; el vencimiento actual requiere nuevo login. Su política de renovación y revocación requiere una decisión de producto. |
| Permisos dinámicos | No existe administración dinámica de permisos; siguen ADMIN/GESTOR/LECTOR y UsuarioLaboratorio. |
| Paginación de otros listados | Equipos/Auditoría tienen código publicado; el backend paginado ya se comprobó en Render. Catálogos, movimientos y otros listados conservan sus contratos. Priorizar ampliación según volumen y consumidores. |
| CRUD de Rol | No hay administración del catálogo Rol por API; asignar uno de los tres roles a un Usuario sí está implementado. |
| Versionado histórico completo | Movimientos conservan el hecho del traslado y auditoría registra acciones; no reconstruyen cada versión de nombres/datos. |
| Configuración persistida | Sin contrato backend; el cierre del 26 de octubre retira menú y ruta del placeholder. Definir opciones y persistencia solo si se prioriza esta ampliación. |
| Otros formatos de reportes | CSV de agregados incluido en el cierre del 26 de octubre; PDF/XLSX y exportación detallada siguen pendientes de priorización. No hay endpoint nuevo de exportación. |
| Kubernetes / operación avanzada | Sin evidencia de implementación; no se necesitan para el Docker local comprobado. |

Un pendiente de esta tabla es **BACKLOG**, no un bug de una función existente.
Las mejoras opcionales no justifican renumerar reglas, modificar migraciones
aplicadas o cambiar contratos sin autorización.

## Contratos que se mantienen

Equipo BAJA no se reactiva ni se borra físicamente. PUT de Equipo conserva
código interno y laboratorio. Los movimientos no tienen POST genérico, PUT ni
DELETE. La custodia no concede permisos. El historial se consulta por alcance
actual de origen o destino; mantenimiento usa el laboratorio actual del Equipo.

El smoke Docker de 94 comprobaciones HTTP y 30 aserciones funcionales no se suma
a JUnit ni a las 67 pruebas frontend. La evidencia previa de 319 JUnit se documenta
por separado de las 233 históricas del cierre de Sprint 7. El 26 de octubre se
aprobaron **90/90 tests frontend** y diez comprobaciones simuladas, y después
ocho comprobaciones con API/BD reales. El 27 de octubre se aprobaron **93/93
tests frontend** y build de 1679 módulos. La regresión backend en PostgreSQL
temporal aprobó **365/365**, con JAR correcto; importador **28/28**.
El commit 58ac8f6 se publicó en main y GitHub Actions aprobó los cinco trabajos,
repitió esas pruebas y publicó ambas imágenes; la inspección independiente de
cuatro etiquetas GHCR confirmó sus digests. Ver
[anexo del cierre](../despliegue/verificacion-cierre-2026-10-27.md).
