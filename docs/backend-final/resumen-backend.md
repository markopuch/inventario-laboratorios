# Resumen del backend — estado vigente

**Actualizado el 3 de octubre de 2026.** Referencia: commit `e1ce75a`.
El backend y el frontend Jason de ese commit están publicados en GHCR y
desplegados en Docker local con PostgreSQL. Los tres servicios quedaron saludables.
Este documento describe el estado actual; el [Sprint 7](../sprints_realizados-backend/sprint-7.md)
y su [verificación final](verificacion-final.md) conservan el cierre histórico
V9/233 pruebas. La [evidencia vigente](../despliegue/verificacion-docker-actions-2026-10-03.md)
separa las mediciones de cada etapa.

## Qué hace

Inventario de Laboratorios organiza Sedes, Áreas y Laboratorios; clasifica
Equipos; controla estado, custodia y ubicación; administra usuarios y sus
asignaciones; registra traslados y mantenimiento; ofrece reportes y consulta
administrativa de auditoría.

Utiliza Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway, JPA/Hibernate,
Spring Security, JWT, BCrypt, MapStruct, Lombok y Gradle. Conserva la arquitectura
y los patrones del curso:

`Cliente → Security/JWT → Controller → DTO → Mapper → Domain → Service → Repository → Entity/JPA → PostgreSQL`.

Controllers coordinan HTTP, Mappers convierten datos y Services concentran
reglas, autorización y transacciones. La API usa DTOs públicos y errores
controlados; no expone Entities, hashes, SQL ni stack traces.

## Modelo y persistencia

El modelo actual tiene **12 entidades/tablas y 16 FK**: Sede, Área, Laboratorio,
Categoría, Subcategoría, Rol, Usuario, UsuarioLaboratorio, Equipo,
MovimientoEquipo, Mantenimiento y Auditoría. El
[modelo vigente V13](../Erd_actual/modelo-vigente-v13.md) presenta las relaciones.
El ERD v2 y sus SVG conservan la instantánea de 10 entidades/V1–V9.

Flyway es la fuente del esquema. V10 añade el estado operativo de Laboratorio;
V11 refuerza la unicidad del correo sin distinguir mayúsculas; V12 incorpora
Mantenimiento; V13 incorpora Auditoría. Las 13 migraciones quedaron exitosas
en Docker y los checksums V1–V9 no cambiaron. Hibernate usa
`ddl-auto=validate`, con Flyway habilitado y `open-in-view=false`.

Estado operativo de Laboratorio (OPERATIVO/MANTENIMIENTO) es independiente
de `activo`. Un laboratorio activo en mantenimiento no pierde su identidad
ni su asignabilidad solamente por ese estado.

## Funciones y seguridad

Hay **70 combinaciones método y ruta de aplicación**, verificables en 19
Controllers: AUTH 3; Categoría 7; Subcategoría 6; Sede 7; Área 7; Laboratorio 6;
Catálogos ADMIN 5; Usuario ADMIN 7; UsuarioLaboratorio 2; Equipo 6;
Movimiento 3; Mantenimiento 5; Reportes 5; Auditoría 1.
Filtros y variantes de roles no aumentan el total; Actuator es infraestructura.
El [catálogo](endpoints.md) detalla solicitudes, respuestas y permisos.

| Rol | Capacidad |
|---|---|
| ADMIN | Administración global de catálogos, cuentas, roles asignados y laboratorios; equipos, mantenimiento, reportes, historia y auditoría. |
| GESTOR | Consulta global de catálogos y gestión de Equipo/Mantenimiento en laboratorios asignados; traslada con alcance en ambos extremos; reportes e historia autorizados. |
| LECTOR | Lectura de catálogos y consulta de equipos, mantenimiento, historia y reportes dentro de su alcance; no escribe ni consulta administración de usuarios/auditoría. |

Rol significa **qué** puede hacer el usuario; UsuarioLaboratorio significa
**dónde**. El filtro JWT recarga usuario/rol vigentes y los servicios leen las
asignaciones actuales. Desactivar una cuenta bloquea su acceso; cambiar el rol
o las asignaciones afecta las siguientes solicitudes. Ser custodio no concede
permisos. ADMIN conserva consulta histórica global; GESTOR/LECTOR ven movimientos
si origen **o** destino está dentro de su alcance actual.

La administración de cuentas ya permite alta, edición pública, actividad,
rol y restablecimiento de contraseña. No hay registro público ni CRUD Rol.
Se preserva el último ADMIN activo y se mantienen contraseñas BCrypt fuera
de respuestas y registros de auditoría.

## Equipo, traslado y mantenimiento

Equipo admite CRUD, filtros y baja lógica. Código interno y laboratorio no
cambian por PUT; el laboratorio se modifica exclusivamente mediante traslado.
Un Equipo BAJA conserva consulta e historial y rechaza edición, traslado y nueva baja.

Traslado valida destino activo y diferente, toma origen del Equipo bloqueado
y actor del contexto autenticado. UPDATE Equipo e INSERT Movimiento confirman
en una sola transacción. GESTOR necesita origen y destino autorizados.
La historia no tiene edición/borrado libre y permanece después de bajas.

Mantenimiento se crea PROGRAMADO. Solo ese estado admite edición; puede
iniciarse o cancelarse. EN_PROCESO cambia el Equipo a MANTENIMIENTO, conserva
su estado anterior y bloquea PUT/DELETE/traslado. Completar o cancelar restaura
el estado previo. V12 impide dos mantenimientos EN_PROCESO para el mismo Equipo.

Los reportes usan datos reales con filtros por organización, fechas y estados.
Los servicios registran acciones auditables sin contraseñas ni JWT. La consulta
de auditoría es exclusivamente ADMIN; no pretende reconstruir todas las versiones
de los atributos históricos.

## Evidencia y límites

| Etapa | Evidencia |
|---|---|
| Cierre Sprint 7 | 233/233 JUnit en 33 suites, compileJava/test/bootJar y 28 solicitudes HTTP; histórico del 21 de septiembre. |
| Artefactos backend posteriores | [40 XML locales previos](../despliegue/evidencias/reportes-backend-2026-10-03.json): 319 pruebas, cero fallos/errores/omitidas; no reejecutadas en este paso ni atribuidas al build de Actions. |
| Actions e1ce75a | Ambos trabajos verdes y ambas imágenes publicadas; frontend 67 pruebas. El Dockerfile backend empaqueta con `bootJar -x test`. |
| Docker actualizado | 94/94 comprobaciones HTTP por Nginx, 30 aserciones funcionales y siete controles SQL sin incidencias en una base aislada con las mismas imágenes. |

Se comprobó login de los tres roles, alcance, catálogos, usuarios,
Equipo, mantenimiento, traslado, baja, historia, reportes y auditoría.
La baja se ejecutó por HTTP: su confirmación nativa bloqueó la automatización
del navegador y se canceló. Las demás evidencias de interfaz están descritas
en el [JSON del smoke](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json).

La base habitual Docker mantuvo el contenedor y volumen. Conteos y contenido
de nueve tablas anteriores quedaron iguales, con 3 usuarios y 4 asignaciones.
Se aplicaron las migraciones existentes V10–V13 después de guardar un backup
privado. No se hicieron escrituras de prueba por API en esa base. La base
`inventario_verificacion_docker_actual_5149b3ad` y sus recursos temporales
fueron eliminados; el entorno habitual permaneció saludable.

## Uso y próximos pasos

Docker local: frontend `http://localhost:3000`; backend
`http://localhost:8080`; salud `/actuator/health`.
La ejecución Java local sigue disponible mediante
[iniciar-backend.ps1](../../iniciar-backend.ps1), con variables externas.
Las credenciales de PostgreSQL y la contraseña de login de un usuario son
datos distintos. Se completan privadamente y no se versionan.

Frontend Jason, Docker, usuarios, mantenimiento y auditoría ya están
implementados. Nube/base gestionada, refresh token, permisos dinámicos,
paginación y CRUD Rol permanecen en el [backlog vigente](backlog.md).
La publicación en GHCR no equivale a un despliegue en la nube.
