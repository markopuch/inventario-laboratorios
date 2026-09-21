# Sprint 4B–4D — Organización: Sede → Área → Laboratorio

Esta guía describe las tres verticales organizacionales y permite comprobarlas
manualmente. El [registro de Sprint 4](sprint-4.md) conserva el cierre histórico
de Sprint 4A y registra los resultados de este incremento.

## 1. Objetivo y esquema de partida

Sprint 4B implementa Sede, 4C implementa Área y 4D implementa Laboratorio.
Las tablas ya existían desde V1; este incremento agrega sus APIs, servicios,
modelos, reglas, seguridad y pruebas. Una tabla existente no equivale a una API.
Se conservan Java 21, Spring Boot 4.1.1, Gradle 9.7.1, MapStruct 1.6.3,
Lombok y JJWT 0.13.0, así como la organización de paquetes y estilo del curso.

La inspección de V1 y PostgreSQL confirmó estas columnas editables:

| Recurso | Campos | Restricciones de tamaño |
|---|---|---|
| Sede | `nombre`, `direccion`, `distrito`, `departamento` | 100, 200, 100 y 100 caracteres |
| Área | `nombre`, `descripcion`, `id_sede` | 100 y 255 caracteres; FK obligatoria |
| Laboratorio | `nombre`, `codigo`, `ubicacion`, `id_area` | 100, 30 y 200 caracteres; FK obligatoria |

Las tres tablas ya tenían `activo NOT NULL DEFAULT TRUE` y
`fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`: no se agregan
columnas redundantes. Las PK son `SERIAL`. Sede no tiene nombre único.
Laboratorio ya tenía código único global, aunque una versión histórica de
RN-13 lo describía por Área; se corrigió la regla documental.

## 2. Arquitectura y recorrido de una solicitud

```text
HTTP / JSON → JwtAuthFilter / Spring Security → Controller → Request + @Valid
    → Mapper → Domain → Service transaccional
    → Entity JPA → Repository → PostgreSQL

Sede (1) ← Área (N)
Área (1) ← Laboratorio (N)
```

En POST y PUT, el Controller convierte la entrada a dominio y delega. El
Service normaliza texto, comprueba las reglas, resuelve las entidades padre y
persiste. La Entity guardada vuelve a dominio dentro de la transacción; el
Controller devuelve un Response mediante MapStruct. En GET se consulta una
entidad activa; en DELETE se modifica su estado. Ni JSON ni Controller exponen
entidades JPA.

## 3. Sede como raíz

`SedeEntity` mapea `sede`; `Sede` es un modelo separado sin JPA. Sede no recibe
un ID padre. Los campos opcionales son dirección, distrito y departamento.
PostgreSQL genera ID y fecha; el Service fija `activo=true` al crear.
No se impone unicidad a `nombre`, porque ni el esquema vigente ni las reglas
establecen esa restricción. Dos sedes con igual nombre pueden tener IDs distintos.

## 4. Área → Sede con ManyToOne

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_sede", nullable = false)
private SedeEntity sede;
```

Muchas áreas pueden pertenecer a la misma sede. La propiedad Java representa
la relación y PostgreSQL guarda su FK en `id_sede`. `LAZY` permite diferir la
carga del padre; las consultas de lectura que lo necesitan usan `@EntityGraph`.
El dominio `Area` contiene una `Sede` de dominio, sin anotaciones de persistencia.

## 5. Laboratorio → Área con ManyToOne

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_area", nullable = false)
private AreaEntity area;
```

Cada laboratorio tiene un área obligatoria. `Laboratorio` contiene un `Area`
de dominio. El Response expone solo `area: {id, nombre}`; no agrega un árbol
completo de sede, áreas, laboratorios y equipos.

## 6. Por qué no hay OneToMany bidireccional

`SedeEntity` no mantiene una colección de áreas y `AreaEntity` no mantiene
laboratorios. Para listar hijos consultamos su Repository mediante el ID del
padre. Así las relaciones son simples, evitamos sincronizar dos lados de una
asociación y el contrato JSON no puede recorrer ciclos de entidades.
Tampoco se agrega una colección de equipos en Laboratorio.

## 7. ID del padre en Request y objeto padre en Entity

Los requests de Área reciben `idSede`; los de Laboratorio reciben `idArea`.
Ambos campos requieren `@NotNull` y `@Positive`. El ID es una referencia enviada
por el cliente, no una prueba de que exista una fila activa.

El Mapper construye la referencia de dominio; el Service consulta y bloquea al
padre real. Tras validarlo, asigna `SedeEntity` o `AreaEntity` a la hija. Los
requests no ofrecen campos `id`, `activo` ni `fechaCreacion` editables.
Nombre y código obligatorios usan `@NotBlank`; todos los textos usan `@Size`
según V1. Se aplica `trim`. PUT reemplaza los campos editables: omitir un texto
opcional equivale a dejarlo en `null`. No se implementa PATCH.

## 8. Services: reglas y transacciones

`SedeService`, `AreaService` y `LaboratorioService` concentran el negocio.
Las lecturas usan transacciones de solo lectura y las escrituras `@Transactional`.
Los servicios conservan ID, estado y fecha en PUT y usan `saveAndFlush` para
detectar conflictos de base de datos dentro de la operación.
Un Controller no pregunta por duplicados ni decide si puede desactivar un padre.

## 9. Mappers: transformaciones

Los tres mappers siguen `componentModel="spring"` y
`unmappedTargetPolicy=ReportingPolicy.ERROR`: un campo sin mapear debe resolverse
durante compilación. `convert`, `toEntity`, `toResponse` y `copy(@MappingTarget…)`
transforman estructuras, sin consultar repositorios ni decidir permisos.
La copia de PUT ignora los campos controlados por el servidor. En Área y
Laboratorio también conserva la relación hasta que el Service asigna el padre
validado. Los resúmenes de padre solo tienen ID y nombre.

## 10. Repositories: consultas y protección en persistencia

Los tres repositorios extienden `JpaRepository<…, Integer>`. Hay consultas para
listar activos, obtener un activo por ID y buscarlo con bloqueo de escritura.
Área agrega consultas por sede, duplicados por sede y existencia de hijas activas.
Laboratorio agrega consultas por área, código global y existencia de hijos activos.
Las búsquedas de duplicados incluyen inactivos y excluyen el ID propio en PUT.
Las consultas de hijos y padres se expresan mediante JPA; no se introduce SQL
nativo de aplicación para estas reglas. `@EntityGraph` evita una consulta
adicional por cada padre al convertir una lista.

## 11. Baja lógica

DELETE carga el recurso activo con bloqueo, comprueba sus restricciones y guarda
`activo=false`. Devuelve `204 No Content`, con cuerpo vacío. No usa
`repository.delete()`. GET lista solo activos; detalle, PUT y DELETE de una fila
inactiva devuelven 404. La fila y sus referencias permanecen en PostgreSQL.
Un nombre de Área sigue reservado dentro de su sede después de la baja; el
código de Laboratorio sigue reservado globalmente. No se agrega reactivación.

## 12. RN-31: padre activo

| Hija | Padre necesario para crear o mover |
|---|---|
| Subcategoría | Categoría activa |
| Área | Sede activa |
| Laboratorio | Área activa |

Padre inexistente devuelve 404; padre existente e inactivo devuelve 409.
La búsqueda jerárquica devuelve 404 si su padre no existe o está inactivo.
Padre activo sin hijos activos devuelve `200 []`. Estas comprobaciones se hacen
en el Service y dentro de la transacción de escritura cuando se modifica algo.

## 13. RN-32: no desactivar un padre con hijos activos

| Operación | Condición que provoca 409 |
|---|---|
| DELETE Categoría | Tiene subcategorías activas |
| DELETE Sede | Tiene áreas activas |
| DELETE Área | Tiene laboratorios activos |

El padre conserva `activo=true`. Si solo quedan hijos inactivos, su baja puede
continuar. La comprobación consulta al Repository de la hija; no necesita una
colección `@OneToMany`. Laboratorio → Equipo queda pendiente.

## 14. Mover Área de Sede

PUT de Área puede cambiar `idSede`. Primero se comprueba la hija activa y luego
la sede destino existente y activa. Se valida nombre único dentro del destino,
excluyendo el ID de la misma área. Cambia la FK; conserva ID y fecha.
Sus laboratorios siguen asociados a la misma área: no se recrean ni se cambian
sus IDs. Un conflicto revierte la operación completa.

## 15. Mover Laboratorio de Área

PUT de Laboratorio puede cambiar `idArea` si el destino existe y está activo.
La unicidad del código permanece global; cambiar de área no evita un duplicado.
Se conserva ID, fecha y estado. Este cambio organizacional no implementa
traslados de Equipo ni genera MovimientoEquipo, verticales aún pendientes.

## 16. Unicidad de Área y V8

La combinación `id_sede + UPPER(nombre)` es única, incluyendo áreas inactivas.
`Mecatrónica` y `mecatrónica` colisionan en una misma sede; se permiten en sedes
distintas. El Service anticipa el conflicto y V8 agrega
`uq_area_sede_nombre_ignore_case` para proteger escrituras concurrentes.

La migración es
`V8__area_nombre_unico_por_sede_sin_mayusculas.sql`.
Comprueba primero si hay duplicados lógicos; si los encuentra, se detiene sin
borrar ni fusionar filas. La restricción anterior `uq_area_nombre_sede` permanece.
La redundancia se conserva expresamente; no se optimizan índices en este sprint.

## 17. Código global de Laboratorio y V9

`L201` y `l201` colisionan aunque sus laboratorios pertenezcan a distintas áreas
o sedes. El índice `uq_laboratorio_codigo_ignore_case` usa `UPPER(codigo)` sin
incluir `id_area`. La migración es
`V9__laboratorio_codigo_unico_sin_mayusculas.sql`.
Los códigos de filas inactivas siguen reservados.

V9 se detiene si detecta duplicados, conserva los datos y mantiene la restricción
anterior `uq_laboratorio_codigo`. No se modifica V1 ni otra migración aplicada.
RN-13 queda alineada con la decisión global vigente desde Sprint 2.

## 18. Seguridad y errores

| Operación de organización | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| GET lista, detalle y consultas jerárquicas | Sí | Sí | Sí |
| POST, PUT, DELETE | Sí | No | No |

Son catálogos globales. No se consulta `usuario_laboratorio` ni se limita por
laboratorio asignado. Se conserva el login y el JWT existentes.

| Código | Significado |
|---|---|
| 200 | Consulta o PUT correcto |
| 201 | Creación; incluye `Location` |
| 204 | Baja lógica; sin cuerpo |
| 400 | Campo, ID o JSON inválido |
| 401 | JWT ausente, inválido o vencido |
| 403 | Usuario autenticado con rol sin permiso |
| 404 | Recurso inexistente/inactivo, padre inexistente o padre no disponible en GET jerárquico |
| 409 | Duplicado, padre inactivo o padre con hijos activos |
| 500 | Error interno con mensaje genérico, sin detalles sensibles |

Se reutilizan `ApiError`, `GlobalExceptionHandler`, `ResourceNotFoundException`
y `ConflictException`. Los nombres de las restricciones conocidas se traducen
a 409, también si PostgreSQL usa mensajes en otro idioma. Los clientes no reciben
consultas SQL, trazas, hashes ni secretos.

## 19. Concurrencia

Crear o mover una hija bloquea al padre destino con `PESSIMISTIC_WRITE`, el mismo
bloqueo que usa DELETE del padre. Si la hija confirma primero, la baja encuentra
hijos activos y responde 409. Si confirma primero la baja, crear o mover la hija
encuentra un padre inactivo y responde 409. No queda una hija activa bajo un
padre desactivado por estas operaciones.

PUT y DELETE de una misma hija bloquean su fila; una actualización que espera
una baja debe volver a comprobar que siga activa y responder 404. Los índices
únicos cierran la carrera entre comprobar duplicados e insertar. Estas garantías
pertenecen a los servicios y sus transacciones: una edición SQL externa debe
respetar las mismas reglas.

## 20. Los 17 endpoints

| Método | Ruta | Resultado correcto |
|---|---|---|
| POST | `/api/sedes` | 201 y `Location` |
| GET | `/api/sedes` | 200, lista activa |
| GET | `/api/sedes/{id}` | 200, detalle activo |
| PUT | `/api/sedes/{id}` | 200 |
| DELETE | `/api/sedes/{id}` | 204 |
| POST | `/api/areas` | 201 y `Location` |
| GET | `/api/areas` | 200, lista activa |
| GET | `/api/areas/{id}` | 200, detalle con resumen de sede |
| GET | `/api/sedes/{idSede}/areas` | 200, hijas activas de sede activa |
| PUT | `/api/areas/{id}` | 200 |
| DELETE | `/api/areas/{id}` | 204 |
| POST | `/api/laboratorios` | 201 y `Location` |
| GET | `/api/laboratorios` | 200, lista activa |
| GET | `/api/laboratorios/{id}` | 200, detalle con resumen de área |
| GET | `/api/areas/{idArea}/laboratorios` | 200, hijos activos de área activa |
| PUT | `/api/laboratorios/{id}` | 200 |
| DELETE | `/api/laboratorios/{id}` | 204 |

## 21. Postman: preparación y secuencia manual

### Arranque y variables

Desde la raíz del proyecto, con PostgreSQL iniciado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

Espera `Started InventarioApplication` y deja abierta esa terminal. Si aparece
`Port 8080 was already in use`, ya existe un proceso usando el puerto: comprueba
su identidad y cierra la instancia anterior antes de iniciar otra. Un mensaje
Gradle `BUILD SUCCESSFUL` por sí solo no demuestra que Spring Boot arrancó.
Al terminar usa `Ctrl+C`. No necesitas ejecutar tests para usar Postman.

Crea un Environment y selecciónalo. Usa estas variables:

| Variable | Valor inicial |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `corrida` | Un sufijo nuevo y corto, por ejemplo `26092101` |
| `tokenAdmin`, `tokenGestor`, `tokenLector` | Vacíos; pegar el `accessToken` del login |
| `sedeAId`, `sedeBId` | Vacíos; IDs reales devueltos por POST |
| `areaAId`, `areaBId` | Vacíos; IDs reales devueltos por POST |
| `laboratorioId` | Vacío; ID real devuelto por POST |
| `padreInexistenteId` | Vacío; entero positivo cuya ausencia compruebes con SQL |

Cada nueva ejecución completa debe tener otra `corrida`: las bajas conservan
nombres/códigos reservados. Mantén `ORG-{{corrida}}` dentro de los 30 caracteres
máximos del código. Guarda los IDs sin comillas; en los cuerpos van como
`"idSede": {{sedeAId}}` y `"idArea": {{areaAId}}`.

Para login usa `POST {{baseUrl}}/api/auth/login`, **No Auth**, headers
`Content-Type: application/json` y `Accept: application/json`, Body **raw → JSON**:

```json
{
  "userName": "marko",
  "password": "<CONTRASEÑA_DEMO>"
}
```

Reemplaza el marcador por la contraseña local de esa cuenta. Espera 200 y
`usuario.rol=ADMIN`; copia solo `accessToken` a `tokenAdmin`, sin comillas ni
prefijo `Bearer`. Repite con `aldo`/GESTOR y `romel`/LECTOR para las otras variables.
En las solicitudes protegidas selecciona **Authorization → Bearer Token** y
escribe `{{tokenAdmin}}`. Postman añade `Bearer`; no dupliques la cabecera.
Obtén nuevos tokens si vencen o cambia la clave tras reiniciar.

### Convención de las solicitudes siguientes

Salvo que un paso indique otro rol o No Auth, usa `{{tokenAdmin}}`.
Todas usan `Accept: application/json`; POST/PUT agregan
`Content-Type: application/json` y Body **raw → JSON**. GET/DELETE usan Body
**none**. Envía manualmente con **Send** y continúa solo si obtienes el resultado
esperado. Las respuestas siguientes muestran campos relevantes; los IDs y fechas
los genera tu servidor, no los copies de un ejemplo.

### Paso 1 — Crear dos sedes: POST /api/sedes

Envía `POST {{baseUrl}}/api/sedes`:

```json
{
  "nombre": "Sede A Org {{corrida}}",
  "direccion": "Av. de prueba 100",
  "distrito": "Barranco",
  "departamento": "Lima"
}
```

Espera 201 y `Location`; guarda `id → sedeAId`. Comprueba nombre, dirección,
distrito, departamento, `activo=true` y `fechaCreacion`. Repite cambiando
`nombre` a `Sede B Org {{corrida}}`; guarda `id → sedeBId`.
No uses las sedes iniciales del inventario para las bajas de esta secuencia.

### Paso 2 — Lista y detalle de Sede

| Método | URL | Esperado |
|---|---|---|
| GET | `{{baseUrl}}/api/sedes` | 200; aparecen A y B activas |
| GET | `{{baseUrl}}/api/sedes/{{sedeAId}}` | 200; ID coincide con A |
| GET | `{{baseUrl}}/api/sedes/{{sedeAId}}/areas` | 200 y `[]` antes de crear áreas |

### Paso 3 — Actualizar Sede: PUT /api/sedes/{id}

`PUT {{baseUrl}}/api/sedes/{{sedeAId}}`:

```json
{
  "nombre": "Sede A Org {{corrida}}",
  "direccion": "Av. de prueba 200",
  "distrito": "Barranco",
  "departamento": "Lima"
}
```

Espera 200. Dirección actualizada; ID, fecha y `activo` conservados.

### Paso 4 — Crear dos áreas: POST /api/areas

`POST {{baseUrl}}/api/areas`:

```json
{
  "nombre": "Area Org {{corrida}}",
  "descripcion": "Área para verificar la jerarquía",
  "idSede": {{sedeAId}}
}
```

Espera 201. Guarda `id → areaAId`. La respuesta contiene `sede` con `id` igual
a `sedeAId` y su nombre, además de `id`, `nombre`, `descripcion`, `activo` y fecha.
Repite con **otro nombre** `Area B Org {{corrida}}` y `idSede={{sedeBId}}`;
guarda `id → areaBId`. Así el movimiento posterior de A hacia B será válido.

### Paso 5 — Consultar las áreas

| Método | URL | Esperado |
|---|---|---|
| GET | `{{baseUrl}}/api/areas` | 200; contiene ambas activas |
| GET | `{{baseUrl}}/api/areas/{{areaAId}}` | 200; resumen de sede A |
| GET | `{{baseUrl}}/api/sedes/{{sedeAId}}/areas` | 200; contiene `areaAId` |

### Paso 6 — PUT propio y cambio de Sede

`PUT {{baseUrl}}/api/areas/{{areaAId}}`:

```json
{
  "nombre": "Area Org {{corrida}}",
  "descripcion": "Actualización conservando nombre propio",
  "idSede": {{sedeAId}}
}
```

Espera 200: el propio nombre no es un duplicado. Envía otra vez el mismo PUT
cambiando únicamente `idSede` a `{{sedeBId}}`. Espera 200 con `sede.id=sedeBId`.
Repite GET jerárquico de A y B: A queda `[]`; B contiene `areaAId` y `areaBId`.
El ID y fecha del área no cambian.

### Paso 7 — Crear Laboratorio: POST /api/laboratorios

`POST {{baseUrl}}/api/laboratorios`:

```json
{
  "nombre": "Laboratorio Org {{corrida}}",
  "codigo": "ORG-{{corrida}}",
  "ubicacion": "Piso de pruebas 1",
  "idArea": {{areaAId}}
}
```

Espera 201 y guarda `id → laboratorioId`. Comprueba `activo=true`, fecha y
`area: {id, nombre}`, con `area.id=areaAId`.

### Paso 8 — Consultar Laboratorio

| Método | URL | Esperado |
|---|---|---|
| GET | `{{baseUrl}}/api/laboratorios` | 200; contiene tu laboratorio |
| GET | `{{baseUrl}}/api/laboratorios/{{laboratorioId}}` | 200; datos y resumen del área |
| GET | `{{baseUrl}}/api/areas/{{areaAId}}/laboratorios` | 200; contiene `laboratorioId` |
| GET | `{{baseUrl}}/api/areas/{{areaBId}}/laboratorios` | 200 y `[]` |

### Paso 9 — PUT propio y cambio de Área

`PUT {{baseUrl}}/api/laboratorios/{{laboratorioId}}`:

```json
{
  "nombre": "Laboratorio Org {{corrida}}",
  "codigo": "ORG-{{corrida}}",
  "ubicacion": "Piso de pruebas 2",
  "idArea": {{areaAId}}
}
```

Espera 200 y ubicación actualizada; mantener el código propio está permitido.
Repite cambiando `idArea` a `{{areaBId}}`: espera 200 y `area.id=areaBId`.
GET por A queda vacío; GET por B contiene el laboratorio. ID y fecha permanecen.

### Paso 10 — Conflictos de unicidad: 409

Repite el POST de Área con `nombre="area org {{corrida}}"` e
`idSede={{sedeBId}}`: espera 409, pues A ya se movió a B.
Para demostrar que el mismo nombre sí se permite en otra sede, repite ese POST
con `idSede={{sedeAId}}`: espera 201, guarda temporalmente su ID y dale baja con
`DELETE {{baseUrl}}/api/areas/<ID recibido>` → 204. No cambies `areaAId` ni
`areaBId`. Esta baja de la hija temporal deja nuevamente a Sede A sin hijas
activas y permite continuar la secuencia.

Repite el POST de Laboratorio del paso 7 cambiando `codigo` a
`org-{{corrida}}` y manteniendo `idArea={{areaAId}}`: espera 409. El original
está ahora en B y aun así colisiona: el código es global, sin distinguir caja.
Los errores no deben crear filas ni mostrar detalles SQL.

### Paso 11 — Padres con hijos activos: 409

| Método | URL | Esperado |
|---|---|---|
| DELETE | `{{baseUrl}}/api/sedes/{{sedeBId}}` | 409: mantiene dos áreas activas |
| DELETE | `{{baseUrl}}/api/areas/{{areaBId}}` | 409: mantiene el laboratorio activo |

GET de esos padres sigue devolviendo 200 con `activo=true`.

### Paso 12 — Baja lógica de Laboratorio y código reservado

`DELETE {{baseUrl}}/api/laboratorios/{{laboratorioId}}` → 204, cuerpo vacío.
GET de ese ID → 404. Repetir DELETE → 404; PUT del paso 9 → 404.
SQL debe seguir mostrando la fila con `activo=false`.

Repite el POST del paso 7 con el mismo código: 409, incluso después de la baja.
La consulta por `areaBId` devuelve ahora `[]`.

### Paso 13 — Baja lógica de Área y padre inactivo

`DELETE {{baseUrl}}/api/areas/{{areaBId}}` → 204, pues ya no tiene laboratorios
activos. GET/PUT/DELETE de ese ID → 404. La ruta
`GET {{baseUrl}}/api/areas/{{areaBId}}/laboratorios` también devuelve 404.

Intenta POST de Laboratorio con **código nuevo** `OTR-{{corrida}}` e
`idArea={{areaBId}}`: 409 por padre inactivo. Intenta POST de Área con nombre
`Area B Org {{corrida}}` y `idSede={{sedeBId}}`: 409 por nombre reservado.

### Paso 14 — Baja lógica de Sede y movimiento a padre inactivo

Sede A quedó sin áreas en el paso 6. Envía
`DELETE {{baseUrl}}/api/sedes/{{sedeAId}}` → 204; GET/PUT/DELETE de esa sede → 404.
`GET {{baseUrl}}/api/sedes/{{sedeAId}}/areas` → 404.

Repite POST de Área con un nombre nuevo y `idSede={{sedeAId}}`: 409. Repite PUT
de `areaAId` del paso 6 indicando la sede A inactiva: 409. Confirma mediante
GET que el área permanece activa en B.

Para probar movimiento de un laboratorio activo hacia un área inactiva, crea
otro laboratorio con código nuevo `MOV-{{corrida}}` en `areaAId`, guarda su ID
temporalmente y envía PUT de ese nuevo ID con `idArea={{areaBId}}`: 409. El
laboratorio debe seguir en A. Dale baja antes de continuar con la baja de A.

### Paso 15 — Validación, padres ausentes y JSON inválido

Con ADMIN, repite los modelos válidos cambiando solo el campo que indica cada
fila. Restaura el cuerpo válido después de cada intento.

| Solicitud | Cambio | Esperado |
|---|---|---|
| POST Sede | `nombre` vacío o solo espacios | 400, campo `nombre` en errores |
| POST Área | `idSede` nulo, omitido o cero | 400 |
| POST Laboratorio | `codigo` solo espacios o `idArea=-1` | 400 |
| POST cualquiera | JSON mal formado, por ejemplo una coma final | 400 |
| GET `/api/areas/abc` o `/api/areas/0` | ID no numérico o no positivo | 400 |
| POST Área | `idSede` positivo comprobado inexistente | 404 |
| POST Laboratorio | `idArea` positivo comprobado inexistente | 404 |
| GET jerárquico | Padre positivo comprobado inexistente | 404 |

Antes de elegir `padreInexistenteId`, consulta en pgAdmin la tabla del padre,
incluyendo inactivos. Por ejemplo, comprueba por separado que estas consultas
devuelvan cero filas y solo entonces guarda `999999` en esa variable:

```sql
SELECT id_sede FROM sede WHERE id_sede = 999999;
SELECT id_area FROM area WHERE id_area = 999999;
```

Un error de validación tiene esta estructura. La fecha siguiente es ilustrativa;
en tu respuesta `timestamp` y `path` corresponden a la solicitud que enviaste:

```json
{
  "timestamp": "2026-09-21T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene campos inválidos.",
  "path": "/api/sedes",
  "errors": { "nombre": "El nombre es obligatorio" }
}
```

### Paso 16 — JWT y roles: 401 y 403

Repite GET de listas de sedes, áreas y laboratorios con `tokenGestor` y luego
`tokenLector`: 200. Repite detalle de la sede B, del área A y consultas
jerárquicas de padres activos: 200. Para detalle de laboratorio usa uno activo
obtenido desde GET lista; no uses el que diste de baja.

Con cada uno de esos dos tokens, intenta POST de los tres recursos con cuerpo
válido, así como PUT/DELETE de IDs activos: 403. La autorización ocurre antes
de la lógica de negocio. Las escrituras no deben persistir.

Selecciona **No Auth** y verifica que no exista una cabecera Authorization
manual: GET de cualquiera de las tres listas → 401. Selecciona Bearer Token
y escribe una cadena inválida → 401. Usa nuevamente el token ADMIN después.
401 significa falta de autenticación válida; 403 significa rol insuficiente.

### Paso 17 — Finalizar bajas y comprobar persistencia

Si creaste el laboratorio temporal del paso 14, dale baja primero. Después:

1. DELETE de `areaAId` → 204; no quedan laboratorios activos en ella.
2. DELETE de `sedeBId` → 204; las dos áreas de esta secuencia ya están inactivas.
3. SQL conserva todas esas filas con `activo=false`; los GET individuales → 404.

Para comprobar persistencia de un recurso activo, puedes detenerte antes de las
bajas, guardar su GET, detener Spring Boot con Ctrl+C y volverlo a iniciar con
la misma base. Obtén token nuevo y repite GET: ID, campos, relación y fecha deben
ser iguales. Después de las bajas puedes repetir el mismo reinicio: seguirán
ocultas en GET y conservadas como inactivas en SQL. No recrees los registros
para esta comprobación. No se afirma haber automatizado la interfaz de Postman.

## 22. Consultas SQL de verificación en pgAdmin

Conecta Query Tool a la base que deseas comprobar. Las siguientes consultas
son de lectura y no muestran credenciales. También sirven para inspeccionar
duplicados **antes** de aplicar V8/V9. Si hay duplicados, revísalos antes de
migrar; no borres, fusiones ni ejecutes `repair` para ocultarlos.

```sql
-- 1. Jerarquía completa, incluidas filas inactivas.
SELECT s.id_sede, s.nombre AS sede, s.activo AS sede_activa,
       a.id_area, a.nombre AS area, a.activo AS area_activa,
       l.id_laboratorio, l.codigo, l.nombre AS laboratorio,
       l.activo AS laboratorio_activo
FROM sede s
LEFT JOIN area a ON a.id_sede = s.id_sede
LEFT JOIN laboratorio l ON l.id_area = a.id_area
ORDER BY s.id_sede, a.id_area, l.id_laboratorio;

-- 2. Sedes activas.
SELECT id_sede, nombre, direccion, distrito, departamento, activo, fecha_creacion
FROM sede WHERE activo = TRUE ORDER BY id_sede;

-- 3. Áreas activas.
SELECT id_area, nombre, descripcion, id_sede, activo, fecha_creacion
FROM area WHERE activo = TRUE ORDER BY id_area;

-- 4. Laboratorios activos.
SELECT id_laboratorio, nombre, codigo, ubicacion, id_area, activo, fecha_creacion
FROM laboratorio WHERE activo = TRUE ORDER BY id_laboratorio;

-- 5. Debe devolver cero: áreas activas bajo sedes inactivas.
SELECT a.id_area, a.nombre, a.id_sede
FROM area a JOIN sede s ON s.id_sede = a.id_sede
WHERE a.activo = TRUE AND s.activo = FALSE;

-- 6. Debe devolver cero: laboratorios activos bajo áreas inactivas.
SELECT l.id_laboratorio, l.codigo, l.id_area
FROM laboratorio l JOIN area a ON a.id_area = l.id_area
WHERE l.activo = TRUE AND a.activo = FALSE;

-- 7. Debe devolver cero; incluye nombres reservados de áreas inactivas.
SELECT id_sede, UPPER(nombre) AS nombre_comparable, COUNT(*) AS cantidad,
       ARRAY_AGG(id_area ORDER BY id_area) AS ids
FROM area
GROUP BY id_sede, UPPER(nombre)
HAVING COUNT(*) > 1;

-- 8. Debe devolver cero; código global, sin agrupar por área.
SELECT UPPER(codigo) AS codigo_comparable, COUNT(*) AS cantidad,
       ARRAY_AGG(id_laboratorio ORDER BY id_laboratorio) AS ids
FROM laboratorio
GROUP BY UPPER(codigo)
HAVING COUNT(*) > 1;

-- 9. Índices nuevos y restricciones anteriores conservadas.
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public' AND tablename IN ('area', 'laboratorio')
ORDER BY tablename, indexname;

-- 10. Tras aplicar la versión de este sprint: V1–V9, success=true.
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

En 9 deben aparecer `uq_area_sede_nombre_ignore_case` y
`uq_laboratorio_codigo_ignore_case`, ambos únicos. El primero incluye sede y
`upper(nombre)`; el segundo solo `upper(codigo)`. Los índices de las restricciones
anteriores siguen presentes. Para comprobar baja o movimiento individual,
consulta la fila por su ID real: `activo` cambia a false en DELETE; la FK cambia
en PUT de movimiento; ID y fecha se conservan. pgAdmin no interpreta variables
`{{…}}` de Postman: reemplázalas manualmente por sus valores numéricos.

## 23. Tests y verificación reproducible

La suite completa se ejecutó en
`inventario_verificacion_s4b_org_20260921_c8f2`: **140 pruebas aprobadas,
0 fallos, 0 errores y 0 omitidas**. Son 99 anteriores y 41 nuevas: 6 de Mappers,
11 de Services, 1 de errores, 14 de integración y 9 de concurrencia. Antes se
ejecutaron 65 unitarias, todas aprobadas, y la compilación pasó.
`bootJar` se construyó correctamente. Flyway V1–V9 quedó con `success=true` en
la base temporal y, después de las verificaciones, en la habitual. Los conteos
2 categorías, 4 subcategorías, 3 usuarios, 1 sede, 2 áreas y 2 laboratorios se
conservaron, al igual que el contenido completo de sus filas. La comparación no
imprimió valores sensibles. Los índices nuevos y antiguos permanecieron y las
consultas de inconsistencias devolvieron cero filas.

La base temporal se eliminó tras comprobar que no tenía conexiones. El JAR de
verificación se detuvo y los puertos 8080/8081 quedaron libres. El reporte
completo está en [Sprint 4, secciones 14–16 del incremento 4B–4D](sprint-4.md).

La cobertura nueva comprende los tres CRUD, campos protegidos, validación,
relaciones, movimientos, padres ausentes/inactivos, duplicados con y sin
mayúsculas, baja lógica, nombres/códigos reservados, consultas jerárquicas,
JWT/roles y carreras de escritura. Se reutilizan las pruebas de Categoría,
Subcategoría y autenticación como regresión.

Para compilar desde `backend/inventario`, sin modificar datos:

```powershell
.\gradlew.bat classes testClasses --no-daemon --console=plain
```

Para repetir solo pruebas unitarias, sin cargar la aplicación ni PostgreSQL:

```powershell
.\gradlew.bat test --rerun-tasks --tests '*MapperTest' --tests '*ServiceTest' --tests '*GlobalExceptionHandlerTest' --tests '*JwtServiceTest' --no-daemon --console=plain
```

Para integración crea una base **exclusiva**, conectado a `postgres` en pgAdmin,
ejecutando por separado y con autocommit:

```sql
CREATE DATABASE inventario_verificacion_manual_org;
```

Configura `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=dev`
y `DEMO_USER_PASSWORD` siguiendo los avisos seguros del [README](../README.md#variables-de-entorno-y-ejecución-en-powershell).
No guardes sus valores en archivos ni los imprimas. Luego, desde el backend:

```powershell
$previousDbUrlOrg = $env:DB_URL
try {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_verificacion_manual_org'
    .\gradlew.bat test --rerun-tasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'La suite de pruebas falló.' }
    .\gradlew.bat bootJar --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'No se construyó bootJar.' }
} finally {
    $env:DB_URL = $previousDbUrlOrg
}
```

Las pruebas que escriben en PostgreSQL solo deben usar bases con prefijo
`inventario_verificacion_`. Las condiciones de habilitación impiden ejecutar
integración contra la base habitual. Revisa el reporte: una prueba omitida no
es una prueba aprobada. El HTML queda en
`backend/inventario/build/reports/tests/test/index.html` y XML en
`backend/inventario/build/test-results/test`.

Comprueba Flyway V1–V9 y las consultas SQL anteriores en esa base. Después de
terminar y cerrar las conexiones, puedes eliminar **solo la base temporal que
creaste**, nunca `inventario_laboratorios`. La verificación de este sprint sigue
el orden: compilación, unitarias, suite con BD temporal, bootJar, Flyway/SQL y
recién entonces migraciones en la base habitual. Se comparan antes/después los
conteos de categoría, subcategoría, usuario, sede, área y laboratorio.

## 24. Archivos creados

Las rutas Java parten de `backend/inventario/src/main/java/com/utec/inventario/`.
En cada fila, los tres nombres separados son archivos independientes:

| Ruta / archivos | Propósito |
|---|---|
| `entity/SedeEntity.java`, `AreaEntity.java`, `LaboratorioEntity.java` | Mapeos de V1 y relaciones JPA |
| `domain/Sede.java`, `Area.java`, `Laboratorio.java` | Modelos separados sin JPA |
| `dto/request/CreateSedeRequest.java`, `UpdateSedeRequest.java` | Campos editables y validaciones de Sede |
| `dto/request/CreateAreaRequest.java`, `UpdateAreaRequest.java` | Campos e ID de Sede |
| `dto/request/CreateLaboratorioRequest.java`, `UpdateLaboratorioRequest.java` | Campos e ID de Área |
| `dto/response/SedeResponse.java`, `AreaResponse.java`, `LaboratorioResponse.java` | Respuestas públicas controladas |
| `dto/response/SedeResumenResponse.java`, `AreaResumenResponse.java` | ID y nombre del padre |
| `mapper/SedeMapper.java`, `AreaMapper.java`, `LaboratorioMapper.java` | Conversiones y copia de PUT |
| `repository/SedeRepository.java`, `AreaRepository.java`, `LaboratorioRepository.java` | Lecturas, unicidad y bloqueos |
| `service/SedeService.java`, `AreaService.java`, `LaboratorioService.java` | Reglas y transacciones |
| `controller/SedeController.java`, `AreaController.java`, `LaboratorioController.java` | CRUD de los tres recursos |
| `controller/SedeAreaController.java` | GET jerárquico delegado a `AreaService` |
| `controller/AreaLaboratorioController.java` | GET jerárquico delegado a `LaboratorioService` |

Se agregan V8 y V9 en `backend/inventario/src/main/resources/db/migration/`,
las pruebas de organización bajo
`backend/inventario/src/test/java/com/utec/inventario/` y este documento:

| Test nuevo | Propósito |
|---|---|
| `mapper/SedeMapperTest.java` | Requests, respuestas y campos protegidos |
| `mapper/AreaMapperTest.java` | Relación y copia de Área |
| `mapper/LaboratorioMapperTest.java` | Relación y copia de Laboratorio |
| `service/SedeServiceTest.java` | Normalización, baja y restricciones de padre |
| `service/AreaServiceTest.java` | Padre activo, unicidad, movimientos y baja |
| `service/LaboratorioServiceTest.java` | Padre activo, código global y baja |
| `OrganizacionIntegrationTests.java` | CRUD HTTP real, SQL, validación y roles |
| `OrganizacionConcurrenciaTests.java` | Transacciones concurrentes y restricciones reales |

El inventario exacto del cierre está en [Sprint 4](sprint-4.md).

## 25. Archivos modificados

| Ruta | Motivo |
|---|---|
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Permitir lectura a los tres roles y escritura a ADMIN en las nuevas rutas |
| `backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java` | Traducir duplicados conocidos de Área/Laboratorio a 409 |
| Tests existentes de errores | Proteger las nuevas restricciones sin perder los casos anteriores |
| `README.md` | Estado real, rutas y migraciones |
| `docs/reglas-negocio.md` | RN-13 global y RN-31/RN-32 en tres jerarquías, sin renumerar |
| `docs/matriz-permisos.md` | 17 rutas organizacionales y alcance global vigente |
| `docs/sprint-4.md` | Conservar cierre 4A y registrar 4B–4D |

No se cambian versiones, migraciones ya ejecutadas, login ni generación de JWT.

## 26. Funcionalidades diferidas

Quedan pendientes UsuarioLaboratorio, alcance por laboratorio, Equipo,
MovimientoEquipo y administración completa de usuarios. Sus tablas existentes
no significan que ya tengan vertical Java. También quedan para otro alcance
frontend, Docker, mantenimiento y auditoría general.

Las restricciones «no desactivar Laboratorio con Equipos activos» y «no
desactivar Subcategoría con Equipos activos» se implementarán con Equipo.
No se agrega `EquipoRepository` para anticiparlas. Las restricciones asociadas
a UsuarioLaboratorio se definirán al implementar esa vertical.
