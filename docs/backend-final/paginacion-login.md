# Paginación, búsqueda de equipos y límite de login

Contrato actualizado el **27 de octubre de 2026**. Conserva las **70 operaciones**
existentes y Flyway V1–V13. Esta guía describe el código; las comprobaciones de
JUnit, UI y despliegue se registran por separado.

## Consultas con paginación opcional

| Método y ruta | Acceso | Elementos de content | Orden |
|---|---|---|---|
| GET /api/equipos | ADMIN global; GESTOR/LECTOR con alcance | EquipoResponse | id ascendente |
| GET /api/admin/equipos | ADMIN | EquipoResponse | id ascendente |
| GET /api/admin/auditoria | ADMIN | AuditoriaResponse | fecha descendente e id descendente |

Si se omiten **ambos** parámetros, `page` y `size`, la respuesta sigue siendo
el arreglo JSON anterior, incluido `[]` cuando no hay resultados. Enviar
**cualquiera** activa la respuesta paginada. `q` por sí solo no la activa.

| Parámetro | Regla |
|---|---|
| page | Entero base cero, mínimo 0. Si se envía solo size, utiliza 0. |
| size | Entero entre 1 y 100. Si se envía solo page, utiliza 20. |

```http
GET /api/equipos
GET /api/equipos?page=0
GET /api/admin/equipos?size=10
GET /api/admin/auditoria?page=1&size=20&entidad=equipo
```

El objeto paginado contiene exactamente estos campos:

```json
{
  "content": [],
  "number": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

`content` conserva los campos públicos de cada DTO. `number` es la página
solicitada y `size` su tamaño efectivo. Los totales describen todo el conjunto
**después de aplicar filtros y permisos**, antes de seleccionar la página.
`totalPages` es el redondeo superior de `totalElements / size`.

Una página posterior a la última devuelve `200` con `content: []`, el número
solicitado, los totales reales y `last: true`. No se cambia silenciosamente a la
última página. `first` indica que `number` es cero, aunque no haya resultados.

Valores negativos, no enteros, vacíos (`page=` o `size=`), tamaño 0 o superior
a 100 y enteros fuera de rango devuelven `400 ApiError`. También se rechaza
`page * size > 2147483647`, límite del desplazamiento de JPA. No hay parámetro
`sort` público: el orden estable lo decide el servidor.
Los demás listados y los agregados de Reportes conservan sus contratos.

## Búsqueda de equipos en el servidor

Ambos GET de equipos admiten `q` junto a `estado`, `idLaboratorio`,
`idSubcategoria` y `requiereMantenimiento`:

- Entrada de hasta 200 caracteres; trim al comienzo/final. Vacío o blanco no
  aplica búsqueda.
- Coincidencia parcial sin distinguir mayúsculas en `nombre`, `codigoInterno`,
  `marca`, `modelo` y **nombre del laboratorio**.
- `%`, `_` y `!` se interpretan como texto literal, no como comodines SQL.
- Búsqueda, filtros y alcance se combinan en servidor antes del conteo y del
  recorte. Un campo nulo no anula coincidencias en otro campo.

```http
GET /api/equipos?q=Microscopio
GET /api/equipos?page=0&size=20&q=Microscopio&idLaboratorio=12
```

GESTOR/LECTOR sin alcance reciben cero resultados; un laboratorio explícito
fuera de alcance conserva el `403` anterior. ADMIN mantiene lectura global,
incluidos BAJA y laboratorios inactivos cuando corresponde. Auditoría conserva
sus seis filtros y los días UTC; no admite `q`.

## Límite temporal de inicio de sesión

`POST /api/auth/login` conserva `AuthRequest`, `200 AuthResponse` y sus errores
de validación/autenticación. Añade límites por instancia:

| Límite predeterminado | Clave y ventana |
|---|---|
| Cinco fallos de autenticación | Usuario normalizado agregado, independiente del origen; 900 segundos desde el primer fallo |
| Sesenta solicitudes de login | Origen; 60 segundos desde la primera solicitud |

El usuario se normaliza con trim/minúsculas; la contraseña no se transforma.
Los primeros cinco fallos secuenciales devuelven `401`; el siguiente dentro
de la ventana devuelve `429`, aunque entonces se envíe una contraseña válida.
Un éxito previo al bloqueo reinicia los fallos de ese usuario, pero no
la cuota general del origen. Las reservas concurrentes también cuentan para
impedir superar el límite mientras BCrypt trabaja.

La cuota general se comprueba antes de interpretar body/JWT y cuenta también
solicitudes malformadas y posteriormente denegadas. Un `400` de validación no
se convierte en fallo de credenciales del usuario, pero consume cuota del
origen. El límite cubre únicamente el POST de login.

El origen procede de `HttpServletRequest.getRemoteAddr()`. El filtro no toma
la dirección de `Forwarded` o `X-Forwarded-For` enviados por el cliente.
Con un proxy, distintos usuarios pueden compartir el mismo peer y su cuota
general de solicitudes. El límite de fallos por username no depende de ese peer:
acumula intentos contra el mismo nombre aunque lleguen desde conexiones u
orígenes diferentes. Esto evita depender de la estabilidad del peer del proxy.
No equivale automáticamente a la IP pública de cada navegador: verificar
la configuración de infraestructura del entorno concreto.

Al alcanzar un límite se devuelve `429 Too Many Requests`, `ApiError` público
y `Retry-After` como entero positivo de segundos. La misma respuesta cubre
cuentas conocidas y desconocidas; no revela existencia. La capacidad acotada
de contadores también responde 429 al estar ocupada, sin crecimiento ilimitado.

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 900
```

```json
{
  "timestamp": "2026-10-27T00:00:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Demasiados intentos de inicio de sesión. Espera antes de intentarlo nuevamente.",
  "path": "/api/auth/login"
}
```

La espera concreta depende del tiempo restante. Respetar la cabecera; no
reintentar automáticamente ni guardar un token de esa respuesta.
Un tercero que acumule fallos contra un username puede causar también esa
espera temporal al dueño de la cuenta. No se modifica su estado en la BD ni
se garantiza defensa distribuida frente a intentos atendidos por otras réplicas.
Los contadores viven en memoria, con caducidad y hasta 10000 entradas por mapa.
Se reinician con el proceso y no se comparten entre réplicas. No hay Redis,
migración ni bloqueo persistente de cuentas en PostgreSQL. No cambia la política
de contraseñas 15 caracteres Unicode / 72 bytes UTF-8, las claves existentes ni
la vigencia de JWT emitidos.

| Variable opcional | Predeterminado |
|---|---:|
| LOGIN_MAX_FAILURES | 5 |
| LOGIN_FAILURE_WINDOW_SECONDS | 900 |
| LOGIN_IP_ATTEMPTS | 60 |
| LOGIN_IP_WINDOW_SECONDS | 60 |
| LOGIN_MAX_ENTRIES | 10000 |

Se valida al arrancar: contadores positivos, ventanas entre 1 y 86400 segundos,
máximo de fallos hasta 1000 y cuotas/capacidad hasta 100000. La configuración
se toma al arrancar; no existe endpoint administrativo para modificarla.

## Comprobación con Postman

La [colección](postman/README.md) mantiene 75 solicitudes. `page` y `size` se
entregan deshabilitados; `paginaListado=0` y `tamanoPaginaListado=20` son valores
iniciales para habilitarlos. Equipos añade `filtroBusquedaEquipo` vacío y `q`
deshabilitado. Habilitar solo los parámetros necesarios.

Los scripts distinguen arreglo/página según los parámetros activos y comprueban
campos, totales, límites, orden y página solicitada. Los tres logins verifican
éxito completo o `429` con ApiError y Retry-After; el límite nunca conserva
token/ID. Filtros inválidos son casos negativos explícitos: los GET ordinarios
siguen esperando `200`.

La validación estática/sandbox no hace HTTP ni prueba una base. La regresión
backend utiliza PostgreSQL temporal y
[verificar-backend.ps1](../../verificar-backend.ps1), sin provocar bloqueos
o crear fixtures en bases habituales.

Fuentes: [PaginacionRequest](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/PaginacionRequest.java),
[EquipoController](../../backend/inventario/src/main/java/com/utec/inventario/controller/EquipoController.java),
[AuditoriaController](../../backend/inventario/src/main/java/com/utec/inventario/controller/AuditoriaController.java),
[LoginRateLimiter](../../backend/inventario/src/main/java/com/utec/inventario/security/LoginRateLimiter.java),
[LoginRateLimitFilter](../../backend/inventario/src/main/java/com/utec/inventario/security/LoginRateLimitFilter.java)
y [configuración](../../backend/inventario/src/main/resources/application.properties).

La [verificación local del 27](../despliegue/verificacion-paginacion-login-2026-10-27.md)
registra 406 pruebas backend, 107 frontend, build y recorrido Chrome con API/BD
temporales aprobados. Después se publicó el backend `34d8033` y luego el frontend
`7412831`. Actions 40564481861 del primer commit aprobó 5/5 trabajos, con 406
pruebas backend, 93 del frontend anterior y 28 del importador. Render desplegó
ese backend en `dep-dbgf7bdg1s2s739dull0`, Live en 4m16s, salud UP y trece
migraciones al día; ADMIN consultó una página de 20 equipos de un total de 258.
Vercel reporta success para el SHA exacto del nuevo frontend. Su CI
40565271628 aprobó 406 backend, 107 frontend/build 1681 y 28 importador. La UI
pública verificó tamaños 20/50/100, segunda página/búsqueda con 258 equipos y
Auditoría 20/100 sobre 351 eventos. El límite agregado corregido de login en
cloud se comprobó después, como se detalla a continuación.
La implementación y las pruebas locales no acreditan por sí solas ese límite
tras un proxy. El [informe cloud](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md)
registra commit, despliegue y comportamientos observados por separado.
El contrato agregado por username documentado aquí es una corrección posterior
al observar seis 401 en la primera prueba cloud. Se publicó en `ef31d65`, tras
**410/410 pruebas backend locales** sin fallos/errores/omitidas, JAR y BUILD
SUCCESSFUL en 2m17s. Actions 40567596491 aprobó 5/5 trabajos, con 410 backend,
107 frontend/build 1681 y 28 importador, y publicó ambas imágenes. Vercel
reporta success para ef31d65, ficha E4o5pumzx8Z9hPRVXSsvtQKYysTu. Render
desplegó ese SHA en dep-dbgfh18473hc739j058g, Live en 11m46s y salud UP.
El guard posterior observó cuatro GET públicos sin caché con el contrato
previo pese al Live del panel; GitHub ef31d65 sí contiene el contrato agregado.
Se inició Clear build cache & deploy del mismo SHA, dep-dbgfo1mi0phs73dt8020.
Quedó Live en 4m52s, startup 171.8 s y OpenAPI pública corregida. Cinco POST
con conexiones independientes a Render devolvieron 401 para un username
ficticio; el sexto desde la UI Vercel activó espera 868→839 s y botón Reintentar
deshabilitado. La discrepancia quedó resuelta tras reconstruir, sin causa
exacta demostrada. La comprobación API cloud final aprobó **59/59** el 27, de
14:37:40 a 14:38:42 Lima: seis conexiones independientes dieron
`[401, 401, 401, 401, 401, 429]`, Retry-After 893 s, no-store y ApiError.
Un login ADMIN posterior devolvió 200; páginas, búsqueda, Auditoría, alcance de
Jorge con 56 equipos y proxy correctos. Se conservaron 258 equipos, seis
usuarios y 351 auditorías. El [JSON final](../despliegue/evidencias/paginacion-login-cloud-2026-10-27.json)
se conserva separado de la prueba inicial fallida. Los 406 tests y el recorrido
temporal con 53 fixtures anteriores conservan su alcance propio.
Referencias de diseño: [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
y [HTTP 429/Retry-After, RFC 6585](https://www.rfc-editor.org/rfc/rfc6585#section-4).
