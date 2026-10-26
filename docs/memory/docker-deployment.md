# Memoria de Docker, CI/CD y deployment

**Actualización de organización, 26 de octubre de 2026:** la variante Marko y sus
sprints quedan solo locales, ignorados por Git. Un clon nuevo no contiene
`frontend/version-marko/` ni `docs/sprints_realizados-frontend-Marko/`.
La retirada del seguimiento no modifica Compose, Dockerfiles, CI ni Vercel:
todos seleccionan Jason. Los resultados operativos que siguen pertenecen a la
auditoría inicial; esta reorganización no vuelve a acreditar servicios o login.

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

## 2. Estado comprobado frente a antecedentes

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

[.github/workflows/imagen.yml](../../.github/workflows/imagen.yml) se activa por push a `main` cuando cambia backend, Jason o el propio workflow; también admite `workflow_dispatch` sobre `main`. Matriz de dos jobs (`backend`, `frontend-jason`), Ubuntu `latest`, `fail-fast: false`. Permisos `contents: read`, `packages: write`.

Acciones declaradas: `actions/checkout@v7` con `persist-credentials: false`, `actions/setup-node@v7`, `docker/setup-buildx-action@v4`, `docker/login-action@v4`, `docker/build-push-action@v7`. Publica:

- `ghcr.io/markopuch/inventario-laboratorios-backend:latest` y `:sha-<7 caracteres>`.
- `ghcr.io/markopuch/inventario-laboratorios-frontend-jason:latest` y `:sha-<7 caracteres>`.

El frontend ejecuta `npm ci` y `npm test` desde el checkout completo antes del Docker build, porque sus pruebas de contratos leen DTO Java fuera del contexto Docker del frontend. El backend construye el JAR con `-x test`: **el workflow actual no ejecuta JUnit**. Publicar una imagen en GHCR no actualiza por sí solo Docker local ni demuestra deploy a Render/Vercel.

### Última ejecución visible consultada hoy

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

- Docker local no observable hoy: verificar inventario real de contenedores/imágenes/volumen cuando el motor esté disponible, sin recrear datos.
- La salud cloud actual no quedó acreditada tras intentos de 35 s y 50 s; evitar presentar la verificación del 11 de octubre como estado actual.
- Cuentas/login de Render y datos gestionados pendientes de comprobación; no asumir importación de la base local.
- No se verificaron commit desplegado, plan, región, versiones de runtime, backups ni expiración de servicios Render/Vercel.
- El PostgreSQL nativo escucha en todas las interfaces; no se inspeccionaron firewall, `listen_addresses` ni `pg_hba.conf`, por lo que no se afirma acceso remoto efectivo. El Compose limita backend/frontend a loopback y no publica PostgreSQL.
- Backend CI omite JUnit; las pruebas locales históricas no prueban automáticamente cada nueva imagen.
- Versiones base Docker y tags de acciones son mutables; no están fijados a digest/SHA. No se modificó esta decisión durante la auditoría.
- La documentación de octubre 3 es valiosa para evidencia reproducible, pero su apartado de nube quedó superado por octubre 11; consultar esta memoria junto con el historial y la configuración actual.

## 9. Fuentes principales

- Configuración activa: [Compose](../../backend/inventario/docker-compose.yml), [Dockerfile backend](../../backend/inventario/Dockerfile), [Dockerfile Jason](../../frontend/version-jason/frontend/Dockerfile), [Nginx](../../frontend/version-jason/frontend/nginx.conf), [Vercel](../../frontend/version-jason/frontend/vercel.json), [workflow](../../.github/workflows/imagen.yml), [application.properties](../../backend/inventario/src/main/resources/application.properties).
- Versiones declaradas: [build.gradle.kts](../../backend/inventario/build.gradle.kts), [wrapper](../../backend/inventario/gradle/wrapper/gradle-wrapper.properties), package.json/lockfile de Jason. Las versiones Marko son evidencia de la copia local/histórica excluida; ver [anexo frontend](frontend.md). Versiones instaladas consultadas por comandos de lectura.
- Evidencia local previa: [informe Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md), [JSON Docker](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json), [JSON Actions e1ce75a](../despliegue/evidencias/actions-ghcr-e1ce75a-2026-10-03.json).
- Evidencia remota de hoy: API GitHub y [run 38183985364](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364), GET HTTP públicos descritos; sin autenticación de aplicación ni escrituras.
- Historial conversacional facilitado por el agente principal: chat **Etapa final**, verificación del 11 de octubre, y chat **Resumen grabaciones - docker 24,25,26y28**, publicación Vercel y nueva base Render sin importación confirmadas por el usuario.
