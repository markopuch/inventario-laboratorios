# Resumen del backend — cierre técnico de Sprint 7

Este documento conserva el cierre funcional y las mediciones de Sprint 7.
**Actualización de contexto — 2026-10-03:** después de ese cierre se integró el
frontend de Jason, se validó el flujo completo con PostgreSQL/backend/frontend
en Docker y se publicaron ambas imágenes en GHCR mediante Actions. La
[evidencia posterior](../despliegue/verificacion-docker-actions-2026-10-03.md)
registra esos resultados sin atribuir una nueva ejecución de la suite Gradle.

## Qué entrega el proyecto

Inventario de Laboratorios es una API REST para organizar laboratorios,
clasificar Equipos, controlar su estado/ubicación y registrar traslados. El
backend funciona con Java 21, Spring Boot 4.1.1, PostgreSQL, JPA, Flyway, JWT,
BCrypt y MapStruct. Mantiene la estructura del proyecto y el estilo de los
ejemplos del profesor: Controllers, DTOs, Domain, Mappers, Services, Repositories
y Entities con responsabilidades separadas.

Sprint 7 es el cierre técnico y documental de las funciones de Sprint 1–6.
Consolida la auditoría, los contratos, las pruebas reproducibles y los artefactos
de Postman; no agrega otro módulo de negocio. El backend cerrado comprende
autenticación, cinco catálogos, asignaciones, Equipos y Movimientos. Crear o editar
usuarios mediante API, mantenimiento como Entity y las demás ampliaciones se
encuentran en el [backlog](backlog.md).

## Modelo y arquitectura

El modelo contiene **10 entidades, 76 columnas y 13 FK**: Sede, Área,
Laboratorio, Categoría, Subcategoría, Rol, Usuario, UsuarioLaboratorio, Equipo
y MovimientoEquipo. Flyway V1–V9 determina su estructura; Hibernate valida el
esquema y no lo genera. Sprint 4E reutilizó la tabla puente de V2, y Sprint 5/6
usaron Equipo/Movimiento de V3 sin necesitar V10.

La organización es Sede → Área → Laboratorio y la clasificación es Categoría →
Subcategoría. Equipo tiene un Laboratorio y una Subcategoría obligatorios, más
un custodio opcional. UsuarioLaboratorio representa la relación N:M mediante
una PK compuesta. Movimiento registra Equipo, origen opcional para legacy,
destino, actor, tipo, motivo y fecha. Los nuevos traslados siempre toman el origen
actual de Equipo.

Una petición pasa por Spring Security, Controller y Service; el servicio aplica
reglas y transacciones, Repository consulta PostgreSQL y Mapper transforma
Entity/dominio en DTO público. No se devuelven Entities ni hashes. La
[arquitectura](arquitectura-backend.md), el [cotejo Entity/Flyway](auditoria-entity-flyway.md)
y los [ocho flujos](flujos-principales.md) explican este recorrido.

## Funciones y contratos principales

Las **42 operaciones HTTP** se distribuyen así: AUTH 3, Categoría 6,
Subcategoría 5, Sede 6, Área 6, Laboratorio 5, asignaciones 2, Equipo 6 y
Movimiento 3. Las listas jerárquicas se cuentan con el padre de su ruta y los
filtros no incrementan el total. El [catálogo de endpoints](endpoints.md) muestra
los métodos, rutas, permisos y respuestas.

Los catálogos permiten alta, consulta, edición y baja lógica. Las hijas requieren
padres activos y un padre con hijas activas no puede desactivarse. Los nombres
de Área/Subcategoría son únicos dentro de su padre; el código de Laboratorio
es único globalmente sin distinguir mayúsculas. Las bajas conservan esas reservas.

Equipo tiene filtros de estado, laboratorio, subcategoría y mantenimiento,
combinables con AND. Código interno y series informadas son únicos según V3;
las series blancas se guardan como null. El responsable es opcional. PUT
reemplaza campos editables y rechaza código interno/laboratorio/fechas ajenos al
contrato. DELETE cambia a BAJA: no elimina físicamente ni permite reactivar.
BAJA conserva consultas e historia, pero no admite otra baja, edición o traslado.

Las asignaciones se reemplazan como un conjunto completo y atómico. Lista vacía
es válida; se deduplican IDs, se valida cada laboratorio y se desactivan/reactivan
relaciones sin perder su fecha original. Puede configurarse un usuario inactivo,
aunque siga sin poder autenticarse. Una asignación activa bloquea la baja del
Laboratorio incluso si su usuario está inactivo.

## Seguridad y alcance

| Rol | Capacidad vigente |
|---|---|
| ADMIN | Administración global de catálogos/asignaciones, Equipos e historia |
| GESTOR | Escritura de Equipos dentro del alcance y traslado entre dos extremos permitidos |
| LECTOR | Consulta de Equipos e historia autorizados, sin escritura |

Los tres roles consultan catálogos globales. El alcance efectivo para GESTOR y
LECTOR exige asignación activa y Laboratorio activo; ADMIN recibe alcance global.
Usuario y Rol deben estar activos. Los datos vigentes se recargan desde la base,
por lo que cambiar asignaciones no exige otro login con un JWT aún válido.
**Custodiar un Equipo no concede acceso.**

ADMIN conserva acceso histórico global a Equipos BAJA y laboratorios inactivos.
Un usuario restringido ve Equipo por su ubicación actual, pero ve un Movimiento
si origen **o** destino está dentro de su alcance actual. Esto permite consultar
historia parcial aunque el Equipo haya salido del laboratorio. Los filtros de
alcance se ejecutan en PostgreSQL, sin cargar todas las filas para filtrar en Java.

## Traslado, historia e integridad

POST `/api/equipos/{idEquipo}/traslados` acepta destino, motivo y ubicación
interna destino opcional. Actor, origen, tipo y fecha proceden del servidor.
Exige Equipo no BAJA y destino existente, activo y distinto; GESTOR necesita
ambos extremos. ADMIN puede recuperar un Equipo legacy desde origen inactivo
hacia destino activo. La respuesta 200 incluye Equipo actualizado y Movimiento.

Equipo y Movimiento se guardan en una sola transacción. Un error posterior al
UPDATE revierte ambas escrituras. Se coordinan locks de actor, Equipo y
laboratorios para proteger traslados, bajas y revocaciones concurrentes.
El traslado conserva responsable, Subcategoría, código, estado y fechaCreacion;
ubicación omitida/null/blanca se limpia. La fecha del evento viene del default
PostgreSQL, que representa el inicio de la transacción, no el commit.

No existe edición/borrado de Movimientos ni POST genérico para fabricarlos.
El historial se ordena por fecha DESC/ID DESC y conserva referencias históricas
tras bajas lógicas. Sus resúmenes muestran nombres actuales, sin versionarlos.
Movimiento por sí solo no bloquea la baja de Laboratorio; sí la bloquean
asignaciones activas o Equipos no BAJA.

## Verificación y reproducibilidad

El cierre anterior, Sprint 6, tuvo **233 pruebas aprobadas**. Sprint 7 reejecutó
las 233 sin modificarlas ni agregar otras: **233 aprobadas en 33 suites, cero
fallos, errores y omitidas**. compileJava, test y bootJar finalizaron correctamente.
El JAR real arrancó en 8,32 segundos; las 28 solicitudes HTTP de comprobación
tuvieron el estado esperado. Se verificaron login, jerarquías, asignaciones,
Equipo, traslado, historia parcial y errores 400/401/403/409. La instancia fue
detenida y su puerto quedó libre. La [evidencia final](verificacion-final.md)
separa estos resultados nuevos de las cifras históricas.

La auditoría inicial encontró 10 tablas y nueve migraciones exitosas. La base
habitual tenía tres usuarios, cero asignaciones, dos categorías, cuatro
subcategorías, una sede, dos áreas, dos laboratorios, cero Equipos y cero
Movimientos. Los conteos y huellas públicas de esas nueve tablas quedaron
idénticos después de la verificación. La base temporal
`inventario_verificacion_s7_cierre_20260921_a73f` conservó V1–V9 exitosas y sus
checksums; los cinco controles SQL de consistencia dieron cero incidencias.
Tras limpiar los fixtures y confirmar cero conexiones, se eliminó únicamente
esa base temporal y se comprobó su ausencia. No se escribieron datos demo en la
base habitual.

El script [verificar-backend.ps1](../../verificar-backend.ps1) recibe conexión
y contraseña demo mediante variables externas, exige una base local existente
`inventario_verificacion_*` y ejecuta compilación, tests y empaquetado. Verifica
XML sin pruebas omitidas y no crea/elimina bases. Los artefactos Postman incluyen
colección y environment con secretos, tokens e IDs vacíos: deben completarse
localmente y ejecutarse en el orden guiado.

## Cómo comenzar y qué queda fuera

Con PostgreSQL disponible, [iniciar-backend.ps1](../../iniciar-backend.ps1)
prepara variables que falten y arranca en 8080. También puede usarse Run sobre
InventarioApplication si el IDE tiene esas variables. La opción de crear cuentas
demo es explícita; no cambia cuentas existentes ni agrega Equipos/Movimientos.
El [README](../../README.md) contiene configuración, ejecución y enlaces vigentes.

El frontend de Jason y Docker local ya se implementaron después de Sprint 7;
la publicación de backend/frontend en GHCR está confirmada para el commit
`9956939`. La verificación Docker obtuvo 33/33 comprobaciones HTTP y 24/24
aserciones, y el trabajo frontend de Actions aprobó 27 pruebas. Son evidencias
distintas de las 233 pruebas Gradle del cierre funcional, no una suma de suites.
La base temporal Docker y sus recursos se eliminaron, con la base habitual
preservada. Render y una base gestionada aún no tienen evidencia de despliegue.

Administración completa de usuarios, mantenimiento, auditoría general, refresh
token y permisos dinámicos permanecen fuera del backend cerrado. No se agregan
esas funciones para ampliar artificialmente el cierre. Las nuevas prioridades
pueden centrarse en el despliegue remoto o en una ampliación autorizada del producto.
Los reportes de sprints anteriores se conservan como evidencia histórica, con sus
resultados y pendientes de aquel momento; [Sprint 7](../sprints_realizados-backend/sprint-7.md)
concentra el cierre funcional y [Sprint 8](../sprints_realizados-backend/sprint-8.md)
documenta el empaquetado y la publicación posteriores.
