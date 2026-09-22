# Reglas de negocio - Sistema de Inventario de Laboratorios

**Proyecto:** API REST de inventario de equipos de laboratorio  
**Versión:** 1.0  
**Estado:** Diseño base y reglas incorporadas hasta Sprint 5

**Fuente de verdad:** migraciones Flyway y código del backend

## 1. Alcance

Estas reglas definen cómo se registran, consultan, modifican, trasladan y dan de baja los equipos. También establecen cómo se controlan los usuarios, roles y laboratorios autorizados.

El modelo considera tres roles:

- `ADMIN`: acceso global y administración del sistema.
- `GESTOR`: gestión de equipos dentro de los laboratorios que tiene asignados.
- `LECTOR`: consulta de equipos y movimientos dentro de los laboratorios que tiene asignados.

La pertenencia de un usuario a uno o más laboratorios se administra mediante `usuario_laboratorio`. El campo `equipo.id_responsable` identifica al custodio del equipo, pero no concede permisos de acceso.

UsuarioLaboratorio y el cálculo de alcance efectivo están implementados desde
Sprint 4E. Sprint 5 implementa Equipo y aplica alcance a su CRUD y filtros.
MovimientoEquipo, traslados e historial describen funcionalidad futura.
Los catálogos Categoría, Subcategoría, Sede, Área
y Laboratorio siguen siendo globales: los tres roles consultan y solo ADMIN
escribe. JWT y usuarios con rol están implementados; no hay administración
completa de usuarios.

## 2. Reglas de identidad, usuarios y seguridad

### RN-01. Correo único

Cada usuario debe registrarse con un correo electrónico único. No se permite crear dos usuarios con el mismo correo.

### RN-02. Contraseña protegida

La contraseña debe almacenarse únicamente como hash seguro, por ejemplo BCrypt. Ni la contraseña original ni `password_hash` deben aparecer en respuestas de la API, registros de aplicación o mensajes de error.

### RN-03. Usuario activo

Solo los usuarios activos pueden autenticarse y utilizar los endpoints protegidos. Un usuario inactivo conserva sus registros históricos, pero no puede operar en el sistema.

### RN-04. Rol obligatorio

Todo usuario debe tener exactamente un rol activo: `ADMIN`, `GESTOR` o `LECTOR`.

### RN-05. Asignación de laboratorios

Un usuario puede tener cero, una o varias asignaciones de laboratorio mediante
`usuario_laboratorio`. La combinación `id_usuario + id_laboratorio` no puede
repetirse, incluso si la fila está inactiva. ADMIN puede configurar asignaciones
de un usuario inactivo; su estado sigue impidiendo autenticarse y no elimina
sus asignaciones.

## 3. Reglas de autorización

### RN-06. Acceso global del administrador

Un usuario con rol `ADMIN` puede consultar y administrar equipos de cualquier laboratorio.

### RN-07. Alcance del gestor

Un usuario con rol `GESTOR` solo puede crear, editar, trasladar o dar de baja equipos pertenecientes a laboratorios que tenga asignados.

### RN-08. Alcance del lector

Un usuario con rol `LECTOR` solo puede consultar equipos y movimientos pertenecientes a sus laboratorios. No puede crear, editar, trasladar, dar de baja ni gestionar usuarios.

### RN-09. Separación entre rol y pertenencia

El rol define qué tipo de operación puede ejecutar el usuario. `usuario_laboratorio` define sobre qué laboratorios puede ejecutarla. Ambas validaciones deben cumplirse.

### RN-10. El responsable no concede permisos

Que un usuario figure como `id_responsable` de un equipo no le concede acceso automático al laboratorio ni privilegios de edición.

## 4. Reglas de organización y catálogos

### RN-11. Jerarquía organizacional

Toda área debe pertenecer a una sede y todo laboratorio debe pertenecer a un área.

### RN-12. Jerarquía de categorías

Toda subcategoría debe pertenecer a una categoría.

### RN-13. Catálogos sin duplicados lógicos

El nombre de Área es único dentro de su Sede, sin distinguir mayúsculas:
`id_sede + UPPER(nombre)`. El nombre de Subcategoría sigue el mismo criterio
dentro de Categoría: `id_categoria + UPPER(nombre)`.

El código de Laboratorio es **único globalmente**, sin distinguir mayúsculas:
`UPPER(codigo)`, independientemente del Área o Sede. Esta redacción corrige la
descripción histórica por Área y conserva la decisión de V1; V9 refuerza su
comparación sin distinguir mayúsculas. No se cambió a unicidad por Área.

Las bajas lógicas conservan los nombres/códigos reservados. PUT excluye el ID
propio al comprobar duplicados y valida el destino al mover una hija. No existe
una regla de unicidad del nombre de Sede; no se incorpora esa restricción.

## 5. Reglas de equipos

### RN-14. Código interno único

`codigo_interno` identifica al equipo dentro del sistema y no puede repetirse,
incluso si otro equipo con ese código está en BAJA. Se conserva la comparación
sensible a mayúsculas de V3; no se agregan índices UPPER ni otra migración.

### RN-15. Series únicas cuando se informan

`serie_utec` y `numero_serie` no pueden repetirse cuando contienen un valor.
Cuando no se informan, o contienen solo espacios, se almacenan como `NULL`,
no como cadenas vacías. Los valores informados se recortan y mantienen la
unicidad sensible a mayúsculas de V3, incluyendo equipos en BAJA. PUT excluye
el ID propio al comprobar duplicados.

### RN-16. Relaciones obligatorias

Todo equipo debe estar asociado a:

- un laboratorio existente;
- una subcategoría existente.

El responsable es opcional tanto al registrar como al editar. Ser custodio no
exige una asignación de laboratorio ni concede autorización; RN-39 precisa
cuándo se valida su estado activo.

### RN-17. Estados permitidos

El estado del equipo debe ser uno de los siguientes:

- `OPERATIVO`
- `MANTENIMIENTO`
- `INOPERATIVO`
- `BAJA`

### RN-18. Baja lógica

La operación HTTP `DELETE` no elimina físicamente el registro. Cambia el estado
del equipo a `BAJA` y actualiza `fecha_actualizacion`, conservando el resto de
la información. No se permite POST con BAJA ni cambiar a BAJA mediante PUT:
ambos casos devuelven 409 y la baja se realiza mediante DELETE.

### RN-19. Restricciones de un equipo dado de baja

Un equipo con estado `BAJA` no puede editarse ni trasladarse; PUT y un segundo
DELETE devuelven 409. GET de lista y detalle sigue pudiendo mostrarlo dentro de
la autorización vigente, incluido el filtro `estado=BAJA`.

### RN-20. Consistencia de mantenimiento

Cuando `requiere_mantenimiento = true`, el equipo debe poder identificarse mediante filtros de mantenimiento. Esta marca no reemplaza al estado del equipo.

## 6. Reglas de traslado y trazabilidad

RN-21 a RN-25 describen el futuro flujo de traslado. Sprint 5 no implementa
MovimientoEquipo ni endpoints de traslado; PUT de Equipo conserva su laboratorio
para no eludir estas reglas.

### RN-21. Destino diferente al origen

Un equipo no puede trasladarse al mismo laboratorio en el que ya se encuentra.

### RN-22. Autorización sobre origen y destino

Para trasladar un equipo, un `GESTOR` debe estar autorizado tanto en el laboratorio de origen como en el laboratorio de destino. `ADMIN` está exonerado de esta limitación por tener alcance global.

### RN-23. Movimiento obligatorio

Todo cambio de laboratorio debe generar un registro en `movimiento_equipo` con:

- equipo trasladado;
- laboratorio de origen;
- laboratorio de destino;
- usuario que ejecutó la operación;
- tipo de movimiento;
- motivo;
- fecha y hora.

### RN-24. Operación transaccional

La actualización del laboratorio del equipo y la creación del movimiento deben ejecutarse dentro de una misma transacción. Si una parte falla, ninguna modificación debe persistir.

### RN-25. Motivo obligatorio

Todo traslado debe incluir un motivo no vacío y suficientemente descriptivo.

## 7. Reglas de API y errores

### RN-26. Validación de solicitudes

La API debe rechazar solicitudes con campos obligatorios vacíos, identificadores inválidos, correos mal formados o valores fuera del dominio permitido.

### RN-27. Códigos HTTP consistentes

| Situación | Código esperado |
|---|---:|
| Operación correcta de consulta o actualización | `200 OK` |
| Creación correcta | `201 Created` |
| Baja lógica correcta sin cuerpo | `204 No Content` |
| Solicitud inválida | `400 Bad Request` |
| Falta de autenticación o token inválido | `401 Unauthorized` |
| Usuario autenticado sin permiso | `403 Forbidden` |
| Recurso inexistente | `404 Not Found` |
| Duplicado o regla de negocio incompatible | `409 Conflict` |

### RN-28. Errores sin información sensible

Los errores no deben devolver contraseñas, hashes, tokens, credenciales de PostgreSQL, consultas SQL completas ni trazas internas.

## 8. Reglas para filtros y consultas

### RN-29. Los filtros respetan el alcance

Los filtros por estado, laboratorio, subcategoría o mantenimiento nunca deben permitir que un usuario acceda a datos fuera de sus laboratorios autorizados.

### RN-30. Historial visible según alcance

`ADMIN` puede consultar todos los movimientos. `GESTOR` y `LECTOR` solo pueden consultar movimientos relacionados con laboratorios de su alcance.

## 9. Criterios de aceptación del documento

Las reglas quedan correctamente implementadas cuando se demuestra que:

- Flyway crea todas las restricciones estructurales posibles.
- El servicio valida las reglas que dependen del usuario autenticado o del estado actual del equipo.
- Los controladores solo coordinan HTTP y delegan la lógica al servicio.
- Las pruebas automatizadas cubren los casos permitidos y rechazados.
- Postman evidencia respuestas `200`, `201`, `204`, `400`, `401`, `403`, `404` y `409`.

## 10. Reglas incorporadas en Sprint 4A y extendidas en Sprint 4B–4D

RN-01 a RN-30 conservan su numeración y describen el diseño del sistema completo;
su presencia no implica que la vertical de movimientos ya esté implementada.
Equipo se incorpora en Sprint 5. Las reglas siguientes se introdujeron en Sprint 4A para
**Categoría → Subcategoría** y ahora también aplican a **Sede → Área** y
**Área → Laboratorio**, conservando su numeración.

### RN-31. Padre activo

Una entidad hija no puede crearse ni reasignarse bajo un padre inactivo. Para
Subcategoría requiere Categoría activa; Área requiere Sede activa; Laboratorio
requiere Área activa. Padre inexistente devuelve `404 Not Found`; padre existente
pero inactivo devuelve `409 Conflict` al crear o mover una hija.
La consulta jerárquica de un padre inexistente o inactivo devuelve 404. Padre
activo sin hijos activos devuelve `200 []`. Crear o mover una hija y dar de baja
su padre coordinan sus escrituras mediante bloqueo del padre dentro de la
transacción. El Mapper solo transforma; el Service resuelve y valida al padre.

### RN-32. Baja de padre con hijos activos

No se puede desactivar una Categoría mientras tenga Subcategorías activas, una
Sede mientras tenga Áreas activas ni un Área mientras tenga Laboratorios activos.
La operación devuelve `409 Conflict` y conserva el padre activo. Si no tiene
hijos activos, su baja lógica puede continuar y devuelve `204 No Content`.
La regla se comprueba en `CategoriaService`, `SedeService` y `AreaService`,
respectivamente, consultando al Repository de la hija después de bloquear al
padre. No se implementa en Controller ni requiere una colección bidireccional.

En este sprint, la unicidad de RN-13 para Subcategoría se precisa como
`id_categoria + UPPER(nombre)`, incluyendo nombres reservados por bajas lógicas.
El mismo nombre puede existir en categorías diferentes. Las once reglas
RN-S4A-01 a RN-S4A-11 y sus comprobaciones se detallan en la
[guía de Sprint 4A](sprints/sprint-4a-subcategorias.md#validación-reglas-y-transacciones).

En Sprint 4B–4D, Sede, Área y Laboratorio usan `activo=false` para DELETE,
conservan ID y fecha en PUT y ocultan inactivos en GET. PUT/DELETE de inactivos
devuelven 404. Los índices V8/V9 protegen también los duplicados concurrentes.
La [guía de organización](sprints/sprint-4b-organizacion.md) describe sus pruebas.

**Actualización Sprint 5:** Subcategoría y Laboratorio no pueden darse de baja
si tienen Equipos cuyo estado sea distinto de BAJA; RN-41 detalla la regla.
Se conserva el bloqueo por asignaciones activas de UsuarioLaboratorio de Sprint 4E.

## 11. Reglas incorporadas en Sprint 4E

### RN-33. Laboratorio asignable

Cada laboratorio solicitado debe existir y estar activo. Un ID inexistente
devuelve 404 y uno inactivo devuelve 409. Se valida el conjunto completo antes
de modificar asignaciones. El estado del usuario destinatario no impide que
ADMIN prepare su configuración: un usuario inactivo mantiene bloqueada su
autenticación, pero puede conservar, recibir o retirar asignaciones.

### RN-34. Reemplazo atómico de asignaciones

`PUT /api/admin/usuarios/{idUsuario}/laboratorios` reemplaza todas las asignaciones
explícitas activas en una transacción. `idsLaboratorio` es obligatorio, admite
lista vacía y exige elementos no nulos y positivos; IDs repetidos se normalizan
como conjunto. Cualquier error conserva íntegro el conjunto anterior. Las
relaciones retiradas cambian a `activo=false`; una relación solicitada de nuevo
se reactiva, conserva su PK compuesta y su `fecha_asignacion` original. Nunca se
borra físicamente al desasignar ni se duplica el par. Repetir el mismo PUT es
idempotente. Se bloquea primero al Usuario y después los laboratorios solicitados
en orden ascendente para serializar reemplazos y coordinarse con sus bajas.

### RN-35. Laboratorio con usuarios asignados

No se puede dar de baja un Laboratorio mientras exista alguna asignación
`usuario_laboratorio.activo=true`, incluso si su usuario está inactivo. DELETE
devuelve 409 y conserva el laboratorio activo. Las asignaciones inactivas no
bloquean la baja. `LaboratorioService` comprueba la regla después de bloquear el
laboratorio; agregar o reactivar asignaciones usa el mismo bloqueo. Si gana la
asignación, la baja recibe 409; si gana la baja, la asignación recibe 409.

### RN-36. Alcance efectivo

`GET /api/auth/me/laboratorios` obtiene el usuario del contexto autenticado.
ADMIN tiene `alcanceGlobal=true` y todos los laboratorios activos, aunque no
tenga asignaciones explícitas. GESTOR y LECTOR tienen `alcanceGlobal=false` y
solo laboratorios activos con una asignación activa. El GET administrativo
representa asignaciones explícitas, también para ADMIN, y no su alcance global.
Cambiar asignaciones no exige renovar un JWT que siga siendo válido: el servicio
lee el estado actual. El catálogo `GET /api/laboratorios` mantiene lectura global.
Equipo utiliza este servicio desde Sprint 5; `id_responsable` **no concede alcance**.
RN-40 distingue el alcance de laboratorios activos del acceso histórico global
de ADMIN a los Equipos existentes.

La [guía de Sprint 4E](sprints/sprint-4e-usuario-laboratorio.md) documenta el flujo,
la concurrencia, los endpoints y su verificación manual sin asumir IDs.

## 12. Precisiones incorporadas en Sprint 5

### RN-37. Código interno inmutable

`codigoInterno` se recibe al registrar y no cambia después del alta. PUT permite
reemplazar solo los campos editables; si el JSON incluye ese campo inmutable,
devuelve 400 sin modificar el equipo. Tampoco permite editar el ID ni las fechas
controladas por el servidor.

### RN-38. Laboratorio inmutable mediante PUT

Cambiar Laboratorio representa un traslado y exige el futuro flujo de
MovimientoEquipo. `idLaboratorio` no forma parte de UpdateEquipoRequest. Si se
envía en PUT, la API devuelve 400 y explica que el cambio se realiza mediante
el flujo de traslado. No se implementa todavía ese endpoint; el CRUD conserva
el laboratorio original.

### RN-39. Referencias activas

POST exige Subcategoría y Laboratorio existentes y activos. PUT permite cambiar
Subcategoría, cuyo destino también debe existir y estar activo. Si se informa
un responsable nuevo, debe existir y estar activo; ausencia devuelve 404 e
inactividad 409. El responsable puede retirarse con `null`; conservar el mismo
responsable posteriormente inactivo no equivale a asignar uno nuevo y se permite.
Nunca se consulta su asignación de laboratorio para decidir si puede ser custodio.
No se inventan requisitos de actividad en otros ancestros que el contrato no exija.

### RN-40. Alcance aplicado a Equipos

ADMIN consulta todos los Equipos existentes, incluso BAJA y registros históricos
en laboratorios posteriormente inactivos. Crear exige laboratorio activo.
GESTOR consulta y escribe únicamente en su alcance efectivo; LECTOR solamente
consulta dentro de ese alcance. Los listados restringen laboratorios en la
consulta a PostgreSQL y no cargan toda la tabla para filtrar en Java.

Un filtro explícito de laboratorio fuera del alcance de GESTOR/LECTOR devuelve
403; sin filtro, alcance vacío devuelve `200 []`. Un detalle inexistente devuelve
404 y uno existente fuera del alcance devuelve 403. ADMIN puede filtrar por un
laboratorio existente incluso inactivo. `GET /api/laboratorios` sigue siendo un
catálogo global; `/api/auth/me/laboratorios` conserva su contrato de laboratorios
activos. La responsabilidad no concede alcance. Movimientos siguen pendientes.

### RN-41. Catálogos con Equipos no dados de baja

Subcategoría y Laboratorio no pueden darse de baja mientras tengan Equipos con
`estado <> 'BAJA'`. Sus Services bloquean al padre y comprueban la existencia
mediante EquipoRepository; no se resuelve en Controller ni con borrado físico.
Con solo Equipos BAJA, Subcategoría puede darse de baja y Laboratorio puede
hacerlo si además no tiene asignaciones activas de UsuarioLaboratorio.
Crear Equipo y modificar su Subcategoría coordina bloqueos con esas bajas.
Las FK RESTRICT conservan referencias históricas; no impiden por sí solas
cambiar el estado lógico de un padre.

La [guía de Sprint 5](sprints/sprint-5-equipos.md) explica contratos, filtros,
fechas, concurrencia y comprobaciones manuales. RN-01 a RN-36 mantienen
su numeración; las menciones a traslados siguen describiendo funcionalidad futura.
