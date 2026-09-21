# Sprint 4A — Subcategorías

Este sprint añade la vertical backend de Subcategoría y su relación con
Categoría. El [resumen de Sprint 4](sprint-4.md) reúne el avance y los pendientes.
La tabla ya existía desde V1; ahora tiene Entity, dominio, DTOs, Mapper,
Repository, Service y API REST. Se conservan Java, Spring Boot, Gradle,
MapStruct, Lombok, JJWT y la organización de paquetes existente.

**Resultado de cierre:** 99 pruebas ejecutadas y aprobadas, 0 fallidas, 0 errores
y 0 omitidas. V7 aplicada en la base de verificación y en la base habitual.
Los datos existentes de categorías, subcategorías y usuarios se conservaron.

## Relación Categoría → Subcategoría

Una categoría es el padre y puede agrupar varias subcategorías. Cada subcategoría
es una hija y pertenece a una sola categoría. Por ejemplo, Electrónica puede
contener Osciloscopios y Sensores. La relación de catálogo es **1:N**.

Es el primer ejercicio de vertical de catálogo relacionado después de Categoría;
no es la primera relación JPA absoluta del proyecto: ya existe Usuario → Rol.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_categoria", nullable = false)
private CategoriaEntity categoria;
```

`@ManyToOne` expresa que muchas subcategorías pueden compartir una categoría.
`LAZY` permite diferir la carga del padre hasta necesitarlo. El servicio convierte
a dominio dentro de su transacción; no entrega proxies de Hibernate al cliente.
`@JoinColumn` identifica la columna `subcategoria.id_categoria`, que guarda la
FK hacia `categoria.id_categoria`. La FK real y su obligatoriedad las define
Flyway; Hibernate solo valida el esquema.

El JSON de entrada recibe un `idCategoria` entero porque el cliente selecciona
un padre existente. La Entity guarda una `CategoriaEntity` porque JPA necesita
modelar esa asociación. El Mapper puede representar ese ID en el dominio; el
Service busca y valida el registro real antes de asignarlo a la Entity.

No se necesita una colección de hijos en `CategoriaEntity` para consultar la
relación. El Repository de Subcategoría consulta por ID del padre. La respuesta
incluye un resumen `{id, nombre}` de Categoría y evita referencias circulares.

## Responsabilidad de cada capa

| Capa | Responsabilidad en este sprint |
|---|---|
| Controller | Recibe HTTP, aplica `@Valid`, llama al Service y devuelve DTOs y estados HTTP |
| Request DTO | Define `nombre`, `descripcion` e `idCategoria`; no permite controlar ID, estado ni fecha mediante esos modelos |
| Response DTO | Expone ID, nombre, descripción, estado, fecha y un resumen de categoría |
| Mapper | Convierte requests, dominio, Entity y respuestas; copia los campos editables en PUT |
| Domain | Representa `Subcategoria` y su `Categoria` sin anotaciones JPA |
| Service | Valida padre, duplicados y estado; normaliza texto; coordina transacciones y bloqueos |
| Entity | Mapea columnas y la relación persistente con `CategoriaEntity` |
| Repository | Consulta y guarda mediante Spring Data JPA; ofrece consultas de activas, duplicados y bloqueos |

MapStruct utiliza `componentModel="spring"` y
`unmappedTargetPolicy=ReportingPolicy.ERROR`. Su código se genera al compilar;
se edita la interfaz `SubcategoriaMapper`, no el archivo generado. El Mapper no
consulta repositorios: convertir una forma de datos en otra no debe decidir si
una categoría existe, está activa o permite una operación.

`copy(@MappingTarget ...)` actualiza la Entity recuperada sin sobrescribir ID,
estado ni fecha. La relación persistente también la asigna explícitamente el
Service con una categoría validada. El dominio contiene una `Categoria` separada
de `CategoriaEntity`; no transporta una sesión JPA ni anotaciones de persistencia.

Lombok genera accesores, constructores y builders. La Entity emplea
`@Getter`/`@Setter`, sin `@Data`, siguiendo Categoría. El ID usa `IDENTITY`,
compatible con `SERIAL`. PostgreSQL genera `fecha_creacion` y Hibernate recupera
el valor con `@Generated(event=INSERT)`; el cliente no decide esa fecha.

## Validación, reglas y transacciones

POST y PUT exigen `nombre` no vacío, máximo 100 caracteres; `descripcion` es
opcional, máximo 255; `idCategoria` es obligatorio y positivo. Los textos se
recortan en sus extremos. PUT reemplaza los campos editables completos: una
descripción nula u omitida se guarda como `NULL`. No se incorpora PATCH.

| Regla | Comportamiento |
|---|---|
| RN-S4A-01 | Crear o mover exige categoría existente; padre inexistente → 404 |
| RN-S4A-02 | Crear o mover exige padre activo; padre inactivo → 409 |
| RN-S4A-03 | Nombre único sin distinguir mayúsculas dentro de la misma categoría; duplicado → 409; otro padre puede repetirlo |
| RN-S4A-04 | Una subcategoría inactiva conserva reservado su nombre dentro de su categoría |
| RN-S4A-05 | GET devuelve solo activas; detalle inactivo → 404 |
| RN-S4A-06 | DELETE cambia `activo=false` y guarda; éxito → 204, sin borrado físico |
| RN-S4A-07 | PUT sobre una subcategoría inactiva → 404 |
| RN-S4A-08 | PUT excluye el propio ID de la búsqueda de duplicados; mantener el nombre está permitido |
| RN-S4A-09 | Cambiar de categoría repite existencia, estado activo y unicidad en el destino |
| RN-S4A-10 | DELETE de categoría con subcategorías activas → 409; el padre permanece activo |
| RN-S4A-11 | GET jerárquico de categoría inexistente o inactiva → 404 |

Las consultas usan transacciones de solo lectura. Las escrituras validan y
persisten dentro de `@Transactional`. Crear o mover una hija bloquea la fila de
su padre destino durante la validación. La baja de Categoría bloquea esa misma
fila antes de consultar hijos activos. Así se serializan ambas operaciones:
si gana el alta, la baja encuentra hijos y devuelve 409; si gana la baja, el
alta encuentra un padre inactivo y devuelve 409.

PUT y DELETE de Subcategoría usan bloqueo pesimista de la hija. Si DELETE
termina primero, un PUT que espera debe volver a comprobar el estado y devolver
404, sin reactivar la fila. Estos bloqueos protegen las escrituras realizadas
por los servicios; una modificación SQL externa debe respetar las mismas reglas.

La validación de hijos pertenece a `CategoriaService`, mediante
`SubcategoriaRepository`, y no al Controller. Sin ella, una baja lógica del padre
dejaría hijas activas bajo una categoría no disponible en la API normal.

**Regla diferida:** «No desactivar Subcategoría con Equipos activos» se aplicará
cuando exista la vertical de Equipo. Sprint 4A no crea `EquipoEntity`,
`EquipoRepository` ni `EquipoService` para adelantarla.

## Unicidad y Flyway V7

V1 tenía una restricción de nombre y categoría sensible a mayúsculas. V7 añade
un índice único sobre `(id_categoria, UPPER(nombre))`, incluyendo filas inactivas.
El Service anticipa conflictos con `IgnoreCase`; el índice protege también
escrituras que compiten. Dos nombres equivalentes en padres distintos son válidos.

La nueva migración es
`V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql`. No cambia
V1–V6 ni elimina o fusiona datos. Antes de aplicarla, ejecuta la consulta de
duplicados de la sección SQL. Debe devolver cero filas. Si devuelve registros,
revisa los datos antes de arrancar la versión que aplica V7; no uses `repair`
para ocultar el conflicto.

El índice se llama `uq_subcategoria_categoria_nombre_ignore_case`. La migración
también comprueba duplicados antes de crearlo y se detiene si los encuentra.

Las restricciones conocidas de duplicado se traducen a 409 mediante el manejador
global, incluidos los metadatos estructurados del driver PostgreSQL cuando el
servidor usa mensajes en español. Otros fallos internos generan 500 genérico.

## Seguridad y estados HTTP

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET lista, detalle y lista por categoría | Sí | Sí | Sí |
| POST, PUT y DELETE de subcategorías | Sí | No | No |
| DELETE de categoría | Sí, sujeto a regla de hijos activos | No | No |

Se reutilizan `JwtAuthFilter`, `SecurityConfig`, usuarios y roles JPA. No cambia
el login. Los catálogos no tienen alcance por laboratorio en este sprint.
Sin token válido se devuelve 401; con un rol sin permiso, 403. Ambos controles
ocurren antes de ejecutar el negocio: para comprobar un 400, 404 o 409 de escritura
usa un token ADMIN válido.

| Estado | Significado |
|---|---|
| 200 | GET o PUT correcto |
| 201 | POST correcto, con `Location` |
| 204 | Baja lógica correcta, cuerpo vacío |
| 400 | Validación, JSON o formato de ID inválidos |
| 401 | Token ausente, inválido o vencido |
| 403 | Rol sin permiso |
| 404 | Hija inexistente/inactiva o padre inexistente; padre inactivo en GET jerárquico |
| 409 | Duplicado, padre inactivo al escribir o categoría con hijas activas |
| 500 | Fallo interno sin detalles sensibles en la respuesta |

Los errores reutilizan `ApiError`, `ResourceNotFoundException`,
`ConflictException` y `GlobalExceptionHandler`. Su forma general es:

```json
{
  "timestamp": "2026-09-20T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "La categoría seleccionada está inactiva.",
  "path": "/api/subcategorias"
}
```

La fecha y el mensaje dependen del caso. Validación agrega `errors` por campo.
No se devuelven SQL, trazas, contraseñas ni hashes.

## Flujos completos

### POST

1. Postman envía JSON y `Authorization: Bearer <token>`.
2. `JwtAuthFilter` valida el token y recupera el usuario y rol vigentes;
   `SecurityConfig` exige ADMIN.
3. `SubcategoriaController` recibe `CreateSubcategoriaRequest`; `@Valid` comprueba
   las restricciones de los campos.
4. `SubcategoriaMapper` convierte el request a `Subcategoria` de dominio.
5. `SubcategoriaService` inicia la escritura, obtiene y bloquea la categoría
   mediante `CategoriaRepository` y comprueba existencia y estado.
6. Normaliza el texto y consulta duplicados dentro de esa categoría.
7. El Mapper crea `SubcategoriaEntity`; el servicio asigna la
   `CategoriaEntity` validada y controla el estado inicial activo.
8. `SubcategoriaRepository.saveAndFlush()` persiste. PostgreSQL genera ID y fecha
   y verifica FK e índice único.
9. Entity → Domain → `SubcategoriaResponse` → JSON con categoría resumida.
   El Controller devuelve 201 y `Location: /api/subcategorias/<id>`.

### GET

JWT y rol → Controller → Service de solo lectura → Repository de activas →
Entity → Mapper → Domain → Response → 200. El detalle ausente o inactivo produce
404. La ruta jerárquica valida primero que la categoría esté activa y después
lista solo sus hijas activas. Una categoría activa sin hijas devuelve `[]`.

### PUT

JWT ADMIN → validación de `UpdateSubcategoriaRequest` → dominio → Service →
búsqueda bloqueante de hija activa → validación del padre destino → duplicados
excluyendo el ID propio → Mapper `copy` de campos editables → asignación del padre
validado → guardado → respuesta 200. Conserva ID, estado y fecha. Si cualquier
regla falla, no persiste el cambio de nombre, descripción ni categoría.

### DELETE

JWT ADMIN → Controller → Service → búsqueda bloqueante de hija activa →
`setActivo(false)` → guardado → 204 sin cuerpo. No hay `repository.delete()`.
GET, PUT y DELETE posteriores sobre esa hija dan 404, pero SQL conserva la fila.
Al borrar una categoría, su Service comprueba además que no tenga hijas activas.

## Postman: preparación

Inicia el backend según el [README](../README.md#arranque-rápido-en-windows).
Debe aparecer `Started InventarioApplication` y la terminal debe permanecer
abierta. `ECONNREFUSED` significa que Postman no logró conectar al servidor;
no es un error de contraseña ni una respuesta 401 del backend.

Las pruebas crean datos y realizan bajas lógicas. Usa una base dedicada
`inventario_verificacion_*` para repetirlas libremente, o categorías creadas
expresamente para la comprobación. Los nombres dados de baja siguen reservados:
para una nueva ejecución cambia el sufijo de corrida.

Configura un Environment en Postman y selecciónalo:

| Variable | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `tokenAdmin` | Token obtenido al iniciar sesión como `marko` |
| `tokenGestor` | Token de `aldo` |
| `tokenLector` | Token de `romel` |
| `categoriaAId` | ID real de una categoría activa de prueba |
| `categoriaBId` | ID real de otra categoría activa de prueba |
| `subcategoriaId` | ID devuelto por el caso 2 |
| `subcategoriaBId` | ID devuelto por el caso 3 |
| `categoriaTemporalId` | ID de la categoría temporal del caso 16 o 18 |
| `corrida` | Sufijo nuevo, por ejemplo `20260920-01`; cámbialo al repetir todo |
| `categoriaInexistenteId` | Un entero positivo cuya ausencia se confirma en el caso 10 |
| `subcategoriaApoyoId` | ID de la hija de apoyo creada en el caso 17 |
| `subcategoriaOtraCategoriaId` | ID creado en el caso 13, para seguimiento |

Los IDs son números en los cuerpos JSON: escribe `{{categoriaAId}}` sin comillas.
No asumas los IDs de las semillas. Los valores de `corrida` sí van dentro de
cadenas JSON. Conserva tokens y contraseñas como valores locales; no los exportes.

### Obtener los tres tokens

Para cada usuario realiza **POST `{{baseUrl}}/api/auth/login`**, Authorization
**No Auth**, Headers **`Content-Type: application/json`**, Body **raw → JSON**:

```json
{
  "userName": "marko",
  "password": "<contraseña demo configurada>"
}
```

Sustituye el marcador por la contraseña real de esa cuenta. La respuesta esperada
es 200 con `accessToken`, `tokenType: "Bearer"`, `expiresIn` y `usuario`.
Copia únicamente `accessToken` a `tokenAdmin`, sin anteponer `Bearer`.
Repite cambiando `userName` por `aldo` y `romel`, y guarda `tokenGestor` y
`tokenLector`. También puedes guardar el token desde Scripts → Post-response:

```javascript
pm.test("Login correcto", () => pm.response.to.have.status(200));
if (pm.response.code === 200) {
  pm.environment.set("tokenAdmin", pm.response.json().accessToken);
}
```

Cambia el nombre de variable del script para GESTOR y LECTOR. Si el token vence
o reinicias con otra clave JWT, vuelve a iniciar sesión. Las cuentas demo se
crean con el perfil `dev` en una base nueva; reiniciar no cambia su contraseña.

### Convenciones para las 21 pruebas

En cada prueba selecciona Authorization → **Bearer Token** y usa la variable
indicada. Postman construye `Authorization: Bearer <valor>`; no agregues otra
cabecera Authorization manual. Usa `Accept: application/json` en todas.
En POST y PUT añade `Content-Type: application/json` y Body → raw → JSON.
GET y DELETE llevan Body → none y no necesitan Content-Type. El caso 21 usa
No Auth y exige eliminar cualquier Authorization heredada o manual.

Las respuestas aproximadas siguientes omiten campos solo para facilitar la
lectura. Una respuesta de subcategoría completa tiene esta forma:

```json
{
  "id": 10,
  "nombre": "Subcategoria Sprint 4A 20260920-01",
  "descripcion": "Subcategoría creada para validar Sprint 4A",
  "activo": true,
  "fechaCreacion": "2026-09-20T12:00:00Z",
  "categoria": { "id": 20, "nombre": "Categoría de prueba A" }
}
```

Los números y la fecha son ilustrativos. Comprueba tus IDs y fechas reales.

### 1. Listar categorías y obtener IDs

- **Método y URL:** GET `{{baseUrl}}/api/categorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 200, arreglo de categorías con `id`, `nombre` y `activo:true`.
- **Comprobación:** elige dos categorías activas distintas y guarda sus IDs en
  `categoriaAId` y `categoriaBId`; comprueba que ambos GET de detalle devuelvan 200.

Para evitar dar de baja datos existentes, crea primero dos categorías destinadas
a esta corrida si no tienes dos de prueba. Cada una usa POST
`{{baseUrl}}/api/categorias`, Bearer `{{tokenAdmin}}`, Content-Type JSON y:

```json
{ "nombre": "Categoria A Sprint 4A {{corrida}}", "descripcion": "Prueba manual" }
```

Repite con `Categoria B Sprint 4A {{corrida}}`. Ambos deben devolver 201 y un ID.
Después ejecuta el GET anterior y selecciona esos IDs. No uses números supuestos.

### 2. Crear subcategoría

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Sprint 4A {{corrida}}",
  "descripcion": "Subcategoría creada para validar Sprint 4A",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 201, `Location`, respuesta completa con `activo:true` y
`categoria.id` igual a A. Guarda `id` como `subcategoriaId`.
**Reglas:** RN-S4A-01/02 y persistencia JPA de la relación.

### 3. Crear segunda subcategoría

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Secundaria 4A {{corrida}}",
  "descripcion": "Segunda prueba",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 201, respuesta con nombre secundario y padre A. Guarda `id` como
`subcategoriaBId`. **Reglas:** RN-S4A-01/02/03; un padre admite varias hijas.

### 4. Listar subcategorías activas

- **Método y URL:** GET `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 200, arreglo de respuestas con `activo:true`, incluyendo los IDs
  de los casos 2 y 3. Cada elemento incluye `categoria`.
- **Regla:** RN-S4A-05.

### 5. Obtener por ID

- **Método y URL:** GET `{{baseUrl}}/api/subcategorias/{{subcategoriaId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 200, `id` solicitado, nombre del caso 2 y
  `categoria: {"id": <categoriaAId>, "nombre": "..."}`.
- **Regla:** RN-S4A-05 y representación controlada del padre, sin Entity completa.

### 6. Listar por categoría

- **Método y URL:** GET `{{baseUrl}}/api/categorias/{{categoriaAId}}/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 200, arreglo que contiene las dos hijas creadas. Todos sus
  elementos tienen `activo:true` y `categoria.id` igual a A.
- **Reglas:** RN-S4A-05/11.

### 7. Actualizar y mantener el nombre propio

- **Método y URL:** PUT `{{baseUrl}}/api/subcategorias/{{subcategoriaId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Sprint 4A Actualizada {{corrida}}",
  "descripcion": "Descripción actualizada",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 200, nombre y descripción actualizados; mismo ID y fecha, padre A.
Repite exactamente el PUT: debe devolver 200 de nuevo, sin falso duplicado.
**Reglas:** RN-S4A-07/08; PUT completo.

### 8. Cambiar de categoría

- **Método y URL:** PUT `{{baseUrl}}/api/subcategorias/{{subcategoriaId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Sprint 4A Actualizada {{corrida}}",
  "descripcion": "Ahora asociada a otra categoría",
  "idCategoria": {{categoriaBId}}
}
```

**Esperado:** 200, mismo ID y `categoria.id` igual a B. Repite las listas por A
y B: esta hija deja de aparecer en A y aparece en B. **Regla:** RN-S4A-09.

### 9. Nombre vacío

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{ "nombre": "", "descripcion": "Inválida", "idCategoria": {{categoriaAId}} }
```

**Esperado:** 400, `ApiError` con `status:400` y `errors.nombre`; no crea fila.
**Comprobación:** Bean Validation. Repite con `idCategoria` nulo, omitido, cero
o negativo: también debe devolver 400, con error del campo correspondiente.

### 10. Categoría inexistente

Antes, prueba GET `{{baseUrl}}/api/categorias/999999` con Bearer
`{{tokenAdmin}}`, Accept JSON y sin body. Solo si devuelve 404 y SQL confirma
que el ID no está almacenado, guarda `999999` en `categoriaInexistenteId`.
Si existe, elige otro entero positivo y repite: un GET 404 también puede
significar inactividad, por eso para este caso verifica la ausencia en SQL.

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Categoria inexistente test {{corrida}}",
  "descripcion": "No debe guardarse",
  "idCategoria": {{categoriaInexistenteId}}
}
```

**Esperado:** 404, `ApiError` con `status:404` y mensaje de categoría inexistente;
no inserta fila. **Regla:** RN-S4A-01. Puedes repetir el PUT del caso 8 cambiando
solo el padre por este ID: debe devolver 404 y conservar el padre B anterior.

### 11. Duplicado en la misma categoría

Repite el alta del caso 3, que continúa activa en A:

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Secundaria 4A {{corrida}}",
  "descripcion": "Debe rechazarse",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 409, `ApiError` con `status:409` y mensaje de duplicado; una sola
fila conserva ese nombre en A. **Regla:** RN-S4A-03.

### 12. Duplicado ignorando mayúsculas

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "subcategoria secundaria 4a {{corrida}}",
  "descripcion": "No debe duplicar cambiando mayúsculas",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 409, error de duplicado, sin nueva fila. **Regla:** RN-S4A-03 e
índice V7. Conserva el mismo `corrida`; solo cambia las letras del nombre.

### 13. Mismo nombre en otra categoría

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Secundaria 4A {{corrida}}",
  "descripcion": "Mismo nombre, otro padre",
  "idCategoria": {{categoriaBId}}
}
```

**Esperado:** 201, nuevo ID y padre B. Guarda el ID como
`subcategoriaOtraCategoriaId`. **Regla:** RN-S4A-03; unicidad dentro del padre.

### 14. Baja lógica

- **Método y URL:** DELETE `{{baseUrl}}/api/subcategorias/{{subcategoriaBId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 204, cuerpo vacío.

Después ejecuta GET y un segundo DELETE sobre esa misma URL, con idéntica
autorización y sin body: ambos deben devolver 404 (`ApiError`). Un PUT al mismo
ID con el JSON válido del caso 3 también debe devolver 404. SQL conserva la fila
con `activo=false`; ya no aparece en listas activas.
**Reglas:** RN-S4A-05/06/07.

### 15. El nombre de la inactiva sigue reservado

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Subcategoria Secundaria 4A {{corrida}}",
  "descripcion": "No debe reemplazar la fila inactiva",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 409, error de duplicado. La hija inactiva de A mantiene su ID y
estado; la de B sigue siendo otra fila válida. **Regla:** RN-S4A-04.

### 16. Padre inactivo

Prepara un padre sin hijas: POST `{{baseUrl}}/api/categorias`, Bearer
`{{tokenAdmin}}`, Accept JSON y Content-Type JSON:

```json
{ "nombre": "Categoria temporal 4A {{corrida}}", "descripcion": "Padre de prueba" }
```

Espera 201 y guarda `id` en `categoriaTemporalId`. Después DELETE
`{{baseUrl}}/api/categorias/{{categoriaTemporalId}}`, mismo token, Accept JSON,
sin body: espera 204, cuerpo vacío. Ahora realiza:

- **Método y URL:** POST `{{baseUrl}}/api/subcategorias`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers:** Accept JSON y Content-Type JSON.
- **Body:**

```json
{
  "nombre": "Hija de padre inactivo {{corrida}}",
  "descripcion": "Debe ser rechazada",
  "idCategoria": {{categoriaTemporalId}}
}
```

**Esperado:** 409, mensaje «La categoría seleccionada está inactiva.»; SQL no
muestra una hija nueva. **Regla:** RN-S4A-02. Repite el PUT del caso 8 hacia este
padre: 409, sin mover la hija. Finalmente GET
`{{baseUrl}}/api/categorias/{{categoriaTemporalId}}/subcategorias`, mismo token,
Accept JSON, sin body: 404. Esta última consulta comprueba RN-S4A-11.

### 17. Impedir la baja de categoría con hijas activas

La hija del caso 2 ya se movió a B y la del caso 3 se dio de baja. Por eso crea
**una hija activa de apoyo en A** antes de intentar la baja; no dependas de semillas.
POST `{{baseUrl}}/api/subcategorias`, Bearer `{{tokenAdmin}}`, Accept JSON y
Content-Type JSON:

```json
{
  "nombre": "Hija de apoyo A 4A {{corrida}}",
  "descripcion": "Impide la baja del padre",
  "idCategoria": {{categoriaAId}}
}
```

Espera 201, guarda `id` en `subcategoriaApoyoId` y confirma que aparece en GET
`{{baseUrl}}/api/categorias/{{categoriaAId}}/subcategorias` con el mismo token.

- **Método y URL:** DELETE `{{baseUrl}}/api/categorias/{{categoriaAId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 409, mensaje que indica que la categoría contiene subcategorías
  activas. GET de categoría A debe seguir devolviendo 200 y `activo:true`.
- **Regla:** RN-S4A-10.

### 18. Permitir baja de categoría sin hijas activas

Crea otra categoría vacía con POST `{{baseUrl}}/api/categorias`, Bearer
`{{tokenAdmin}}`, Accept JSON y Content-Type JSON:

```json
{ "nombre": "Categoria vacia 4A {{corrida}}", "descripcion": "Sin hijas" }
```

Espera 201 y guarda su ID en `categoriaTemporalId`, reemplazando el valor anterior
solo después de terminar el caso 16. GET
`{{baseUrl}}/api/categorias/{{categoriaTemporalId}}/subcategorias`, con el mismo
token y sin body, debe devolver 200 y `[]`.

- **Método y URL:** DELETE `{{baseUrl}}/api/categorias/{{categoriaTemporalId}}`.
- **Authorization:** Bearer `{{tokenAdmin}}`.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 204, cuerpo vacío. SQL conserva la categoría con `activo=false`.
- **Regla:** RN-S4A-10 permite la baja si no hay hijas activas.

### 19. Seguridad de GESTOR

- **Consulta:** GET `{{baseUrl}}/api/subcategorias`, Bearer `{{tokenGestor}}`,
  Accept JSON y sin body → 200, arreglo de activas.
- **Escritura:** POST `{{baseUrl}}/api/subcategorias`, Bearer `{{tokenGestor}}`,
  Accept JSON y Content-Type JSON, body:

```json
{
  "nombre": "Intento gestor 4A {{corrida}}",
  "descripcion": "No autorizado",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 403, `ApiError` con `status:403`; no crea fila. Repite también GET
de detalle y jerárquico: 200. PUT válido sobre `subcategoriaId` y DELETE sobre
`subcategoriaApoyoId` con este token deben devolver 403. **Comprobación:**
GESTOR consulta el catálogo, no lo administra.

### 20. Seguridad de LECTOR

- **Consulta:** GET `{{baseUrl}}/api/subcategorias`, Bearer `{{tokenLector}}`,
  Accept JSON y sin body → 200, arreglo de activas.
- **Escritura:** POST `{{baseUrl}}/api/subcategorias`, Bearer `{{tokenLector}}`,
  Accept JSON y Content-Type JSON, body:

```json
{
  "nombre": "Intento lector 4A {{corrida}}",
  "descripcion": "No autorizado",
  "idCategoria": {{categoriaAId}}
}
```

**Esperado:** 403, `ApiError` con `status:403`; no crea fila. También puede
consultar el detalle y la lista jerárquica (200); PUT y DELETE deben dar 403.
**Comprobación:** LECTOR tiene lectura sin escritura.

### 21. Sin token

- **Método y URL:** GET `{{baseUrl}}/api/subcategorias`.
- **Authorization:** No Auth; elimina la cabecera Authorization si existe.
- **Headers/body:** Accept JSON; sin body.
- **Esperado:** 401, `ApiError` con `status:401` y mensaje genérico de
  autenticación; no devuelve el catálogo.
- **Comprobación:** JWT obligatorio. Un token alterado o vencido también da 401.

No borres filas por SQL para limpiar estas pruebas en la base habitual. Si usaste
datos propios de prueba y quieres darlos de baja, utiliza la API con ADMIN:
primero las hijas activas y después sus categorías. Para repetir, cambia `corrida`.

## Comprobación manual en PostgreSQL / pgAdmin

Abre Query Tool en la misma base configurada en `DB_URL`. Las variables
`{{...}}` pertenecen a Postman: no las pegues directamente en SQL. Sustituye los
IDs o nombres de los filtros por valores reales de tus respuestas.

```sql
-- Relación real, estado y fecha; incluye también filas inactivas.
SELECT
    sc.id_subcategoria,
    sc.nombre,
    sc.descripcion,
    sc.activo,
    sc.fecha_creacion,
    c.id_categoria,
    c.nombre AS categoria,
    c.activo AS categoria_activa
FROM subcategoria sc
JOIN categoria c ON c.id_categoria = sc.id_categoria
ORDER BY sc.id_subcategoria;

-- Coincide con las hijas visibles en el GET de lista.
SELECT *
FROM subcategoria
WHERE activo = TRUE
ORDER BY id_subcategoria;

-- Debe devolver cero filas. Ejecutar también ANTES de aplicar V7.
SELECT
    id_categoria,
    UPPER(nombre) AS nombre_comparable,
    COUNT(*) AS cantidad,
    ARRAY_AGG(id_subcategoria ORDER BY id_subcategoria) AS ids
FROM subcategoria
GROUP BY id_categoria, UPPER(nombre)
HAVING COUNT(*) > 1;

-- Debe mostrar un índice UNIQUE sobre id_categoria y upper(nombre).
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'subcategoria'
ORDER BY indexname;

-- Tras aplicar V7, versiones 1–7 con success=true.
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Consistencia tras escrituras de la API: se esperan cero filas.
SELECT sc.id_subcategoria, sc.nombre, sc.id_categoria
FROM subcategoria sc
JOIN categoria c ON c.id_categoria = sc.id_categoria
WHERE sc.activo = TRUE AND c.activo = FALSE;

-- Antes del caso 10: este ID debe NO existir, ni siquiera inactivo.
SELECT id_categoria, nombre, activo
FROM categoria
WHERE id_categoria = 999999;
```

La baja del caso 14 conserva su fila con `activo=false`. El caso 8 cambia
`id_categoria` sin cambiar ID ni fecha. Los intentos rechazados no agregan filas.
En el caso 17 la categoría sigue activa; en 16 y 18 los padres temporales quedan
inactivos. Para mirar solo la corrida, usa por ejemplo:

```sql
-- Sustituye el sufijo por tu valor de corrida.
SELECT id_subcategoria, nombre, id_categoria, activo
FROM subcategoria
WHERE nombre LIKE '%20260920-01'
ORDER BY id_subcategoria;
```

Para comprobar persistencia, detén Spring Boot con Ctrl+C y vuelve a iniciarlo
con la misma `DB_URL`. Obtén tokens nuevos si cambió la clave JWT. Repite GET de
la hija movida, GET de la hija inactiva y SQL: el movimiento debe conservarse,
la inactiva debe seguir dando 404 y su fila debe continuar almacenada.

## Tests automáticos

La cobertura de Sprint 4A incluye Mapper (relación y campos protegidos), Service
(padre existente/activo, duplicados por padre, actualización y baja), HTTP y
seguridad (400/401/403/404/409 y permisos de los tres roles) y regresiones de
concurrencia de hija/padre. Se conserva la suite de Categoría y JWT.

| Clase | Casos definidos | Cobertura |
|---|---:|---|
| `mapper/SubcategoriaMapperTest` | 4 | Requests y padre por ID; copia conserva campos del servidor y padre; Entity/listas y resumen de respuesta |
| `service/SubcategoriaServiceTest` | 17 | Normalización, validación del padre, duplicados dentro del padre, consultas, PUT y baja lógica |
| `SubcategoriaIntegrationTests` | 28 | HTTP real, validación, roles JWT, relación persistida, baja lógica y reglas de categoría |
| `SubcategoriaConcurrenciaTests` | 6 | Alta/movimiento frente a baja del padre, PUT frente a DELETE de hija y duplicado protegido por el índice |
| `service/CategoriaServiceTest` | 1 nuevo | Categoría con hijas activas no puede darse de baja |
| `exception/GlobalExceptionHandlerTest` | 3 nuevos | Restricciones de subcategoría, error PostgreSQL en español y respuesta sin detalles SQL |

Estos son los casos definidos en el código; la siguiente anotación distingue
su ejecución efectiva de su existencia.

**Ejecución de cierre: 99 ejecutadas, 99 aprobadas, 0 fallidas, 0 errores y
0 omitidas.** Los 59 casos nuevos complementan los 40 anteriores. Se utilizó
`inventario_verificacion_s4a_20260920_a93d`; al terminar se verificó que no quedaron
filas de prueba y se eliminó esa base temporal. `bootJar` también se construyó
correctamente. Las pruebas que modifican datos no usaron la base habitual.

En `inventario_laboratorios` se aplicó V7 y se comprobó el índice y el historial
Flyway V1–V7. La comparación de datos completos antes/después confirmó que las
2 categorías, 4 subcategorías y 3 usuarios no cambiaron. Con la aplicación
empaquetada se verificaron los tres logins, la lista, el detalle y la lista por
categoría para los tres roles (200), y la ausencia de JWT (401). No se provocaron
pruebas destructivas en esa base. La guía de Postman queda para comprobación del
usuario; no se ejecutó automáticamente su interfaz.

Para pruebas unitarias, desde `backend/inventario`:

```powershell
.\gradlew.bat test --rerun-tasks --tests '*SubcategoriaMapperTest' --tests '*SubcategoriaServiceTest' --tests '*CategoriaServiceTest' --tests '*GlobalExceptionHandlerTest'
```

Para la suite completa, crea una base exclusiva desde pgAdmin, conectado a
`postgres`, con autocommit y ejecutando por separado:

```sql
CREATE DATABASE inventario_verificacion_manual_4a;
```

En PowerShell configura `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, perfil `dev` y
`DEMO_USER_PASSWORD` siguiendo el [README](../README.md#variables-de-entorno-y-ejecución-en-powershell).
Después, desde el backend, cambia la URL solo durante la prueba:

```powershell
$previousDbUrl4a = $env:DB_URL
try {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_verificacion_manual_4a'
    .\gradlew.bat test --rerun-tasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'La suite de pruebas falló.' }
} finally {
    $env:DB_URL = $previousDbUrl4a
}
```

Las pruebas que modifican PostgreSQL requieren una base cuyo nombre empiece por
`inventario_verificacion_`. No deben ejecutarse pruebas destructivas contra
`inventario_laboratorios`. Si las pruebas de integración aparecen omitidas,
revisa la URL y las condiciones de habilitación: omitidas no significa aprobadas.
El reporte se genera en `backend/inventario/build/reports/tests/test/index.html`
y los resultados XML en `build/test-results/test`.

## Archivos creados y modificados

Las rutas Java siguientes parten de
`backend/inventario/src/main/java/com/utec/inventario/`:

| Archivo nuevo | Propósito |
|---|---|
| `entity/SubcategoriaEntity.java` | Mapeo JPA y relación obligatoria con Categoría |
| `domain/Subcategoria.java` | Modelo de negocio separado de persistencia |
| `dto/request/CreateSubcategoriaRequest.java` | Entrada y validaciones de creación |
| `dto/request/UpdateSubcategoriaRequest.java` | Entrada y validaciones de PUT completo |
| `dto/response/SubcategoriaResponse.java` | Respuesta pública sin Entity |
| `dto/response/CategoriaResumenResponse.java` | ID y nombre del padre |
| `mapper/SubcategoriaMapper.java` | Conversiones MapStruct y copia de campos editables |
| `repository/SubcategoriaRepository.java` | Actividad, búsqueda por padre, duplicados y bloqueos |
| `service/SubcategoriaService.java` | Reglas de negocio y transacciones de la hija |
| `controller/SubcategoriaController.java` | API REST de Subcategoría |
| `controller/CategoriaSubcategoriaController.java` | GET jerárquico que delega en `SubcategoriaService.listarPorCategoria` |

También se agrega
`backend/inventario/src/main/resources/db/migration/V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql`
para proteger la unicidad y los tests de Sprint 4A bajo
`backend/inventario/src/test/java/com/utec/inventario/`.

| Archivo modificado | Motivo |
|---|---|
| `service/CategoriaService.java` | Bloquear baja del padre con hijas activas |
| `config/SecurityConfig.java` | Permitir lecturas de los tres roles y escrituras solo ADMIN en las nuevas rutas |
| `exception/GlobalExceptionHandler.java` | Traducir restricciones conocidas de nombre de subcategoría a 409 |
| Tests existentes de Categoría y errores | Incorporar la nueva dependencia y proteger regresiones |
| `README.md` | Estado de Sprint 4A, rutas y nueva migración |
| `docs/reglas-negocio.md` | RN-31 y RN-32, sin renumerar RN-01 a RN-30 |
| `docs/matriz-permisos.md` | Permisos explícitos de las seis rutas |

Se crean además este documento y `docs/sprint-4.md`. `CategoriaRepository` se
reutiliza sin cambios: el servicio busca el padre con
`findForUpdateByIdCategoriaAndActivoTrue` y, cuando no lo encuentra, distingue
ausencia e inactividad mediante `existsById`. `CategoriaController` tampoco
incorpora lógica de Subcategoría: el pequeño controller jerárquico delega en el
mismo servicio de la hija. Las lecturas del Repository de Subcategoría usan
`@EntityGraph` para cargar el padre sin una consulta adicional por cada fila;
las búsquedas con bloqueo de hija se mantienen separadas.

## Pendientes

La suite, el arranque y la migración de Sprint 4A están verificados. Queda
disponible la comprobación manual del usuario en Postman.
Sprint 4B, las verticales de Sede, Área y Laboratorio, el alcance por laboratorio,
UsuarioLaboratorio, Equipo, MovimientoEquipo y la administración completa de
usuarios siguen pendientes. Las tablas de esas funcionalidades no equivalen a
una API implementada. La regla de Equipos activos no se adelanta en Sprint 4A.
