# 1. Resumen Sprint 8

**Docker local completo y publicación del backend/frontend en GHCR verificados.**
Este sprint continúa el cierre funcional del [Sprint 7](sprint-7.md). El alcance
inicial fue backend + PostgreSQL; la actualización del 2026-10-03 registra además
la integración del frontend de Jason con Nginx y la publicación de ambas imágenes.

Se externalizó el puerto, se incorporó Actuator, se permitió consultar la salud
sin autenticación, se construyó `inventario-backend:local` y se preparó Compose.
El usuario ejecutó el entorno y compartió ambos servicios en estado `healthy`
y la respuesta `status: UP` de `/actuator/health`.

El workflow `.github/workflows/imagen.yml` utiliza una matriz para publicar
backend y frontend Jason con nombres de imagen separados. La
[ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900)
terminó con ambos trabajos en verde para el commit `9956939`. Se verificaron
directamente las etiquetas `latest` y `sha-9956939` y sus digests en GHCR.

**Fecha inicial de documentación:** 2026-10-01. **Evidencia actualizada:** 2026-10-03.
El recorrido inicial conserva sus resultados históricos. Las verificaciones
posteriores de Docker, Actions y GHCR se distinguen de aquella preparación en la
[evidencia consolidada](../despliegue/verificacion-docker-actions-2026-10-03.md).
Esta actualización documental no ejecuta nuevamente Docker ni las suites.

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

# 3. Objetivo y alcance inicial

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

La actividad inicial no implementó funciones nuevas de inventario, cambios de
esquema ni Docker del frontend. Este último se incorporó posteriormente y ya
forma parte del entorno verificado. Kubernetes, CI avanzado, Render, AWS y una
base gestionada siguen fuera de lo comprobado. No se migró la información de
la base de Windows al contenedor.

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
│   └── version-jason/
│       └── frontend/
│           ├── Dockerfile
│           ├── .dockerignore
│           └── nginx.conf
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
proyecto `inventario-docker`. Inicialmente tenía dos servicios; actualmente
integra los tres componentes:

| Servicio | Imagen | Configuración principal |
|---|---|---|
| `db` | `postgres:18-alpine` | Base nueva y volumen persistente |
| `backend` | `inventario-backend:local` | Imagen propia y contexto `build: .` |
| `frontend` | `inventario-frontend-jason:local` | React/Vite servido por Nginx; contexto `../../frontend/version-jason/frontend` |

El frontend se publica en `http://localhost:3000` y usa `/api` mediante el proxy
Nginx hacia `backend:8080`. La ruta alternativa de Nginx permite recargar las
rutas del cliente. El backend conserva `http://localhost:8080`; PostgreSQL no
publica un puerto al anfitrión. Los tres servicios se observaron saludables.

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

El Compose habitual no activa automáticamente el perfil `dev` ni entrega
`DEMO_USER_PASSWORD`. Los usuarios de Windows no aparecen por conectar el
backend a otra base. La demostración completa utilizó fixtures y credenciales
externas exclusivamente en una base temporal; no creó cuentas ni datos de
inventario en la base habitual.

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

# 15. Arranque inicial y evidencia posterior

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

La comprobación posterior del 2026-10-03 incluyó los tres servicios saludables,
recorrido en navegador y 33 comprobaciones HTTP adicionales. Se usaron las mismas
imágenes locales en un entorno aislado, con backend en 18081, frontend en 3001 y
base `inventario_verificacion_docker_26e35749`. La base temporal y sus recursos
Docker se eliminaron al terminar; el entorno habitual permaneció intacto. Véase
la [evidencia completa](../despliegue/verificacion-docker-actions-2026-10-03.md).

# 16. Alcance de la validación

| Comprobación | Estado y evidencia |
|---|---|
| Puerto configurable, Actuator y regla de salud | Archivos revisados |
| Salud del backend antes de Docker | HTTP 200 y `UP` comprobados durante la preparación |
| Construcción de imagen | Proceso finalizado correctamente y metadatos inspeccionados |
| Backend, PostgreSQL y frontend en Compose | Tres servicios saludables; lectura y flujo completo verificados el 2026-10-03 |
| Salud de la instancia Docker | Backend `UP`; frontend servido por Nginx y proxy `/api` funcional |
| Login y operaciones de inventario sobre Docker | ADMIN/GESTOR/LECTOR, creación, consulta, edición, traslado, historial, baja, filtros y alcance aprobados |
| Comprobaciones complementarias | 33/33 HTTP y 24/24 aserciones de verificación; no son pruebas JUnit |
| Suite Gradle de 233 pruebas | Antecedente de Sprint 7; no reejecutada en esta verificación ni en el build de Actions |
| Pruebas frontend en Actions | 27/27 aprobadas en el trabajo del frontend Jason |
| Workflow disponible en el árbol local | `imagen.yml`, matriz backend/frontend y contextos separados |
| Ejecución remota de Actions y paquetes GHCR | Run 37136137900: ambos trabajos verdes; etiquetas y digests comprobados |
| Flyway e integridad temporal | V1–V9 exitosas; cinco controles SQL con cero incidencias |
| Base habitual y limpieza | Nueve conteos iguales antes/después; base, contenedores, red y volumen temporales eliminados |

`UP` no sustituye las pruebas de login, roles, CRUD, traslados ni persistencia
después de recrear un contenedor. El build usa `-x test`; no corresponde afirmar
que las 233 pruebas del cierre anterior se ejecutaron otra vez.

# 17. Automatización con imagen.yml

El archivo [imagen.yml](../../.github/workflows/imagen.yml) está en la ubicación
correcta y define `Imagenes Docker del backend y frontend Jason`. Una matriz
genera los trabajos `Publicar backend` y `Publicar frontend-jason`.

| Elemento | Configuración revisada |
|---|---|
| Activación automática | Push a `main` con cambios en backend, frontend Jason o workflow |
| Filtros de rutas | `backend/inventario/**`, `frontend/version-jason/frontend/**` y `.github/workflows/imagen.yml` |
| Activación manual | `workflow_dispatch` |
| Restricción del trabajo | `github.ref == 'refs/heads/main'` |
| Ejecutor | `ubuntu-latest` |
| Permisos | `contents: read` y `packages: write` |
| Descarga del repositorio | `actions/checkout@v7`, sin persistir credenciales |
| Autenticación | `docker/login-action@v4` con `GITHUB_TOKEN` |
| Pruebas de frontend | Node 22 con `actions/setup-node@v7`, `npm ci` y `npm test`; 27 aprobadas en la ejecución confirmada |
| Preparación de construcción | `docker/setup-buildx-action@v4` |
| Construcción/publicación | `docker/build-push-action@v7` |

La adaptación esencial al monorepo mantiene estos contextos separados:

```yaml
backend:
  contexto: ./backend/inventario
  dockerfile: ./backend/inventario/Dockerfile
frontend-jason:
  contexto: ./frontend/version-jason/frontend
  dockerfile: ./frontend/version-jason/frontend/Dockerfile
```

El workflow no se necesita por el solo hecho de tener subcarpetas: automatiza
la publicación. La subcarpeta exige ajustar las rutas. No requiere mover el
Dockerfile a la raíz ni separar el frontend en otro repositorio.

# 18. Nombre de imagen, etiquetas y autenticación

El workflow forma el nombre `ghcr.io/${GITHUB_REPOSITORY,,}-${COMPONENTE}`, usando
minúsculas. Para la ejecución verificada del commit `9956939`, se publicaron:

```text
ghcr.io/markopuch/inventario-laboratorios-backend:latest
ghcr.io/markopuch/inventario-laboratorios-backend:sha-9956939
ghcr.io/markopuch/inventario-laboratorios-frontend-jason:latest
ghcr.io/markopuch/inventario-laboratorios-frontend-jason:sha-9956939
```

`latest` apunta a la publicación más reciente realizada por este flujo. La
etiqueta `sha-...` permite relacionarla con el commit usado para construirla.
Los sufijos `-backend` y `-frontend-jason` identifican los componentes separados.
Los digests consultados coinciden con las salidas de Actions y están registrados
en la [evidencia consolidada](../despliegue/verificacion-docker-actions-2026-10-03.md).

La imagen local sigue siendo `inventario-backend:local`; el Compose actual no
se cambió para descargar desde GHCR. Se añade la etiqueta OCI
`org.opencontainers.image.source` para identificar el repositorio asociado.

`GITHUB_TOKEN` es la credencial del workflow. No debe sustituirse por la
contraseña de la cuenta ni por las variables locales de PostgreSQL/JWT.
Esta construcción no necesita arrancar la base ni subir `.env`.

# 19. Publicación verificada en GitHub

La publicación preparada inicialmente ya se ejecutó en `main` para el commit
`99569392035fc975171d2df6929a1aa26111b139`. La
[ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900)
terminó correctamente:

| Trabajo | ID | Resultado |
|---|---|---|
| Publicar backend | `111240944686` | Compilación y publicación exitosas; Dockerfile usa `bootJar -x test` |
| Publicar frontend-jason | `111240944823` | 27 pruebas aprobadas, build y publicación exitosos |

Ambos paquetes existen con `latest` y `sha-9956939`; la inspección del registro
confirmó sus digests. La política de visibilidad pública o privada no se cambió
ni se atribuye como parte de esta evidencia. Si la entrega requiere descargas
sin autenticación, esa decisión se revisará por separado.

El botón manual depende de que el workflow esté en la rama predeterminada.
La guardia actual solo permite publicar desde `main`. Publicar una imagen no
despliega la API ni modifica los contenedores locales ya ejecutados.

# 20. Archivos incorporados durante la actividad

Rutas relativas a la raíz del repositorio:

| Ruta | Propósito y estado |
|---|---|
| `backend/inventario/Dockerfile` | Construcción en dos etapas y ejecución Java 21 |
| `backend/inventario/.dockerignore` | Excluir cachés, secretos locales y archivos no necesarios |
| `backend/inventario/docker-compose.yml` | Backend, PostgreSQL y posteriormente frontend; salud, red y volumen |
| `.github/workflows/imagen.yml` | Automatización ejecutada: matriz backend/frontend con publicación confirmada en GHCR |
| `frontend/version-jason/frontend/Dockerfile` | Build React/Vite y ejecución del frontend con Nginx |
| `frontend/version-jason/frontend/.dockerignore` | Exclusiones del contexto frontend |
| `frontend/version-jason/frontend/nginx.conf` | Proxy `/api`, rutas SPA y salud del frontend |
| `docs/despliegue/verificacion-docker-actions-2026-10-03.md` | Evidencia consolidada del flujo, limpieza, Actions e imágenes |
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

La redacción inicial creó únicamente `sprint-8.md`. La actualización posterior
actualiza los Markdown que presentan el estado vigente y conserva los cierres
anteriores como historia. No modifica código, configuración, workflows ni
registros de Git. Las eliminaciones de `.gitkeep` del Sprint 7 no se atribuyen aquí.

# 22. Incidencias y decisiones

| Situación | Tratamiento durante la actividad |
|---|---|
| Docker instalado pero motor inaccesible | Comprobar Docker Desktop; posteriormente el motor respondió |
| Duda sobre cerrar PowerShell | Se comprobó que Docker seguía respondiendo después de cerrarlo |
| Consulta de salud sin backend en 8080 | Distinguir aplicación detenida de error de configuración; después respondió HTTP 200 |
| Duda entre 8080 y 8081 | Detener backend local y adoptar `127.0.0.1:8080:8080` |
| ShopEasy usa otro stack de referencia | Adaptar Java 21, PostgreSQL y Flyway; no copiar su configuración completa |
| Dockerfile dentro de una subcarpeta | Ajustar contexto y ruta en el workflow, sin reorganizar el repositorio |
| Docker del frontend, inicialmente futuro | Incorporado con Nginx, imagen separada y trabajo propio dentro del mismo workflow |

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
- [x] Separar los nombres de imagen de backend y frontend Jason.
- [x] Integrar frontend, backend y PostgreSQL en Compose y comprobar sus estados saludables.
- [x] Verificar login, roles, alcance y flujo completo de inventario en Docker aislado.
- [x] Registrar 33/33 comprobaciones HTTP y 24/24 aserciones, sin sumarlas a JUnit.
- [x] Confirmar una ejecución verde de ambos trabajos en GitHub Actions.
- [x] Confirmar ambos paquetes GHCR, etiquetas y digests publicados.
- [x] Confirmar 27/27 pruebas del frontend en Actions.
- [x] Preservar la base habitual y eliminar todos los recursos temporales de verificación.

La regresión Gradle de 233 pruebas no se reejecutó en esta validación; permanece
como antecedente aprobado de Sprint 7. La visibilidad del paquete y una posible
regresión adicional son decisiones separadas, no resultados atribuidos al build.

# 24. Estado final y próximos pasos

**Alcance Docker local + publicación GHCR cerrado y documentado.** El entorno
completo funciona con los tres servicios, el flujo de inventario fue validado
en una base temporal y ambas imágenes se publicaron mediante Actions. El
frontend tiene su propio trabajo e imagen dentro del workflow compartido.

Los pasos de validación Docker y publicación ya tienen evidencia. El siguiente
trabajo de despliegue será preparar la base gestionada y el backend en la nube,
si se autoriza, siguiendo la sesión 28. Descargar las imágenes de GHCR en Compose
es una alternativa operativa: el Compose vigente sigue construyendo localmente.

La publicación en Render u otra plataforma, las revisiones avanzadas y
Kubernetes quedan fuera de lo comprobado aquí. El cierre funcional del Sprint 7
se conserva como antecedente histórico, no como una prohibición de iniciar este
nuevo trabajo de empaquetado y despliegue.
