# Sprint 6 — Movimiento de Equipos, traslado e historial

Continúa el cierre de Sprint 5 de **203 pruebas aprobadas**. Implementa traslado
transaccional e historial inmutable desde la API sobre la tabla de V3.
**Flyway permanece en V9: no se necesita V10.** El [reporte Sprint 6](sprint-6.md)
registra los archivos y resultados finales. Los documentos anteriores conservan
sus cierres históricos.

## 1. Objetivo

Permitir cambiar el laboratorio actual de un Equipo mediante un caso de uso
específico que registre origen, destino, actor, motivo y fecha. Equipo y Movimiento
deben confirmarse o revertirse juntos. El historial se consulta bajo autorización;
no se edita ni borra mediante API.

## 2. Editar no equivale a trasladar

PUT Equipo conserva código y laboratorio. Editar actualiza datos técnicos o
custodia; trasladar cambia la ubicación y crea historia. Desde Sprint 6 se usa
`POST /api/equipos/{idEquipo}/traslados`. Se mantiene la validación estricta del
UpdateEquipoRequest de Sprint 5 para impedir un traslado por PUT.

## 3. Esquema real de V3

La inspección de [V3](../../backend/inventario/src/main/resources/db/migration/V3__crear_equipos_y_movimientos.sql)
y PostgreSQL confirmó estas ocho columnas de `movimiento_equipo`:

| Columna | Tipo | Nullability | Regla/default |
|---|---|---|---|
| `id_movimiento` | SERIAL → INTEGER | NOT NULL | PK y secuencia propia |
| `id_equipo` | INTEGER | NOT NULL | FK Equipo |
| `id_laboratorio_origen` | INTEGER | NULL | FK Laboratorio |
| `id_laboratorio_destino` | INTEGER | NOT NULL | FK Laboratorio |
| `id_usuario_actor` | INTEGER | NOT NULL | FK Usuario |
| `tipo_movimiento` | VARCHAR(30) | NOT NULL | CHECK BTRIM no vacío; **sin DEFAULT** |
| `motivo` | VARCHAR(500) | NOT NULL | CHECK BTRIM no vacío; sin DEFAULT |
| `fecha_movimiento` | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP |

Las cuatro FK usan `ON UPDATE RESTRICT ON DELETE RESTRICT` y tienen cuatro
índices adicionales, uno por FK. No existe UNIQUE de negocio, `activo`, ubicación
interna en Movimiento, CHECK de lista cerrada de tipos ni CHECK origen diferente
de destino. La API aplica sus reglas sin inventar restricciones físicas.

## 4. MovimientoEquipoEntity

Mapea la tabla real con ID generado, cuatro asociaciones LAZY y fecha
OffsetDateTime. Usa Lombok para getters/setters, Builder y constructores, sin
`@Data`. No agrega colecciones de movimientos a Equipo, Laboratorio o Usuario.
La Entity no se devuelve directamente en HTTP.

## 5. Domain

MovimientoEquipo es un modelo sin anotaciones JPA: ID, Equipo, origen, destino,
usuario actor, tipo, motivo y fecha. TrasladoEquipo agrupa el Equipo actualizado
y su Movimiento confirmado. Los contratos HTTP permanecen separados de Entity
y Domain.

## 6. Tipo TRASLADO

El único tipo creado funcionalmente es `TipoMovimientoEquipo.TRASLADO`, elegido
por el servidor. El cliente no lo envía. Entity, Domain y response conservan el
tipo como String porque V3 permite otros textos no vacíos de máximo 30 caracteres.
Esto permite leer datos históricos válidos sin inventar otros casos de uso ni
cerrar artificialmente el esquema con un enum persistido.

## 7. Relaciones JPA

Cada Movimiento tiene un Equipo, un destino y un actor obligatorios. Origen es
nullable en V3 y se mapea así. Se usan cuatro `@ManyToOne(fetch=LAZY)` y
JoinColumn con los nombres/nulabilidad reales. Las consultas controlan qué
relaciones necesitan; no se recorre un árbol bidireccional.

## 8. Equipo

Procede del ID de la URL y se busca con bloqueo de escritura. El Movimiento
referencia ese Equipo; no una copia. El traslado conserva código interno,
Subcategoría, responsable, estado y fechaCreacion. Cambia laboratorio, ubicación
interna y fechaActualizacion.

## 9. Origen

Se obtiene del laboratorio actual del Equipo **después de bloquearlo**. Nunca
se toma del JSON ni de un dato leído antes del bloqueo. Aunque V3 admita origen
nulo en historia previa, los nuevos TRASLADO registran el origen real obligatorio
del Equipo. Las consultas históricas deben tolerar el origen null.

## 10. Destino

El cliente envía `idLaboratorioDestino`, entero positivo. Debe existir y estar
activo: 404 si falta, 409 si está inactivo. No puede coincidir con el origen:
409 sin modificar Equipo ni crear Movimiento. ADMIN también necesita destino
activo.

## 11. Actor

Se obtiene del principal autenticado; su ID no se acepta en el body. El bloqueo
del actor coordina la operación con cambios de asignaciones. Se devuelve solo
`id`, `userName`, `nombre` y `apellido`, obtenidos como datos públicos, sin
contraseña/hash, UserInfoDetails ni contexto de seguridad.

## 12. Request

TrasladarEquipoRequest admite exactamente tres campos:

```json
{
  "idLaboratorioDestino": {{laboratorioBId}},
  "motivo": "Traslado de prueba Sprint 6",
  "ubicacionInternaDestino": "Armario B"
}
```

La variable de Postman debe contener el ID real. Destino usa NotNull/Positive;
motivo, NotBlank/Size(500); ubicación opcional, Size(200). Los strings se recortan.
El request aplica validación estricta local sin cambiar el JSON de otros módulos.

## 13. Campos controlados por el servidor

Origen, actor, tipo, fecha, Equipo, estado y responsable no pertenecen al request.
Un campo ajeno, incluido cualquiera de esos, produce 400 mediante
CampoTrasladoNoEditableException. Se conserva el patrón local JsonAnySetter
utilizado para el PUT de Equipo. No se confía en IDs de usuario del cliente.

## 14. Motivo

Es obligatorio, no blanco, de máximo **500 caracteres**, porque V3 usa
VARCHAR(500), no TEXT. Se almacena recortado. El CHECK de V3 refuerza que BTRIM
no resulte vacío, y Bean Validation rechaza la entrada inválida antes del
caso de uso.

## 15. Ubicación interna

Si el destino informa texto, Equipo.ubicacionInterna recibe el valor recortado.
Si se omite, es null o queda blanco, se limpia a null para no conservar un
armario del laboratorio anterior. Máximo 200, como la columna de Equipo.
No se agrega ubicación al Movimiento porque V3 no la contiene.

## 16. Responses

POST devuelve 200 con `TrasladoEquipoResponse`:

| Campo | Contenido |
|---|---|
| `equipo` | EquipoResponse completo con laboratorio/ubicación/fecha actualizados |
| `movimiento` | MovimientoEquipoResponse del evento confirmado |

MovimientoEquipoResponse tiene ocho campos: `id`, `tipoMovimiento`, `motivo`,
`fechaMovimiento`, `equipo`, `laboratorioOrigen`, `laboratorioDestino` y `actor`.
Equipo se resume en `id/codigoInterno/nombre`; cada laboratorio en
`id/codigo/nombre`; actor en `id/userName/nombre/apellido`. Origen puede ser null
al leer historia permitida por V3. Se reutiliza LaboratorioEquipoResponse.

Los resúmenes muestran los datos **actuales** de las entidades referenciadas.
El evento conserva sus referencias y motivo, pero no guarda versiones históricas
del nombre de Equipo, de Laboratorio o del actor.

## 17. Mapper

MovimientoEquipoMapper utiliza MapStruct con componente Spring y
ReportingPolicy.ERROR. Convierte Entity → Domain → Response y listas, con
resúmenes controlados. No consulta repositorios, decide alcance, obtiene el
principal, elige origen/destino ni modifica Equipo.

## 18. Repository

MovimientoEquipoRepository extiende JpaRepository. Consulta por Equipo,
lista global y lista restringida por laboratorios permitidos. La restricción es
origen **o** destino, aplicada en PostgreSQL, con filtro opcional idLaboratorio.
Se cargan relaciones necesarias de forma controlada y actores públicos sin
consultas repetidas por cada fila ni exposición de información sensible.

## 19. Service

MovimientoEquipoService concentra el traslado y las consultas históricas.
Valida actor, rol, Equipo, estado, destino y alcance; coordina bloqueos; cambia
Equipo y crea Movimiento en una transacción. Usa AlcanceLaboratorioService de
Sprint 4E. Los Controllers coordinan HTTP y delegan el negocio.

## 20. Transacción

El orden del traslado es: bloquear actor; bloquear Equipo; obtener origen real;
bloquear origen/destino en orden ascendente por ID; validar destino y alcance;
preparar Movimiento con actor/tipo/motivo; actualizar Equipo; guardar/flush de
Equipo; guardar/flush de Movimiento; devolver ambos mediante DTO.

Todo ocurre bajo `@Transactional`. Un flush envía SQL, pero no confirma la
transacción por separado. Ambos cambios solo persisten cuando se confirma
la operación completa.

## 21. Rollback

Referencia inválida, BAJA, mismo destino, alcance insuficiente o un fallo de
persistencia revierten toda la operación. No debe quedar Equipo movido sin
Movimiento ni un Movimiento de traslado sin cambio de Equipo. La suite incluye
comprobaciones sobre datos y conteos; el código productivo no introduce fallos
artificiales para simular errores.

## 22. Origen actual y traslados sucesivos

Un segundo traslado lee la ubicación dejada por el primero después de adquirir
el bloqueo del Equipo. Dos solicitudes al mismo destino dan un traslado válido
y un 409. Dos destinos diferentes pueden producir dos traslados consecutivos
válidos si cada operación conserva alcance sobre el nuevo origen y destino.
No se agregan claves de idempotencia en este sprint.

## 23. Alcance en origen y destino

GESTOR necesita simultáneamente acceso efectivo a ambos laboratorios. Tener
solo origen o solo destino produce 403, sin cambios. El alcance vigente depende
de asignación activa y laboratorio activo. Ser custodio no concede este acceso.
El JSON no puede seleccionar un actor con más permisos.

## 24. ADMIN

Puede trasladar globalmente a un destino activo y consultar toda la historia.
Si datos antiguos o SQL externo dejaron un Equipo no BAJA en un laboratorio
inactivo, ADMIN puede moverlo a un destino activo. Esta política no crea ni
corrige datos legacy automáticamente y no cambia las reglas normales de baja
de Laboratorio.

## 25. GESTOR

Solo traslada si conserva alcance en origen y destino al ejecutar el caso de uso.
El bloqueo de lectura del actor se coordina con el bloqueo de escritura del
reemplazo de asignaciones: si se revoca primero, el traslado devuelve 403; si
el traslado gana bajo autorización válida, termina y la revocación ocurre después.

## 26. LECTOR

No puede trasladar: POST produce 403 antes de negocio. Sí puede consultar
movimientos relacionados con su alcance actual. Esa lectura no equivale a
permiso para ver cualquier Equipo o para editar un movimiento.

## 27. Equipo BAJA

BAJA impide nuevos traslados y produce 409. OPERATIVO, MANTENIMIENTO e INOPERATIVO
sí pueden trasladarse. Dar de baja Equipo no elimina el historial ni lo oculta
a quien pueda consultar sus movimientos. No se agrega reactivación de BAJA.

## 28. Destino inactivo

Produce 409 para todos los roles que pueden trasladar. El traslado bloquea el
destino igual que LaboratorioService: si la baja ganó, detecta inactividad; si
el traslado ganó, la baja encuentra un Equipo no BAJA y se rechaza.

## 29. Mismo destino

Si coincide con el laboratorio actual del Equipo bloqueado, se rechaza con 409:
«El laboratorio destino debe ser diferente al laboratorio actual.» No se crea
un movimiento redundante ni se modifica la fecha de Equipo.

## 30. Fechas

`fechaMovimiento` se genera con PostgreSQL DEFAULT CURRENT_TIMESTAMP y se
recupera con el patrón de fecha generada de Hibernate, como OffsetDateTime.
El cliente no la envía. `fechaActualizacion` de Equipo usa reloj Java UTC,
como PUT/DELETE de Sprint 5; `fechaCreacion` se conserva. No se crea trigger.

CURRENT_TIMESTAMP corresponde al inicio de la transacción PostgreSQL, no a su
commit. El orden de consulta requerido es fecha DESC, ID DESC; bajo concurrencia
no se promete que la fecha describa el orden de confirmación. La cadena de
ubicaciones se protege con el bloqueo de Equipo.

## 31. Historial inmutable

No existen POST directo a `/api/movimientos`, PUT ni DELETE de movimientos.
Un evento solo se crea mediante el traslado confirmado. «Inmutable» se refiere
al contrato de esta API; no se inventa una auditoría de versiones ni una nueva
restricción SQL contra cambios externos.

## 32. Historial por Equipo

`GET /api/equipos/{idEquipo}/movimientos` devuelve 404 si el Equipo no existe.
ADMIN obtiene toda su historia. GESTOR/LECTOR obtienen solo los movimientos
visibles por origen o destino en alcance. Si no hay ninguno visible pero el
laboratorio actual del Equipo está permitido, la respuesta es `200 []`; si
ambas condiciones fallan, 403.

Por tanto, puede haber historia visible aunque GET detalle del Equipo dé 403
porque ahora esté en otro laboratorio. El endpoint de historial no reutiliza
esa denegación de detalle para ocultar eventos que sí estén autorizados.

## 33. Lista global y filtro

`GET /api/movimientos` devuelve todos a ADMIN y solo los visibles a GESTOR/LECTOR.
Alcance vacío produce `200 []`. El único filtro nuevo es `idLaboratorio`:
coincide con origen **o** destino. ADMIN acepta cualquier laboratorio existente,
incluido inactivo; GESTOR/LECTOR reciben 403 si el filtro no pertenece a su
alcance actual. ID de filtro inválido produce 400.

## 34. Alcance del historial

Se usa el alcance **actual**, no una copia de permisos del día del movimiento.
Revocar una asignación puede retirar visibilidad en la siguiente petición con
el mismo JWT válido. El Repository restringe las filas en PostgreSQL: no carga
toda la historia para filtrarla después en Java.

## 35. Laboratorios históricos inactivos

ADMIN sigue viendo los eventos aunque origen o destino estén inactivos.
GESTOR/LECTOR necesitan que al menos uno de los extremos esté en su alcance
efectivo actual. La existencia de movimientos, por sí sola, no bloquea DELETE
lógico de Laboratorio. Sus bloqueos siguen siendo asignaciones activas y Equipos
no BAJA. Las FK conservan la referencia porque la fila del laboratorio permanece.

## 36. Bloqueo de Equipo

El traslado comparte el bloqueo de escritura de Equipo con PUT/DELETE.
Si DELETE confirma primero, el traslado ve BAJA y da 409. Si el traslado gana,
DELETE actúa sobre la nueva ubicación si su actor conserva autorización.
Dos traslados no pueden tomar ambos un origen desactualizado del mismo Equipo.

## 37. Bloqueo de laboratorios

Se bloquean origen y destino por ID ascendente tras actor y Equipo. Esto evita
órdenes opuestos al trabajar con dos laboratorios. Se comparte la protección
del destino con su baja y se conserva el orden compatible con asignaciones y
CRUD de Equipo. Origen puede estar inactivo para ADMIN; destino debe estar activo.

## 38. Concurrencia

Orden del traslado: actor con FOR SHARE → Equipo con FOR UPDATE → laboratorios
por ID ascendente. Las regresiones verifican solicitudes simultáneas al mismo
destino, a destinos diferentes, traslado frente a DELETE Equipo, revocación de
alcance y baja del destino. El objetivo es consistencia de estado y eventos,
no duplicar llamadas sin un solapamiento controlado.

## 39. Tres endpoints y códigos HTTP

| Método | Ruta | Permiso | Éxito |
|---|---|---|---|
| POST | `/api/equipos/{idEquipo}/traslados` | ADMIN o GESTOR en ambos extremos | 200, equipo y movimiento |
| GET | `/api/equipos/{idEquipo}/movimientos` | ADMIN, GESTOR, LECTOR según historia visible | 200, lista |
| GET | `/api/movimientos` | ADMIN, GESTOR, LECTOR según alcance | 200, lista |

400: formato/validación/campos ajenos. 401: JWT inválido o ausente. 403: rol o
alcance insuficiente. 404: Equipo o destino inexistente; ADMIN también recibe
404 al filtrar un laboratorio inexistente. 409: BAJA, mismo destino o destino
inactivo. 500: error genérico, sin SQL ni información sensible.

## 40. Postman: secuencia manual

### Preparación y cuidado de datos previos

Inicia el backend con el [README](../../README.md#arranque-rápido-en-windows).
Configura `baseUrl=http://localhost:8080`, `corrida` corta/única y `demoPassword`
solo como valor local secreto. Obtén tokens de marko/ADMIN, aldo/GESTOR y
romel/LECTOR mediante POST `/api/auth/login`, No Auth:

```json
{"userName":"marko","password":"{{demoPassword}}"}
```

Guarda `accessToken` local en `tokenAdmin`; repite los otros nombres para
`tokenGestor` y `tokenLector`. No exportes tokens ni credenciales. Obtén IDs de
usuarios con SQL seguro y guarda `aldoId`, `romelId` y un `responsableId` activo.
Guarda las asignaciones originales de Aldo/Romel mediante GET administrativo
antes de cambiarlas.

Para aislamiento, crea padres propios con las APIs de catálogos; la preparación
de la [guía Sprint 5](sprint-5-equipos.md#37-postman-preparación-y-secuencia-manual)
explica la secuencia. Usa nombres/códigos nuevos prefijados S6, una Subcategoría
activa y tres laboratorios activos A, B y C. Guarda los IDs reales devueltos en
`subcategoriaId`, `laboratorioAId`, `laboratorioBId`, `laboratorioCId`. No supongas
valores numéricos ni reutilices códigos de laboratorios dados de baja.

Asigna Aldo a A/B mediante PUT ADMIN del Sprint 4E y Romel solo a A. Por ejemplo:

```json
{"idsLaboratorio":[{{laboratorioAId}},{{laboratorioBId}}]}
```

Crea un Equipo en A con POST `/api/equipos`, tokenAdmin:

```json
{
  "codigoInterno":"EQ-S6-{{corrida}}",
  "serieUtec":null,
  "numeroSerie":null,
  "nombre":"Equipo de traslado Sprint 6",
  "marca":null,
  "modelo":null,
  "estado":"OPERATIVO",
  "anio":2026,
  "ordenCompra":null,
  "ubicacionInterna":"Armario A",
  "comentario":"Prueba de traslado",
  "requiereMantenimiento":false,
  "idSubcategoria":{{subcategoriaId}},
  "idLaboratorio":{{laboratorioAId}},
  "idResponsable":{{responsableId}}
}
```

Guarda `equipoId`, su fechaCreacion, fechaActualizacion, responsable y
Subcategoría. Las pruebas manuales siguientes sí escriben si las ejecutas;
utiliza una base personal de prueba si deseas conservar intacta la habitual.
El cierre automático del sprint no crea datos demo allí.

### Caso 1 — ADMIN traslada A → B

POST `{{baseUrl}}/api/equipos/{{equipoId}}/traslados`, Bearer `{{tokenAdmin}}`:

```json
{
  "idLaboratorioDestino":{{laboratorioBId}},
  "motivo":"  Traslado de prueba Sprint 6  ",
  "ubicacionInternaDestino":"  Armario B  "
}
```

Esperado: 200, `equipo.laboratorio.id` B, ubicación `Armario B`, nueva
fechaActualizacion, mismo código/responsable/Subcategoría/fechaCreacion/estado.
`movimiento` muestra origen A, destino B, actor ADMIN, tipo TRASLADO, motivo
recortado y fecha generada. Guarda `movimiento.id` como `movimientoId`.

### Caso 2 — Historia y lista global

GET `/api/equipos/{{equipoId}}/movimientos` y GET `/api/movimientos` con ADMIN:
200 e inclusión del evento. Filtros `?idLaboratorio={{laboratorioAId}}` y
`?idLaboratorio={{laboratorioBId}}` deben incluirlo por origen y destino,
respectivamente. Romel con alcance A también puede ver este evento, aunque
el Equipo esté ahora en B, fuera de su alcance de detalle.

### Caso 3 — Repetir el mismo destino

Repite POST hacia B: 409. Consulta GET de Equipo, historial y SQL: mismo
laboratorio, misma fecha de actualización y ningún movimiento adicional.

### Caso 4 — GESTOR traslada B → A y limpia ubicación

Con tokenGestor y asignaciones A/B, envía:

```json
{"idLaboratorioDestino":{{laboratorioAId}},"motivo":"Retorno de prueba"}
```

Esperado: 200. Equipo queda en A, `ubicacionInterna=null`, actor Aldo y un nuevo
evento B → A. Repite otro traslado válido con ubicación null o blanca si quieres
corroborar la misma semántica; un mismo destino sigue siendo 409.

### Caso 5 — GESTOR sin alcance en uno de los extremos

Con ADMIN deja a Aldo asignado solo a A. POST A → B con tokenGestor: 403,
sin cambios. Luego asígnalo solo a B y repite: 403 porque falta origen. Restaura
A/B y conserva el mismo JWT válido para comprobar que no es necesario otro login.

### Caso 6 — LECTOR y campos del servidor

POST válido con tokenLector: 403. Con ADMIN, añade por separado al request
`idLaboratorioOrigen`, `idUsuarioActor`, `tipoMovimiento`, `fechaMovimiento`,
`idEquipo`, `estado` o `responsable`: 400. Un campo desconocido también produce
400. Revisa que no cambió Equipo ni aumentó el historial.

### Caso 7 — Cadena A → B → C y visibilidad parcial

Con ADMIN mueve A → B y después B → C con motivos distintos. Deja a Aldo y Romel
solo con A. El historial por Equipo les devuelve 200 únicamente con eventos
que toquen A; el evento B → C no debe aparecer. GET detalle del Equipo en C
puede devolver 403 mientras su historial parcial sigue autorizado.

GET global sin filtro aplica la misma restricción. Un filtro explícito B para
esos usuarios da 403, aunque algunos eventos visibles por A también nombren B.
No basta con que un laboratorio aparezca como el otro extremo de un evento.

### Caso 8 — Alcance vacío y Equipo sin historia

Quita temporalmente todas las asignaciones de Aldo: GET `/api/movimientos`
devuelve `[]`; historial del Equipo en C devuelve 403. Restablece A.
Crea otro Equipo nuevo sin movimientos en A y guarda `equipoSinHistoriaId`:
su historial con Aldo devuelve `200 []`. Equipo positivo inexistente confirmado
por SQL debe dar 404. No se inventan IDs.

### Caso 9 — Validación de motivo, destino y ubicación

| Entrada inválida sobre un request que, por lo demás, es válido | Esperado |
|---|---|
| Motivo ausente, null o solo espacios | 400 |
| Motivo de 501 caracteres | 400 |
| Destino ausente, null, cero, negativo o no numérico | 400 |
| Ubicación destino de 201 caracteres | 400 |
| JSON mal formado | 400 |
| Equipo/ID de filtro cero, negativo o no convertible | 400 |

En cada caso conserva las capturas de IDs y fechas anteriores para comprobar
que no hubo modificación parcial.

### Caso 10 — Destino inexistente o inactivo

Usa el ID positivo ausente obtenido con SQL para destino: 404. Crea un laboratorio
temporal D propio, sin equipos/asignaciones; dale baja con ADMIN y usa su ID:
409. El Equipo principal sigue en C y el número de movimientos no cambia.
Un filtro ADMIN de laboratorio inexistente da 404; uno histórico inactivo
existente sí puede consultarse.

### Caso 11 — MANTENIMIENTO e INOPERATIVO

Con PUT Equipo completo cambia únicamente los datos necesarios para establecer
MANTENIMIENTO, conservando los campos editables que quieras mantener; después
traslada desde su ubicación actual a otro laboratorio activo diferente: 200.
Repite con INOPERATIVO: también 200. No incluyas código, laboratorio ni fechas
en PUT. Usa el JSON completo del [Sprint 5](sprint-5-equipos.md#12-updaterequest).
Anota la ubicación final real para las pruebas siguientes.

### Caso 12 — Historia sobre un laboratorio que se da de baja

A tiene eventos históricos. Asegura que el Equipo principal esté fuera de A;
si volvió allí en el caso anterior, trasládalo a B/C con ADMIN. Da de baja
`equipoSinHistoriaId` y cualquier otro Equipo de prueba no BAJA que siga en A.
Retira asignaciones activas a A de todos los usuarios utilizados, preservando
sus conjuntos originales para restauración.

DELETE `/api/laboratorios/{{laboratorioAId}}` con ADMIN: 204 si ya no tiene
asignaciones activas ni Equipos no BAJA. Los movimientos históricos no lo
bloquean. GET historial con ADMIN y filtro global A: 200 con los eventos previos.
SQL conserva las FK a A inactivo. No desactives un laboratorio preexistente.

### Caso 13 — BAJA conserva historia

Da de baja el Equipo principal con ADMIN: 204. Intenta un traslado hacia un
laboratorio activo diferente al actual: 409. Consulta historial con ADMIN:
200 y eventos anteriores. Un lector con alcance actual sobre B o C podrá ver
los eventos relacionados con ese extremo, aunque A esté inactivo.

### Caso 14 — Autenticación y contrato inmutable

Repite los tres endpoints sin token o con token inválido: 401. LECTOR no puede
trasladar y GESTOR necesita ambos extremos. La API no ofrece POST directo de
Movimiento, edición ni eliminación; las rutas nuevas solo crean mediante traslado
y consultan historia. No expongas tokens en capturas de verificación.

### Caso 15 — Concurrencia y restauración

La suite coordina carreras reales; dos clics en Postman no garantizan solapamiento.
Al mismo destino solo debe crearse un traslado, con el segundo en 409. A destinos
distintos puede quedar una cadena de dos traslados válidos. Traslado/DELETE,
revocación y baja del destino deben respetar el orden de los bloqueos.

Restaura las asignaciones originales guardadas de Aldo/Romel y otros usuarios
utilizados. Da de baja todos los Equipos de prueba aún no BAJA y después los
padres propios en orden Laboratorio → Área → Sede y Subcategoría → Categoría.
Los movimientos y Equipos BAJA permanecen físicamente: no hay endpoint para
borrar la historia. Verifica conteos/consistencia con SQL. Las políticas legacy
(origen inactivo y origen null de historia previa) se cubren en fixtures aislados;
no alteres por SQL datos habituales solo para fabricarlas.

## 41. SQL de verificación

Consultas de lectura para pgAdmin, sin hashes, contraseñas ni JWT. Donde se indica
un código, reemplázalo por el código real de tu Equipo/Laboratorio de prueba.

```sql
-- 1. Equipo y ubicación actual; fechas y custodio preservado.
SELECT e.id_equipo, e.codigo_interno, e.estado, e.id_subcategoria,
       e.id_responsable, e.id_laboratorio, l.codigo AS laboratorio,
       e.ubicacion_interna, e.fecha_creacion, e.fecha_actualizacion
FROM equipo e JOIN laboratorio l ON l.id_laboratorio=e.id_laboratorio
ORDER BY e.id_equipo;

-- 2. Movimiento completo: LEFT JOIN porque V3 permite origen NULL.
SELECT m.id_movimiento, m.tipo_movimiento, m.motivo, m.fecha_movimiento,
       e.id_equipo, e.codigo_interno, e.nombre AS equipo,
       lo.id_laboratorio AS origen_id, lo.codigo AS origen_codigo, lo.nombre AS origen,
       ld.id_laboratorio AS destino_id, ld.codigo AS destino_codigo, ld.nombre AS destino,
       u.id_usuario AS actor_id, u.username AS actor_username
FROM movimiento_equipo m
JOIN equipo e ON e.id_equipo=m.id_equipo
LEFT JOIN laboratorio lo ON lo.id_laboratorio=m.id_laboratorio_origen
JOIN laboratorio ld ON ld.id_laboratorio=m.id_laboratorio_destino
JOIN usuario u ON u.id_usuario=m.id_usuario_actor
ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC;

-- 3. Historial de un Equipo seleccionado por código real.
SELECT m.id_movimiento, m.id_equipo, m.id_laboratorio_origen,
       m.id_laboratorio_destino, m.id_usuario_actor, m.tipo_movimiento,
       m.motivo, m.fecha_movimiento
FROM movimiento_equipo m JOIN equipo e ON e.id_equipo=m.id_equipo
WHERE e.codigo_interno='REEMPLAZAR_POR_CODIGO_REAL'
ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC;

-- 4. Laboratorio como origen O destino, incluso si ya está inactivo.
WITH filtro AS (
    SELECT id_laboratorio FROM laboratorio WHERE codigo='REEMPLAZAR_POR_CODIGO_REAL'
)
SELECT m.id_movimiento, m.id_equipo, m.id_laboratorio_origen,
       m.id_laboratorio_destino, m.fecha_movimiento
FROM movimiento_equipo m, filtro f
WHERE m.id_laboratorio_origen=f.id_laboratorio
   OR m.id_laboratorio_destino=f.id_laboratorio
ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC;

-- 5. Equipo actual frente al último TRASLADO insertado por Equipo.
-- La API serializa inserciones con bloqueo del Equipo; la fecha DB es inicio de Tx.
SELECT e.id_equipo, e.codigo_interno, e.id_laboratorio AS actual,
       ultimo.id_movimiento, ultimo.id_laboratorio_destino AS ultimo_destino,
       e.id_laboratorio=ultimo.id_laboratorio_destino AS coincide
FROM equipo e
JOIN LATERAL (
    SELECT m.id_movimiento, m.id_laboratorio_destino
    FROM movimiento_equipo m
    WHERE m.id_equipo=e.id_equipo AND m.tipo_movimiento='TRASLADO'
    ORDER BY m.id_movimiento DESC LIMIT 1
) ultimo ON TRUE
ORDER BY e.id_equipo;

-- 6. Origen igual a destino: cero para traslados hechos mediante la API.
SELECT id_movimiento, id_equipo FROM movimiento_equipo
WHERE tipo_movimiento='TRASLADO'
  AND id_laboratorio_origen=id_laboratorio_destino;

-- 7. Sin Equipo: cero por FK, incluso si Equipo está en BAJA.
SELECT m.id_movimiento FROM movimiento_equipo m
LEFT JOIN equipo e ON e.id_equipo=m.id_equipo WHERE e.id_equipo IS NULL;

-- 8. Usuarios públicos para ID real y actor de eventos.
SELECT u.id_usuario, u.username, u.nombre, u.apellido, u.activo, r.nombre AS rol
FROM usuario u JOIN rol r ON r.id_rol=u.id_rol ORDER BY u.id_usuario;
SELECT m.id_movimiento, u.id_usuario, u.username, m.tipo_movimiento, m.motivo
FROM movimiento_equipo m JOIN usuario u ON u.id_usuario=m.id_usuario_actor
ORDER BY m.id_movimiento;

-- 9. Historia con algún laboratorio inactivo: se conserva para ADMIN.
SELECT m.id_movimiento, m.id_equipo, lo.codigo AS origen,
       lo.activo AS origen_activo, ld.codigo AS destino, ld.activo AS destino_activo
FROM movimiento_equipo m
LEFT JOIN laboratorio lo ON lo.id_laboratorio=m.id_laboratorio_origen
JOIN laboratorio ld ON ld.id_laboratorio=m.id_laboratorio_destino
WHERE lo.activo=FALSE OR ld.activo=FALSE
ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC;

-- 10. Flyway: V1–V9, sin V10.
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 11. Ocho columnas reales, PK, cuatro FK, dos CHECK e índices.
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema='public' AND table_name='movimiento_equipo' ORDER BY ordinal_position;
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint WHERE conrelid='public.movimiento_equipo'::regclass ORDER BY conname;
SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname='public' AND tablename='movimiento_equipo' ORDER BY indexname;

-- 12. Positivos ausentes: verificar que siguen ausentes antes de probar 404.
SELECT COALESCE(MAX(id_equipo)::bigint,0)+1 AS equipo_inexistente_id FROM equipo;
SELECT COALESCE(MAX(id_laboratorio)::bigint,0)+1 AS laboratorio_inexistente_id FROM laboratorio;

-- 13. Consistencia de ubicación de Equipos no BAJA: cero tras operar por la API.
SELECT e.id_equipo, e.id_laboratorio FROM equipo e
JOIN laboratorio l ON l.id_laboratorio=e.id_laboratorio
WHERE e.estado<>'BAJA' AND l.activo=FALSE;

-- 14. Conteos públicos para comparar antes/después.
SELECT 'equipo' AS tabla, COUNT(*) FROM equipo
UNION ALL SELECT 'movimiento_equipo', COUNT(*) FROM movimiento_equipo
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'usuario_laboratorio', COUNT(*) FROM usuario_laboratorio
UNION ALL SELECT 'laboratorio', COUNT(*) FROM laboratorio;
```

La consulta 5 comprueba la cadena generada por esta API, no cambios externos ni
tipos de movimiento legacy con otra semántica. Su selección por ID de inserción
no sustituye el orden HTTP fecha DESC/ID DESC. Los candidatos ausentes deben
caber en un entero positivo aceptado por la API; no son IDs fijos de ejemplo.

## 42. Tests y verificación

Base anterior: **203 pruebas**, conservadas sin modificaciones. Resultado Sprint 6:
**233 aprobadas de 233: 30 nuevas, 0 fallos, 0 errores y 0 omitidas**, en 33 suites.
Las nuevas son 12 de integración HTTP, 10 de concurrencia/rollback, 6 de Service
y 2 de Mapper. Se verificaron los XML JUnit de la ejecución final.

Base temporal: `inventario_verificacion_s6_movimientos_20260921_c9e5`.
`compileJava`, `test` y `bootJar` terminaron correctamente. El JAR generado es
`backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar`.
Los fixtures quedaron limpios: Equipo 0, Movimiento 0, UsuarioLaboratorio 0;
se conservaron los registros base. Con cero conexiones abiertas se eliminó
exclusivamente esa base temporal, sin FORCE; PostgreSQL confirmó su ausencia.

La cobertura verifica mappers públicos, reglas de traslado, conservación de
datos, ubicación, fechas, tipos legacy, atomicidad, visibilidad parcial y filtros,
roles, laboratorios históricos y carreras coordinadas. Toda escritura de pruebas
se ejecuta exclusivamente en `inventario_verificacion_*`, con fixtures propios.
No se insertan movimientos, equipos ni asignaciones demo en la base habitual.

Con una base temporal preparada y variables DB/JWT en memoria, desde
`backend/inventario`:

```powershell
.\gradlew.bat compileJava --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat bootJar --no-daemon --console=plain
```

No uses `inventario_laboratorios` para la suite con escritura. Los reportes
JUnit quedan en `backend/inventario/build/reports/tests/test/index.html`.
La limpieza verifica fixtures y conexiones antes de eliminar solo la base
temporal. La base habitual quedó preservada: Equipo 0, Movimiento 0, Usuario 3,
UsuarioLaboratorio 0 y Laboratorio 2 antes y después, con iguales huellas de
los datos públicos. V1–V9 siguen exitosas y sus archivos conservan sus hashes.

## 43. Archivos creados

Rutas relativas a `backend/inventario/src/main/java/com/utec/inventario/`:

| Archivo | Propósito |
|---|---|
| `entity/MovimientoEquipoEntity.java` | Ocho columnas y cuatro relaciones LAZY |
| `domain/MovimientoEquipo.java` | Evento sin JPA |
| `domain/TipoMovimientoEquipo.java` | TRASLADO controlado por servidor |
| `domain/TrasladoEquipo.java` | Resultado conjunto de Equipo y Movimiento |
| `dto/request/TrasladarEquipoRequest.java` | Destino, motivo y ubicación opcional |
| `dto/response/MovimientoEquipoResponse.java` | Evento público con resúmenes |
| `dto/response/EquipoMovimientoResumenResponse.java` | ID, código y nombre del Equipo |
| `dto/response/UsuarioMovimientoResponse.java` | Cuatro datos públicos del actor |
| `dto/response/TrasladoEquipoResponse.java` | EquipoResponse actualizado y Movimiento |
| `mapper/MovimientoEquipoMapper.java` | Conversiones y listas |
| `repository/MovimientoEquipoRepository.java` | Historia, filtro y alcance en SQL |
| `service/MovimientoEquipoService.java` | Traslado atómico y consulta histórica |
| `controller/EquipoMovimientoController.java` | Traslado e historial por Equipo |
| `controller/MovimientoEquipoController.java` | Lista global de movimientos |
| `exception/CampoTrasladoNoEditableException.java` | Campos ajenos del request → 400 |

Pruebas nuevas, relativas a `backend/inventario/src/test/java/com/utec/inventario/`:

| Archivo | Casos y propósito |
|---|---|
| `MovimientoEquipoIntegrationTests.java` | 12: HTTP, roles, alcance e historial |
| `MovimientoEquipoConcurrenciaTests.java` | 10: nueve carreras y rollback real PostgreSQL |
| `service/MovimientoEquipoServiceTest.java` | 6: reglas, bloqueos y consultas |
| `mapper/MovimientoEquipoMapperTest.java` | 2: conversiones y datos públicos |

Documentos nuevos:
`docs/sprints/sprint-6-movimientos.md` y `docs/sprints/sprint-6.md`.

## 44. Archivos modificados

EquipoEntity permite persistir el cambio de laboratorio por el caso de uso de
traslado; UpdateEquipoRequest y EquipoMapper siguen impidiéndolo en PUT.
SecurityConfig incorpora las tres rutas y GlobalExceptionHandler traduce
el error local del request estricto. Se reutilizan reglas de LaboratorioService:
no se agrega un bloqueo por historia.

README, RN-21–25 y RN-42–45, matriz y tres Markdown ERD reflejan el estado actual.
El lógico y su SVG muestran diez entidades implementadas; no cambian modelo
físico ni cardinalidades. Las guías de Sprint 5 y anteriores se conservan.
La comparación con la copia inicial confirmó **21 archivos nuevos y 10 modificados**:
15 clases productivas, cuatro clases de pruebas y dos documentos nuevos;
tres clases productivas y siete archivos de documentación modificados.
Las pruebas y documentos de sprints anteriores no cambiaron. El bloque Mermaid
físico es idéntico; el lógico conserva sus diez nodos y trece relaciones, cambiando
solo el estado de implementación. El SVG se regeneró y se inspeccionó visualmente.

## 45. Pendientes

Administración completa de usuarios, mantenimiento como Entity, auditoría general,
frontend, Docker, permisos dinámicos y refresh token. No se crean edición/borrado
de movimientos, reactivación de Equipo BAJA ni nuevas funciones de mantenimiento.
Sprint 6 termina con traslado e historial.
