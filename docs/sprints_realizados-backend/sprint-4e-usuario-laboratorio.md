# Sprint 4E — UsuarioLaboratorio y alcance de laboratorios

Guía del incremento posterior a las 140 pruebas del cierre de Sprint 4B–4D.
Conserva la estructura del backend y usa la tabla creada por V2: **no agrega
V10 ni cambia V1–V9**. Los ejemplos utilizan IDs obtenidos del sistema y variables
de Postman; no incluyen contraseñas, hashes ni tokens reales.

## 1. Objetivo

Permitir que ADMIN consulte y reemplace las asignaciones explícitas de un usuario,
que cada usuario consulte su alcance efectivo y que un laboratorio con
asignaciones activas no pueda darse de baja. Se prepara un servicio reutilizable
para Equipo. Equipo, MovimientoEquipo y administración completa de usuarios
permanecen pendientes.

## 2. Relación N:M Usuario ↔ Laboratorio

Un usuario puede tener cero o varios laboratorios asignados; un laboratorio puede
estar asignado a cero o varios usuarios. Cada asignación referencia exactamente
un usuario y un laboratorio. Rol sigue siendo una relación obligatoria de Usuario.

```text
Rol (1) ← (N) Usuario
                 │ 1
                 │ 0..N
           UsuarioLaboratorio
                 │ 0..N
                 │ 1
             Laboratorio

Rol = qué operación puede hacer.
Alcance = en qué laboratorios puede hacerla.
```

## 3. Tabla puente existente

Fuente: [V2](../../backend/inventario/src/main/resources/db/migration/V2__crear_usuarios_y_seguridad.sql).

| Columna | Tipo PostgreSQL | Nulabilidad | Clave / default |
|---|---|---|---|
| `id_usuario` | `INTEGER` | NOT NULL | Parte de PK; FK a `usuario(id_usuario)` |
| `id_laboratorio` | `INTEGER` | NOT NULL | Parte de PK; FK a `laboratorio(id_laboratorio)` |
| `activo` | `BOOLEAN` | NOT NULL | `DEFAULT TRUE` |
| `fecha_asignacion` | `TIMESTAMPTZ` | NOT NULL | `DEFAULT CURRENT_TIMESTAMP` |

La PK es `(id_usuario,id_laboratorio)`. Ambas FK tienen `ON UPDATE RESTRICT`
y `ON DELETE RESTRICT`. Existe el índice `idx_usuario_laboratorio_id_laboratorio`;
las búsquedas por usuario aprovechan la primera columna de la PK. La estructura
ya es suficiente: implementar una Entity no requiere crear otra migración.

## 4. PK compuesta

`UsuarioLaboratorioId` identifica el par, no una nueva secuencia. Dos objetos ID
con los mismos componentes deben ser iguales y tener el mismo hash; por eso la
clase implementa `Serializable` y define igualdad por ambos componentes mediante
Lombok. No se confunde esta clase de identidad con una Entity normal.

## 5. @Embeddable

`@Embeddable` indica que `UsuarioLaboratorioId` se incorpora al mapeo de otra
Entity. Contiene `idUsuario` e `idLaboratorio` con los nombres reales de columnas.
No tiene tabla independiente ni endpoint.

## 6. @EmbeddedId

`UsuarioLaboratorioEntity` utiliza `@EmbeddedId` para que JPA reconozca una sola
clave primaria compuesta. El Repository recibe esa clase como tipo del ID.
`findById` recupera el par aunque esté inactivo y permite reactivarlo.

## 7. @MapsId

`@MapsId("idUsuario")` vincula el componente de la PK con la relación Usuario;
`@MapsId("idLaboratorio")` hace lo mismo para Laboratorio. Las FK forman parte de
la misma identidad, sin columnas duplicadas. El Service resuelve las entidades
reales y asigna ambas referencias al crear la relación.

## 8. ManyToOne hacia Usuario

La asignación referencia `UsuarioEntity` mediante `@ManyToOne(fetch=LAZY)` y
`@JoinColumn(name="id_usuario", nullable=false)`. Varias asignaciones pueden
referenciar al mismo usuario. No se agrega una colección bidireccional a Usuario.
ADMIN puede administrar la configuración de un usuario inactivo; esa decisión
no cambia la regla de autenticación de usuarios activos.

## 9. ManyToOne hacia Laboratorio

La segunda asociación usa `@ManyToOne(fetch=LAZY)` y
`@JoinColumn(name="id_laboratorio", nullable=false)`. Laboratorio conserva su
relación previa con Área. No se agrega una colección de usuarios en Laboratorio:
las búsquedas y verificaciones se hacen desde el Repository de asignaciones.

## 10. Por qué no usamos ManyToMany directo

La relación contiene datos propios: `activo` y `fecha_asignacion`. Una colección
directa de laboratorios ocultaría el ciclo de vida de cada par. La Entity puente
permite consultar, desactivar y reactivar esa relación de forma explícita.

## 11. Entity propia, Domain y Mapper

`UsuarioLaboratorioEntity` representa persistencia; `UsuarioLaboratorio` es el
dominio sin anotaciones JPA. MapStruct convierte los datos públicos hacia
responses. La Entity utiliza getters/setters y constructores; no `@Data` ni
colecciones que recorran automáticamente ambos lados de la relación.

Los DTO no exponen Entities, credenciales ni `passwordHash`. El usuario del
response administrativo contiene solamente `id`, `userName`, `nombre`,
`apellido` y `rol`; cada laboratorio contiene `id`, `codigo` y `nombre`.

## 12. Activo en la asignación

`usuario_laboratorio.activo` pertenece a la relación. Es distinto de
`usuario.activo` y de `laboratorio.activo`. Un usuario inactivo puede conservar
asignaciones activas y estas bloquean la baja del laboratorio. Un usuario
inactivo continúa sin autenticarse.

## 13. Fecha original de asignación

`fecha_asignacion` representa cuándo se creó por primera vez el par. PostgreSQL
asigna el valor al insertar; desactivar, reactivar o repetir un PUT conserva la
fecha original. No hay `fecha_desasignacion`, historial adicional ni columna nueva.
La API administrativa ofrece el conjunto público; la fecha se comprueba con SQL.

## 14. Rol y alcance

El rol define **qué** operación puede realizar el usuario; el alcance define
**dónde**. El servicio de alcance calcula laboratorios; no sustituye las reglas
de rol de Spring Security. Las operaciones futuras de Equipo deberán comprobar
ambas condiciones. Ser `id_responsable` de un Equipo **no concede alcance**.

## 15. Asignación explícita y alcance efectivo

| Concepto | Fuente | Endpoint |
|---|---|---|
| Asignaciones explícitas activas | Filas de `usuario_laboratorio` con `activo=true` del destinatario | GET administrativo |
| Alcance efectivo | Rol vigente y laboratorios activos permitidos al principal autenticado | GET de alcance propio |

El GET administrativo funciona también para usuarios inactivos y para ADMIN.
Una lista explícita vacía de ADMIN no significa alcance vacío. Las listas de
respuesta se ordenan por ID de laboratorio ascendente.

## 16. ADMIN global

ADMIN obtiene `alcanceGlobal=true` y todos los laboratorios activos, sin necesitar
una fila en la tabla puente. Sus asignaciones explícitas pueden existir, pero no
limitan ese alcance. Ningún rol obtiene acceso efectivo a un laboratorio inactivo
a través del nuevo servicio.

## 17. GESTOR y LECTOR asignados

Ambos obtienen `alcanceGlobal=false`. Para aparecer, una asignación debe estar
activa y su laboratorio también. Una lista vacía es un resultado normal. El
servicio consulta el estado actual: cambiar asignaciones se refleja en la
siguiente petición con el mismo JWT válido, sin iniciar sesión nuevamente.

`GET /api/laboratorios` conserva el catálogo global activo para los tres roles.
Para preguntar por el alcance se utiliza `/api/auth/me/laboratorios`; no son
consultas equivalentes para GESTOR/LECTOR. Equipo todavía no está implementado.

## 18. PUT de reemplazo

`reemplazarAsignaciones(Integer,List<Integer>)` sustituye todo el conjunto activo
del destinatario. `idsLaboratorio` es obligatorio; sus elementos son enteros
positivos y no nulos. `[]` retira todas las asignaciones. Los IDs duplicados se
normalizan como conjunto: `[A,A,B]` equivale a `[A,B]`. Repetir el mismo conjunto
no crea filas nuevas ni cambia fechas.

## 19. Atomicidad

El Service ejecuta el reemplazo en una transacción: bloquea al Usuario, comprueba
su existencia, normaliza los IDs, valida y bloquea todos los laboratorios destino,
carga las relaciones existentes y después modifica el conjunto. Si cualquiera
no existe (404) o está inactivo (409), no se modifica ninguna asignación.
Usuario inexistente devuelve 404. Un ID de ruta no positivo o no válido devuelve
400 antes de aplicar cambios.

## 20. Baja lógica de la asignación

Las filas que salen del conjunto solicitado cambian a `activo=false`; no se usa
borrado físico. El par y su fecha siguen presentes. Una respuesta `laboratorios:[]`
no significa que todas las filas históricas del destinatario hayan desaparecido.

## 21. Reactivación

Si el par ya existe inactivo, el Service pone `activo=true` en esa misma fila.
Si nunca existió, lo crea. La PK compuesta impide duplicar el par; el bloqueo del
usuario serializa reemplazos concurrentes antes de decidir crear o reactivar.

## 22. Bloqueo de Usuario

Un bloqueo pesimista de escritura sobre Usuario coordina dos PUT del mismo
destinatario. El segundo trabaja sobre el estado confirmado por el primero. El
resultado final corresponde a uno de los conjuntos completos; no a una mezcla
parcial. Usuarios diferentes pueden trabajar de forma independiente.

## 23. Bloqueo de Laboratorio

Los laboratorios solicitados se bloquean en orden ascendente después del Usuario.
Asignar/reactivar y dar de baja un laboratorio comparten ese bloqueo. Ordenar los
IDs evita que dos solicitudes los tomen en sentidos opuestos. Quitar una
asignación no introduce una relación activa nueva.

## 24. Conflicto al desactivar Laboratorio

Después de bloquear la fila, `LaboratorioService` consulta si hay asignaciones
activas. Si existen devuelve 409 con el mensaje:

> No se puede desactivar el laboratorio porque tiene asignaciones activas de usuarios.

El laboratorio queda activo. La regla incluye usuarios inactivos y no se delega
al Controller ni a la FK: cambiar `activo` no es un borrado físico. Si solo hay
asignaciones inactivas, puede darse de baja con 204. La regla relacionada con
Equipos permanece pendiente.

## 25. Seguridad

Spring Security exige ADMIN en las dos rutas administrativas. El alcance propio
acepta ADMIN, GESTOR y LECTOR, y utiliza el principal del contexto, sin recibir
`idUsuario` del cliente. Sin JWT válido hay 401; rol insuficiente produce 403.
Se conservan login, BCrypt, firma y expiración del JWT. Una asignación preparada
no habilita a un usuario inactivo para autenticarse.

## 26. Los tres endpoints

| Método | Ruta | Permiso | Éxito |
|---|---|---|---|
| GET | `/api/admin/usuarios/{idUsuario}/laboratorios` | ADMIN | 200, usuario y asignaciones explícitas activas |
| PUT | `/api/admin/usuarios/{idUsuario}/laboratorios` | ADMIN | 200, conjunto explícito final |
| GET | `/api/auth/me/laboratorios` | ADMIN, GESTOR, LECTOR | 200, alcance efectivo propio |

No se agregan POST, DELETE de asignaciones ni un listado general de usuarios.
`UsuarioLaboratorioController` administra las asignaciones; `AuthController`
conserva la convención `/me` para el principal autenticado.

## 27. JSON de entrada y respuesta

Entrada de PUT en Postman; las variables deben contener números reales:

```json
{
  "idsLaboratorio": [{{laboratorioAId}}, {{laboratorioBId}}]
}
```

Respuesta administrativa, con valores ilustrativos que deben corresponder a la
respuesta real; el orden es ID ascendente:

```json
{
  "usuario": {
    "id": 2,
    "userName": "aldo",
    "nombre": "Aldo",
    "apellido": "Ejemplo",
    "rol": "GESTOR"
  },
  "laboratorios": [
    {"id": 1, "codigo": "L201", "nombre": "Laboratorio de electrónica"}
  ]
}
```

Esos IDs y nombres son solamente una ilustración del formato; no se utilizan
como valores fijos en la secuencia manual. Un GET de alcance de ADMIN devuelve
`{"alcanceGlobal":true,"laboratorios":[...]}`; GESTOR/LECTOR utilizan `false`.
PUT `{"idsLaboratorio":[]}` y el alcance sin asignaciones devuelven listas vacías.

## 28. Postman: secuencia manual

### Preparación y conservación del estado anterior

Inicia el backend con el comando del [README](../../README.md#arranque-rápido-en-windows).
Define `baseUrl=http://localhost:8080`. Guarda `demoPassword` como valor local
secreto de Postman; no lo exportes. Usa `marko` para ADMIN, `aldo` para GESTOR y
`romel` para LECTOR, con sus contraseñas vigentes.

1. Ejecuta `POST {{baseUrl}}/api/auth/login`, **No Auth**, Body raw JSON:

   ```json
   {"userName":"marko","password":"{{demoPassword}}"}
   ```

   Guarda `accessToken` de la respuesta en `tokenAdmin` local. Repite para `aldo`
   y `romel` y guarda `tokenGestor` y `tokenLector`. No incluyas tokens en capturas.
2. Ejecuta `GET {{baseUrl}}/api/laboratorios` con Bearer `{{tokenAdmin}}`. Escoge
   dos laboratorios activos y guarda sus IDs en `laboratorioAId` y
   `laboratorioBId`. Si faltan, crea laboratorios de prueba mediante la API
   organizacional usando un Área activa real y códigos nuevos.
3. Obtén los IDs de los usuarios mediante la consulta SQL segura de la sección
   29. Guarda `markoId`, `aldoId` y `romelId`. No existe un nuevo endpoint de
   listado general de usuarios.
4. Antes de cualquier PUT, consulta los tres usuarios con el GET administrativo
   y guarda **sus conjuntos originales de IDs**. Registra también el resultado
   SQL de asignaciones y fechas. Al final restaurarás esas configuraciones.
   Usa una base personal de prueba si necesitas dejar la base habitual intacta.

Estas operaciones manuales sí cambian asignaciones si ejecutas PUT. La ejecución
automática del sprint no asignó laboratorios demo en la base habitual. Restaurar
el conjunto activo no elimina las filas nuevas inactivas, porque la API conserva
la historia mínima del par.

### Caso 1 — ADMIN asigna dos laboratorios

`PUT {{baseUrl}}/api/admin/usuarios/{{aldoId}}/laboratorios`, Bearer
`{{tokenAdmin}}`, Body raw JSON:

```json
{"idsLaboratorio":[{{laboratorioAId}},{{laboratorioBId}}]}
```

Esperado: 200, datos públicos de Aldo y ambos laboratorios, sin duplicados.

### Caso 2 — Consultar asignaciones explícitas

`GET {{baseUrl}}/api/admin/usuarios/{{aldoId}}/laboratorios`, Bearer ADMIN.
Esperado: 200 y exactamente los dos IDs anteriores. Revisa SQL para registrar
la fecha original de los pares.

### Caso 3 — Alcance de Aldo y catálogo global

`GET {{baseUrl}}/api/auth/me/laboratorios`, Bearer `{{tokenGestor}}`.
Esperado: 200, `alcanceGlobal=false`, ambos laboratorios. Compara con
`GET {{baseUrl}}/api/laboratorios`: este último conserva el catálogo global y
puede incluir otros laboratorios activos. No envíes un ID de usuario al alcance.

### Caso 4 — Alcance de Romel

Con ADMIN reemplaza las asignaciones de `{{romelId}}` por
`{"idsLaboratorio":[{{laboratorioAId}}]}`. Consulta `/api/auth/me/laboratorios`
con `{{tokenLector}}`: 200, `alcanceGlobal=false`, solamente A. Esto concede
alcance, sin habilitar escrituras administrativas al rol LECTOR.

### Caso 5 — Alcance global de Marko

Consulta `/api/auth/me/laboratorios` con `{{tokenAdmin}}`: 200,
`alcanceGlobal=true`, todos los laboratorios activos del catálogo. Consulta
también el GET administrativo de `{{markoId}}`: muestra solo sus asignaciones
explícitas. Si no tiene ninguna, la lista vacía no limita el alcance global.
Para demostrarlo si ya tenía asignaciones, después de guardarlas puedes usar
PUT vacío sobre Marko y repetir ambas consultas; restáuralas al terminar.

### Caso 6 — Reemplazar, repetir y normalizar duplicados

PUT Aldo con `{"idsLaboratorio":[{{laboratorioBId}}]}`: 200. A queda inactiva y
B activa. Repite exactamente el PUT y luego envía
`{"idsLaboratorio":[{{laboratorioBId}},{{laboratorioBId}}]}`. Ambos dan 200 con
un solo B; el número de filas y las fechas de los pares no cambian. Reutiliza
el mismo token de Aldo para comprobar alcance B sin un nuevo login.

### Caso 7 — Vacío y reactivación

PUT Aldo con `{"idsLaboratorio":[]}`: 200, lista vacía. Su alcance queda vacío;
SQL conserva las filas con `activo=false`. Vuelve a enviar `[A,B]` con las
variables del caso 1: 200 y ambas relaciones reactivadas con su fecha original.

### Caso 8 — Laboratorio inexistente y rollback

Con la consulta de la sección 29 obtén un ID positivo ausente y guárdalo en
`laboratorioInexistenteId`; comprueba que sigue ausente antes de usarlo. Guarda
el GET y las filas de Aldo. Envía:

```json
{"idsLaboratorio":[{{laboratorioBId}},{{laboratorioInexistenteId}}]}
```

Esperado: 404. GET y SQL deben conservar **A y B activos**, sin cambios parciales
ni de fecha. La presencia de un destino válido no permite aplicar parte del PUT.

### Caso 9 — Laboratorio temporal e inactivo

Obtén un Área activa con `GET /api/areas` y guarda `areaId`. Genera un código
nuevo para esta ejecución, de máximo 30 caracteres, en `codigoTemporal`.
Con ADMIN ejecuta `POST /api/laboratorios`:

```json
{
  "nombre":"Prueba manual de asignaciones",
  "codigo":"{{codigoTemporal}}",
  "ubicacion":"Prueba Sprint 4E",
  "idArea":{{areaId}}
}
```

Guarda el ID de la respuesta 201 en `laboratorioTemporalId`. Sin asignarlo,
ejecuta DELETE de ese ID: 204. Intenta PUT Aldo con B y ese ID: 409. Verifica que
las asignaciones anteriores siguen idénticas. No reutilices el código de un
laboratorio dado de baja: permanece reservado globalmente.

### Caso 10 — Asignación activa impide DELETE

Crea otro laboratorio temporal con otro código, guarda su ID en
`laboratorioBloqueadoId` y asigna a Aldo `[A,B,laboratorioBloqueadoId]`. DELETE
de ese laboratorio con ADMIN debe dar 409 y GET del laboratorio debe dar 200.
Retíralo de Aldo mediante PUT `[A,B]`; si lo asignaste a otro usuario, retíralo
también de ese usuario. DELETE vuelve a intentarse: 204. SQL debe conservar el
par inactivo y su fecha original.

### Caso 11 — Usuarios y validación de entrada

| Solicitud | Esperado |
|---|---|
| GET/PUT administrativo con un ID positivo de usuario confirmado inexistente | 404 |
| Usuario existente sin asignaciones | GET 200 y lista vacía |
| PUT `{}` o `{"idsLaboratorio":null}` | 400 |
| PUT `{"idsLaboratorio":[null]}`, `[0]` o `[-1]` | 400 |
| JSON mal formado o un identificador no numérico | 400 |
| ID de ruta cero, negativo o no convertible a entero | 400 |

Cada caso usa el mismo envoltorio `idsLaboratorio`; por ejemplo, para cero el
body completo es `{"idsLaboratorio":[0]}`. No uses un número positivo al azar
para demostrar ausencia; obtén uno confirmado con SQL.

Si existe un usuario inactivo en tu base de prueba, ADMIN puede consultar y
reemplazar sus asignaciones: guarda/restaura primero su conjunto. Una asignación
activa de ese usuario también impide DELETE del laboratorio temporal con 409.
Su login sigue rechazado. No hay endpoint de este sprint para desactivar
usuarios; no cambies datos de usuarios habituales solo para fabricar este caso.
La suite aislada cubre el escenario con fixtures propios.

### Caso 12 — Seguridad

Repite GET y PUT administrativos de Aldo con `tokenGestor` y `tokenLector`:
403 en los cuatro casos y asignaciones sin cambio. Repite los tres endpoints
sin Authorization o con un token inválido: 401. Cada token válido consulta su
propio alcance con 200. Un parámetro de cliente no selecciona otro usuario.

### Caso 13 — Concurrencia y limpieza

Las regresiones automáticas coordinan ambos órdenes de asignar frente a DELETE
y dos PUT simultáneos del mismo usuario. Dos pestañas de Postman pueden explorar
el resultado, pero pulsar Send manualmente no garantiza solapamiento real.
En la carrera de laboratorio solo son válidos asignación 200 + DELETE 409, o
DELETE 204 + asignación 409. Dos PUT válidos deben terminar en uno de sus
conjuntos completos.

Restaura las listas originales guardadas de Marko, Aldo, Romel y cualquier otro
usuario utilizado, mediante PUT ADMIN. Verifica que los laboratorios originales
sigan activos antes de restaurar; si alguien los cambió durante la prueba,
revisa ese cambio antes de continuar. Retira todas las asignaciones a los
laboratorios temporales y luego dales baja por API. Conserva los laboratorios
preexistentes y no borres físicamente filas puente. Ejecuta las consultas finales
para comprobar ausencia de pares duplicados y de asignaciones activas a
laboratorios inactivos.

## 29. SQL de verificación

Estas consultas de pgAdmin son de lectura. No seleccionan credenciales ni hashes.
Ejecuta las consultas sobre la base que usas para la comprobación manual.

```sql
-- 1. IDs reales y estado de usuarios.
SELECT id_usuario, username, nombre, apellido, activo
FROM usuario ORDER BY id_usuario;

-- 2. IDs activos para las variables de Postman.
SELECT id_laboratorio, codigo, nombre, id_area
FROM laboratorio WHERE activo = TRUE ORDER BY id_laboratorio;

-- 3. Estado completo de asignaciones y su fecha original.
SELECT ul.id_usuario, u.username, r.nombre AS rol, u.activo AS usuario_activo,
       ul.id_laboratorio, l.codigo, l.nombre AS laboratorio,
       l.activo AS laboratorio_activo, ul.activo, ul.fecha_asignacion
FROM usuario_laboratorio ul
JOIN usuario u ON u.id_usuario = ul.id_usuario
JOIN rol r ON r.id_rol = u.id_rol
JOIN laboratorio l ON l.id_laboratorio = ul.id_laboratorio
ORDER BY u.username, l.codigo;

-- 4. Asignaciones explícitas activas, sin confundirlas con alcance ADMIN.
SELECT id_usuario, id_laboratorio, activo, fecha_asignacion
FROM usuario_laboratorio WHERE activo = TRUE
ORDER BY id_usuario, id_laboratorio;

-- 5. Relaciones conservadas tras desasignar.
SELECT id_usuario, id_laboratorio, activo, fecha_asignacion
FROM usuario_laboratorio WHERE activo = FALSE
ORDER BY id_usuario, id_laboratorio;

-- 6. Duplicados del par: cero filas.
SELECT id_usuario, id_laboratorio, COUNT(*)
FROM usuario_laboratorio
GROUP BY id_usuario, id_laboratorio HAVING COUNT(*) > 1;

-- 7. Asignación activa a laboratorio inactivo: cero tras operar por la API.
SELECT ul.id_usuario, ul.id_laboratorio
FROM usuario_laboratorio ul
JOIN laboratorio l ON l.id_laboratorio = ul.id_laboratorio
WHERE ul.activo = TRUE AND l.activo = FALSE;

-- 8. Alcance efectivo de GESTOR/LECTOR activos, sin ADMIN global.
SELECT u.id_usuario, u.username, r.nombre AS rol,
       l.id_laboratorio, l.codigo, l.nombre
FROM usuario u
JOIN rol r ON r.id_rol = u.id_rol
JOIN usuario_laboratorio ul ON ul.id_usuario = u.id_usuario
JOIN laboratorio l ON l.id_laboratorio = ul.id_laboratorio
WHERE u.activo AND r.activo AND ul.activo AND l.activo
  AND r.nombre IN ('GESTOR', 'LECTOR')
ORDER BY u.id_usuario, l.id_laboratorio;

-- 9. Asignaciones que bloquean DELETE, incluso de usuarios inactivos.
SELECT l.id_laboratorio, l.codigo, u.username, u.activo AS usuario_activo
FROM laboratorio l
JOIN usuario_laboratorio ul ON ul.id_laboratorio = l.id_laboratorio
JOIN usuario u ON u.id_usuario = ul.id_usuario
WHERE ul.activo = TRUE ORDER BY l.id_laboratorio, u.id_usuario;

-- 10. Candidatos positivos ausentes; verificar de nuevo antes de usarlos.
SELECT COALESCE(MAX(id_laboratorio)::bigint, 0) + 1 AS laboratorio_inexistente_id
FROM laboratorio;
SELECT COALESCE(MAX(id_usuario)::bigint, 0) + 1 AS usuario_inexistente_id
FROM usuario;

-- 11. Las cuatro columnas originales; ninguna nueva en Sprint 4E.
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'usuario_laboratorio'
ORDER BY ordinal_position;

-- 12. PK/FK, acciones y el índice existente de laboratorio.
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint WHERE conrelid = 'public.usuario_laboratorio'::regclass
ORDER BY conname;
SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'usuario_laboratorio'
ORDER BY indexname;

-- 13. Flyway continúa en V9; no se necesita V10.
SELECT version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

-- 14. Conteos de referencia, sin exponer contenido sensible.
SELECT 'usuario' AS tabla, COUNT(*) FROM usuario
UNION ALL SELECT 'laboratorio', COUNT(*) FROM laboratorio
UNION ALL SELECT 'usuario_laboratorio', COUNT(*) FROM usuario_laboratorio;
```

Compara los resultados de fechas de la consulta 3 antes y después de los casos
de quitar/reactivar. Un candidato ausente mayor que el máximo entero de la API
no sirve para una prueba 404; debe ser un ID positivo válido y confirmado ausente.

## 30. Tests y verificación reproducible

Base anterior: **140 pruebas aprobadas**. Se agregaron **28 invocaciones**:
**168 aprobadas de 168, cero fallos, errores y omitidas**. Base temporal utilizada:
`inventario_verificacion_s4e_scope_20260921_a7d9`. La compilación, la suite completa
y `bootJar` finalizaron correctamente.

Tras verificar los fixtures quedaron 3 roles, 3 usuarios, 2 categorías,
4 subcategorías, 1 sede, 2 áreas, 2 laboratorios y 0 asignaciones. Las consultas
de pares duplicados y asignaciones activas a laboratorios inactivos devolvieron
cero filas. Se cerraron las conexiones y se eliminó la base temporal; su ausencia
se confirmó en el catálogo PostgreSQL. V1–V9 conservaron sus huellas SHA-256.

La base habitual conservó 3 usuarios, 2 laboratorios y 0 asignaciones, con
conteos y huellas de datos públicos idénticos antes/después. No se crearon
asignaciones demo ni se modificaron sus datos para completar este sprint.

La cobertura añadida comprueba clave compuesta y mappers públicos; consulta,
reemplazo, vacío, duplicados, idempotencia, reactivación y fecha; rollback;
alcance de los tres roles; laboratorio y asignación inactivos; configuración de
usuario inactivo; DELETE bloqueado; 401/403; ambos órdenes de las carreras con
laboratorio y dos reemplazos del mismo usuario. Las 140 pruebas previas son
regresión; el número final cuenta invocaciones JUnit, no solicitudes HTTP.

Toda prueba que escriba en PostgreSQL utiliza exclusivamente una base cuyo
nombre comience con `inventario_verificacion_`. No ejecutes la suite de integración
contra `inventario_laboratorios`. El entorno de verificación define `DB_URL`,
`DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `DEMO_USER_PASSWORD` y perfil `dev` solo
en memoria; no se versionan sus valores. Con una base temporal nueva preparada
y esas variables configuradas, desde `backend/inventario`:

```powershell
.\gradlew.bat compileJava --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat bootJar --no-daemon --console=plain
```

Comprueba los reportes en `backend/inventario/build/reports/tests/test/index.html`.
Al terminar, revisa fixtures, cierra las conexiones de la prueba y elimina solo
esa base temporal si no quedan conexiones ajenas. La base habitual no necesita
migraciones ni asignaciones demo para completar este sprint.

## 31. Archivos creados

Las rutas Java parten de `backend/inventario/src/main/java/com/utec/inventario/`.

| Archivo | Propósito |
|---|---|
| `entity/UsuarioLaboratorioId.java` | PK compuesta serializable con igualdad por par |
| `entity/UsuarioLaboratorioEntity.java` | Puente JPA con EmbeddedId, MapsId, estado y fecha |
| `domain/UsuarioLaboratorio.java` | Dominio de asignación sin JPA |
| `domain/UsuarioLaboratorios.java` | Usuario y conjunto explícito sin DTO ni JPA |
| `domain/AlcanceLaboratorios.java` | Dominio del alcance global o asignado |
| `dto/request/ActualizarLaboratoriosUsuarioRequest.java` | Validación de lista y sus elementos |
| `dto/response/UsuarioResumenResponse.java` | Cinco campos públicos de usuario |
| `dto/response/LaboratorioAlcanceResponse.java` | ID, código y nombre del laboratorio |
| `dto/response/UsuarioLaboratoriosResponse.java` | Usuario y asignaciones explícitas |
| `dto/response/AlcanceLaboratoriosResponse.java` | Indicador global y alcance efectivo |
| `mapper/UsuarioLaboratorioMapper.java` | Conversiones de dominio y responses públicos |
| `repository/UsuarioLaboratorioRepository.java` | Búsquedas por par, usuario, laboratorio y estado |
| `service/UsuarioLaboratorioService.java` | Reemplazo atómico, baja y reactivación |
| `service/AlcanceLaboratorioService.java` | Cálculo y comprobación reutilizable de alcance |
| `controller/UsuarioLaboratorioController.java` | GET/PUT administrativos |

Pruebas nuevas, relativas a `backend/inventario/src/test/java/com/utec/inventario/`:

| Archivo | Invocaciones nuevas | Cobertura |
|---|---:|---|
| `entity/UsuarioLaboratorioIdTest.java` | 1 | Igualdad y hash de la PK compuesta |
| `mapper/UsuarioLaboratorioMapperTest.java` | 2 | Mapeo y campos públicos |
| `service/UsuarioLaboratorioServiceTest.java` | 5 | Reemplazo y reglas |
| `service/AlcanceLaboratorioServiceTest.java` | 3 | Alcance y comprobación reutilizable de acceso |
| `UsuarioLaboratorioIntegrationTests.java` | 12 | HTTP, roles, persistencia, fecha y rollback |
| `UsuarioLaboratorioConcurrenciaTests.java` | 4 | Asignación/DELETE y reemplazos simultáneos |

Además se agrega una regresión en `service/LaboratorioServiceTest.java`, para
un total de 28 invocaciones nuevas. Las 140 previas se mantienen.
Esta guía se crea en `docs/sprints/sprint-4e-usuario-laboratorio.md`, conservando
las carpetas organizadas por el usuario.

## 32. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/repository/UsuarioRepository.java` | Bloqueo del destinatario para serializar PUT |
| `backend/inventario/src/main/java/com/utec/inventario/repository/LaboratorioRepository.java` | Comprobar existencia activa para el servicio de alcance |
| `backend/inventario/src/main/java/com/utec/inventario/service/LaboratorioService.java` | Impedir baja con asignaciones activas |
| `backend/inventario/src/main/java/com/utec/inventario/controller/AuthController.java` | Alcance del principal en `/me/laboratorios` |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Roles en los tres endpoints nuevos |
| `backend/inventario/src/test/java/com/utec/inventario/service/LaboratorioServiceTest.java` | Regresión de baja impedida por asignaciones activas |
| `README.md` | Estado, endpoints, alcance y enlaces actuales |
| `docs/reglas-negocio.md` | RN-33 a RN-36 sin renumerar RN-01 a RN-32 |
| `docs/matriz-permisos.md` | Permisos vigentes frente a futuras verticales |
| `docs/sprints/sprint-4.md` | Reporte de cierre 4E conservando historia 4A y 4B–4D |
| `docs/Erd_actual/erd-logico-v2.md` | UsuarioLaboratorio pasa a implementado; modelo idéntico |
| `docs/Erd_actual/erd-logico-v2.svg` | Exportación regenerada con UsuarioLaboratorio en azul |
| `docs/Erd_actual/erd-fisico-v2.md` | Solo estado de implementación, sin cambiar esquema |
| `docs/Erd_actual/erd-v2-cambios.md` | Estado actual de ocho entidades implementadas y dos futuras |

`DemoUsuariosConfig` y V1–V9 se conservan. Las consultas públicas de Usuario no
seleccionan su contraseña/hash; el bloqueo obtiene solo el ID. Los diagramas
físicos conservan su estructura y exportaciones.

## 33. Pendientes para Equipo

Equipo deberá reutilizar `AlcanceLaboratorioService` para restringir datos y
comprobar permisos según operación y laboratorio; no basta con aceptar un ID del
cliente. Permanecen pendientes Equipo, MovimientoEquipo, traslados, aplicación
del alcance a esas verticales, administración completa de usuarios y frontend.
No se anticipan Entity, Repository ni endpoints de Equipo en Sprint 4E.
