# 1. Resumen Sprint 8

**Docker del backend operativo en local; automatización de publicación en GHCR
preparada y pendiente de confirmar en GitHub.** Este sprint continúa el cierre
funcional del [Sprint 7](sprint-7.md) e incorpora el empaquetado y la ejecución
del backend de Inventario con PostgreSQL en contenedores separados.

Se externalizó el puerto, se incorporó Actuator, se permitió consultar la salud
sin autenticación, se construyó `inventario-backend:local` y se preparó Compose.
El usuario ejecutó el entorno y compartió ambos servicios en estado `healthy`
y la respuesta `status: UP` de `/actuator/health`.

También existe el workflow `.github/workflows/imagen.yml`, adaptado a la
subcarpeta del backend y con un nombre de imagen exclusivo para este componente.
La existencia del archivo no demuestra que ya se haya ejecutado correctamente
en GitHub ni que el paquete esté publicado.

**Fecha de documentación:** 2026-10-01. Este reporte distingue la revisión de
archivos actuales, las comprobaciones realizadas durante la actividad y las
salidas compartidas por el usuario. Al redactarlo no se volvieron a ejecutar
Docker, Gradle, Git, pruebas ni consultas a PostgreSQL.

# 2. Estado inicial

El backend se encontraba cerrado funcionalmente para la entrega del Sprint 7.
Ese reporte registra 233 pruebas aprobadas y una verificación HTTP completa de
su alcance. Son resultados históricos: no se presentan como una nueva ejecución
de pruebas de este sprint.

La aplicación utiliza Java 21, Spring Boot, Gradle con Kotlin DSL, Spring
Security/JWT y PostgreSQL. Las credenciales de base de datos y la clave JWT ya
se recibían mediante variables de entorno. Flyway administraba V1–V9 y Hibernate
tenía `ddl-auto=validate`.

Al comenzar esta actividad faltaban Dockerfile, `.dockerignore`, Compose y el
workflow de imagen. El puerto era fijo y no estaban incorporados Actuator ni
la autorización explícita de su endpoint de salud.

# 3. Objetivo y alcance

Empaquetar exclusivamente el backend, ejecutar una instancia reproducible junto
a una base PostgreSQL independiente y preparar la construcción/publicación
automática de su imagen en GitHub Container Registry, GHCR.

Incluido en este sprint:

- Configuración por variables y comprobación de salud.
- Dockerfile de dos etapas con Java 21.
- Exclusión de archivos locales del contexto de construcción.
- Construcción de la imagen local del backend.
- Compose con backend, PostgreSQL, red y volumen persistente.
- Preparación de `imagen.yml` para el repositorio actual.
- Registro de evidencias, precauciones y pendientes.

No se implementan funciones nuevas de inventario, cambios de esquema, Docker
del frontend, Kubernetes, CI avanzado, Render, AWS ni una base gestionada en la
nube. Tampoco se migra la información de la base de Windows al contenedor.

# 4. Relación con la sesión 26

La actividad adapta la guía y el kit del profesor al proyecto Inventario.
ShopEasy es la referencia, no el código que se debe sustituir por el propio.

| Guía del profesor | Recorrido seguido con Inventario |
|---|---|
| Paso 0: preparar el computador | Paso 1: ubicación del backend y Docker disponible |
| Paso 1: configuración externa y Actuator | Paso 2: conservar variables y completar salud/puerto |
| Paso 2: Dockerfile y construcción | Pasos 3 y 4: preparar archivos y construir imagen |
| Paso 3: CI/CD simple | Paso 7: preparar `imagen.yml` y publicación en GHCR |
| Paso 5: Compose | Pasos 5 y 6: preparar PostgreSQL y ejecutar ambos servicios |

En este recorrido se comprobó primero el funcionamiento local con Compose y se
retomó después la automatización. El workflow no se omitió por utilizar un
monorepo: se adaptó su contexto de construcción.

Las revisiones avanzadas y Kubernetes son actividades adicionales de la guía.
La base gestionada y Render pertenecen al despliegue posterior y no se dan por
realizados en este reporte.

# 5. Estructura del repositorio

Se conserva backend y frontend en un solo repositorio. Un repositorio no implica
una única imagen ni obliga a desplegar sus componentes juntos.

```text
inventario-laboratorios/
├── .github/
│   └── workflows/
│       └── imagen.yml
├── backend/
│   └── inventario/
│       ├── Dockerfile
│       ├── .dockerignore
│       ├── docker-compose.yml
│       ├── .env                  (local; no versionar)
│       ├── .gitattributes
│       ├── build.gradle.kts
│       ├── settings.gradle.kts
│       ├── gradlew
│       ├── gradle/
│       └── src/
├── frontend/
└── docs/
    └── sprints_realizados-backend/
        └── sprint-8.md
```

Los comandos locales de Docker/Compose se ejecutan desde `backend/inventario`.
El workflow está en `.github/workflows` de la raíz, no dentro del backend.
No se reorganizaron carpetas para esta actividad.

# 6. Configuración externa

Se conserva la configuración existente de
[application.properties](../../backend/inventario/src/main/resources/application.properties):

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.enabled=true

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:1800}
```

El puerto fijo se reemplazó por:

```properties
server.port=${PORT:8080}
```

Esto permite entregar un puerto al arrancar y mantiene 8080 como valor por
defecto. No se incorporaron contraseñas reales al archivo ni se reemplazó por
la configuración MySQL de ShopEasy. Las propiedades que ocultan detalles de
errores internos se conservaron.

# 7. Actuator y seguridad de la salud

En [build.gradle.kts](../../backend/inventario/build.gradle.kts) se agregó:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

En `application.properties`:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=never
```

En [SecurityConfig.java](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java)
se permitió exclusivamente la consulta GET de salud y sus subrutas:

```java
.requestMatchers(HttpMethod.GET,
        "/actuator/health", "/actuator/health/**").permitAll()
```

Se conserva `.anyRequest().denyAll()` y las reglas de autenticación, roles y
alcance de la API. No se habilitó acceso anónimo a Equipos ni se abrió todo
`/actuator/**`. Las consultas de salud se hicieron sin encabezado Authorization.

# 8. Dockerfile del backend

El [Dockerfile](../../backend/inventario/Dockerfile) adapta el archivo del kit
para utilizar Java 21 en ambas etapas:

| Etapa | Imagen base | Responsabilidad |
|---|---|---|
| Compilación, `build` | `eclipse-temurin:21-jdk` | Resolver dependencias y generar el JAR |
| Ejecución | `eclipse-temurin:21-jre-alpine` | Ejecutar únicamente el JAR con Java |

La primera etapa copia el Wrapper y los archivos `.kts`, normaliza los finales
de línea de `gradlew`, le concede permiso de ejecución y resuelve dependencias.
Después copia `src` y ejecuta:

```text
./gradlew bootJar -x test --no-daemon
```

La segunda etapa copia el JAR como `/app/app.jar`, configura el usuario no
privilegiado `10001` y lo ejecuta mediante:

```text
java -XX:MaxRAMPercentage=75 -jar app.jar
```

`EXPOSE 8080` documenta el puerto; no lo publica por sí mismo en Windows.
PostgreSQL no se instala dentro de esta imagen. Las migraciones de `src` se
empaquetan como recursos de la aplicación, no como una segunda inicialización
independiente de la base.

# 9. Contexto de construcción y secretos

El [.dockerignore](../../backend/inventario/.dockerignore) excluye `build/`,
`.gradle/`, `target/`, `bin/`, `out/`, metadatos del repositorio/IDE, logs,
`.env`, `.env.*`, `application-local.properties`, datos locales y archivos de
despliegue que no necesita esta construcción.

**`.gradle/` y `gradle/` son distintos:** se excluye la caché local con punto,
pero se conserva la carpeta del Wrapper sin punto. También se conservan `src`
y las migraciones V1–V9.

El [`.gitattributes` existente](../../backend/inventario/.gitattributes) ya
configuraba `gradlew` con LF; no necesitó reemplazarse. Durante la preparación
se confirmó que el JAR y las propiedades del Wrapper estaban registrados en Git.

`.dockerignore` evita enviar archivos al constructor; `.gitignore` evita su
inclusión normal en Git. Ninguno elimina un secreto que ya estuviera incluido
en un archivo versionado. No se debe copiar `.env` dentro del Dockerfile.

# 10. Construcción local

Durante el paso 4 se ejecutó desde `backend/inventario`:

```powershell
docker build --progress=plain -t inventario-backend:local .
```

La primera ejecución descargó las imágenes Java, Gradle y dependencias. No fue
necesario modificar los archivos preparados para completar la construcción.

Evidencia registrada en esa ejecución:

```text
> Task :compileJava
> Task :processResources
> Task :classes
> Task :resolveMainClassName
> Task :bootJar
BUILD SUCCESSFUL in 1m 9s
```

Ese tiempo corresponde a la tarea de compilación/empaquetado mostrada por
Gradle, no al tiempo total de descarga y construcción de Docker.

Docker finalizó nombrando la imagen `inventario-backend:local`. La inspección
posterior registró ID corto `d7b458a96483`, plataforma `linux/amd64`, usuario
`10001`, directorio `/app` y entrada `java ... -jar app.jar`. El ID identifica
aquella construcción; no se garantiza que se conserve al reconstruir.

# 11. Compose y PostgreSQL independiente

El [docker-compose.yml](../../backend/inventario/docker-compose.yml) define el
proyecto `inventario-docker` y dos servicios:

| Servicio | Imagen | Configuración principal |
|---|---|---|
| `db` | `postgres:18-alpine` | Base nueva y volumen persistente |
| `backend` | `inventario-backend:local` | Imagen propia y contexto `build: .` |

La base se llama `inventario_laboratorios` y el usuario de conexión Docker es
`inventario`. Son recursos de la instancia del contenedor; no reutilizan ni
modifican automáticamente las credenciales de PostgreSQL instalado en Windows.

Se eligió PostgreSQL 18 para coincidir con la versión principal de la instalación
local revisada. El volumen se monta en `/var/lib/postgresql`, correspondiente a
la distribución de datos de la imagen PostgreSQL 18, en lugar de trasladar sin
ajustes la ruta de la plantilla PostgreSQL 16.

El servicio `db` no publica puertos al anfitrión. El backend se conecta mediante:

```text
jdbc:postgresql://db:5432/inventario_laboratorios
```

Dentro de la red de Compose, `db` identifica al servicio PostgreSQL. `localhost`
dentro del backend identificaría al propio contenedor y no sería esa base.

# 12. Puerto 8080 y alternativa 8081

Inicialmente se propuso 8081 en Windows porque el backend iniciado desde
PowerShell ocupaba 8080. Después se decidió detener la instancia local y usar
el mismo puerto que en la clase.

La configuración final es:

```yaml
ports:
  - "127.0.0.1:8080:8080"
```

El primer puerto es el de Windows; el segundo, el del contenedor. La dirección
`127.0.0.1` limita la publicación al equipo local. La API se consulta en
`http://localhost:8080`.

8081 no era un requisito de Docker ni un cambio del puerto interno de Spring.
Era una alternativa para mantener dos instancias encendidas. Para reutilizar
8080, no debe existir otro proceso ocupando ese puerto de Windows.

# 13. Variables locales y JWT

Compose obtiene dos valores del `.env` ubicado junto al archivo Compose:

| Variable local | Se entrega al contenedor como | Propósito |
|---|---|---|
| `DOCKER_DB_PASSWORD` | `POSTGRES_PASSWORD` y `DB_PASSWORD` | Autenticación entre backend y la nueva base |
| `DOCKER_JWT_SECRET` | `JWT_SECRET` | Firma y validación de tokens del entorno Docker |

El prefijo `DOCKER_` evita confundir estas variables con las utilizadas para
arrancar Java directamente desde la terminal. Se generaron siguiendo la guía
dos valores aleatorios independientes; JWT exige al menos 32 bytes aleatorios
codificados en Base64.

El Compose utiliza expresiones `${VARIABLE:?mensaje}` para detener la
configuración si falta alguno de esos valores. No se registran sus contenidos
en este documento. Al redactarlo se confirmó solamente la existencia de `.env`,
sin leerlo ni exponer sus secretos.

No se activa automáticamente el perfil `dev` ni se entrega `DEMO_USER_PASSWORD`.
Los usuarios de la base de Windows no aparecen por conectar el backend a la
base nueva. Crear usuarios de demostración requerirá una acción separada y
controlada sobre ese entorno.

# 14. Orden de arranque y persistencia

`db` tiene un healthcheck con `pg_isready`. El backend declara
`depends_on: db: condition: service_healthy`, por lo que espera a que la base
esté disponible antes de arrancar. Su propio healthcheck consulta
`http://localhost:8080/actuator/health` dentro del contenedor.

El volumen lógico `postgres_data` conserva los datos entre arranques. Con el
nombre de proyecto definido, su nombre administrado esperado es
`inventario-docker_postgres_data`, salvo una configuración externa distinta.
No se volvió a consultar Docker para inspeccionarlo durante esta redacción.

No se montó un directorio de datos de PostgreSQL de Windows, no se importó una
copia de sus datos y no se agregó el `init.sql` de ShopEasy. La creación del
esquema de Inventario sigue a cargo de Flyway. Se mantiene `ddl-auto=validate`.

**No usar `docker compose down -v` como solución genérica de errores:** puede
eliminar los datos del volumen. Cambiar la contraseña en `.env` tampoco cambia
automáticamente la contraseña de una base ya inicializada.

# 15. Arranque realizado y evidencias compartidas

El usuario realizó la validación y el arranque guiado desde `backend/inventario`:

```powershell
docker compose config --quiet
docker compose config --services
docker compose up --build -d
docker compose ps
docker compose logs --tail=80 backend
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

Estos comandos se conservan como referencia del procedimiento; no se volvieron
a ejecutar para crear este documento. Se registran expresamente como evidencia
las salidas de estado y salud que el usuario compartió:

| Contenedor | Servicio | Estado compartido | Puertos compartidos |
|---|---|---|---|
| `inventario-docker-backend-1` | `backend` | `Up ... (healthy)` | `127.0.0.1:8080->8080/tcp` |
| `inventario-docker-db-1` | `db` | `Up ... (healthy)` | `5432/tcp` |

La consulta de salud devolvió:

```text
groups                  status
{liveness, readiness}    UP
```

Esto confirma el estado comunicado en esa comprobación, no una monitorización
continua. No se incorporó una transcripción completa de los logs ni un listado
SQL de `flyway_schema_history` del contenedor.

# 16. Alcance de la validación

| Comprobación | Estado y evidencia |
|---|---|
| Puerto configurable, Actuator y regla de salud | Archivos revisados |
| Salud del backend antes de Docker | HTTP 200 y `UP` comprobados durante la preparación |
| Construcción de imagen | Proceso finalizado correctamente y metadatos inspeccionados |
| Backend y PostgreSQL en Compose | Ambos `healthy` según salida compartida por el usuario |
| Salud de la instancia Docker | `UP` según salida compartida por el usuario |
| Login y operaciones de inventario sobre la base Docker | No verificados en esta actividad |
| Suite automatizada después de los cambios | No ejecutada en esta actividad |
| Workflow disponible en el árbol local | Archivo `imagen.yml` revisado |
| Ejecución remota de Actions y paquete GHCR | Pendientes de evidencia |

`UP` no sustituye las pruebas de login, roles, CRUD, traslados ni persistencia
después de recrear un contenedor. El build usa `-x test`; no corresponde afirmar
que las 233 pruebas del cierre anterior se ejecutaron otra vez.

# 17. Automatización con imagen.yml

El archivo [imagen.yml](../../.github/workflows/imagen.yml) ya existe en la
ubicación correcta y define `Imagen Docker del backend`.

| Elemento | Configuración revisada |
|---|---|
| Activación automática | Push a `main` con cambios en backend o workflow |
| Filtros de rutas | `backend/inventario/**` y `.github/workflows/imagen.yml` |
| Activación manual | `workflow_dispatch` |
| Restricción del trabajo | `github.ref == 'refs/heads/main'` |
| Ejecutor | `ubuntu-latest` |
| Permisos | `contents: read` y `packages: write` |
| Descarga del repositorio | `actions/checkout@v7`, sin persistir credenciales |
| Autenticación | `docker/login-action@v4` con `GITHUB_TOKEN` |
| Construcción/publicación | `docker/build-push-action@v7` |

La adaptación esencial al monorepo es:

```yaml
context: ./backend/inventario
file: ./backend/inventario/Dockerfile
```

El workflow no se necesita por el solo hecho de tener subcarpetas: automatiza
la publicación. La subcarpeta exige ajustar las rutas. No requiere mover el
Dockerfile a la raíz ni separar el frontend en otro repositorio.

# 18. Nombre de imagen, etiquetas y autenticación

El workflow forma el nombre `ghcr.io/${GITHUB_REPOSITORY,,}-backend`, usando
minúsculas. Para `markopuch/inventario-laboratorios`, el resultado previsto es:

```text
ghcr.io/markopuch/inventario-laboratorios-backend:latest
ghcr.io/markopuch/inventario-laboratorios-backend:sha-<7 caracteres del commit>
```

`latest` apunta a la publicación más reciente realizada por este flujo. La
etiqueta `sha-...` permite relacionarla con el commit usado para construirla.
El sufijo `-backend` reserva un nombre distinto del futuro frontend.

La imagen local sigue siendo `inventario-backend:local`; el Compose actual no
se cambió para descargar desde GHCR. Se añade la etiqueta OCI
`org.opencontainers.image.source` para identificar el repositorio asociado.

`GITHUB_TOKEN` es la credencial del workflow. No debe sustituirse por la
contraseña de la cuenta ni por las variables locales de PostgreSQL/JWT.
Esta construcción no necesita arrancar la base ni subir `.env`.

# 19. Pendientes de publicación en GitHub

La preparación del workflow está documentada, pero no hay evidencia aportada
en esta actividad de su ejecución remota. Para cerrar esa parte falta:

1. Confirmar la rama principal y que los archivos necesarios estén en GitHub.
2. Revisar lo preparado para commit, sin incorporar `.env` ni cambios ajenos.
3. Incorporar el workflow y los cambios del backend a `main` mediante el flujo
   de trabajo del repositorio; si se utiliza otra rama, revisar el pull request.
4. Verificar en Actions una ejecución verde de `Imagen Docker del backend`.
5. Confirmar el paquete `inventario-laboratorios-backend` y ambas etiquetas.
6. Registrar el commit, enlace de ejecución y nombre/digest de imagen publicado.
7. Decidir explícitamente si el paquete debe hacerse público para la clase.

Una publicación nueva en GHCR es privada por defecto. Hacerla pública permite
descargar el programa compilado; debe revisarse antes que sea apropiado
distribuirlo y que no contenga secretos. No se cambió ninguna visibilidad.

El botón manual depende de que el workflow esté en la rama predeterminada.
La guardia actual solo permite publicar desde `main`. Publicar una imagen no
despliega la API ni modifica los contenedores locales ya ejecutados.

# 20. Archivos incorporados durante la actividad

Rutas relativas a la raíz del repositorio:

| Ruta | Propósito y estado |
|---|---|
| `backend/inventario/Dockerfile` | Construcción en dos etapas y ejecución Java 21 |
| `backend/inventario/.dockerignore` | Excluir cachés, secretos locales y archivos no necesarios |
| `backend/inventario/docker-compose.yml` | Backend, PostgreSQL, salud, red y volumen |
| `.github/workflows/imagen.yml` | Automatización preparada para construir/publicar en GHCR |
| `docs/sprints_realizados-backend/sprint-8.md` | Este reporte del sprint |

También existe `backend/inventario/.env` como archivo local no destinado al
repositorio. Se menciona su función, no sus valores. Los archivos Docker y el
workflow ya existían al comenzar la redacción de este reporte.

# 21. Archivos modificados y elementos conservados

| Archivo modificado durante la actividad | Cambio |
|---|---|
| `backend/inventario/build.gradle.kts` | Incorporación de Actuator |
| `backend/inventario/src/main/resources/application.properties` | Puerto variable y configuración de salud |
| `backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java` | Permitir GET de salud, conservando la protección del resto |

Se conservaron la arquitectura de negocio, controladores, DTOs, entidades,
servicios, repositorios y migraciones del backend. No se introdujo V10.
`.gitattributes` y el Wrapper existentes se reutilizaron.

**Esta tarea de documentación crea únicamente `sprint-8.md`.** No modifica
README, código, configuración, workflows, otros sprints ni registros de Git.
Las eliminaciones de `.gitkeep` descritas en el Sprint 7 no se atribuyen a este.

# 22. Incidencias y decisiones

| Situación | Tratamiento durante la actividad |
|---|---|
| Docker instalado pero motor inaccesible | Comprobar Docker Desktop; posteriormente el motor respondió |
| Duda sobre cerrar PowerShell | Se comprobó que Docker seguía respondiendo después de cerrarlo |
| Consulta de salud sin backend en 8080 | Distinguir aplicación detenida de error de configuración; después respondió HTTP 200 |
| Duda entre 8080 y 8081 | Detener backend local y adoptar `127.0.0.1:8080:8080` |
| ShopEasy usa otro stack de referencia | Adaptar Java 21, PostgreSQL y Flyway; no copiar su configuración completa |
| Dockerfile dentro de una subcarpeta | Ajustar contexto y ruta en el workflow, sin reorganizar el repositorio |
| Futuro Docker del frontend | Reservar otra imagen/workflow y mantener los componentes separados |

Después de la construcción realizada en el paso 4, el usuario pidió ejecutar
personalmente los comandos siguientes. El arranque de Compose se realizó de
esa manera y sus resultados se compartieron en la conversación.

# 23. Checklist Sprint 8

- [x] Identificar la carpeta real del backend y conservar la estructura del repositorio.
- [x] Comprobar Docker Desktop disponible durante la actividad.
- [x] Conservar conexión y JWT mediante variables externas.
- [x] Hacer configurable el puerto con `PORT`.
- [x] Agregar Actuator y autorizar únicamente la consulta de salud necesaria.
- [x] Preparar Dockerfile con Java 21 y usuario no privilegiado.
- [x] Preparar `.dockerignore` sin excluir Wrapper ni migraciones.
- [x] Construir e inspeccionar `inventario-backend:local`.
- [x] Preparar PostgreSQL separado con volumen persistente.
- [x] Establecer conexión interna mediante `db:5432`.
- [x] Comprobar ambos servicios `healthy`, según evidencia compartida.
- [x] Comprobar `status: UP` en la instancia Docker, según evidencia compartida.
- [x] Preparar `imagen.yml` en la raíz y adaptar el contexto del backend.
- [x] Separar el nombre de imagen del futuro frontend.
- [ ] Confirmar una ejecución verde del workflow en GitHub Actions.
- [ ] Confirmar paquete GHCR, etiquetas y digest publicados.
- [ ] Resolver y documentar la visibilidad del paquete según la entrega.
- [ ] Verificar login y operaciones de inventario sobre la nueva base Docker.
- [ ] Reejecutar y registrar las pruebas automatizadas después de estos cambios.

# 24. Estado final y próximos pasos

**Empaquetado y ejecución local completados; automatización implementada en
archivo local, publicación remota pendiente de evidencia.** El Sprint 8 no se
declara cerrado en su alcance completo de Docker más GHCR.

El siguiente paso inmediato es confirmar Actions y el paquete publicado. Luego
se podrá preparar, si corresponde, un Compose que descargue esa imagen en vez
de construirla localmente y verificarla con PostgreSQL.

El frontend conservará su propia imagen y workflow. Una futura integración de
los tres servicios deberá cuidar puertos, URL de la API, secretos y reutilización
del volumen; no exige separar repositorios ni rehacer el Dockerfile del backend.

La publicación en Render u otra plataforma, las revisiones avanzadas y
Kubernetes quedan fuera de lo comprobado aquí. El cierre funcional del Sprint 7
se conserva como antecedente histórico, no como una prohibición de iniciar este
nuevo trabajo de empaquetado y despliegue.
