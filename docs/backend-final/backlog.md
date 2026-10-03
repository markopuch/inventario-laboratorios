# Backlog fuera del backend cerrado

Sprint 7 cierra el backend de inventario, asignaciones, Equipos y traslados.
Este documento distingue ampliaciones futuras de defectos de lo implementado.
No activa rutas ni permisos y no supone autorización para implementar nuevos
módulos. El [inventario de auditoría](inventario-auditoria.md) registra el alcance
revisado y la [verificación final](verificacion-final.md) su evidencia.

**Actualización posterior al cierre de Sprint 7 — 2026-10-03:** el frontend de
Jason ya está integrado y Docker local + publicación de ambas imágenes en GHCR
están verificados. No cambian las funciones ni los contratos del backend cerrado.
La [evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
y el [Sprint 8](../sprints_realizados-backend/sprint-8.md) registran esos avances.

## Ampliaciones de producto

| Tema | Estado actual | Trabajo futuro por definir |
|---|---|---|
| Administración completa de usuarios | JPA/JWT, perfil propio y asignaciones implementados; sin CRUD Usuario/Rol | Alta/edición, actividad de cuentas, políticas de correo/contraseña y cambios de rol |
| Mantenimiento | Estado MANTENIMIENTO y bandera/filtro de Equipo disponibles | Entity, órdenes, responsables, programación e historial de mantenimiento |
| Auditoría general | Movimiento registra actor/origen/destino/motivo/fecha de traslado | Auditoría transversal y, si se requiere, versiones de los datos de las entidades |
| Frontend | Cliente Jason integrado: autenticación, roles/alcance y operaciones de inventario; flujo Docker validado y 27 pruebas aprobadas en Actions | Evolución de interfaz y módulos nuevos únicamente cuando se prioricen; publicación del cliente en la nube si se requiere |
| Docker/despliegue | PostgreSQL, backend y frontend/Nginx operativos en Compose; imágenes separadas y dos trabajos Actions con publicación GHCR confirmada | Base gestionada, backend en Render u otra plataforma, operación y monitoreo del entorno remoto |
| Refresh token | JWT con vencimiento; nuevo login cuando vence | Política de renovación, revocación y ciclo de sesión |
| Permisos dinámicos | Tres roles del contrato actual y alcance de laboratorio | Administración de permisos y su modelo, si el producto lo necesita |

La administración propuesta de usuarios tiene estado **DOCUMENTAL/DISEÑO** y
su implementación está **PENDIENTE FUERA DEL BACKEND CERRADO**. La unicidad del
correo ya existe en PostgreSQL, pero hoy no hay un request que reciba email:
validar su formato es **NO APLICA** a la API vigente. No se agregan endpoints
ficticios a la matriz implementada para representar estas propuestas.

## Mejoras técnicas opcionales

| Mejora | Razón | Límite del cierre actual |
|---|---|---|
| Paginación | Listados crecerán con el uso | No se cambia el contrato de listas existente |
| Uniformidad de IDs inválidos | Categoría devuelve 404 para IDs enteros inexistentes, incluso no positivos | Los módulos con Positive devuelven 400; se documenta la diferencia |
| Evaluar índices redundantes | Conviven UNIQUE originales e índices funcionales posteriores | Se conservan V1–V9; cualquier cambio requiere evidencia y migración futura |
| Snapshot histórico de nombres | Hoy los resúmenes muestran datos actuales de entidades referidas | Movimiento conserva el hecho y sus FK, no versiones de nombres |

No se detectó un bug funcional demostrado que obligue a implementar estas
mejoras para cerrar Sprint 7. Los resultados medidos de la regresión de Sprint 7
constan en el documento de verificación; una mejora de diseño no sustituye
esa evidencia.

## Decisiones que permanecen

No hay reactivación de Equipo BAJA, borrado físico ni edición/eliminación de
Movimientos por API. PUT no cambia código interno ni laboratorio de Equipo.
El responsable no concede permisos. Los catálogos tienen lectura global y el
alcance de Equipos/historia se calcula con asignaciones actuales.

Estas decisiones son contratos del backend cerrado. Revisarlas requiere una
necesidad explícita y un nuevo sprint; no se presentan como fallos pendientes.
El frontend y el empaquetado Docker ya avanzaron después de ese cierre. El
próximo trabajo de despliegue corresponde a la nube; las ampliaciones de negocio
de esta lista siguen requiriendo priorización. Las verificaciones HTTP de Docker
y las pruebas del frontend no sustituyen ni se suman a las 233 pruebas Gradle.
