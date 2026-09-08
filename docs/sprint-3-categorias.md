# Sprint 3 — Categorías

La vertical implementada permite crear, listar, consultar, actualizar y dar de
baja categorías en PostgreSQL. Se conservaron Java 21, Spring Boot 4.1.1, Gradle
9.7.1, la configuración mediante variables de entorno y `ddl-auto=validate`.
Las migraciones V1–V4 y los tests existentes no se modificaron.

## Arquitectura

```text
Postman envía JSON
  → CategoriaController recibe CreateCategoriaRequest / UpdateCategoriaRequest
  → Bean Validation comprueba los campos
  → CategoriaMapper convierte el DTO en Categoria
  → CategoriaService aplica reglas y transacciones
  → CategoriaMapper convierte Categoria en CategoriaEntity
  → CategoriaRepository utiliza JPA/Hibernate
  → PostgreSQL persiste la fila

PostgreSQL → Entity → Mapper → Domain → Service
  → Controller → Mapper → CategoriaResponse → JSON de respuesta
```

Una Entity describe columnas y persistencia; el dominio representa la categoría
en la lógica del servicio. Un DTO define qué datos atraviesan HTTP. El Mapper
copia entre esos modelos; no consulta la base ni decide reglas de negocio.
El Controller coordina HTTP y no utiliza el Repository.

El Service recibe dominio. Sus consultas son de solo lectura y sus escrituras
son transaccionales. Usa búsquedas que filtran `activo=true`; un ID inactivo se
considera inexistente. En las comprobaciones de nombres sí se incluyen las
categorías inactivas: la baja lógica no libera el nombre.

POST establece `activo=true`, deja que PostgreSQL genere ID y fecha y devuelve
201 con el cuerpo y la cabecera `Location`. PUT reemplaza nombre y descripción,
conserva ID/estado/fecha y permite mantener el nombre propio. Si omites la
descripción o envías `null`, se guarda `NULL`, porque PUT reemplaza los campos
editables completos. Se recortan los espacios de los extremos de los textos.

DELETE cambia `activo` a `false` y guarda mediante JPA; no usa `repository.delete`.
Repetir GET, PUT o DELETE sobre esa categoría devuelve 404.

## MapStruct, Lombok y fechas

MapStruct 1.6.3 genera `CategoriaMapperImpl` durante la compilación. La interfaz
declara cinco conversiones y usa `componentModel="spring"` y
`unmappedTargetPolicy=ReportingPolicy.ERROR` para detectar campos destino olvidados.
Los campos controlados por el servidor se ignoran explícitamente al recibir DTOs.

El resultado generado se encuentra normalmente en:

```text
backend/inventario/build/generated/sources/annotationProcessor/java/main/com/utec/inventario/mapper/CategoriaMapperImpl.java
```

No edites ese archivo: se regenera. Modifica la interfaz `CategoriaMapper`.
`lombok-mapstruct-binding:0.2.0` permite que MapStruct reconozca los accesores que
Lombok genera. Lombok aporta getters, setters y constructores sin argumentos al
dominio y a la Entity; constructores de dependencias al Controller y Service;
y el logger al manejador de errores. Los DTOs son records de Java, que ya generan
su constructor y accesores. No se usa `@Data` en la Entity para evitar igualdad,
hash y representaciones automáticas de todos los campos de persistencia.

`GenerationType.IDENTITY` utiliza la generación de ID que ofrece el `SERIAL`
existente. `fechaCreacion` usa `OffsetDateTime`: PostgreSQL aplica
`DEFAULT CURRENT_TIMESTAMP` y `@Generated(event=INSERT)` recupera ese valor.
La columna no se incluye en INSERT ni UPDATE de Hibernate. Hibernate no genera
el esquema: Flyway continúa siendo la fuente de verdad.

## Unicidad y V5

La restricción original de V1 era sensible a mayúsculas. V5 añade:

```sql
CREATE UNIQUE INDEX uq_categoria_nombre_ignore_case ON categoria (UPPER(nombre));
```

Esto coincide con la comparación `IgnoreCase` de Spring Data. El Service consulta
duplicados antes de guardar y el índice impide que dos solicitudes simultáneas
inserten nombres equivalentes. La violación de esos índices de nombre se traduce
a 409 sin incluir detalles de PostgreSQL en el JSON. V5 no modifica filas.

En otra base, revisa posibles duplicados antes de arrancar:

```sql
SELECT UPPER(nombre) AS nombre_comparable, COUNT(*) AS cantidad,
       ARRAY_AGG(id_categoria ORDER BY id_categoria) AS ids
FROM categoria
GROUP BY UPPER(nombre)
HAVING COUNT(*) > 1;
```

Si aparecen filas, revisa esos datos antes de aplicar V5. No cambies V1–V4 ni
ocultes el conflicto con `repair`; no se eliminan categorías automáticamente.

## Validación y errores

- `@NotBlank`: el nombre no puede ser `null`, vacío ni solo espacios.
- `@Size`: máximo 100 caracteres para nombre y 255 para descripción.
- `@Valid`: activa la validación del DTO recibido en POST y PUT.
- La unicidad es una regla de negocio: nombre duplicado, incluyendo inactivos,
  devuelve 409. Al actualizar, la comprobación excluye el ID de la propia categoría.

`GlobalExceptionHandler` centraliza los errores mediante `ApiError`, con
`timestamp`, `status`, `error`, `message` y `path`. La validación agrega `errors`
con el campo y un mensaje claro, sin repetir el valor rechazado. El error 500
expone un mensaje genérico; el detalle técnico se registra únicamente en el servidor.
También se mantienen respuestas coherentes para JSON inválido, IDs no numéricos,
métodos no admitidos y tipos de contenido incorrectos.

Ejemplo de validación:

```json
{
  "timestamp": "2026-09-07T23:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene campos inválidos.",
  "path": "/api/categorias",
  "errors": { "nombre": "El nombre es obligatorio" }
}
```

## Arranque manual en Windows PowerShell

Desde la raíz del repositorio:

```powershell
Set-Location .\backend\inventario
$env:DB_URL = "jdbc:postgresql://localhost:5432/inventario_laboratorios"
$env:DB_USER = Read-Host "Usuario local de PostgreSQL"
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new(
    "", (Read-Host "Contraseña local de PostgreSQL" -AsSecureString)
).Password

# Compilación opcional de clases principales, sin tests.
.\gradlew.bat classes -x test

# Mantén esta terminal abierta mientras utilizas Postman.
.\gradlew.bat bootRun
```

Flyway aplicará V5 si V1–V4 ya estaban aplicadas. En una base vacía ejecutará V1–V5.
No se inicia ninguna suite de tests con esos comandos.

En otra ventana de PowerShell, con las instalaciones locales habituales:

```powershell
Start-Process -FilePath "$env:LOCALAPPDATA\Postman\Postman.exe"
Start-Process -FilePath 'C:\Program Files\PostgreSQL\18\pgAdmin 4\runtime\pgAdmin4.exe'
```

## Configuración temporal de Security

**Configuración temporal de Sprint 3. Será reemplazada cuando se implemente
autenticación/autorización.** Las rutas `/api/categorias` y `/api/categorias/**`
usan `permitAll` y están exceptuadas de CSRF para permitir las escrituras manuales.
Las otras rutas están denegadas. Los despachos internos de error están permitidos.
Form login, HTTP Basic y logout están deshabilitados. No hay implementación de
JWT, login ni usuarios de aplicación. En Postman utiliza **Authorization → No Auth**.

## Preparar las once solicitudes de Postman

Base: `http://localhost:8080/api/categorias`.
Para POST y PUT selecciona **Body → raw → JSON**. Para GET y DELETE, **Body → none**.
Configura **No Auth** en todas las solicitudes. No necesitas scripts de Postman.

V4 ya inserta `Electrónica` e `Instrumentación`; intentar crear `Instrumentación`
devuelve 409. Para comprobar un POST exitoso se usa `Instrumentación Sprint 3`.
En una repetición de la guía elige nombres nuevos: los nombres de categorías dadas
de baja siguen reservados.

Después de las solicitudes 1 y 2, copia manualmente el `id` de cada respuesta a
las variables de colección `categoriaId` y `roboticaId`. No asumas que son 1 y 2.

### 1. Crear una categoría nueva

- Método: **POST**.
- URL: `http://localhost:8080/api/categorias`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "Instrumentación Sprint 3",
  "descripcion": "Equipos utilizados para medición e instrumentación"
}
```

Esperado: **201 Created**, cabecera `Location`, ID generado, `activo=true` y
`fechaCreacion` informada. Guarda su ID como `categoriaId`. Comprueba creación y
campos controlados por el servidor.

### 2. Crear otra categoría

- Método: **POST**.
- URL: `http://localhost:8080/api/categorias`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "Robótica",
  "descripcion": "Equipos y componentes utilizados en sistemas robóticos"
}
```

Esperado: **201 Created**. Guarda el ID como `roboticaId`. Comprueba una segunda
inserción independiente.

### 3. Listar categorías activas

- Método: **GET**.
- URL: `http://localhost:8080/api/categorias`.
- Header: `Accept: application/json`.
- Body: ninguno.

Esperado: **200 OK**, arreglo con las categorías activas ordenadas por ID.
Incluye los datos iniciales y las dos categorías nuevas. Comprueba lectura de lista.

### 4. Obtener la categoría creada por ID

- Método: **GET**.
- URL: `http://localhost:8080/api/categorias/{{categoriaId}}`.
- Header: `Accept: application/json`.
- Body: ninguno.

Esperado: **200 OK**, la categoría de la prueba 1 con su mismo ID y fecha.
Comprueba búsqueda individual y conversión de Entity a Response.

### 5. Actualizar la categoría

- Método: **PUT**.
- URL: `http://localhost:8080/api/categorias/{{categoriaId}}`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "Instrumentación Electrónica",
  "descripcion": "Equipos de medición, adquisición y análisis de señales"
}
```

Esperado: **200 OK**, campos editables actualizados, mismo ID y fecha de creación.
Repite exactamente el PUT: debe seguir devolviendo 200, porque el nombre propio
no es un duplicado. Comprueba reemplazo completo y exclusión del propio ID.

### 6. Rechazar nombre vacío

- Método: **POST**.
- URL: `http://localhost:8080/api/categorias`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "",
  "descripcion": "Categoría inválida"
}
```

Esperado: **400 Bad Request** con un mensaje en `errors.nombre`.
Comprueba Bean Validation; no se inserta ninguna categoría.

### 7. Rechazar duplicado exacto

- Método: **POST**.
- URL: `http://localhost:8080/api/categorias`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "Robótica",
  "descripcion": "Intento duplicado"
}
```

Esperado: **409 Conflict**. Comprueba la regla de unicidad contra la prueba 2.

### 8. Rechazar duplicado ignorando mayúsculas

- Método: **POST**.
- URL: `http://localhost:8080/api/categorias`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "robótica",
  "descripcion": "Intento duplicado con minúsculas"
}
```

Esperado: **409 Conflict**. Comprueba que cambiar mayúsculas/minúsculas no evita
la regla. Como comprobación adicional, enviar este body mediante PUT al ID de la
prueba 1 también debe devolver 409, porque el nombre pertenece a otra categoría.

### 9. Consultar un ID inexistente

- Método: **GET**.
- URL: `http://localhost:8080/api/categorias/999999`.
- Header: `Accept: application/json`.
- Body: ninguno.

Esperado: **404 Not Found**, suponiendo que ese ID no existe en tu base.
Comprueba el manejo de recurso inexistente.

### 10. Actualizar un ID inexistente

- Método: **PUT**.
- URL: `http://localhost:8080/api/categorias/999999`.
- Headers: `Content-Type: application/json`, `Accept: application/json`.
- Body:

```json
{
  "nombre": "Prueba",
  "descripcion": "Registro inexistente"
}
```

Esperado: **404 Not Found**. Comprueba que PUT no crea una fila cuando el ID no existe.

### 11. Dar de baja lógicamente

- Método: **DELETE**.
- URL: `http://localhost:8080/api/categorias/{{roboticaId}}`.
- Header: `Accept: application/json`.
- Body: ninguno.

Esperado: **204 No Content**, sin cuerpo. Luego, con los mismos headers y sin body:

- `GET http://localhost:8080/api/categorias/{{roboticaId}}` → **404**.
- `GET http://localhost:8080/api/categorias` → **200**, ya no incluye esa categoría.
- Repetir el POST de la prueba 7 → **409**: una categoría inactiva conserva su nombre.

Comprueba baja lógica y ocultamiento de inactivos; pgAdmin debe conservar la fila.

## Verificación manual en pgAdmin

Selecciona la base `inventario_laboratorios` y abre Query Tool:

```sql
-- Flyway debe mostrar V1–V5 exitosas tras el arranque.
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Todas las categorías, incluyendo inactivas.
SELECT *
FROM categoria
ORDER BY id_categoria;

-- Categorías visibles para los GET de la API.
SELECT *
FROM categoria
WHERE activo = TRUE
ORDER BY id_categoria;

-- Robótica debe conservar su fila con activo = false después de DELETE.
SELECT id_categoria, nombre, activo
FROM categoria
ORDER BY id_categoria;

-- Confirmar el índice nuevo de V5.
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'categoria'
ORDER BY indexname;
```

Para comprobar persistencia entre arranques, detén Spring Boot con `Ctrl+C`, vuelve
a ejecutar `bootRun` con las mismas variables y repite las consultas GET/SQL.
La categoría actualizada debe conservar sus cambios y la dada de baja debe seguir
inactiva. Esta comprobación también es manual.

La implementación fue compilada sin tests. El arranque con V5, las solicitudes
Postman y la comprobación de persistencia mediante la API quedan pendientes de
tu ejecución manual. No se crearon ni ejecutaron pruebas automáticas.
