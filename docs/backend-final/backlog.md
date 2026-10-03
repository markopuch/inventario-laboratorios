# Backlog y alcance vigente

Estado documental al **3 de octubre de 2026**, commit `e1ce75a`.
El [cierre de Sprint 7](../sprints_realizados-backend/sprint-7.md) conserva su
alcance histórico V1–V9. Las ampliaciones posteriores ya forman parte de la
aplicación y no deben seguir presentándose como funciones pendientes.
La [evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
distingue publicación, despliegue y comprobaciones de funcionamiento.

## Funciones ya implementadas

| Tema | Estado vigente y evidencia |
|---|---|
| Usuarios administrativos | ADMIN lista, consulta, crea y edita usuarios; cambia actividad, rol y contraseña; configura asignaciones. `AdminUsuarioController` y `AdminUsuarioService` protegen al último ADMIN activo. No hay registro público ni CRUD del catálogo Rol. |
| Mantenimiento | Entity/tabla desde V12; consulta, programación, edición y ciclo PROGRAMADO → EN_PROCESO → COMPLETADO/CANCELADO. Un mantenimiento en proceso bloquea edición, baja y traslado de Equipo. |
| Auditoría | Tabla V13, registro de acciones de los servicios y consulta/filtros exclusivamente ADMIN mediante `GET /api/admin/auditoria`. No es un versionado completo de cada entidad. |
| Reportes | Cinco consultas reales de resumen, equipos, movimientos y mantenimiento; filtros y alcance en servidor. |
| Catálogos | Consulta administrativa de activos/inactivos y PATCH de actividad, además del CRUD y consultas globales activas. |
| Frontend | Cliente activo de Jason en React/Vite, adaptado a los contratos vigentes y servido por Nginx. La base frontend inicial de Marko se conserva como antecedente. |
| Docker y Actions | PostgreSQL, backend y frontend saludables; imágenes `sha-e1ce75a` publicadas en GHCR y desplegadas localmente. El workflow construye/publica ambos componentes. |

Estas funciones se implementaron **después** del cierre Sprint 7. Reconocerlas
en documentación no modifica los resultados ni las restricciones de aquel sprint.

## Pendientes de despliegue y producto

| Tema | Estado y trabajo por definir |
|---|---|
| Nube | Render u otra plataforma, base gestionada y publicación remota del frontend aún sin evidencia de despliegue. Definir variables privadas, red, backups y operación antes de publicar. |
| Refresh token | No existe; el vencimiento actual requiere nuevo login. Su política de renovación y revocación requiere una decisión de producto. |
| Permisos dinámicos | No existe administración dinámica de permisos; siguen ADMIN/GESTOR/LECTOR y UsuarioLaboratorio. |
| Paginación | Listas sin contrato nuevo de paginación; evaluar solo si el volumen lo exige. |
| CRUD de Rol | No hay administración del catálogo Rol por API; asignar uno de los tres roles a un Usuario sí está implementado. |
| Versionado histórico completo | Movimientos conservan el hecho del traslado y auditoría registra acciones; no reconstruyen cada versión de nombres/datos. |
| Configuración persistida | No hay contrato backend de persistencia de preferencias/configuración de la pantalla frontend; definirlo si se requiere. |
| Auditoría en interfaz | La consulta API ADMIN ya existe; una pantalla Jason para consultarla es una ampliación de interfaz pendiente. |
| Exportación de reportes | Los cinco GET y sus filtros ya existen; exportación CSV/PDF u otro formato requiere priorización y un contrato propio si aplica. |
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
por separado de las 233 históricas del cierre de Sprint 7. No se ejecutó una suite
nueva durante esta actualización de Markdown.
