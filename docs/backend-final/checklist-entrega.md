# Checklist de entrega del backend — 26 requisitos

## Cómo leer los estados

`VERIFICADO` indica que el requisito tiene evidencia revisada: código y
compilación para los elementos de estructura, y ejecución registrada para los
elementos de comportamiento. La ejecución nueva de Sprint 7 completó
`compileJava`, `test` y `bootJar` con `--rerun-tasks`: seis tareas ejecutadas y
233/233 pruebas aprobadas en 33 suites, sin fallos, errores u omitidas. No se
añadieron tests. El JAR arrancó en 8,32 segundos, validó JPA y las nueve
migraciones, y superó 28 peticiones HTTP de comprobación. Los resultados
detallados corresponden a [verificación final](verificacion-final.md).

Los endpoints de demostración se ejecutan en la base temporal de verificación,
con IDs y tokens obtenidos durante la preparación de fixtures. `<id>` y los
otros nombres entre ángulos son marcadores que se sustituyen; no son IDs fijos
de la base habitual. Para cuerpos y respuestas, consultar
[endpoints](endpoints.md) y [flujos principales](flujos-principales.md).

## Matriz de entrega

| N.º | Requisito | Estado | Archivo / evidencia revisada | Endpoint o demostración |
|---:|---|---|---|---|
| 1 | Spring Boot | VERIFICADO | [InventarioApplication](../../backend/inventario/src/main/java/com/utec/inventario/InventarioApplication.java), [build.gradle.kts](../../backend/inventario/build.gradle.kts). JAR compilado y arrancado en 8,32 s en 127.0.0.1:56448. | Instancia temporal respondió al login y al CRUD; fue detenida al terminar. |
| 2 | API REST | VERIFICADO | [Controllers](../../backend/inventario/src/main/java/com/utec/inventario/controller), [42 operaciones](endpoints.md). Suite aprobada y 28 peticiones HTTP de cierre exitosas. | Crear recurso, consultar, editar y dar de baja mediante los métodos correspondientes. |
| 3 | GET | VERIFICADO | [CategoriaController](../../backend/inventario/src/main/java/com/utec/inventario/controller/CategoriaController.java). Consultas cubiertas por la suite; smoke confirmó detalle de Equipo, alcance e historial. | `GET /api/categorias` y `GET /api/categorias/<id>`. |
| 4 | POST | VERIFICADO | [EquipoController](../../backend/inventario/src/main/java/com/utec/inventario/controller/EquipoController.java), creación con 201 y `Location`; Equipo creado como GESTOR durante smoke. | `POST /api/equipos` con ADMIN o GESTOR autorizado. |
| 5 | PUT | VERIFICADO | [EquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/EquipoService.java), [UpdateEquipoRequest](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/UpdateEquipoRequest.java); edición comprobada y protección cubierta por la suite. | `PUT /api/equipos/<id>`; código y laboratorio no son editables. |
| 6 | DELETE | VERIFICADO | [EquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/EquipoService.java). Smoke confirmó 204 y consulta posterior de BAJA e historial. | `DELETE /api/equipos/<id>` → 204; luego GET muestra BAJA. |
| 7 | PostgreSQL | VERIFICADO | [Inventario inicial](inventario-auditoria.md) y [cotejo](auditoria-entity-flyway.md): diez tablas, 76 columnas y trece FK. Nueve conteos/huellas de la base habitual idénticos al finalizar; cinco comprobaciones SQL sin incidencias. | Consultas de esquema/conteos de solo lectura; escrituras únicamente en base temporal. |
| 8 | JPA | VERIFICADO | [application.properties](../../backend/inventario/src/main/resources/application.properties), `ddl-auto=validate`, `open-in-view=false`; validación JPA de arranque y persistencia real comprobadas. | Arranque con validación del esquema y CRUD sobre PostgreSQL temporal. |
| 9 | Entity | VERIFICADO | [Auditoría Entity/Flyway](auditoria-entity-flyway.md): diez Entities y `UsuarioLaboratorioId`, tipos, longitudes, fechas y claves cotejados; compilación y JPA validate aprobados. | Consulta de Equipo con sus resúmenes públicos; la Entity no es contrato HTTP. |
| 10 | Repository | VERIFICADO | [EquipoRepository](../../backend/inventario/src/main/java/com/utec/inventario/repository/EquipoRepository.java), [MovimientoEquipoRepository](../../backend/inventario/src/main/java/com/utec/inventario/repository/MovimientoEquipoRepository.java): filtros/persistencia comprobados en suite y smoke. | `GET /api/equipos?idLaboratorio=<lab>` y `GET /api/movimientos?idLaboratorio=<lab>`. |
| 11 | Service | VERIFICADO | [EquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/EquipoService.java), [MovimientoEquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/MovimientoEquipoService.java): reglas, rollback y concurrencia cubiertos por la suite aprobada. | Traslado válido y rechazo por BAJA/mismo destino. |
| 12 | Controller | VERIFICADO | [EquipoController](../../backend/inventario/src/main/java/com/utec/inventario/controller/EquipoController.java), [EquipoMovimientoController](../../backend/inventario/src/main/java/com/utec/inventario/controller/EquipoMovimientoController.java): capas revisadas y rutas ejecutadas. | `POST /api/equipos/<id>/traslados`. |
| 13 | DTO | VERIFICADO | [Requests](../../backend/inventario/src/main/java/com/utec/inventario/dto/request) y [responses](../../backend/inventario/src/main/java/com/utec/inventario/dto/response): contratos revisados, compilados y comprobados en tests. | GET Equipo/Movimiento; JSON mínimo de responsable/actor, sin Entity ni hash. |
| 14 | MapStruct | VERIFICADO | [EquipoMapper](../../backend/inventario/src/main/java/com/utec/inventario/mapper/EquipoMapper.java), [MovimientoEquipoMapper](../../backend/inventario/src/main/java/com/utec/inventario/mapper/MovimientoEquipoMapper.java), `ReportingPolicy.ERROR`; compilación nueva y suite de cierre aprobadas. | Implementaciones generadas compiladas; conversiones comprobadas por los tests. |
| 15 | Lombok | VERIFICADO | [EquipoEntity](../../backend/inventario/src/main/java/com/utec/inventario/entity/EquipoEntity.java) y [Equipo](../../backend/inventario/src/main/java/com/utec/inventario/domain/Equipo.java); `compileJava` y `bootJar` de cierre completados. | Getters, setters, builders y constructores compilados; no tiene endpoint propio. |
| 16 | Validation | VERIFICADO | [TrasladarEquipoRequest](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/TrasladarEquipoRequest.java): límites cubiertos por tests; smoke rechazó actor enviado por cliente con 400. | `POST /api/equipos/<id>/traslados` con motivo vacío/campo sensible → 400. |
| 17 | Spring Security | VERIFICADO | [SecurityConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java), reglas por método/ruta y `denyAll` residual; 401/403 comprobados en smoke. | Sin JWT → 401; LECTOR en POST traslado → 403 antes del negocio. |
| 18 | JWT | VERIFICADO | [JwtService](../../backend/inventario/src/main/java/com/utec/inventario/security/JwtService.java), [JwtAuthFilter](../../backend/inventario/src/main/java/com/utec/inventario/security/JwtAuthFilter.java); suite de seguridad y login de tres roles aprobados. | Login → token; peticiones Bearer válidas y rechazo de autenticación inválida. |
| 19 | Rol | VERIFICADO | [RolEntity](../../backend/inventario/src/main/java/com/utec/inventario/entity/RolEntity.java), [SecurityConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java); ADMIN/GESTOR/LECTOR utilizados en smoke. | Login para cada rol; GESTOR traslada y LECTOR recibe 403 al intentar escribir. |
| 20 | Pertenencia | VERIFICADO | [UsuarioLaboratorioService](../../backend/inventario/src/main/java/com/utec/inventario/service/UsuarioLaboratorioService.java), [AlcanceLaboratorioService](../../backend/inventario/src/main/java/com/utec/inventario/service/AlcanceLaboratorioService.java). Smoke asignó GESTOR a A/B y LECTOR a A; suite cubrió revocación. | PUT de asignaciones, GET de alcance y comparación de acceso al Equipo/historia tras moverlo a B. |
| 21 | Reglas de negocio | VERIFICADO | [Reglas](../reglas-negocio.md), Services y [arquitectura](arquitectura-backend.md): suite aprobada; smoke confirmó mismo destino 409 y BAJA con historia conservada. | Mismo destino o Equipo BAJA → 409; destino inexistente → 404; sin alcance → 403. |
| 22 | Filtros | VERIFICADO | [EquipoRepository](../../backend/inventario/src/main/java/com/utec/inventario/repository/EquipoRepository.java), [MovimientoEquipoRepository](../../backend/inventario/src/main/java/com/utec/inventario/repository/MovimientoEquipoRepository.java); filtros SQL y alcance comprobados. | `GET /api/equipos?estado=OPERATIVO&idLaboratorio=<lab>&requiereMantenimiento=false`; `GET /api/movimientos?idLaboratorio=<lab>`. |
| 23 | Flyway | VERIFICADO | [Migraciones V1–V9](../../backend/inventario/src/main/resources/db/migration), [inventario](inventario-auditoria.md) y cierre en `inventario_verificacion_s7_cierre_20260921_a73f`: nueve versiones con checksums idénticos y success=true. | `flyway_schema_history` comprobado; no se creó V10 ni se alteraron migraciones. |
| 24 | Errores HTTP | VERIFICADO | [GlobalExceptionHandler](../../backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java), [SecurityErrorHandler](../../backend/inventario/src/main/java/com/utec/inventario/security/SecurityErrorHandler.java). Suite aprobada y errores esperados 400/401/403/409 observados en smoke. | 404 y otros contratos cubiertos por tests; comprobar ausencia de SQL, hash y traza en JSON. |
| 25 | Transacciones | VERIFICADO | [MovimientoEquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/MovimientoEquipoService.java): UPDATE Equipo e INSERT Movimiento juntos; regresiones de rollback/concurrencia aprobadas en la suite. | Traslado exitoso, fallo tardío con rollback y carreras de traslado/baja/revocación en pruebas. |
| 26 | Tests | VERIFICADO | [Tests existentes](../../backend/inventario/src/test/java/com/utec/inventario), [verificación final](verificacion-final.md): nueva ejecución 233/233 aprobados, 33 suites, 0 fallos/errores/omitidos y 0 tests añadidos. | Suite en `inventario_verificacion_s7_cierre_20260921_a73f`, 28 peticiones smoke y preservación de la base habitual confirmadas. |

## Condición de cierre

Las 26 filas disponen de la evidencia correspondiente de código, compilación,
suite, arranque o smoke. No se agregaron funcionalidades ni pruebas para
aumentar el número del checklist. El JAR generado tuvo 61.012.323 bytes.

Al terminar se detuvo la instancia controlada (PID 24548), se confirmó libre
el puerto 56448, se limpiaron los fixtures y se eliminaron exclusivamente los
datos/base temporal de verificación sin FORCE y sin conexiones restantes.
Se confirmó la ausencia de `inventario_verificacion_s7_cierre_20260921_a73f`.
Las nueve tablas auditadas de la base habitual conservaron conteos y huellas.
La reproducción de la demostración requiere preparar una nueva base temporal;
la empleada en este cierre ya no permanece disponible.
