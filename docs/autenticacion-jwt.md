# Usuarios y autenticación JWT

La API incorpora usuarios JPA, login con BCrypt y tokens JWT. Se conserva la
organización de carpetas y el flujo de los ejemplos de `Carlos_backend/workshop`,
con las comprobaciones de usuario activo de los otros ejemplos del profesor.

## Cuentas de demostración y permisos

| Usuario de acceso | Nombre | Rol | Consultar categorías | Crear, editar y dar de baja categorías |
|---|---|---|---|---|
| `marko` | Marko Demo | ADMIN | Sí | Sí |
| `aldo` | Aldo Demo | GESTOR | Sí | No |
| `romel` | Romel Demo | LECTOR | Sí | No |

Los correos son ficticios: `marko@inventario.test`, `aldo@inventario.test` y
`romel@inventario.test`. Las tres cuentas toman su contraseña inicial de
`DEMO_USER_PASSWORD`, configurada localmente con el valor acordado para la demo.
Se almacena un hash BCrypt distinto por usuario. No se guarda esa contraseña
en los archivos del proyecto, respuestas ni tokens.

La creación de cuentas se ejecuta únicamente con el perfil **dev**. Reiniciar
no duplica usuarios ni restablece su contraseña, rol o estado. Cambiar la
variable `DEMO_USER_PASSWORD` no modifica las cuentas que ya existen.

Estos permisos aplican a las categorías ya implementadas. Las asignaciones de
laboratorios y los CRUD de equipos y administración de usuarios siguen pendientes;
no se habilitan rutas inexistentes al agregar JWT.

## Cómo se relaciona con el código del profesor

```text
POST /api/auth/login
  → AuthController recibe AuthRequest(userName, password)
  → AuthenticationManager / DaoAuthenticationProvider
  → UsuarioService implements UserDetailsService
  → UsuarioRepository → UsuarioEntity + RolEntity → PostgreSQL
  → UserInfoDetails + BCrypt comprueban identidad y contraseña
  → JwtService firma el token
  → AuthResponse(accessToken, tokenType, expiresIn, usuario)

Solicitud con Authorization: Bearer ...
  → JwtAuthFilter valida firma, emisor y vencimiento
  → UsuarioService consulta usuario y rol actuales en PostgreSQL
  → SecurityContextHolder recibe el usuario autenticado
  → SecurityConfig comprueba los permisos
  → Controller → Service → Repository
```

| Archivo | Responsabilidad |
|---|---|
| `controller/AuthController.java` | Login y consulta del usuario autenticado |
| `dto/request/AuthRequest.java` | Entrada de acceso; excluye contraseña de `toString` y serialización |
| `dto/response/AuthResponse.java` | Token Bearer, duración en segundos y datos públicos del usuario |
| `dto/response/UsuarioResponse.java` | Perfil público, sin contraseña ni hash |
| `domain/Usuario.java` | Representación del usuario fuera de JPA, sin datos secretos |
| `mapper/UsuarioMapper.java` | Entity → dominio → respuesta usando MapStruct |
| `entity/UsuarioEntity.java` y `entity/RolEntity.java` | Columnas de V2/V6 y relación `ManyToOne` usuario–rol |
| `repository/UsuarioRepository.java` y `repository/RolRepository.java` | Consultas JPA; el usuario carga su rol mediante `EntityGraph` |
| `service/UsuarioService.java` | Busca usuario y rol activos; adapta JPA a Spring Security |
| `security/UserInfoDetails.java` | Identidad y authorities `ROLE_ADMIN`, `ROLE_GESTOR` o `ROLE_LECTOR` |
| `security/JwtService.java` | JWT HS256 con emisor, sujeto, ID, emisión y vencimiento |
| `security/JwtAuthFilter.java` | Procesa el Bearer y recarga permisos vigentes |
| `security/SecurityErrorHandler.java` | Respuestas JSON 401/403 y fallos internos del filtro |
| `config/AppConfig.java` | BCrypt, proveedor de autenticación y AuthenticationManager |
| `config/SecurityConfig.java` | Rutas, roles, filtro y sesiones deshabilitadas |
| `config/DemoUsuariosConfig.java` | Creación transaccional de las tres cuentas con perfil dev |

Todas las rutas Java son relativas a
`backend/inventario/src/main/java/com/utec/inventario/`.

Se conserva JJWT **0.13.0**, como en los ejemplos del profesor: `jjwt-api` para
compilar y `jjwt-impl`/`jjwt-jackson` en ejecución. Se mantienen las versiones
existentes de Spring Boot, Java, Gradle, MapStruct y Lombok.

El JWT contiene identidad y fechas. Los roles se consultan en PostgreSQL en cada
petición; desactivar un usuario o rol invalida su acceso inmediatamente, y cambiar
su rol modifica sus permisos incluso si presenta un token anterior. No hay
refresh token ni endpoint de registro público.

El secreto de firma se obtiene de `JWT_SECRET`, en Base64 y con al menos 32 bytes
aleatorios. La duración predeterminada es de 1800 segundos y se puede configurar
mediante `JWT_EXPIRATION_SECONDS`. Una clave nueva invalida los tokens anteriores.

## Migración y persistencia

V6 agrega `usuario.username` y un índice único sobre `UPPER(username)`. Si hubiera
usuarios anteriores, se les asigna `usuario_<id_usuario>` como identificador de
acceso; se conservan sus datos. Las tres cuentas de demostración usan sus nombres
de acceso explícitos. V1–V5 no se editaron.

Flyway administra el esquema y Hibernate sigue usando `ddl-auto=validate`.
Los hashes los genera BCrypt al crear cuentas mediante JPA; no hay contraseñas
incluidas en SQL de migración.

## Arranque en Windows PowerShell

La forma rápida, con PostgreSQL iniciado y desde la raíz del repositorio, es:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

Solicita las credenciales de PostgreSQL si faltan y genera `JWT_SECRET` en
memoria. Las cuentas ya creadas se consultan sin necesidad de activar `dev`.
Usa `-CrearUsuariosDemo` solo para crear las cuentas que falten en una base nueva.
Mantén abierta la terminal hasta terminar con Postman. Una clave nueva requiere
iniciar sesión otra vez. En VS Code está disponible la tarea local
**Iniciar backend de Inventario**, que ejecuta este mismo script.

El error `Could not resolve placeholder 'JWT_SECRET'` indica que se ejecutó el
backend sin esa variable. `ECONNREFUSED` en Postman indica que no hay conexión
con el servidor; comprueba que aparezca `Started InventarioApplication` y que la
terminal del backend siga abierta. `BUILD SUCCESSFUL` por sí solo no confirma
el arranque, porque DevTools puede informar un fallo de aplicación por separado.

Si prefieres configurar las variables manualmente, sigue estos pasos.

Desde la raíz del repositorio:

```powershell
Set-Location .\backend\inventario
$env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_laboratorios'
$env:DB_USER = Read-Host 'Usuario local de PostgreSQL'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Contraseña de PostgreSQL' -AsSecureString)
).Password

# Genera una clave para esta sesión. No la imprimas ni la agregues al repositorio.
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

.\gradlew.bat bootRun
```

Introduce la contraseña de demostración acordada en el segundo aviso de
contraseña; es independiente de la contraseña de PostgreSQL. Espera a que termine
el arranque antes de probar. El backend escucha en `http://localhost:8080`.

Mantén esta terminal abierta. Al reiniciar con `Ctrl+C` y `bootRun` en la misma
ventana, se conserva la clave de esa sesión. Si generas otra clave en una nueva
terminal, inicia sesión otra vez para obtener un token nuevo.

Sin el perfil dev también se puede iniciar el backend con la base preparada y
`JWT_SECRET`; las cuentas existentes se consultan normalmente y no se crean demos.

## Postman: login y autorización

Crea variables de colección `baseUrl` = `http://localhost:8080`, `tokenAdmin`,
`tokenGestor` y `tokenLector`. Copia los tokens manualmente; no necesitas scripts.

1. **Login ADMIN**: POST `{{baseUrl}}/api/auth/login`, Authorization **No Auth**,
   Body **raw → JSON**, `Content-Type: application/json`.

   ```json
   { "userName": "marko", "password": "<CLAVE_DEMO>" }
   ```

   Sustituye el marcador por la contraseña de demo. Espera **200** con
   `accessToken`, `tokenType: "Bearer"`, `expiresIn: 1800` y `usuario.rol: "ADMIN"`.
   Guarda `accessToken` en `tokenAdmin`.

2. **Login GESTOR y LECTOR**: repite con `aldo` y `romel`, usando la misma
   contraseña inicial. Guarda los tokens en `tokenGestor` y `tokenLector`.

3. **Perfil**: GET `{{baseUrl}}/api/auth/me`, Authorization **Bearer Token** con
   `{{tokenAdmin}}`. Espera **200**, `userName: "marko"`, rol ADMIN, sin hash.
   Repite con los otros dos tokens y compara usuario y rol.

4. **Lectura**: GET `{{baseUrl}}/api/categorias` con cada token. Los tres deben
   recibir **200**.

5. **Escritura ADMIN**: POST `{{baseUrl}}/api/categorias`, Bearer `{{tokenAdmin}}`:

   ```json
   { "nombre": "Categoría JWT manual 01", "descripcion": "Creada por ADMIN" }
   ```

   Espera **201** y guarda el ID como `categoriaJwtId`. Elige un nombre nuevo en
   cada repetición, porque los nombres de categorías inactivas siguen reservados.

6. **Escritura GESTOR/LECTOR**: repite POST con cada uno de sus tokens y un nombre
   nuevo. Ambos deben recibir **403**, sin insertar una fila. PUT y DELETE al ID
   de la prueba anterior también deben devolver **403** para ambos roles.

7. **Actualización ADMIN**: PUT `{{baseUrl}}/api/categorias/{{categoriaJwtId}}`,
   Bearer `{{tokenAdmin}}`:

   ```json
   { "nombre": "Categoría JWT manual 01", "descripcion": "Actualizada por ADMIN" }
   ```

   Espera **200**. Conserva el ID y la fecha de creación.

8. **Baja ADMIN**: DELETE a la misma URL con Bearer `{{tokenAdmin}}`. Espera **204**.
   El GET posterior con cualquier token válido devuelve **404** y SQL conserva
   la fila con `activo=false`.

9. **Sin token**: GET `{{baseUrl}}/api/categorias`, Authorization **No Auth**.
   Espera **401**. Ya no corresponde el 200 de la configuración temporal anterior.

10. **Token inválido o alterado**: repite GET con Bearer `token-inventado` o cambia
    un carácter del JWT. Espera **401**, sin detalles técnicos.

11. **Contraseña incorrecta o usuario inexistente**: POST al login con **No Auth**.
    Ambos casos deben devolver **401** con el mismo mensaje genérico.

12. **Validación del login**: POST al login con `{}`. Espera **400**, con errores
    de `userName` y `password`.

13. **Intento de elegir un rol al iniciar sesión**: login de `romel`, añadiendo
    `"rol": "ADMIN"` al JSON. El perfil continúa siendo LECTOR y las escrituras
    siguen devolviendo **403**. El cliente no controla el rol.

14. **Token vencido**: en una sesión de prueba, inicia con
    `$env:JWT_EXPIRATION_SECONDS = '5'`, obtén un token, espera más de cinco
    segundos y consulta categorías. Espera **401**. Después retira esa variable
    con `Remove-Item Env:JWT_EXPIRATION_SECONDS` y reinicia para volver a 30 minutos.

Los errores usan el mismo formato `ApiError`. **401** indica falta de
autenticación válida; **403** indica que el usuario autenticado no tiene permiso.

## Comandos de comprobación en otra ventana PowerShell

```powershell
$base = 'http://localhost:8080'
$demoPassword = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Contraseña de las cuentas demo' -AsSecureString)
).Password

$admin = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ userName = 'marko'; password = $demoPassword } | ConvertTo-Json)
$lector = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ userName = 'romel'; password = $demoPassword } | ConvertTo-Json)

# 200: perfil del administrador.
curl.exe -i "$base/api/auth/me" -H "Authorization: Bearer $($admin.accessToken)"

# 200: el lector sí puede consultar categorías.
curl.exe -i "$base/api/categorias" -H "Authorization: Bearer $($lector.accessToken)"

# 403: el lector no puede escribir, aunque envíe un cuerpo inválido.
'{}' | curl.exe -i -X POST "$base/api/categorias" `
    -H "Authorization: Bearer $($lector.accessToken)" `
    -H 'Content-Type: application/json' --data-binary '@-'

# 401: falta un token válido.
curl.exe -i "$base/api/categorias"
curl.exe -i "$base/api/categorias" -H 'Authorization: Bearer token-inventado'
```

## Comprobación SQL en pgAdmin

```sql
SELECT u.id_usuario, u.username, u.nombre, u.apellido, u.email,
       r.nombre AS rol, u.activo, r.activo AS rol_activo,
       u.fecha_creacion,
       u.password_hash LIKE '$2%' AS tiene_hash_bcrypt
FROM usuario u
JOIN rol r ON r.id_rol = u.id_rol
ORDER BY u.id_usuario;

SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT id_categoria, nombre, descripcion, activo, fecha_creacion
FROM categoria
ORDER BY id_categoria;
```

La primera consulta muestra el indicador de hash BCrypt sin revelar el hash.
Para repetir pruebas de desactivación o cambios de rol utiliza la base separada
de verificación: los tests automáticos restauran esos estados al finalizar.

## Repetir la suite automática

En pgAdmin, conectado a `postgres`, crea una base de pruebas una vez:

```sql
CREATE DATABASE inventario_verificacion_manual;
```

Con las variables del arranque definidas, desde `backend/inventario`:

```powershell
$previousDbUrl = $env:DB_URL
try {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_verificacion_manual'
    .\gradlew.bat test --rerun-tasks --no-daemon --console=plain
} finally {
    $env:DB_URL = $previousDbUrl
}
```

Las pruebas HTTP de seguridad y concurrencia solo se habilitan para nombres de
base `inventario_verificacion_*`. El reporte queda en
`backend/inventario/build/reports/tests/test/index.html`.

## Resultado de la implementación y verificación

Verificación realizada el **8 de septiembre de 2026**:

| Comprobación | Resultado |
|---|---|
| Compilación y empaquetado con `bootJar` | Correctos |
| Suite JUnit completa | 40 pruebas; 0 fallos, 0 errores y 0 omitidas |
| Comprobaciones HTTP/SQL de categorías con JWT ADMIN | 32 de 32 correctas |
| Aplicación de V6 y comprobaciones en la base habitual | 8 de 8 correctas |

La suite incluye acceso de los tres roles, credenciales incorrectas, token
ausente/alterado/vencido, claims obligatorios, estado de usuario y rol, cambio de
permisos con un token anterior, contraseñas multibyte que exceden los 72 bytes de
BCrypt y las regresiones de concurrencia de categorías.

Las tres cuentas están creadas en `inventario_laboratorios`, activas y con sus
roles correspondientes. Se verificaron tres hashes BCrypt distintos, login,
perfil y permisos; un reinicio real conservó ID, hash y rol sin duplicar cuentas.
Los datos de categorías anteriores se compararon y permanecieron iguales.
Flyway tiene V1–V6 exitosas en esa base. No se editaron V1–V5.

La base temporal usada para las pruebas se eliminó al terminar y los procesos de
verificación se detuvieron. Para utilizar la aplicación, ejecuta el arranque
PowerShell de esta guía y obtén un token nuevo en Postman.

Referencias:
[JJWT y sus dependencias](https://github.com/jwtk/jjwt#installation),
[DaoAuthenticationProvider de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html).
