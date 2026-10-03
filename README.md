# Inventario de Laboratorios

Frontend de trabajo actual: [versión Jason — Docker, ejecución local y pruebas manuales](frontend/version-jason/frontend/README.md), conectado al backend Docker del [Sprint 8](docs/sprints_realizados-backend/sprint-8.md). El Compose de `backend/inventario` levanta PostgreSQL, backend y Jason (puerto 3000); `imagen.yml` publica backend y frontend Jason por separado. La [auditoría de integración](docs/sprints_realizados-frontend-Jason/auditoria-integracion.md) registra el estado comprobado; la versión Marko se conserva por separado.

**Estado verificado al 3 de octubre de 2026:** flujo completo en Docker local comprobado,
ambos trabajos de GitHub Actions en verde y ambas imágenes publicadas en GHCR para
el commit `e1ce75a`. Las imágenes publicadas se descargaron y ejecutaron localmente:
PostgreSQL, backend y frontend quedaron saludables. El [reporte de Docker y Actions](docs/despliegue/verificacion-docker-actions-2026-10-03.md)
incluye resultados, manifiestos, capturas y preservación de la base habitual.
El despliegue de PostgreSQL/backend/frontend en la nube queda pendiente.


## 1. Descripción

API REST para organizar laboratorios, clasificar equipos, controlar su ubicación
y registrar traslados con historial, administrar usuarios, programar mantenimientos
y consultar reportes. El cierre histórico de **Sprint 7** fue V9/233 pruebas;
**Sprint 8** incorporó Docker y GHCR. La ampliación posterior del backend para
integrar Jason está implementada y desplegada con **V1–V13**.
El proyecto conserva el estilo de JPA/JWT de los ejemplos del profesor y su
organización actual de carpetas.

El alcance incluye autenticación, cinco catálogos, asignaciones de laboratorios,
Equipos, Movimientos, administración de usuarios, Mantenimientos, Reportes y
Auditoría administrativa. La [ampliación del backend](backend/inventario/ACTUALIZACION-BACKEND.md)
describe los contratos añadidos. El [backlog vigente](docs/backend-final/backlog.md)
separa las capacidades implementadas de las ampliaciones futuras.
La [evidencia Docker actual](frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json)
registra la preservación de la base habitual y las pruebas sobre una base temporal.

## 2. Stack

| Componente | Versión/configuración del proyecto |
|---|---|
| Java | JDK 21 |
| Spring Boot | 4.1.1 |
| Gradle Wrapper | 9.7.1, Kotlin DSL |
| Persistencia | Spring Data JPA / Hibernate y PostgreSQL |
| Migraciones | Flyway, V1–V13 |
| Seguridad | Spring Security, BCrypt, JJWT 0.13.0 |
| Mapeo | MapStruct 1.6.3, Lombok, lombok-mapstruct-binding 0.2.0 |
| Validación y pruebas | Jakarta Validation, JUnit 5, Spring Boot Test |
| Frontend activo | React 18.3.1, Vite 6.4.3, React Router y Axios; versión Jason |
| Docker local | PostgreSQL 18 Alpine, backend Java 21 y frontend servido por Nginx |
| Automatización | GitHub Actions: matriz backend/frontend Jason; imágenes en GHCR |

Se usa el wrapper incluido; no hace falta instalar Gradle por separado.
Las versiones se mantienen respecto al código existente. PostgreSQL debe estar
iniciado y la base de destino debe existir antes de arrancar.

## 3. Arquitectura

```text
JWT / Spring Security
        ↓
Controller → Request DTO → Mapper / Domain
                               ↓
                            Service
                               ↓
                     Repository → Entity → PostgreSQL
                               ↓
                    Domain → Mapper → Response DTO
```

Controller coordina HTTP; Service valida negocio, alcance y transacciones;
Repository consulta PostgreSQL; Entity representa persistencia. Mapper convierte
datos sin consultar la base. Se usan DTOs públicos, relaciones JPA sin colecciones
bidireccionales y `@Transactional` en servicios.

Los paquetes de `src/main/java/com/utec/inventario/` son `config`, `controller`,
`domain`, `dto/request`, `dto/response`, `entity`, `exception`, `mapper`,
`repository`, `security` y `service`; todos contienen implementación.
Consulta [arquitectura](docs/backend-final/arquitectura-backend.md) y los
[flujos principales](docs/backend-final/flujos-principales.md).

## 4. Modelo

**12 entidades y 16 relaciones FK** en el esquema actual V13.
`flyway_schema_history` es infraestructura y no integra ese conteo.
Sprint 7 cerró históricamente con 10 entidades y 13 FK; V12 y V13 añadieron
Mantenimiento y Auditoría.

| Área del modelo | Entidades |
|---|---|
| Organización | Sede → Área → Laboratorio |
| Clasificación | Categoría → Subcategoría |
| Identidad y autorización | Rol, Usuario, UsuarioLaboratorio |
| Inventario | Equipo |
| Trazabilidad | MovimientoEquipo, Auditoría |
| Mantenimiento | Mantenimiento |

UsuarioLaboratorio resuelve Usuario N:M Laboratorio con PK compuesta. Equipo
tiene Laboratorio y Subcategoría obligatorios y responsable opcional. Movimiento
tiene Equipo, destino y actor obligatorios; origen permite null para datos legacy.
Los traslados nuevos siempre toman el origen real del Equipo.

Usuario dispone de operaciones administrativas protegidas para ADMIN; Rol se
usa para autorización y selección de roles, sin un CRUD de roles independiente.
Mantenimiento referencia Equipo y responsable opcional. Auditoría referencia
al actor opcional y conserva su username; entidad/idEntidad no forman una FK.
Consulta el [modelo vigente V13](docs/Erd_actual/modelo-vigente-v13.md).
El [ERD lógico v2](docs/Erd_actual/erd-logico-v2.md), el
[ERD físico v2](docs/Erd_actual/erd-fisico-v2.md) y el
[cotejo de Sprint 7](docs/backend-final/auditoria-entity-flyway.md) conservan
el modelo histórico V1–V9 con su ampliación documentada por separado.

## 5. Roles

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| Leer catálogos | Global | Global | Global |
| Escribir catálogos/asignaciones | Sí | No | No |
| Leer Equipos | Global | Alcance vigente | Alcance vigente |
| Crear, editar o dar de baja Equipos | Global | Alcance vigente | No |
| Trasladar | Global | Origen y destino autorizados | No |
| Consultar historia | Global | Origen o destino autorizado | Origen o destino autorizado |
| Administrar usuarios y consultar auditoría | Sí | No | No |
| Leer Mantenimientos y Reportes | Global | Alcance vigente | Alcance vigente |
| Crear, editar o cambiar estado de Mantenimiento | Global | Alcance vigente | No |

Se comprueba el usuario y rol activos con el estado vigente de PostgreSQL.
La [matriz de permisos](docs/matriz-permisos.md) desarrolla cada operación.
El responsable de Equipo identifica custodia: **no concede permisos**.

## 6. Alcance

`GET /api/auth/me/laboratorios` obtiene el principal autenticado. ADMIN recibe
`alcanceGlobal=true` y laboratorios activos; GESTOR/LECTOR reciben solo asignaciones
activas a laboratorios activos. Cambiar asignaciones no requiere renovar un JWT
válido. El GET administrativo de asignaciones muestra configuración explícita,
también para ADMIN; no equivale a su alcance global.

ADMIN conserva lectura histórica global de Equipos BAJA y laboratorios inactivos.
GESTOR/LECTOR consultan Equipos por su laboratorio actual e historia por origen
**o** destino. Pueden ver movimientos autorizados de un Equipo actualmente fuera
de su alcance, sin obtener acceso a su detalle. Los filtros se aplican en SQL.
Los catálogos permanecen globales.

## 7. Funcionalidades

- Login JWT, perfil propio y consulta del alcance efectivo.
- CRUD de Categoría, Subcategoría, Sede, Área y Laboratorio con baja lógica,
  unicidad, jerarquías y validación de padres.
- Reemplazo atómico de asignaciones; retiro lógico y reactivación del mismo par.
- CRUD de Equipo, cuatro filtros combinables, responsables opcionales y estado BAJA.
- Traslado atómico de Equipo con actor/origen/tipo/fecha controlados por servidor.
- Historial inmutable desde la API, filtros por extremos y visibilidad actual.
- Catálogos administrativos con filtro de activos/inactivos y reactivación.
- Estado operativo de Laboratorio independiente de su baja lógica.
- Administración de usuarios: alta, datos públicos, rol, estado, contraseña y
  asignaciones, con protección del último ADMIN activo.
- Mantenimiento preventivo/correctivo/calibración/otro con ciclo de estados y
  protección del Equipo durante EN_PROCESO.
- Cinco reportes agregados calculados por el servidor según filtros y alcance.
- Auditoría administrativa de cambios, consultable por ADMIN desde la API.
- Errores seguros, DTOs públicos y bloqueos para proteger operaciones concurrentes.

PUT de Equipo rechaza código interno, laboratorio y otros campos inmutables.
BAJA no se edita ni traslada. El traslado exige destino activo/diferente y motivo
no blanco de máximo 500 caracteres; ubicación omitida/null/blanca se limpia.
Los movimientos históricos no bloquean por sí solos la baja lógica de Laboratorio.
Un Mantenimiento EN_PROCESO bloquea edición, baja y traslado del Equipo.
Completarlo o cancelarlo restaura el estado previo del Equipo; no presupone que
un Equipo INOPERATIVO se haya reparado.
Consulta [RN-01–45 y su evidencia](docs/reglas-negocio.md).

## 8. Endpoints

Se cuentan **70 combinaciones método+ruta** de aplicación; los filtros no agregan endpoints.
Actuator health se documenta aparte y no se incluye en ese total.
Las consultas jerárquicas se agrupan con el padre de la ruta.

| Grupo | Cantidad |
|---|---:|
| AUTH | 3 |
| CATEGORIA | 7 |
| SUBCATEGORIA | 6 |
| SEDE | 7 |
| AREA | 7 |
| LABORATORIO | 6 |
| CATALOGOS ADMIN | 5 |
| USUARIO ADMIN | 7 |
| USUARIO_LABORATORIO | 2 |
| EQUIPO | 6 |
| MOVIMIENTO | 3 |
| MANTENIMIENTO | 5 |
| REPORTES | 5 |
| AUDITORIA | 1 |

El [catálogo de endpoints](docs/backend-final/endpoints.md) incluye permisos,
parámetros y respuestas. Base local: `http://localhost:8080`. Login público:
`POST /api/auth/login`, con `userName` y `password`; las otras rutas requieren
`Authorization: Bearer <token>`.

Alta CRUD devuelve 201; traslado devuelve 200 con Equipo actualizado y Movimiento;
baja lógica devuelve 204. Consulta [códigos HTTP](docs/backend-final/codigos-http.md)
para las diferencias reales, incluida la validación histórica de IDs de Categoría.

## 9. PostgreSQL y Flyway

Flyway administra el esquema; Hibernate usa `ddl-auto=validate`.
`open-in-view=false` y `spring.flyway.enabled=true` permanecen configurados.

| Migraciones | Contenido |
|---|---|
| V1–V3 | Organización, clasificación, identidad, asignaciones, Equipos y Movimientos |
| V4 | Roles y catálogos iniciales; no crea usuarios ni Equipos |
| V5 | Categoría sin duplicados de nombre por mayúsculas |
| V6 | Username obligatorio y único sin distinguir mayúsculas |
| V7 | Nombre de Subcategoría único dentro de Categoría |
| V8 | Nombre de Área único dentro de Sede |
| V9 | Código de Laboratorio único globalmente sin distinguir mayúsculas |
| V10 | Estado operativo de Laboratorio, independiente de activo |
| V11 | Unicidad de email sin distinguir mayúsculas; revisión previa de duplicados |
| V12 | Mantenimiento, ciclo de estados y UNIQUE parcial por Equipo EN_PROCESO |
| V13 | Auditoría administrativa de cambios |

La actualización Docker aplicó las migraciones existentes V10–V13; no creó ni
modificó SQL. Las trece migraciones tienen `success=true` y los checksums V1–V9
permanecen iguales. Las FK de inventario/mantenimiento usan RESTRICT; la FK
de actor de Auditoría usa `ON DELETE SET NULL` para conservar el evento.
Las restricciones de baja lógica pertenecen a Services.
Los instantes son TIMESTAMPTZ; `fecha_programada` de Mantenimiento es DATE.
La fecha de Movimiento viene del default PostgreSQL;
`fechaActualizacion` de Equipo se actualiza en Java UTC, sin trigger.

La base habitual es `inventario_laboratorios`. Si preparas una instalación nueva,
crea una base vacía con tu usuario local antes del arranque, por ejemplo desde
pgAdmin conectado a `postgres`:

```sql
CREATE DATABASE inventario_laboratorios;
```

Ejecuta esa sentencia solo si no existe. No uses `repair`, `baseline` ni SQL
manual para ocultar diferencias de un esquema ya existente. El arranque aplica
V1–V13 en una base nueva o valida y completa el historial de una ya preparada.
Antes de actualizar una base existente, conserva un respaldo y revisa el
estado de Flyway. En el despliegue verificado se conservaron el mismo contenedor
y volumen de PostgreSQL, los registros anteriores y las cuatro asignaciones.

## 10. Configuración

<a id="variables-de-entorno-y-ejecución-en-powershell"></a>

| Variable | Uso |
|---|---|
| DB_URL | JDBC PostgreSQL de la base elegida |
| DB_USER / DB_PASSWORD | Credenciales locales de PostgreSQL |
| JWT_SECRET | Clave Base64 de al menos 32 bytes |
| JWT_EXPIRATION_SECONDS | Opcional; 1800 segundos por defecto |
| SPRING_PROFILES_ACTIVE | `dev` solo cuando se necesitan cuentas demo |
| DEMO_USER_PASSWORD | Secreto inicial externo del perfil dev y de pruebas |

En PowerShell, antes de ejecutar Java directamente:

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
```

Las variables pertenecen a esa terminal. En Java/IDE, `.env` no se carga automáticamente.
Docker Compose sí usa el `.env` local de `backend/inventario` para
`DOCKER_DB_PASSWORD` y `DOCKER_JWT_SECRET`. Configura las variables también en
Run/Debug si arrancas desde el IDE. No publiques secretos
ni tokens. Cambiar JWT_SECRET invalida tokens anteriores.

El perfil dev crea marko/ADMIN, aldo/GESTOR y romel/LECTOR con una contraseña
externa; no restablece cuentas existentes ni crea asignaciones, Equipos o
Movimientos demo. Las cuentas existentes funcionan sin dev.

## 11. Ejecución

<a id="arranque-rápido-en-windows"></a>

### 11.1. Docker — modo integrado actual

Con Docker Desktop iniciado y la configuración privada del backend preparada:

```powershell
Set-Location backend/inventario
docker compose config --quiet
docker compose up -d db backend frontend
docker compose ps
Invoke-RestMethod http://localhost:3000/health
Invoke-RestMethod http://localhost:8080/actuator/health | Select-Object status
```

Abre `http://localhost:3000`. Nginx sirve React y reenvía `/api` al backend dentro
de Docker. PostgreSQL conserva su volumen y no publica un puerto al equipo.
El puerto 5173 corresponde a la alternativa de desarrollo con Vite. Las instrucciones
de construcción y ejecución están en el [README de Jason](frontend/version-jason/frontend/README.md).
Los secretos permanecen fuera de Git; iniciar Docker no cambia las contraseñas
de cuentas existentes. Conserva el volumen habitual al detener los servicios.

Para actualizar únicamente backend/frontend a las imágenes ya publicadas de
esta entrega, desde `backend/inventario`, con la base existente respaldada:

```powershell
docker pull ghcr.io/markopuch/inventario-laboratorios-backend:sha-e1ce75a
docker pull ghcr.io/markopuch/inventario-laboratorios-frontend-jason:sha-e1ce75a
docker tag ghcr.io/markopuch/inventario-laboratorios-backend:sha-e1ce75a inventario-backend:local
docker tag ghcr.io/markopuch/inventario-laboratorios-frontend-jason:sha-e1ce75a inventario-frontend-jason:local
docker compose up -d --no-build --no-deps backend frontend
docker compose ps
```

Las etiquetas SHA identifican la versión validada; `latest` puede cambiar.
Este comando conserva PostgreSQL y deja que Flyway valide/aplique las migraciones
incluidas en esa versión. No uses `down -v` para actualizar la instalación habitual.

### 11.2. Java, PowerShell o IDE — alternativa local

Desde la raíz, con PostgreSQL iniciado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

El script solicita la conexión que falte y prepara JWT_SECRET para la sesión.
Para inicializar las tres cuentas en una base nueva, agrega `-CrearUsuariosDemo`;
entonces solicita la contraseña demo. Espera `Started InventarioApplication`,
mantén la terminal abierta y usa Ctrl+C para detenerla.

También puedes ejecutar `InventarioApplication.java` con Run: requiere las mismas
variables en el IDE. El archivo ps1 es una ayuda de configuración y arranque.
Con las variables ya definidas, otra opción desde `backend/inventario` es:

```powershell
.\gradlew.bat bootRun --no-daemon --console=plain
```

El puerto es 8080. `ECONNREFUSED` significa que no hay un servidor accesible en
esa dirección. Si el arranque indica puerto ocupado, identifica primero el proceso:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

Detén desde su terminal únicamente la instancia que hayas iniciado; no arranques
dos copias del backend en el mismo puerto.

## 12. Tests

### Evidencia histórica de Sprint 7

Base anterior de Sprint 6: **233 pruebas**. Sprint 7 reejecutó las **233 y todas
aprobaron**, sin fallos, errores ni omitidas; no se agregaron pruebas ni se
modificaron las anteriores. compileJava y bootJar finalizaron correctamente.
El JAR arrancó en 8,32 segundos y las 28 solicitudes HTTP de comprobación
obtuvieron el estado esperado. La instancia se detuvo y la base temporal se
eliminó después de la revisión; la base habitual quedó idéntica. El detalle está
en [verificación final](docs/backend-final/verificacion-final.md).

### Evidencia posterior: ampliación, frontend, Docker y CI

| Ejecución | Resultado | Alcance |
|---|---|---|
| Backend ampliado: XML locales previos leídos el 3 de octubre | 319 pruebas, 0 fallos, 0 errores, 0 omitidas en 40 suites | [Resumen de artefactos existentes](docs/despliegue/evidencias/reportes-backend-2026-10-03.json); no es una nueva ejecución ni acredita por sí solo el commit exacto |
| Adaptación Jason a V13, 3 de octubre | 67/67 pruebas frontend y build aprobado | [Evidencia de adaptación](frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json); conserva 27 y añade 40 |
| GitHub Actions del commit `e1ce75a` | Backend y frontend Jason en verde; 67 pruebas frontend | [Run 37158784428](https://github.com/markopuch/inventario-laboratorios/actions/runs/37158784428), construcción y publicación GHCR |
| Docker con imágenes `sha-e1ce75a`, 3 de octubre | 94/94 comprobaciones HTTP, 30 aserciones funcionales y siete controles SQL con 0 inconsistencias | Flujo aislado V13, actualización habitual y limpieza verificadas |

Son ejecuciones distintas: no se suman solicitudes HTTP o aserciones al total
JUnit ni se presenta la publicación del backend o el smoke como una nueva
regresión. El job backend usa `bootJar -x test`. La verificación Docker utilizó
solo una base `inventario_verificacion_*`, eliminada al terminar; los nueve
conteos habituales permanecieron iguales. Consulta el
[reporte consolidado](docs/despliegue/verificacion-docker-actions-2026-10-03.md).
La auditoría inicial de Jason (27 pruebas/42 HTTP), Docker previo (33 HTTP/24
aserciones) y Actions `9956939` son antecedentes separados. La documentación
actualizada no reejecutó suites ni modificó datos. La baja del último smoke se
verificó por API debido al bloqueo de su confirmación nativa en la automatización
del navegador; la UI mostró BAJA sin acciones de edición y conservó el historial.

### Repetir la suite del backend

Para repetir la verificación, prepara una base local de pruebas ya existente con
nombre `inventario_verificacion_*`. Desde la raíz configura DB_URL hacia esa
base, DB_USER/DB_PASSWORD y DEMO_USER_PASSWORD externos; este último se solicita
sin mostrarlo:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_verificacion_manual'
$env:DEMO_USER_PASSWORD = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Contraseña local de las cuentas de prueba' -AsSecureString)
).Password
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verificar-backend.ps1
```

El script rechaza una base habitual/no local, genera JWT_SECRET si falta y ejecuta
compileJava, test y bootJar. Comprueba los XML y exige cero fallos, errores y
omitidas. **No crea ni elimina bases**. Si reutilizas una base de pruebas con
cuentas demo, conserva su contraseña original.

Reporte HTML: `backend/inventario/build/reports/tests/test/index.html`.
JAR: `backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar`.
La verificación automatizada escribe fixtures solo en la base de pruebas.

## 13. Postman

Importa la [colección](docs/backend-final/postman/Inventario-Laboratorios.postman_collection.json)
y el [environment](docs/backend-final/postman/Inventario-Laboratorios.postman_environment.json).
La colección conserva el alcance histórico de Sprint 7; no incluye todas las
ampliaciones V10–V13. Para estudiar/probar las 70 operaciones actuales usa el
[catálogo vigente](docs/backend-final/endpoints.md) y los ejemplos de la
[ampliación](backend/inventario/ACTUALIZACION-BACKEND.md).
Completa los secretos localmente; tokens e IDs iniciales están vacíos. Sigue el
[orden guiado](docs/backend-final/endpoints.md), crea padres propios y conserva
asignaciones anteriores antes de reemplazarlas. Los IDs deben proceder de
respuestas o consultas reales.

Las guías de [asignaciones](docs/sprints_realizados-backend/sprint-4e-usuario-laboratorio.md),
[Equipos](docs/sprints_realizados-backend/sprint-5-equipos.md) y
[traslados](docs/sprints_realizados-backend/sprint-6-movimientos.md) incluyen casos manuales y SQL.
La colección no equivale a un Runner integral sin preparación: las bajas deben
ejecutarse después de probar los recursos dependientes. Las verificaciones
HTTP automatizadas y el uso manual de Postman se documentan por separado.

## 14. Documentación

| Documento vigente | Contenido |
|---|---|
| [Resumen del backend](docs/backend-final/resumen-backend.md) | Cierre histórico y ampliación vigente |
| [Inventario de auditoría Sprint 7](docs/backend-final/inventario-auditoria.md) | Estado inicial histórico y hallazgos del cierre |
| [Arquitectura](docs/backend-final/arquitectura-backend.md) | Capas y responsabilidades |
| [Auditoría Entity/Flyway Sprint 7](docs/backend-final/auditoria-entity-flyway.md) | Cotejo histórico V9 y alcance de la ampliación |
| [Flujos principales](docs/backend-final/flujos-principales.md) | Recorridos y ampliaciones de una solicitud |
| [Endpoints](docs/backend-final/endpoints.md) | Las 70 operaciones; cobertura de la colección histórica |
| [Códigos HTTP](docs/backend-final/codigos-http.md) | Éxitos y errores reales |
| [Reglas](docs/reglas-negocio.md) / [permisos](docs/matriz-permisos.md) | RN-01–45 y autorización |
| [JWT](docs/autenticacion-jwt.md) | Login, variables y comprobaciones |
| [Verificación final Sprint 7](docs/backend-final/verificacion-final.md) | Evidencia histórica V9/233, con enlace al estado vigente |
| [Checklist](docs/backend-final/checklist-entrega.md) | Criterios de entrega |
| [Sprint 7](docs/sprints_realizados-backend/sprint-7.md) | Reporte del cierre técnico |
| [Sprint 8](docs/sprints_realizados-backend/sprint-8.md) | Docker local y publicación GHCR verificados |
| [Estado de sprints backend](docs/sprints_realizados-backend/README.md) | Cierres históricos, ampliación posterior y evidencia vigente |
| [Ampliación del backend](backend/inventario/ACTUALIZACION-BACKEND.md) | Contratos V10–V13 para integrar Jason |
| [Docker y Actions](docs/despliegue/verificacion-docker-actions-2026-10-03.md) | Flujo completo, ambos trabajos verdes, imágenes y evidencias persistentes |
| [Sprints Jason](docs/sprints_realizados-frontend-Jason/README.md) | Estado actual de la implementación frontend y sus límites |
| [Frontend](frontend/readme.md) | Versiones Jason/Marko y modos de ejecución |
| [Modelo vigente V13](docs/Erd_actual/modelo-vigente-v13.md) | 12 entidades y 16 FK; ampliación sobre V9 |
| [ERD lógico v2](docs/Erd_actual/erd-logico-v2.md) / [físico v2](docs/Erd_actual/erd-fisico-v2.md) | Mermaid y SVG históricos V1–V9 |
| [Cambios de ERD](docs/Erd_actual/erd-v2-cambios.md) | Comparación con los PDF iniciales |

Los documentos de [Sprint 3](docs/sprints_realizados-backend/sprint-3-categorias.md),
[Sprint 4](docs/sprints_realizados-backend/sprint-4.md), [Sprint 5](docs/sprints_realizados-backend/sprint-5.md) y
[Sprint 6](docs/sprints_realizados-backend/sprint-6.md) conservan resultados y pendientes de cada
cierre. Sus cifras históricas no sustituyen la verificación final. Los PDF
`docs/erd-logico.pdf` y `docs/erd-fisico.pdf` son diseños anteriores; el ERD v2
describe V1–V9 y el documento del modelo V13 registra la ampliación actual.

## 15. Backlog

Usuarios administrativos, Mantenimiento como Entity, Reportes, Auditoría
administrativa, frontend Jason, Docker local y publicación GHCR están
implementados después del cierre histórico de Sprint 7.
Siguen pendientes el despliegue en la nube, Configuración persistida,
una pantalla frontend de Auditoría, exportación de reportes, refresh tokens,
permisos dinámicos y paginación para escala futura. La auditoría administrativa
existente no equivale a una plataforma general de auditoría/observabilidad.

El [backlog](docs/backend-final/backlog.md) separa esas ampliaciones de mejoras
opcionales y no las presenta como defectos del alcance entregado.
La actualización de evidencia/documentación no agregó nuevas funcionalidades.
