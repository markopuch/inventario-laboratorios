# Memoria de Docker, CI/CD y deployment

**Paginación y limitación de login, publicación posterior del 27:** backend406,
frontend107/build1681 y UI real con53 fixtures temporales aprobados. No se cambian
Dockerfiles/Compose ni migraciones. Configuración nueva opcional `LOGIN_MAX_FAILURES`,
`LOGIN_FAILURE_WINDOW_SECONDS`, `LOGIN_IP_ATTEMPTS`, `LOGIN_IP_WINDOW_SECONDS`,
`LOGIN_MAX_ENTRIES`; defaults5/900/60/60/10000. El contrato corregido agrega
fallos por username normalizado, independiente del peer; la cuota general de
solicitudes usa getRemoteAddr y puede compartirse detrás de un proxy. No se
interpretan X-Forwarded-For ni Forwarded. Los contadores son por instancia y se reinician
con el proceso. No configurar trusted headers implícitamente ni afirmar que
se identificó la IP pública de cada navegador. Para probar UI se acortó únicamente
la ventana temporal de fallos a60s, sin tocar servicios habituales; pruebas de
reloj controlado verificaron expiración y concurrencia. Contenedores/redes y
Vite temporales detenidos. La revisión automática bloqueó borrar un fixture
privado de acceso de un entorno ya eliminado dentro de build/ ignorado; sin
claves de bases habituales. [Verificación](../despliegue/verificacion-paginacion-login-2026-10-27.md).
El backend se publicó primero en `34d8033ed4e9e79b1f8767f9e277240b7159891b`.
Actions **40564481861** aprobó 5/5 trabajos (406 backend, 93 frontend anterior y
28 importador). Render desplegó ese SHA, `dep-dbgf7bdg1s2s739dull0`, **Live en
4m16s**, arranque 181.6 s, trece migraciones al día y salud UP. Swagger muestra
page/size/q y 429; ADMIN consultó 20 elementos de un total de 258.
Después se publicó el frontend `74128312fe30a8ac85280bf9c5a6f749ce4d364b`.
Vercel reporta success para ese SHA, ficha `nBQ59JBg7CcodNPtqu2y7NGYCpTs`;
Actions **40565271628** terminó completed/success el 27 a las 14:05:11 Lima:
406 backend, 107 frontend/build 1681 y 28 importador. La UI pública comprobó
tamaños 20/50/100, página 2 de 258 equipos, búsqueda y Auditoría 20/100 sobre
351 eventos. La corrección del límite cloud se comprobó después, como se
detalla a continuación.
El stack Docker principal no se actualizó por publicar commits o imágenes.
[Informe cloud](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md).
La corrección de los fallos agregados por username se publicó en
`ef31d659983493e9ca9eeea2e063caf0b5e63029`; regresión local **410/410**, sin
fallos/errores/omitidas, JAR correcto y BUILD SUCCESSFUL en 2m17s. Actions
**40567596491** aprobó 5/5 a las 14:20:24 Lima: 410 backend, 107 frontend/build
1681, 28 importador y ambas imágenes publicadas. Vercel ef31d65 success,
ficha `E4o5pumzx8Z9hPRVXSsvtQKYysTu`. Render desplegó ef31d65 en
`dep-dbgfh18473hc739j058g`, **Live en 11m46s**, con salud UP. Se observó un
arranque prolongado y un pod nuevo; el diagnóstico PostgreSQL de solo lectura
con `sslmode=verify-full` confirmó 258 equipos, trece migraciones y ausencia
de bloqueadores en ese momento. No se demostró la causa exacta ni se hicieron
reinicios manuales o cambios de configuración antes de ese diagnóstico.
Después, cuatro GET públicos con nonce y sin caché aún mostraron el contrato
previo por usuario/origen; GitHub ef31d65 sí contiene el contrato agregado.
Se contrastaron repo/main, raíz backend/inventario, Dockerfile ./Dockerfile y
contexto . de Render. Esa discrepancia no demuestra su causa. Se inició
**Clear build cache & deploy** del mismo ef31d65, `dep-dbgfo1mi0phs73dt8020`,
con Building y Build cache cleared visibles. El primer intento UI ficticio
devolvió 401 y se detuvo.
La reconstrucción limpia acreditó en logs checkout ef31d659 de main, COPY
de src y bootJar BUILD SUCCESSFUL en 26 s (14:31:30–14:32:41 Lima). El build
correcto se completó con el nuevo Live: **4m52s**, startup 171.8 s y OpenAPI
pública corregida. Cinco POST con conexiones independientes a Render y un
username ficticio devolvieron 401; el sexto desde la UI Vercel activó espera
868→839 s y botón Reintentar deshabilitado. La prueba cruzada acredita el
contador agregado. La discrepancia quedó resuelta tras reconstruir sin causa
exacta demostrada. La comprobación API cloud final aprobó **59/59**, de
14:37:40 a 14:38:42 Lima: cinco 401 y sexto 429 desde conexiones independientes,
Retry-After 893 s, no-store y ApiError. Login ADMIN posterior 200, páginas,
búsqueda, Auditoría, Jorge con 56 equipos y proxy correctos; conteos finales
258 equipos, seis usuarios y 351 auditorías conservados. El JSON fallido
inicial y el final conservan resultados separados en el informe cloud.
El clúster
PostgreSQL privado quedó detenido, con limpieza recursiva rechazada por la
revisión automática (`blocked by policy`, sin motivo adicional); permanece un
residuo privado fuera de Git, además de los anteriores. Un tercero puede provocar una espera
temporal al acumular fallos contra un nombre; no es bloqueo persistente de
cuenta. El contador se reinicia con el proceso y no se comparte entre réplicas.

**Documentación API posterior, 27 de octubre:** el backend incorpora Springdoc
3.2.0 y `API_DOCS_ENABLED` (default true), sin modificar Dockerfiles ni Compose.
La especificación y Swagger UI se sirven desde el backend, no desde las rutas
SPA del frontend Vercel. El workflow ahora incluye cambios de Postman como
disparador y valida la colección/scripts con Python/Node antes de JUnit.
Comprobación local nueva: 373/373 backend y 93/93 frontend; Postman 70 rutas y
75 solicitudes contrastadas sin HTTP. Chrome verificó 70 operaciones, Authorize,
consulta autenticada 200 y autorización no persistida, sin errores de página.
Se ejecutó el JAR nuevo en backend/BD/red Docker temporales, eliminados al final;
stack principal conservado saludable. [Guía y evidencia](../backend-final/openapi.md).
Después se publicó **d8f2123** en main; Actions **40554578802** aprobó 5/5,
373 backend, 93 frontend, 28 importador y Postman. Ambas imágenes GHCR
`sha-d8f2123`/`latest` coinciden por digest. Render hizo Auto-Deploy del commit,
**Live en 4m56s**; startup 175.701 s, V1–V13 validadas sin migración nueva.
Swagger público, Authorize/login/perfil 200, JSON/YAML y salud UP verificados;
258 equipos/seis usuarios/351 auditorías conservados, con proxy Vercel válido.
La autorización de prueba se eliminó al recargar. [Informe cloud](../despliegue/verificacion-swagger-render-2026-10-27.md).
El estado Render 58ac8f6 descrito abajo es el antecedente de la migración.

**Migración de datos a Render, 27 de octubre:** copia SQL finalizada a las
11:22:05 Lima desde un respaldo nuevo de PostgreSQL Windows. Se respaldaron
ambas instancias y se probaron sus restauraciones en PostgreSQL temporal antes
de sobrescribir el destino autorizado. `miproyecto-db` contiene **258 equipos,
seis usuarios, 351 auditorías y cuatro asignaciones**; las trece tablas,
dieciséis FK, once secuencias y Flyway V1–V13 coinciden con el origen respaldado.
El backend fue suspendido y reanudado para el corte. Panel y logs confirman
Docker, plan Free, Oregon, rama main, Root Directory backend/inventario,
despliegue 58ac8f6, Java 21.0.12.1, Spring Boot 4.1.1 y PostgreSQL 18.6.
La comprobación posterior acreditó salud UP y tres logins HTTP 200 (Jason ADMIN,
Jorge GESTOR y Gabriela ADMIN), alcance correcto, seis usuarios, 258 equipos y
351 auditorías. Reportes y proxy autenticado Vercel muestran 258 equipos; el
acceso anónimo a `/api/auth/me` por Vercel devuelve 401. El arranque Free tardó
varios minutos; se esperó la respuesta real, sin equiparar Live con salud.
Windows y Docker se conservaron.
Fuentes y detalles: [informe de migración](../despliegue/verificacion-migracion-render-2026-10-27.md).
Los párrafos previos a esta migración que siguen describen estados históricos.

**Cierre actualizado el 27 de octubre de 2026:** la importación y el recorrido
se comprobaron con backend y PostgreSQL nativos reales; 257 equipos cargados,
31 pendientes y 258 equipos totales. El workflow integra verificaciones
backend/JUnit en PostgreSQL aislado, frontend e importador como requisito para
publicar las dos imágenes en GHCR. Regresión local: importador **28/28**,
frontend **93/93** y backend **365/365**, con build frontend y JAR correctos.
JUnit utilizó PostgreSQL 18 temporal en 55432 y locale builtin `C.UTF-8`;
ese clúster propio se detuvo y eliminó. El
[anexo del cierre](../despliegue/verificacion-cierre-2026-10-27.md)
registra los resultados finales de las suites, commit y Actions. Durante el
cierre hasta las 00:19 el motor Docker estaba inaccesible; después el usuario lo
encendió y se comprobaron tres servicios saludables. El stack sigue con imágenes
anteriores sin revisión OCI identificable. Después, las imágenes nuevas 58ac8f6
aprobaron **22/22 comprobaciones en Docker aislado**, incluidas siete de UI y
cero errores de página; eso no actualizó el stack principal.
Ese cierre inicial no hizo despliegue manual Render/Vercel; la reconstrucción
limpia posterior del límite de login se registra arriba. Sus datos siguen
siendo independientes de Docker.

**Comprobación posterior del 27:** la web Docker en `localhost:3000` devolvió
HTTP 200 y `/api/auth/me` anónimo a través de su proxy devolvió 401. Esa base
contiene tres equipos, dos laboratorios y tres
cuentas (Marko ADMIN, Aldo LECTOR y Romel LECTOR); la nativa conserva 258 equipos
y seis cuentas. No se migraron datos ni se cambiaron roles en esta consulta.
Ver [estado posterior y separación de instancias](../despliegue/verificacion-cierre-2026-10-27.md).

**Mantenimiento Docker posterior, 27 de octubre 00:36:35 Lima:** por solicitud
expresa se restablecieron las tres cuentas demo en una transacción serializable
privada, fuera de la API: tres hashes BCrypt y tres eventos reales
`RESTABLECER_CONTRASENA`, actor Marko. Los veinte eventos previos se conservaron;
auditoría suma **23**. Tres logins HTTP 200 confirmaron Marko ADMIN, Aldo LECTOR
y Romel LECTOR; equipos, dos laboratorios, cuatro asignaciones y roles se
conservaron. Este mantenimiento no cambió los controles del stack anterior;
la política de 15 caracteres/72 bytes del código publicado se verificó por
separado en el smoke de las imágenes nuevas.
El valor de la clave y sus hashes no se documentan. Las seis cuentas nativas
se comprobaron después, de forma independiente.

**Smoke de imágenes publicadas, 27 de octubre 00:44:35–00:45:44 Lima:** backend
y Jason con revisión exacta 58ac8f6 y digests GHCR se ejecutaron en loopback
18080/13000, red propia y PostgreSQL 18.6 en tmpfs, con cero equipos iniciales
y tres cuentas seed. Flyway V1–V13, salud/proxy, tres roles, denegaciones 403,
Auditoría real de una categoría temporal, política de 15 caracteres y CSV UTF-8/BOM de 17
columnas aprobaron; son **22 comprobaciones, incluidas siete de UI**. La
[evidencia versionable](../despliegue/evidencias/docker-smoke-58ac8f6-2026-10-27.json)
registra la eliminación de los tres contenedores propios, su red y tres archivos
privados, sin errores. El stack principal mantuvo exactamente IDs, imágenes y
volúmenes, con tres servicios saludables; no se transfirieron datos.

**Mantenimiento nativo posterior, 27 de octubre 00:46:30 Lima:** se verificaron
los seis restablecimientos puntuales y seis logins HTTP 200 (Marko ADMIN, Aldo
GESTOR, Romel LECTOR, Jason ADMIN, Jorge GESTOR y Gabriela ADMIN). Hubo seis
eventos reales de restablecimiento, actor Jason; los anteriores, roles,
asignaciones y resumen de inventario se conservaron. Las tres claves antiguas
de las cuentas nuevas devolvieron 401. La API rechazó una clave de ocho caracteres
con 400, por lo que la política general de 15/72 permanece intacta. El total de
esta operación es nueve restablecimientos locales: seis nativos y tres Docker;
no incluye la nube ni modifica el stack principal o sus imágenes.

La limpieza final retiró el helper temporal y su import de arranque, además
de los payloads privados de solicitud. El código backend/frontend/scripts/Actions
quedó sin diferencias; `compileJava` aprobó en 19 s. Tras la limpieza, salud
nativa UP y los seis logins HTTP 200 volvieron a comprobarse. El Docker principal
conservó sus tres servicios saludables y volumen original.

El código del cierre se publicó en `main`, commit
[`58ac8f6`](https://github.com/markopuch/inventario-laboratorios/commit/58ac8f6123849e7467663a53ac62b1c9a09318b2).
La [ejecución 40454293249](https://github.com/markopuch/inventario-laboratorios/actions/runs/40454293249)
terminó **completed/success, 5/5 trabajos**, el **27 de octubre 00:18:54 Lima**.
Los logs repitieron backend 365/365, frontend 93/93 y build de 1679 módulos,
e importador 28/28;
las dos imágenes se publicaron con `sha-58ac8f6` y `latest`. La consulta
independiente `docker buildx imagetools inspect` de las cuatro etiquetas pasó
sin daemon, con digests iguales a los de publicación. Los índices OCI incluyen
linux/amd64 y atestación; esto acredita GHCR, no ejecución local de contenedores.
Digests publicados:

- Backend: `sha256:ccde889fa75361e791c9375c031e0c63450c40cf02d34233a4c5af4a61de14b9`.
- Jason: `sha256:183d4100c69eb95879b7ba13fd0e9c3ae7ea6c53118118ae1235c8361efa8a74`.

Las imágenes corresponden al commit de código 58ac8f6; una actualización posterior
solo de Markdown no vuelve a construirlas por los filtros del workflow.

**Actualización de organización, 26 de octubre de 2026:** la variante Marko y sus
sprints quedan solo locales, ignorados por Git. Un clon nuevo no contiene
`frontend/version-marko/` ni `docs/sprints_realizados-frontend-Marko/`.
La retirada del seguimiento no modifica Compose, Dockerfiles, CI ni Vercel:
todos seleccionan Jason. Los resultados operativos que siguen pertenecen a la
auditoría inicial; esta reorganización no vuelve a acreditar servicios o login.

**Actualización funcional local del mismo día:** Jason incorpora CSV de Reportes
y Auditoría ADMIN; Configuración se retiró y Asignaciones tiene acceso de menú.
La suite frontend pasó 90/90, el build transformó 1679 módulos y se verificaron
10 escenarios en navegador con API simulada. No se reconstruyeron imágenes Docker,
en esa primera fase no se inició backend/PostgreSQL ni se publicaron cambios en Render/Vercel.
Las observaciones operativas de la auditoría inicial siguen con su fecha y límites.

Fecha de corte: **26 de octubre de 2026, America/Lima (UTC-05:00)**. Inspección local y consultas remotas de lectura realizadas alrededor de las **10:17–10:26**. El reloj consultado devolvió `2026-10-26 15:25:56 UTC` (10:25:56 Lima).

Repositorio: `C:\Users\marko\Desktop\fullstackweb\Proyecto\v2\inventario-laboratorios\inventario-laboratorios`, rama `main`, HEAD local `0344b3301c93f5b50894fbf454a21c7b129af37b` (`erd update`, 11 de octubre de 2026, 18:23:36 Lima). Remoto `https://github.com/markopuch/inventario-laboratorios.git`. El working tree estaba limpio al comenzar la auditoría; los archivos de memoria se agregaron después. No se ejecutó fetch/pull, build, deploy, login de aplicación, cambio de configuración ni arranque/parada de servicios.

## 1. Arquitectura de ejecución

### Docker local: tres servicios en un Compose

```text
Navegador → 127.0.0.1:3000 → frontend:80 (Nginx, SPA Jason)
                                  │ /api/*, JWT Authorization conservado
                                  ↓
                         backend:8080 (Spring Boot / Java 21)
                                  ↓ JDBC
                             db:5432 (PostgreSQL 18 Alpine)
                                  ↓
                  inventario-docker_postgres_data (volumen persistente)

Equipo anfitrión → 127.0.0.1:8080 → backend:8080 (acceso directo local)
```

La única definición Compose vigente es [backend/inventario/docker-compose.yml](../../backend/inventario/docker-compose.yml), proyecto `inventario-docker`. Usa la red por defecto de Compose, normalmente `inventario-docker_default`; el nombre real no pudo inspeccionarse hoy. PostgreSQL no publica puerto al anfitrión. Su volumen se monta en `/var/lib/postgresql`, adecuado al esquema de la imagen PostgreSQL 18. No confundirlo con el PostgreSQL nativo de Windows que sí escucha en 5432.

| Servicio | Imagen/build | Puerto anfitrión → contenedor | Salud / dependencia |
|---|---|---|---|
| `db` | `postgres:18-alpine` | Sin publicación; interno 5432 | `pg_isready`; 5 s, 20 reintentos, inicio 10 s |
| `backend` | Contexto `backend/inventario`; tag `inventario-backend:local` | `127.0.0.1:8080` → 8080 | GET `/actuator/health`; espera `db` healthy; inicio 60 s |
| `frontend` | Contexto `frontend/version-jason/frontend`; tag `inventario-frontend-jason:local` | `127.0.0.1:3000` → 80 | GET `/health`; espera `backend` healthy |

Los tres tienen `restart: unless-stopped`. La base Docker es `inventario_laboratorios`, rol técnico `inventario`. Los nombres esperados de contenedores son `inventario-docker-db-1`, `inventario-docker-backend-1` y `inventario-docker-frontend-1`; son nombres derivados de Compose y evidencia histórica, no una observación de contenedores activos hoy.

### Desarrollo local sin frontend Docker

Jason usa Vite en `localhost:5173`, puerto estricto. Su proxy `/api` apunta a `API_PROXY_TARGET` o, por defecto, `http://127.0.0.1:8080`. Marko también usa 5173 con puerto estricto, por lo que no deben iniciarse ambas variantes en ese puerto al mismo tiempo. La variante Marko tiene proxy por defecto a `http://localhost:8080` y no está incluida en Compose ni en la matriz CI.

El script [iniciar-backend.ps1](../../iniciar-backend.ps1) inicia Gradle `bootRun` y utiliza por defecto PostgreSQL nativo en `localhost:5432/inventario_laboratorios`. Solicita credenciales ausentes y genera una clave JWT aleatoria para el proceso si no existe. `-CrearUsuariosDemo` activa `dev` y solicita `DEMO_USER_PASSWORD`; es una operación de escritura y no se ejecutó en esta auditoría.

El ajuste del 27 de octubre en ese script y en `verificar-backend.ps1` evita que
Windows PowerShell interrumpa Gradle por avisos de `javac` en stderr: conserva
el código de salida real y restaura `ErrorActionPreference` al terminar la
invocación. Un fallo real de Gradle continúa deteniendo el proceso.

### Publicación en internet

```text
Navegador HTTPS → inventario-laboratorios.vercel.app (SPA Jason)
                     │ rewrite /api/:path*
                     ↓
         inventario-laboratorios-tyni.onrender.com/api/:path*
                     │ Spring Boot / JDBC
                     ↓
               PostgreSQL gestionado en Render
```

La conexión a PostgreSQL gestionado se conoce por el historial del proyecto consultado por el agente principal; no se accedió al panel de Render ni a esa base. No se conoce su versión exacta, tamaño, conteos actuales, backup ni plan a partir de esta inspección.

[vercel.json](../../frontend/version-jason/frontend/vercel.json), agregado en `b164c441ec5e923b94899c32c3b7932c7fcba867`, define dos rewrites en este orden: `/api/:path*` al backend Render y cualquier otra ruta a `/index.html`. El root directory de Vercel comunicado en el historial es `frontend/version-jason/frontend`; no se inspeccionaron los ajustes remotos. El proxy mantiene frontend/API bajo el mismo origen visto por el navegador. La configuración de Render (tipo exacto de despliegue, build/start commands, release actual y variables efectivas) no está exportada en un `render.yaml` ni se verificó desde el panel. La existencia de un Dockerfile no demuestra que Render lo esté usando.

## 2. Estado de la auditoría inicial del 26 de octubre frente a antecedentes

| Componente | Observación del 26 de octubre | Qué puede afirmarse |
|---|---|---|
| Docker local | CLI responde; daemon `desktop-linux` falla por named pipe `dockerDesktopLinuxEngine` inexistente | Motor inaccesible en la sesión; no se pudo listar contenedores, imágenes o volúmenes actuales |
| Frontend Docker local | GET `http://127.0.0.1:3000/health` rechazado | No responde en ese puerto durante la consulta |
| Backend local | GET `http://127.0.0.1:8080/actuator/health` rechazado | No responde en ese puerto durante la consulta |
| Vite local | Sin listener observado en 5173 | Ninguna variante ejecutándose ahí durante la consulta |
| PostgreSQL nativo | Servicio `postgresql-x64-18` Running / Automatic; proceso `postgres`, PID 7056; listeners `0.0.0.0:5432` y `[::]:5432` | Existe un servidor PostgreSQL nativo activo; no demuestra contenido de una base concreta |
| Vercel | GET `https://inventario-laboratorios.vercel.app/login`: HTTP 200, `text/html; charset=utf-8` | Frontend público sirve HTML; no acredita login ni todos los módulos |
| Render | GET `/actuator/health` agotó 35 s; segundo intento independiente agotó 50 s | Salud actual indeterminada; los timeouts no demuestran baja permanente |
| Proxy Vercel → Render | GET `/api/auth/me` agotó 35 s; segundo intento independiente agotó 50 s | No se confirmó hoy la respuesta 401 previamente observada |
| GitHub Actions | API consultada: run `38183985364` completed/success, ambos trabajos success | Última publicación CI visible corresponde a `b164c44`, posterior a la evidencia Docker de octubre 3 |

La comprobación pública del **11 de octubre**, recuperada por el agente principal del chat **Etapa final** (`01a12d6c-bb32-7fa2-9c1c-8e880f23e3b5`), registró `/login` 200, Render `/actuator/health` `UP` y proxy `/api/auth/me` 401 JSON. El usuario también había comunicado que publicó Jason en Vercel y creó una nueva base en Render sin importar la local. Es evidencia histórica/conversacional, no una consulta SQL actual. No quedó confirmado el login en nube ni la creación de las cuentas necesarias en esa base.

El reporte [verificacion-docker-actions-2026-10-03.md](../despliegue/verificacion-docker-actions-2026-10-03.md) llama «actual» al Docker local `e1ce75a` y deja Render/Vercel como futuro. Ese texto describe el corte del 3 de octubre: **no debe usarse para concluir que hoy no hay publicación en internet ni que `e1ce75a` es la última ejecución de Actions**.

## 3. Versiones y herramientas

### Instalación local observada

| Herramienta | Resultado |
|---|---|
| Windows | Microsoft Windows 11 Home, `10.0.26200`, build `26200`, 64 bits |
| Node.js | `v24.16.0` |
| npm | `11.13.0` |
| Java | Oracle Java `21.0.12`, build `21.0.12+7-LTS-205`, 64 bits |
| Git | `2.54.0.windows.1` |
| Docker CLI | `29.8.0`, API 1.56, Go 1.26.8, Windows/amd64 |
| Docker context | `desktop-linux` activo; `default` también configurado |
| Docker Compose | `v5.5.1` |
| Docker Desktop | `4.92.0`, versión instalada informada por registro de desinstalación; no demuestra motor activo |
| Docker Engine servidor | Versión no obtenida por motor inaccesible; no asumir que coincide con CLI |
| Binarios instalación PostgreSQL Windows | `18.6` (`postgres`, `psql`, `pg_dump`); ruta `C:\Program Files\PostgreSQL\18\bin`; sin `SELECT version()` al servidor activo |
| PowerShell | `7.6.5` |
| Python | `3.14.6`, ejecutable `C:\Python314\python.exe` |
| GitHub CLI | `2.101.0` |
| Visual Studio Code | `1.143.1`, registro y metadatos de `Code.exe` |
| Postman | `12.31.7`, registro y metadatos de `Postman.exe` |
| pgAdmin 4 | `9.17`, confirmado por `web/version.py` (`APP_RELEASE=9`, `APP_REVISION=17`); `FileVersion` del runtime Electron es otra numeración |
| Maven / Gradle global | No encontrados en PATH; el proyecto utiliza Gradle Wrapper, no Maven |

`psql` y `pg_dump` existen en la ruta absoluta indicada aunque no se encontraron por nombre en PATH. No se ejecutó `gradlew --version` ni se descargaron dependencias durante esta auditoría; la versión Gradle siguiente proviene de la declaración del wrapper.

### Versiones declaradas para build y contenedores

| Elemento | Declaración / alcance |
|---|---|
| Gradle Wrapper | `9.7.1`, archivo [gradle-wrapper.properties](../../backend/inventario/gradle/wrapper/gradle-wrapper.properties) |
| Java toolchain | 21 |
| Spring Boot | `4.1.1` |
| Spring dependency management plugin | `1.1.7` |
| MapStruct | `1.6.3` |
| JJWT | `0.13.0` |
| Backend build image | `eclipse-temurin:21-jdk` |
| Backend runtime image | `eclipse-temurin:21-jre-alpine` |
| PostgreSQL Docker image | `postgres:18-alpine` |
| Frontend build image y CI | `node:22-alpine` / `actions/setup-node` versión 22 |
| Frontend runtime image | `nginx:stable-alpine` |
| Jason paquete | `frontend-inventario-laboratorios` `1.0.0`; React `^18.3.1`, Vite `^6.0.7` declarados |
| Marko paquete, solo local/histórico | `inventario-frontend` `0.0.0`; React `^19.2.8`, Vite `^8.3.0` declarados; excluido del árbol versionado |

Las versiones con `^` son rangos, no la resolución exacta del lockfile; ver memoria frontend para dependencias resueltas. Los tags de imágenes base son mutables: no determinan patch exacto de Java, Node, PostgreSQL ni Nginx ejecutado en una imagen ya construida. Las versiones del host tampoco demuestran las versiones de Render/Vercel.

## 4. Construcción de las imágenes y Nginx

El [Dockerfile backend](../../backend/inventario/Dockerfile) usa dos etapas. Copia Gradle Wrapper/configuración, normaliza CRLF y permisos de `gradlew`, resuelve dependencias, copia `src` y ejecuta `./gradlew bootJar -x test --no-daemon`. La etapa final solo contiene el JAR, ejecuta UID/GID 10001 sin privilegios y arranca `java -XX:MaxRAMPercentage=75 -jar app.jar`; expone 8080.

El [Dockerfile Jason](../../frontend/version-jason/frontend/Dockerfile) usa `npm ci`, copia las fuentes y ejecuta `npm run build` con `VITE_API_URL=/api` como ARG/ENV de build. Copia `dist` y `nginx.conf` a Nginx. La variable de Vite queda incorporada al bundle y se cambia reconstruyendo/publicando el frontend, no mediante variables runtime de Nginx.

[nginx.conf](../../frontend/version-jason/frontend/nginx.conf) sirve SPA con fallback a `index.html`, `/health` 200 sin consultar la base, assets hash con caché de un año y archivo inexistente 404, `index.html` sin caché y `server_tokens off`. El proxy `/api` conserva ruta completa, query y cabecera `Authorization`; resuelve `backend:8080` con DNS interno Docker `127.0.0.11`, validez 10 s. No hay TLS dentro del Compose; la publicación Docker está enlazada al loopback del equipo.

Ambos `.dockerignore` excluyen `.env*`, cachés y artefactos generados; backend también excluye `application-local.properties`. No copiar secretos a la imagen ni a los documentos de memoria.

## 5. Variables por nombre y función

| Nombre | Consumidor / propósito | Presencia o valor público no sensible |
|---|---|---|
| `DOCKER_DB_PASSWORD` | Compose → `POSTGRES_PASSWORD` y `DB_PASSWORD` | Definida por nombre en `backend/inventario/.env`; valor omitido |
| `DOCKER_JWT_SECRET` | Compose → `JWT_SECRET` | Definida por nombre en el mismo `.env`; valor omitido |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Inicialización PostgreSQL Docker | DB y rol técnicos en Compose; contraseña externa |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Spring datasource | Obligatorios; Compose usa DNS `db:5432`; nube y ejecución nativa requieren destino correcto |
| `JWT_SECRET` | Firma/validación JWT | Obligatorio, Base64 de al menos 32 bytes |
| `JWT_EXPIRATION_SECONDS` | TTL JWT | Default 1800 |
| `PORT` | Puerto Spring | Default 8080; Compose fija 8080; proveedor puede asignarlo |
| `SPRING_PROFILES_ACTIVE` | Perfiles Spring | No se activa `dev` en Compose normal |
| `DEMO_USER_PASSWORD` | Creación inicial de cuentas bajo perfil `dev` | No renueva contraseñas de cuentas existentes; no es contraseña técnica de PostgreSQL |
| `VITE_API_URL` | Base URL Axios frontend | Default/ejemplo `/api`; aparece en `.env` Jason y `.env.local` Marko; valores privados no copiados |
| `API_PROXY_TARGET` | Proxy Vite Jason | Default `http://127.0.0.1:8080` |
| `GITHUB_TOKEN` | Login GHCR en Actions | Secret efímero de Actions; no copiar ni reemplazar por credenciales en YAML |

Spring Boot no carga automáticamente el archivo `.env` de Compose al ejecutar `bootRun`; deben estar las variables en el entorno del proceso o usarse el script de inicio. No mostrar `docker compose config` completo ni `docker inspect` completo en informes: pueden expandir secretos. Para validar sintaxis se usó `docker compose config --quiet`, que pasó; `--services` devolvió `db`, `backend`, `frontend`.

## 6. CI y publicación GHCR

[.github/workflows/imagen.yml](../../.github/workflows/imagen.yml), actualizado
el 27 de octubre, se activa por push/pull request a `main` cuando cambian backend,
Jason, scripts de carga/ejecución o el propio workflow; también admite ejecución
manual. Contiene tres verificaciones en Ubuntu:

- Backend: Java 21 Temurin, PostgreSQL 18 temporal exclusivo del runner y
  `compileJava test bootJar --rerun-tasks`; exige XML JUnit sin fallos, errores u omitidas.
- Jason: Node 22, `npm ci`, `npm test` y `npm run build` desde el monorepo completo.
- Importador: Python 3.14 y openpyxl 3.1.5; preparación/reanudación con datos
  sintéticos y API simulada, sin Excel ni credenciales locales.

La matriz de publicación (`backend`, `frontend-jason`) requiere las tres
verificaciones mediante `needs`. Publica únicamente por push o ejecución manual
en `main`; en pull requests solo se verifican cambios. El permiso `packages: write`
queda limitado al job de publicación; el resto usa `contents: read`.

Acciones declaradas: `actions/checkout@v7` con `persist-credentials: false`,
`actions/setup-java@v5`, `actions/setup-node@v7`, `actions/setup-python@v6`,
`docker/setup-buildx-action@v4`, `docker/login-action@v4`,
`docker/build-push-action@v7`. Publica:

- `ghcr.io/markopuch/inventario-laboratorios-backend:latest` y `:sha-<7 caracteres>`.
- `ghcr.io/markopuch/inventario-laboratorios-frontend-jason:latest` y `:sha-<7 caracteres>`.

En el workflow auditado inicialmente, el frontend ejecutaba `npm ci` y `npm test`
desde el checkout completo antes del Docker build, porque sus pruebas de contratos
leen DTO Java fuera del contexto del frontend. El backend construía el JAR con
`-x test` y aquel workflow no ejecutaba JUnit. El cierre autorizado del 27 de
octubre integra tres verificaciones (backend/PostgreSQL, frontend e importador)
y exige su aprobación antes de publicar imágenes; consultar su resultado en el
anexo del cierre. El Dockerfile backend conserva `bootJar -x test`, porque la
regresión se ejecuta antes de la construcción. Publicar una imagen en GHCR no
actualiza por sí solo Docker local ni demuestra deploy a Render/Vercel.

### Última ejecución visible consultada en la auditoría inicial del 26 de octubre

Fuente de lectura: API GitHub del repositorio mediante el conector GitHub, [lista de runs](https://api.github.com/repos/markopuch/inventario-laboratorios/actions/runs?per_page=4), [run 38183985364](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364) y jobs de esa ejecución.

| Campo | Resultado |
|---|---|
| Commit | `b164c441ec5e923b94899c32c3b7932c7fcba867` |
| Evento / rama | `push` / `main` |
| Inicio | 11 de octubre de 2026, **18:21:37 Lima** (`23:21:37Z`) |
| Última actualización | 11 de octubre de 2026, **18:23:14 Lima** (`23:23:14Z`) |
| Run | `38183985364`, intento 1, completed / success |
| Backend | [job 114602286727](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364/job/114602286727), success; build/publish success; pasos de Node skipped |
| Jason | [job 114602286855](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364/job/114602286855), success; pruebas y build/publish success |

Por la configuración, esa ejecución publica tags `sha-b164c44` y `latest` para ambos componentes. Los jobs confirman que el paso de publicación terminó bien; no se descargaron manifiestos ni se verificó hoy la retención/digest de los paquetes en GHCR. El commit HEAD `0344b33` solo actualiza ERD y no aparece como último run de este workflow; no confundir HEAD con imagen efectiva de cualquier entorno.

### Último Docker local documentado, distinto de la última publicación CI

La [evidencia del 3 de octubre](../despliegue/verificacion-docker-actions-2026-10-03.md) acredita imágenes `sha-e1ce75a` descargadas de GHCR, retagueadas como `inventario-backend:local` / `inventario-frontend-jason:local` y ejecutadas en el Compose habitual, conservando PostgreSQL y volumen:

- Backend digest `sha256:095be10c5cda0ca715f4f06d36e431cdb9e5dd7ee8bf805e6bfab5aa890a668b`.
- Frontend digest `sha256:54baf163b1f499f03c0946ba60f9539e1a4eb6d7a9600c2674f80d86716b2f44`.
- Flyway V1–V13 exitosas; tres servicios healthy en aquel momento.
- Entorno aislado eliminado después de 94/94 comprobaciones HTTP, 30 aserciones funcionales y siete controles SQL sin inconsistencias.
- CI histórico [run 37158784428](https://github.com/markopuch/inventario-laboratorios/actions/runs/37158784428) aprobó 67 pruebas frontend; 319 pruebas backend provienen de reportes locales previos, no de JUnit en CI ni de una ejecución hoy.

Hoy no se pudo comprobar que esas imágenes sigan presentes ni qué tags apuntan a ellas en el Docker local. Ninguna de esas pruebas históricas certifica el estado de la base nueva de Render.

## 7. Runbooks para continuar

Estos comandos son instrucciones para trabajo futuro autorizado; no fueron ejecutados para cambiar el entorno durante esta memoria.

### Inspección segura de Docker

Desde `backend/inventario`, con Docker Desktop disponible:

```powershell
docker version
docker compose config --quiet
docker compose config --services
docker compose ps
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
```

Si el motor vuelve a estar inaccesible, verificar Docker Desktop/contexto antes de asumir pérdida de datos. No crear una base nueva para resolver solo la falta de conexión al daemon.

### Ejecución Docker integrada

1. Confirmar y respaldar la base/volumen que se va a usar. Conservar `.env` privado con las dos variables `DOCKER_*`; no reemplazar contraseñas existentes arbitrariamente.
2. Para levantar imágenes disponibles sin reconstrucción: `docker compose up -d db backend frontend`. Si se quiere compilar las fuentes: `docker compose up -d --build db backend frontend`.
3. Comprobar `docker compose ps`, `/health` del frontend, `/actuator/health` del backend y `/api/auth/me` sin token (401 esperado).
4. Para detener conservando datos: `docker compose stop`.

**No usar `docker compose down -v`, borrar el volumen PostgreSQL, `docker system prune --volumes` ni activar reseeding como solución genérica.** Las migraciones Flyway deben aplicarse hacia delante y Hibernate está en `ddl-auto=validate`.

Para reproducir exactamente el Docker local histórico, usar tags `sha-e1ce75a` y digests documentados. Para otra versión, confirmar ambos tags/digests y compatibilidad de migraciones antes de actualizar. Un rollback de imagen no revierte automáticamente el esquema ni los datos. `latest` no identifica una versión reproducible.

### Desarrollo y pruebas

- Jason: desde su carpeta, `npm ci`, `npm test`, `npm run build`, `npm run dev`. El proxy espera backend en 8080 salvo `API_PROXY_TARGET` explícito.
- Backend nativo: desde raíz, `./iniciar-backend.ps1`; requiere DB y credenciales correctas. El JWT aleatorio por proceso invalida tokens previos cuando cambia.
- Backend completo: [verificar-backend.ps1](../../verificar-backend.ps1) exige una base local existente con nombre `inventario_verificacion_*`, variables externas y perfil `dev`; ejecuta `compileJava test bootJar --rerun-tasks`. No ejecutarlo sobre la base habitual ni la nube. Las pruebas pueden escribir fixtures.
- Respaldos: usar `pg_dump -Fc` con binarios versión 18 y destino privado, validar el backup antes de cambios de esquema; nunca almacenar dumps, hashes de contraseñas, tokens o valores secretos en memoria o Git.

### Diagnóstico de nube

1. Consultar `/login` de Vercel, `/actuator/health` de Render y `/api/auth/me` a través de Vercel sin autenticar; registrar status, content type, fecha y timeout. Un 200 de SPA no equivale a login funcional.
2. Si falla el proxy, revisar primero salud de Render y luego orden de rewrites/VITE_API_URL. No reemplazar `/api` por localhost en el build publicado.
3. Para acreditar despliegue real, leer los paneles/metadatos de Render y Vercel: commit actual, build logs, root directory y nombres de variables sin valores; no redeployar solo para consultar estado.
4. Antes de probar login o sembrar cuentas, resolver qué cuentas existen en la nueva base de Render. El password de PostgreSQL autentica al backend contra la base; no inicia sesión de ADMIN/GESTOR/LECTOR en la web.

## 8. Pendientes y límites concretos

- Docker principal sigue con sus imágenes y datos anteriores. Si se autoriza, actualizarlo conservando volumen y datos; las imágenes 58ac8f6 ya aprobaron un smoke aislado de 22 comprobaciones, sin reemplazar ese stack.
- Los timeouts de 35 s y 50 s pertenecen a la auditoría inicial del 26; el 27 se acreditó salud UP y Render ef31d65 Live. La nueva interfaz/CI se contrastan en el informe cloud, sin reutilizar esos timeouts como estado actual.
- La copia puntual Windows → Render y los logins/roles se comprobaron el 27; no hay sincronización continua ni copia de esos datos al Docker principal.
- Commit, plan Free, región, runtime y respaldos de Render se documentaron en la migración y publicaciones posteriores. Revisar la retención/expiración y el estado del despliegue concreto cuando vuelva a operar; la ficha Vercel por SHA no sustituye comprobar su alias e interfaz.
- El PostgreSQL nativo escucha en todas las interfaces; no se inspeccionaron firewall, `listen_addresses` ni `pg_hba.conf`, por lo que no se afirma acceso remoto efectivo. El Compose limita backend/frontend a loopback y no publica PostgreSQL.
- Versiones base Docker y tags de acciones son mutables; no están fijados a digest/SHA. No se modificó esta decisión durante la auditoría.
- La documentación de octubre 3 es valiosa para evidencia reproducible, pero su apartado de nube quedó superado por octubre 11; consultar esta memoria junto con el historial y la configuración actual.

## 9. Fuentes principales

- Configuración activa: [Compose](../../backend/inventario/docker-compose.yml), [Dockerfile backend](../../backend/inventario/Dockerfile), [Dockerfile Jason](../../frontend/version-jason/frontend/Dockerfile), [Nginx](../../frontend/version-jason/frontend/nginx.conf), [Vercel](../../frontend/version-jason/frontend/vercel.json), [workflow](../../.github/workflows/imagen.yml), [application.properties](../../backend/inventario/src/main/resources/application.properties).
- Versiones declaradas: [build.gradle.kts](../../backend/inventario/build.gradle.kts), [wrapper](../../backend/inventario/gradle/wrapper/gradle-wrapper.properties), package.json/lockfile de Jason. Las versiones Marko son evidencia de la copia local/histórica excluida; ver [anexo frontend](frontend.md). Versiones instaladas consultadas por comandos de lectura.
- Evidencia local previa: [informe Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md), [JSON Docker](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json), [JSON Actions e1ce75a](../despliegue/evidencias/actions-ghcr-e1ce75a-2026-10-03.json).
- Evidencia remota de la auditoría inicial del 26: API GitHub y [run 38183985364](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364), GET HTTP públicos descritos; sin autenticación de aplicación ni escrituras en esa fase.
- Evidencia del cierre del 27: [commit 58ac8f6](https://github.com/markopuch/inventario-laboratorios/commit/58ac8f6123849e7467663a53ac62b1c9a09318b2), [run 40454293249](https://github.com/markopuch/inventario-laboratorios/actions/runs/40454293249) y logs de sus cinco trabajos aprobados; consultas independientes de las cuatro etiquetas GHCR con digests coincidentes. No acredita ejecución de contenedores locales.
- Historial conversacional facilitado por el agente principal: chat **Etapa final**, verificación del 11 de octubre, y chat **Resumen grabaciones - docker 24,25,26y28**, publicación Vercel y nueva base Render sin importación confirmadas por el usuario.
