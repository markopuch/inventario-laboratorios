# 1. Resumen Sprint 6

Se implementó MovimientoEquipo con traslado transaccional e historial. El
Laboratorio de un Equipo cambia exclusivamente mediante POST de traslado;
PUT conserva la prohibición de editarlo. ADMIN traslada globalmente, GESTOR
necesita alcance vigente en ambos extremos y LECTOR consulta historia visible.
Equipo BAJA no se traslada. Cada traslado conserva un evento inmutable en la API.

Se reutiliza V3, sin migración nueva. Flyway permanece en V9 y las diez entidades
del ERD tienen integración Java/API. Resultado final: **233 pruebas aprobadas de
233: 30 nuevas, 0 fallos, 0 errores y 0 omitidas**.
La [guía de 45 temas](sprint-6-movimientos.md) reúne contratos, Postman y SQL.

# 2. Estado inicial

Sprint 5 dejó Equipo implementado y **203 pruebas aprobadas**. La tabla
`movimiento_equipo` existía desde V3, sin vertical Java ni rutas de traslado.
La inspección de PostgreSQL confirmó este esquema, sin diferencias con Flyway:

| Columna | Tipo real | Nulabilidad, restricción y default |
|---|---|---|
| id_movimiento | SERIAL → INTEGER | PK, NOT NULL, secuencia |
| id_equipo | INTEGER | NOT NULL, FK a Equipo |
| id_laboratorio_origen | INTEGER | NULL permitido, FK a Laboratorio |
| id_laboratorio_destino | INTEGER | NOT NULL, FK a Laboratorio |
| id_usuario_actor | INTEGER | NOT NULL, FK a Usuario |
| tipo_movimiento | VARCHAR(30) | NOT NULL, CHECK no blanco, sin default |
| motivo | VARCHAR(500) | NOT NULL, CHECK no blanco, sin default |
| fecha_movimiento | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Son ocho columnas, cuatro FK `ON UPDATE RESTRICT ON DELETE RESTRICT`, cuatro
índices de FK y el índice de la PK. No hay `activo`, UNIQUE de negocio, columna
de ubicación interna, enum SQL cerrado ni CHECK SQL de origen distinto a destino.
`motivo` no es TEXT. V1–V9 estaban aplicadas exitosamente.

# 3. Arquitectura

```text
Usuario actor
      ↓
MovimientoEquipo
   ↙            ↘
Origen         Destino
      ↖       ↗
        Equipo

Usuario → UsuarioLaboratorio → Laboratorio
                             ↓
                     autorización traslado
```

JWT/principal → Controller → request validado → Service transaccional →
Repository/Entity → PostgreSQL. MapStruct transforma Entity, dominio y DTO
público. Se mantiene la organización de paquetes, Lombok, `@Autowired`, JPA y
MapStruct del proyecto. El rol decide la operación; el alcance decide dónde.
Ser responsable de Equipo no concede permisos.

# 4. Archivos creados

Quince archivos de producción, relativos a
`backend/inventario/src/main/java/com/utec/inventario/`:

| Ruta | Propósito |
|---|---|
| `entity/MovimientoEquipoEntity.java` | Ocho columnas y cuatro relaciones JPA |
| `domain/MovimientoEquipo.java` | Evento sin anotaciones de persistencia |
| `domain/TipoMovimientoEquipo.java` | TRASLADO como tipo controlado por servidor |
| `domain/TrasladoEquipo.java` | Resultado conjunto de Equipo y Movimiento |
| `dto/request/TrasladarEquipoRequest.java` | Entrada de tres campos validada |
| `dto/response/MovimientoEquipoResponse.java` | Evento público con resúmenes |
| `dto/response/EquipoMovimientoResumenResponse.java` | ID, código y nombre |
| `dto/response/UsuarioMovimientoResponse.java` | Cuatro campos públicos del actor |
| `dto/response/TrasladoEquipoResponse.java` | Equipo actualizado y nuevo evento |
| `mapper/MovimientoEquipoMapper.java` | Entity → dominio → response y listas |
| `repository/MovimientoEquipoRepository.java` | Historia y alcance filtrados en SQL |
| `service/MovimientoEquipoService.java` | Validación, transacción y consulta |
| `controller/EquipoMovimientoController.java` | Traslado e historia de un Equipo |
| `controller/MovimientoEquipoController.java` | Historial global con filtro |
| `exception/CampoTrasladoNoEditableException.java` | Rechazo seguro de campos ajenos |

Cuatro archivos de pruebas, relativos a
`backend/inventario/src/test/java/com/utec/inventario/`:

| Ruta | Propósito |
|---|---|
| `mapper/MovimientoEquipoMapperTest.java` | Conversiones y datos públicos |
| `service/MovimientoEquipoServiceTest.java` | Reglas y respuestas del servicio |
| `MovimientoEquipoIntegrationTests.java` | HTTP, roles, historia y PostgreSQL |
| `MovimientoEquipoConcurrenciaTests.java` | Carreras y rollback real |

Se crean este reporte y `docs/sprints/sprint-6-movimientos.md`.

# 5. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/entity/EquipoEntity.java` | Permitir el UPDATE de laboratorio dentro del traslado |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Autorizar las tres rutas por rol |
| `backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java` | Traducir campos ajenos del request a 400 seguro |
| `README.md` | Estado actual, rutas, pruebas y enlaces de Sprint 6 |
| `docs/reglas-negocio.md` | RN-21–25 implementadas y RN-42–45 nuevas |
| `docs/matriz-permisos.md` | Roles y alcance de traslado/historia |
| `docs/Erd_actual/erd-logico-v2.md` | Diez entidades implementadas y notas vigentes |
| `docs/Erd_actual/erd-logico-v2.svg` | Exportación actualizada de la fuente lógica |
| `docs/Erd_actual/erd-fisico-v2.md` | Solo estado de implementación y fuentes |
| `docs/Erd_actual/erd-v2-cambios.md` | Estado y explicación del avance |

UpdateEquipoRequest y EquipoMapper mantienen su protección del laboratorio
en PUT. LaboratorioService conserva sus reglas de baja; no se añade un bloqueo
por movimientos históricos. La comparación con los archivos al inicio del sprint
confirmó **21 nuevos y 10 modificados**. Los documentos y las 203 pruebas de
sprints anteriores permanecen intactos.

# 6. Flyway

No hubo migración: V3 contiene el esquema necesario. Versión final **V9**, con
nueve migraciones exitosas; no se crea V10 ni se modifica una migración aplicada.
La aplicación sigue usando `ddl-auto=validate`.
Comprobación final: nueve migraciones exitosas en ambas bases y los hashes de
los nueve archivos V1–V9 idénticos a los capturados antes del sprint.

# 7. MovimientoEquipoEntity

Mapea las ocho columnas reales. Sus cuatro `ManyToOne(fetch = LAZY)` apuntan a
Equipo, Laboratorio origen, Laboratorio destino y Usuario actor. Origen es
opcional para compatibilidad histórica; destino, Equipo y actor son obligatorios.
No se añaden colecciones bidireccionales ni cascadas de borrado.

`tipoMovimiento` es String en Entity, dominio y response porque V3 permite
textos no blancos distintos de TRASLADO. El enum del servidor contiene únicamente
TRASLADO para este caso de uso. Se leen tipos legacy sin habilitar nuevas
operaciones. `fechaMovimiento` se recupera después del INSERT desde el default
PostgreSQL mediante generación de inserción; no viene del cliente.

# 8. Request y Responses

| Método y ruta | Rol / respuesta correcta |
|---|---|
| POST `/api/equipos/{idEquipo}/traslados` | ADMIN/GESTOR autorizado; 200 |
| GET `/api/equipos/{idEquipo}/movimientos` | ADMIN/GESTOR/LECTOR dentro de la visibilidad histórica; 200 |
| GET `/api/movimientos?idLaboratorio=...` | Tres roles; filtro opcional positivo y validado; 200 |

TrasladarEquipoRequest admite `idLaboratorioDestino` obligatorio positivo,
`motivo` obligatorio no blanco de hasta 500 caracteres y `ubicacionInternaDestino`
opcional de hasta 200. El request rechaza cualquier otra propiedad con 400;
no altera globalmente la deserialización del resto del backend.

TrasladoEquipoResponse contiene `equipo` completo actualizado y `movimiento`.
MovimientoEquipoResponse expone ocho propiedades: `id`, `tipoMovimiento`,
`motivo`, `fechaMovimiento`, `equipo`, `laboratorioOrigen`, `laboratorioDestino`
y `actor`. Equipo resume ID/código/nombre; laboratorios ID/código/nombre;
actor ID/userName/nombre/apellido. No se exponen hashes, credenciales, JWT ni
gráficos completos de Entities.

# 9. Mapper

MovimientoEquipoMapper usa `componentModel = "spring"` y
`unmappedTargetPolicy = ERROR`. Convierte Entity a dominio y dominio a DTO,
incluidas listas. El servicio proporciona la proyección pública del actor;
el mapper no recorre UsuarioEntity para obtener contraseñas. La respuesta de
traslado reutiliza EquipoMapper y LaboratorioEquipoResponse ya existentes.

# 10. Repository

La consulta ADMIN obtiene historia global o por Equipo. El filtro de laboratorio
compara origen **o** destino. Las consultas restringidas incorporan los IDs
permitidos en el WHERE de PostgreSQL y combinan los filtros con AND; no descargan
todo el historial para filtrar en memoria. El `LEFT JOIN` de origen conserva
registros legacy con origen null.

El orden es `fecha_movimiento DESC, id_movimiento DESC`. EntityGraph carga Equipo
y laboratorios; los actores se resuelven mediante proyección pública por lote.
Los conjuntos de alcance vacíos se resuelven sin una consulta `IN ()`.

# 11. MovimientoEquipoService

`trasladarEquipo` controla actor vigente, rol, Equipo, extremos, alcance, motivo,
ubicación y la escritura conjunta. `listarPorEquipo` y `listarMovimientos`
resuelven la visibilidad actual. Se reutiliza AlcanceLaboratorioService.

La escritura lleva `@Transactional`; consultas son read-only. Los códigos
esperados son 400 para entrada inválida, 401 sin autenticación válida, 403 por
rol/alcance, 404 para recurso inexistente y 409 para BAJA/destino inactivo/mismo
destino. Los errores de negocio no deben dejar un cambio parcial.

# 12. Traslado

1. El Controller valida los tres campos y toma el ID del actor del principal.
2. El Service bloquea el Usuario actor con FOR SHARE y verifica usuario/rol vigentes.
3. Bloquea Equipo con FOR UPDATE y toma su Laboratorio actual como origen.
4. Rechaza BAJA y destino igual al origen actual.
5. Bloquea origen y destino en orden ascendente de ID; valida existencia y destino activo.
6. Para GESTOR, valida alcance efectivo en origen **y** destino.
7. Construye el evento con Equipo/origen/actor reales, tipo TRASLADO y motivo recortado.
8. Actualiza laboratorio, ubicación y fechaActualizacion de Equipo; ejecuta `saveAndFlush`.
9. Inserta el Movimiento con `saveAndFlush`, obtiene su fecha generada por PostgreSQL y retorna ambos recursos.
10. El commit confirma ambos; cualquier fallo previo lo revierte todo.

Ubicación omitida, null o blanca limpia el valor a null. Un texto se recorta.
Permanecen código interno, responsable, Subcategoría, estado y fechaCreacion.
MANTENIMIENTO e INOPERATIVO son trasladables; BAJA no lo es.

# 13. Atomicidad

Las dos escrituras usan la misma transacción PostgreSQL. No hay commits
intermedios ni `REQUIRES_NEW`. Aunque el UPDATE de Equipo se haya enviado con
flush, un fallo posterior del INSERT revierte laboratorio, ubicación y fecha.
La suite prueba ese fallo real después del UPDATE, exclusivamente en la base
temporal; no se incorporan artificios de pruebas al código de producción.

La fecha del Movimiento usa `CURRENT_TIMESTAMP`, que en PostgreSQL representa
el inicio de la transacción; no promete hora de commit. Equipo actualiza su
fecha en Java UTC como en Sprint 5. La guía distingue el orden HTTP por fecha/ID
del orden de inserción serializado por Equipo usado para comparar destinos.

# 14. Rol y alcance

| Rol | Traslado | Historia |
|---|---|---|
| ADMIN | Global, con destino existente/activo | Global, incluidos extremos históricos inactivos |
| GESTOR | Alcance vigente en origen y destino | Eventos con al menos un extremo dentro del alcance actual |
| LECTOR | 403 | Misma visibilidad histórica restringida |

ADMIN puede trasladar un Equipo legacy no BAJA desde origen inactivo a destino
activo. GESTOR necesita ambos laboratorios activos en su alcance. Asignaciones
y estado del usuario/rol se consultan en la base; cambiar asignaciones no exige
otro login mientras el JWT siga vigente. Custodia no equivale a autorización.

# 15. Historial

Es inmutable desde la API: no se crean PUT, DELETE ni POST genérico de Movimiento.
Un evento restringido es visible si origen **o** destino pertenece al alcance
actual. La baja de Equipo no elimina esa historia. El orden es fecha descendente
con ID descendente como desempate.

En historia por Equipo: inexistente da 404; eventos visibles dan 200 aunque
el Equipo esté actualmente fuera del alcance. Si no hay eventos visibles pero
el laboratorio actual sí pertenece al alcance, retorna `200 []`; si ninguna
condición se cumple, 403. No se reutiliza el permiso del GET detalle para ocultar
historia autorizada. Global sin alcance retorna `[]`; filtro explícito fuera de
alcance da 403. ADMIN valida existencia de su filtro e incluye laboratorios inactivos.

Los resúmenes usan los nombres y códigos **actuales** de las entidades referidas;
no constituyen una instantánea versionada de sus nombres al momento del evento.
El schema no agrega una protección contra modificaciones directas por SQL.

# 16. Equipo BAJA

No se traslada, incluso con ADMIN: 409 y ninguna nueva fila/cambio. Sigue
consultable según permisos y conserva su historial. No se agrega reactivación
de BAJA ni borrado físico.

# 17. Laboratorios inactivos históricos

Las FK se conservan tras una baja lógica. ADMIN puede leer eventos de un
laboratorio inactivo y filtrar por ese ID existente. GESTOR/LECTOR solo ven un
evento cuando alguno de sus extremos pertenece al alcance efectivo vigente.
Origen null legacy también se conserva en la lectura.

Un movimiento histórico **no bloquea** DELETE lógico de Laboratorio. Permanecen
los bloqueos de Sprint 4E/5: asignaciones activas o Equipos no BAJA. RESTRICT de
SQL afecta al borrado físico, no impone por sí mismo esa regla de baja lógica.

# 18. Concurrencia

Orden de locks: Usuario actor FOR SHARE → Equipo FOR UPDATE → laboratorios por
ID ascendente. La revocación administrativa bloquea primero Usuario; los CRUD
previos de Equipo y Laboratorio comparten sus locks. El origen se determina
después de bloquear Equipo.

Se comprueban traslados simultáneos al mismo destino, a distintos destinos,
traslado contra baja de Equipo, contra revocación del alcance y contra baja
del destino. Mismo destino genera un éxito y un 409; destinos diferentes pueden
producir una cadena válida con el origen efectivo actualizado. También se prueba
rollback real ante un fallo tardío del INSERT.
Las **10 pruebas** de MovimientoEquipoConcurrenciaTests aprobaron: nueve casos
de concurrencia y uno de rollback real. Incluyen dos Equipos en sentidos
opuestos y comprueban que los laboratorios se bloquean por ID ascendente.

# 19. Tests

| Medición | Resultado |
|---|---|
| Total anterior | 203 |
| Nuevas pruebas | 30: 12 HTTP + 10 concurrencia/rollback + 6 Service + 2 Mapper |
| Total final / aprobadas | 233 / 233 |
| Fallos / errores / omitidas | 0 / 0 / 0 |
| Suites JUnit | 33 |
| Base temporal | `inventario_verificacion_s6_movimientos_20260921_c9e5` |
| compileJava / test / bootJar | Correctos; ejecución final test + bootJar en 1 min 3 s |

Las pruebas reutilizan PostgreSQL temporal y regresión previa; no emplean la
base habitual para escribir fixtures. La cobertura incluye contratos HTTP,
respuestas públicas, normalización, fechas, legacy, historia por alcance,
historial de BAJA, extremos inactivos, atomicidad y concurrencia.
Se revisaron los XML JUnit y se generó
`backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar` (61 012 323 bytes).
La primera ejecución detectó dos problemas en pruebas nuevas: precisión de
fechas Java/PostgreSQL y configuración estricta de Mockito. Se corrigieron
esas pruebas y se repitió la suite completa; el resultado de la tabla es el final.

Tras la limpieza, la base temporal tenía Equipo 0, Movimiento 0,
UsuarioLaboratorio 0, Usuario 3, Laboratorio 2, Rol 3, Sede 1, Área 2, Categoría 2
y Subcategoría 4. No quedaron movimientos huérfanos ni Equipos no BAJA en
laboratorios inactivos. Se verificaron cero conexiones, se eliminó únicamente
la base temporal sin FORCE y PostgreSQL confirmó que ya no existe.

# 20. Base habitual

Antes: Equipo **0**, Movimiento **0**, Usuario **3**, UsuarioLaboratorio **0**,
Laboratorio **2**. Se capturaron conteos y huellas de datos públicos, sin mostrar
credenciales. No se crean Equipos, movimientos ni asignaciones demo para cerrar
este sprint. La comparación posterior confirmó los mismos conteos **y las
mismas huellas** de los datos públicos en las cinco tablas. Flyway conserva
nueve migraciones exitosas y versión final V9.

| Tabla | Antes | Después |
|---|---|---|
| equipo | 0 | 0 |
| movimiento_equipo | 0 | 0 |
| usuario | 3 | 3 |
| usuario_laboratorio | 0 | 0 |
| laboratorio | 2 | 2 |

# 21. Postman

La [secuencia manual](sprint-6-movimientos.md#40-postman-secuencia-manual) contiene
15 casos: traslados ADMIN/GESTOR, ubicación, permisos, validación, destinos,
estados, historia parcial, extremos inactivos, BAJA, autenticación y restauración.
Usa IDs realmente devueltos, contraseña/tokens como variables locales y padres
propios. Primero se guardan asignaciones existentes y al terminar se restauran.

Los [14 grupos SQL](sprint-6-movimientos.md#41-sql-de-verificación) son consultas
de lectura para verificar movimiento/Equipo/actor, alcance por extremos,
consistencia, migraciones y esquema. Los casos manuales se entregan para que el
usuario los ejecute; no se afirma haberlos operado en la aplicación Postman.

# 22. Documentación

README describe las rutas y el cierre vigente; reglas-negocio implementa
RN-21–25 y agrega RN-42–45 sin renumerar las anteriores; matriz-permisos separa
rol, alcance de traslado y visibilidad de historia. La nueva guía desarrolla
45 temas y este reporte las 24 secciones solicitadas.

Los tres Markdown ERD reflejan **diez entidades implementadas y cero futuras**
dentro del modelo actual, con 13 relaciones. Se actualiza el SVG lógico. Las
columnas, tipos, constraints, defaults, índices y bloques Mermaid físicos
permanecen iguales. Rol/Usuario integrados no implican CRUD administrativo
completo. Los documentos de Sprint 5 y anteriores conservan su estado histórico.
Se compararon los bloques Mermaid con la copia inicial: físico idéntico y lógico
con el mismo modelo. El SVG se regeneró e inspeccionó: diez nodos implementados,
cero futuros y trece relaciones. Los enlaces locales de los documentos se verificaron.

# 23. Pendientes

Administración completa de usuarios, mantenimiento como Entity, auditoría
general, frontend y Docker. Tampoco se agregan permisos dinámicos, refresh token,
edición/borrado de historia ni reactivación de Equipo BAJA. El alcance de este
sprint termina con traslado e historial.

# 24. Checklist

- [x] MovimientoEquipoEntity
- [x] Domain MovimientoEquipo
- [x] TipoMovimientoEquipo
- [x] TrasladarEquipoRequest
- [x] MovimientoEquipoResponse
- [x] TrasladoEquipoResponse
- [x] Mapper
- [x] Repository
- [x] Service
- [x] Controllers
- [x] ManyToOne Equipo
- [x] ManyToOne origen
- [x] ManyToOne destino
- [x] ManyToOne actor
- [x] POST traslado
- [x] GET historial Equipo
- [x] GET movimientos global
- [x] Actor servidor
- [x] Origen servidor
- [x] Tipo servidor
- [x] Fecha servidor
- [x] Motivo obligatorio
- [x] Destino diferente
- [x] Destino existente
- [x] Destino activo
- [x] Equipo no BAJA
- [x] ADMIN global
- [x] GESTOR origen y destino
- [x] LECTOR no traslada
- [x] Cambio Equipo laboratorio
- [x] UbicacionInterna actualizada/limpiada
- [x] FechaActualizacion Equipo
- [x] Responsable preservado
- [x] Subcategoria preservada
- [x] CodigoInterno preservado
- [x] Movimiento creado
- [x] Atomicidad
- [x] Rollback
- [x] Historial inmutable
- [x] Historial ordenado
- [x] Historial por alcance
- [x] Movimiento histórico sobre lab inactivo visible a ADMIN
- [x] Movimiento histórico no bloquea baja lógica Laboratorio
- [x] 400
- [x] 401
- [x] 403
- [x] 404
- [x] 409
- [x] Concurrencia
- [x] Tests: 233 aprobadas, 0 fallos, 0 errores y 0 omitidas
- [x] Postman: guía manual entregada
- [x] SQL: consultas manuales entregadas
- [x] README
- [x] ERD actualizado
- [x] No se implementó mantenimiento
- [x] No se implementó auditoría genérica
