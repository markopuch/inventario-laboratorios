# Inventario de Laboratorios

Backend Spring Boot ubicado en `backend/inventario`. El alcance actual comprende
la base del proyecto (Sprint 1), el esquema PostgreSQL administrado por Flyway
(Sprint 2), la vertical de Categoría (Sprint 3) y usuarios JPA con autenticación
JWT. Los CRUD de equipos y administración de usuarios quedan para sprints posteriores.

## Arranque rápido en Windows

Con PostgreSQL iniciado, abre una terminal en la raíz del proyecto y ejecuta:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

El script solicita usuario y contraseña de PostgreSQL cuando faltan y genera una
clave JWT para esa sesión. Presiona Enter para aceptar el usuario `postgres`.
Espera `Started InventarioApplication` y deja abierta la terminal mientras usas
Postman en `http://localhost:8080`. Detén el backend con `Ctrl+C`.

Las cuentas existentes `marko`, `aldo` y `romel` funcionan sin el perfil `dev`.
Para inicializarlas en una base nueva, agrega `-CrearUsuariosDemo` al comando;
entonces se solicita también la contraseña inicial de demostración.

En este workspace de VS Code también puedes abrir **Terminal → Run Task →
Iniciar backend de Inventario**. La tarea local usa el mismo script. Ejecutar
Java directamente requiere configurar previamente las variables del apartado
de configuración manual. El script no guarda contraseñas ni claves en archivos.

La [guía del Sprint 3](docs/sprint-3-categorias.md) explica la arquitectura,
las once solicitudes manuales de Postman y la comprobación de persistencia en pgAdmin.
Comienza por la [guía de usuarios y JWT](docs/autenticacion-jwt.md) para iniciar
sesión y obtener los tokens que requieren esas solicitudes.
El código sigue convenciones de los ejemplos del curso en `Carlos_backend`:
clases con Lombok, inyección explícita con `@Autowired`, estados HTTP declarados
y mappers con `convert` y `copy`. Se mantiene la organización de paquetes de este
proyecto; la guía incluye la correspondencia con las carpetas del profesor.

## Requisitos

- JDK **21**, disponible en `PATH` o mediante `JAVA_HOME`.
- PostgreSQL instalado y en ejecución; puedes usar pgAdmin para administrar la base.
- Gradle Wrapper incluido: **9.7.1**. No necesitas instalar Gradle globalmente.
- Spring Boot **4.1.1**, conservando la versión existente del proyecto.
- Acceso a Internet para descargar Gradle y las dependencias si no están en caché.

La configuración de Gradle utiliza Kotlin DSL. El `group` y el package base son
`com.utec.inventario`. La clase principal es `InventarioApplication.java`.
Las versiones existentes son compatibles según los
[requisitos oficiales de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).

## Estructura del backend

```text
backend/inventario/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── src/
    ├── main/
    │   ├── java/com/utec/inventario/
    │   │   ├── InventarioApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── domain/
    │   │   ├── dto/request/
    │   │   ├── dto/response/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── mapper/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       └── db/migration/
    └── test/java/com/utec/inventario/
        ├── InventarioApplicationTests.java
        ├── CategoriaConcurrenciaTests.java
        ├── AuthIntegrationTests.java
        ├── exception/GlobalExceptionHandlerTest.java
        ├── mapper/CategoriaMapperTest.java
        ├── security/JwtServiceTest.java
        └── service/CategoriaServiceTest.java
```

Los paquetes vacíos contienen `.gitkeep` para conservarlos en Git, sin clases
ficticias. El test de arranque existente se mantiene sin modificaciones; se
agregaron pruebas de regresión del Sprint 3 y de autenticación JWT.

Las dependencias incluyen Web MVC, JPA, PostgreSQL JDBC, Validation, Security,
Flyway con su módulo PostgreSQL, Lombok y DevTools. Los starters de pruebas
existentes incluyen `spring-boot-starter-test` transitivamente. Sprint 3 incorpora
MapStruct **1.6.3**, su procesador de anotaciones y `lombok-mapstruct-binding:0.2.0`.
Las versiones de Java, Spring Boot, Gradle y Lombok se conservan.
La autenticación utiliza JJWT **0.13.0**, como los ejemplos del profesor,
y BCrypt de Spring Security para verificar los hashes de contraseñas.

## Preparar PostgreSQL

La base esperada es `inventario_laboratorios`. Flyway crea las tablas dentro de
una base existente; no crea el servidor, la base ni el rol PostgreSQL.

1. En pgAdmin, conectado con tu administrador local, crea el rol de ejemplo
   `inventario_app` en **Login/Group Roles**. Activa **Can login?** y configura
   tu contraseña local en **Definition**. No necesita ser superusuario.
2. En Query Tool, conectado a la base `postgres`, ejecuta esta sentencia por
   separado, con autocommit habilitado, si la base todavía no existe:

   ```sql
   CREATE DATABASE inventario_laboratorios OWNER inventario_app;
   ```

3. El primer arranque debe apuntar a una base vacía. El rol de la aplicación
   necesita conexión y permisos `USAGE` y `CREATE` sobre el esquema `public`.
   Si usas una base existente con otro propietario, un administrador puede
   concederlos, conectado a `inventario_laboratorios`:

   ```sql
   GRANT CONNECT ON DATABASE inventario_laboratorios TO inventario_app;
   GRANT USAGE, CREATE ON SCHEMA public TO inventario_app;
   ```

Si la base ya contiene tablas o un historial Flyway, revisa su estado antes del
arranque. No ejecutes un SQL anterior para crear el esquema ni apliques `baseline`
o `repair` para ocultar diferencias. Los borradores originales V1–V4 solo tenían
comentarios: si llegaste a aplicarlos, usa una base de desarrollo vacía y conserva
la anterior hasta revisar sus datos.

## Variables de entorno y ejecución en PowerShell

Abre PowerShell en la raíz del repositorio y entra al backend:

```powershell
Set-Location .\backend\inventario
java -version
```

Configura la conexión y las cuentas de demostración en esa misma terminal.
Introduce las contraseñas mediante los avisos; sus valores no se escriben en
el historial de comandos:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_laboratorios'
$env:DB_USER = Read-Host 'Usuario local de PostgreSQL'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Contraseña local de PostgreSQL' -AsSecureString)
).Password

if (-not $env:JWT_SECRET) {
    $jwtKeyBytes = New-Object byte[] 32
    $jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $jwtRandom.GetBytes($jwtKeyBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
    } finally {
        $jwtRandom.Dispose()
        [Array]::Clear($jwtKeyBytes, 0, $jwtKeyBytes.Length)
    }
}

$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DEMO_USER_PASSWORD = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Contraseña inicial de las tres cuentas demo' -AsSecureString)
).Password
```

Ejecuta en esa misma ventana:

```powershell
.\gradlew.bat bootRun
```

La aplicación usa el puerto `8080`. Las variables anteriores pertenecen a esta
sesión de PowerShell. Un archivo `.env` no se carga automáticamente: configura
las variables en la terminal o en tu entorno de ejecución.

El perfil `dev` crea `marko` (ADMIN), `aldo` (GESTOR) y `romel` (LECTOR) mediante
JPA, usando la contraseña inicial que introduzcas. Reiniciar no duplica usuarios,
no restablece contraseñas ni cambia sus roles o estados. La contraseña de la demo
es independiente de la de PostgreSQL. Sin `dev`, las cuentas existentes siguen
disponibles, pero no se crean cuentas demo.

`JWT_SECRET` es obligatorio y contiene al menos 32 bytes aleatorios en Base64.
Los comandos anteriores lo conservan al reiniciar en la misma terminal; si cambia
la clave, inicia sesión otra vez. Los tokens duran 1800 segundos por defecto.

No guardes credenciales reales en archivos versionados. `.env` y
`application-local.properties` están excluidos de Git.

## Flyway y Hibernate

`application.properties` utiliza exclusivamente estas referencias para conectar:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.enabled=true
```

Flyway ejecuta automáticamente las migraciones pendientes de
`src/main/resources/db/migration` durante el arranque, antes de inicializar JPA.
El starter Flyway y `flyway-database-postgresql` existentes habilitan esta
integración, conforme a la
[documentación de Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

| Migración | Contenido |
|---|---|
| `V1__crear_organizacion_y_catalogos.sql` | `sede`, `area`, `laboratorio`, `categoria`, `subcategoria` |
| `V2__crear_usuarios_y_seguridad.sql` | `rol`, `usuario`, `usuario_laboratorio` |
| `V3__crear_equipos_y_movimientos.sql` | `equipo`, `movimiento_equipo` |
| `V4__insertar_datos_iniciales.sql` | Tres roles y organización/catálogos ficticios |
| `V5__categoria_nombre_unico_sin_mayusculas.sql` | Índice único sobre `UPPER(categoria.nombre)`, incluyendo categorías inactivas |
| `V6__agregar_username_usuario.sql` | Nombre de acceso único sin distinguir mayúsculas; usuarios anteriores reciben `usuario_<id_usuario>` |

Flyway es la fuente oficial del esquema. Cada migración se aplica una vez y queda
registrada con su checksum en `flyway_schema_history`. Tras aplicar estas versiones,
los cambios posteriores deben introducirse mediante nuevas migraciones.

`spring.jpa.hibernate.ddl-auto=validate` indica a Hibernate que valide el esquema
frente a las entidades mapeadas, sin crear, actualizar ni borrar tablas. Ahora
se mapean `CategoriaEntity`, `UsuarioEntity` y `RolEntity`; un arranque correcto
no valida las otras siete tablas mediante JPA. Verifica el esquema mediante las
consultas siguientes.

V1–V4 ya fueron aplicadas en la base local inspeccionada y se conservaron sin
modificaciones. V5 amplía la unicidad de nombre para impedir duplicados que solo
difieran en mayúsculas, incluso ante escrituras concurrentes. No se encontraron
duplicados al inspeccionar la base. Si aparecen antes del próximo arranque, V5
fallará hasta que se revisen; no elimina ni fusiona datos automáticamente.

Decisiones del esquema:

- PK simples con `SERIAL` y PK compuesta en `usuario_laboratorio`.
- Fechas `TIMESTAMPTZ` con `DEFAULT CURRENT_TIMESTAMP`.
- Las 13 FK usan `ON UPDATE RESTRICT ON DELETE RESTRICT` para preservar referencias.
- 12 índices adicionales cubren las FK; `usuario_laboratorio(id_usuario)` ya queda
  cubierto por la primera columna de su PK compuesta.
- `laboratorio.codigo` es único globalmente, conforme a Sprint 2. Esto precisa la
  regla RN-13 del documento de Sprint 0, que lo describía dentro de cada área.
- `equipo.id_responsable` es opcional y no representa autorización. Las series
  opcionales ausentes se representan como `NULL`; sus valores informados son únicos.
- Se validan los cuatro estados del equipo y el año entre 1900 y 2100 si se informa.
- `tipo_movimiento` y `motivo` rechazan cadenas vacías o solo espacios. No se define
  un catálogo cerrado de tipos de movimiento porque aún no fue especificado.
- `fecha_actualizacion` recibe un valor al insertar. Su modificación posterior
  corresponde a la futura lógica de persistencia; no se agregó un trigger.

V4 inserta `ADMIN`, `GESTOR`, `LECTOR`, una sede, dos áreas, los laboratorios `L201`
y `L206`, dos categorías y cuatro subcategorías. No inserta usuarios, hashes,
equipos ni movimientos. Las relaciones de los datos iniciales se resuelven por
nombre/código, sin asumir identificadores numéricos.
Las cuentas demo se insertan posteriormente mediante `DemoUsuariosConfig` y
solo con el perfil `dev`; los hashes BCrypt no forman parte de las migraciones.

Los documentos en `docs/` se conservan como referencias de diseño de Sprint 0;
no implican que los endpoints o reglas de servicio estén implementados. No se
encontró un `proyecto.sql` ni otro esquema SQL histórico en el código fuente.

## Comprobación manual en pgAdmin

Tras iniciar Spring Boot, abre Query Tool conectado a `inventario_laboratorios`:

```sql
-- Deben aparecer las 10 tablas del dominio y flyway_schema_history.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Tras iniciar la versión actual: seis migraciones, versiones 1–6, con success = true.
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- ADMIN, GESTOR, LECTOR.
SELECT id_rol, nombre, activo FROM rol ORDER BY nombre;

-- Usuarios de aplicación y sus roles; no muestra contraseñas ni hashes.
SELECT u.id_usuario, u.username, u.nombre, r.nombre AS rol, u.activo
FROM usuario AS u
JOIN rol AS r ON r.id_rol = u.id_rol
ORDER BY u.id_usuario;

-- Una sede, dos áreas y los laboratorios L201 y L206.
SELECT s.nombre AS sede, a.nombre AS area, l.codigo, l.nombre AS laboratorio
FROM sede AS s
JOIN area AS a ON a.id_sede = s.id_sede
JOIN laboratorio AS l ON l.id_area = a.id_area
ORDER BY l.codigo;

-- Dos categorías y cuatro subcategorías.
SELECT c.nombre AS categoria, sc.nombre AS subcategoria
FROM categoria AS c
JOIN subcategoria AS sc ON sc.id_categoria = c.id_categoria
ORDER BY c.nombre, sc.nombre;

-- Restricciones y acciones de las claves foráneas.
SELECT t.relname AS tabla, c.conname, pg_get_constraintdef(c.oid) AS definicion
FROM pg_constraint AS c
JOIN pg_class AS t ON t.oid = c.conrelid
JOIN pg_namespace AS n ON n.oid = t.relnamespace
WHERE n.nspname = 'public' AND c.contype IN ('p', 'u', 'f', 'c')
ORDER BY t.relname, c.conname;

SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;
```

`bootRun` aplica las migraciones pendientes hasta V6. Los resultados de la
verificación actual y los comandos para repetirla están en la
[guía de usuarios y JWT](docs/autenticacion-jwt.md).
La compilación de las clases principales se puede comprobar sin tests con:

```powershell
.\gradlew.bat classes -x test
```

## API de categorías — Sprint 3

| Método | Ruta | Resultado correcto |
|---|---|---|
| GET | `/api/categorias` | 200, lista de categorías activas |
| GET | `/api/categorias/{id}` | 200, categoría activa |
| POST | `/api/categorias` | 201, categoría creada y cabecera `Location` |
| PUT | `/api/categorias/{id}` | 200, reemplazo de nombre y descripción |
| DELETE | `/api/categorias/{id}` | 204, baja lógica mediante `activo=false` |

POST y PUT reciben exclusivamente el modelo editable `nombre` y `descripcion`.
El servidor controla ID, estado y fecha. Los errores se devuelven como JSON:
400 para entradas inválidas, 401 para autenticación ausente o inválida,
403 para permisos insuficientes, 404 para categorías inexistentes o inactivas,
409 para nombres duplicados y 500 con un mensaje genérico para fallos internos.

## Autenticación y permisos

`POST /api/auth/login` es público y recibe `userName` y `password`. Devuelve
`accessToken`, `tokenType`, `expiresIn` y los datos públicos del usuario.
`GET /api/auth/me` devuelve el perfil asociado a un token válido.

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| Consultar categorías y perfil propio | Sí | Sí | Sí |
| Crear, actualizar y dar de baja categorías | Sí | No | No |

En Postman selecciona **No Auth únicamente para el login**. En las solicitudes
de categorías usa **Bearer Token** con el token de `marko` para completar las
once pruebas de negocio. Sin token válido, las categorías devuelven 401; con
un rol sin permiso de escritura, devuelven 403.

`SecurityConfig` usa sesiones deshabilitadas y `JwtAuthFilter` consulta el usuario
y rol vigentes mediante JPA en cada petición. El JWT se envía exclusivamente en
`Authorization`; no se usan cookies para autenticar, por lo que CSRF está
deshabilitado. Form login, HTTP Basic y logout están deshabilitados y las otras
rutas quedan denegadas. Los despachos internos de error están permitidos.

La [guía de usuarios y JWT](docs/autenticacion-jwt.md) contiene el flujo completo,
las pruebas de login y roles, los comandos PowerShell y las consultas SQL.

`Instrumentación` ya existe por V4: un POST con ese nombre devolverá 409.
La guía usa `Instrumentación Sprint 3` y los IDs realmente devueltos por la API.
La guía del Sprint 3 conserva los resultados históricos de la revisión anterior
a JWT y las correcciones de concurrencia. Sus pasos manuales se actualizaron para
utilizar el token ADMIN. Postman queda disponible para tu comprobación manual.
