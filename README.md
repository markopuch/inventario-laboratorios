# Inventario de Laboratorios

Frontend de trabajo actual: [versión Jason — Docker, ejecución local y pruebas manuales](frontend/version-jason/frontend/README.md), conectado al backend Docker del [Sprint 8](docs/sprints_realizados-backend/sprint-8.md). El Compose de `backend/inventario` levanta PostgreSQL, backend y Jason (puerto 3000); `imagen.yml` publica backend y frontend Jason por separado. La [auditoría de integración](docs/sprints_realizados-frontend-Jason/auditoria-integracion.md) registra el estado comprobado; la versión Marko se conserva por separado.


## 1. Descripción

API REST para organizar laboratorios, clasificar equipos, controlar su ubicación
y registrar traslados con historial. El backend de `backend/inventario` llega al
cierre técnico de **Sprint 7**, después de las funcionalidades de Sprint 1–6.
El proyecto conserva el estilo de JPA/JWT de los ejemplos del profesor y su
organización actual de carpetas.

El alcance incluye autenticación, cinco catálogos, asignaciones de laboratorios,
Equipos y Movimientos. La administración completa de usuarios y los módulos del
[backlog](docs/backend-final/backlog.md) quedan fuera de este cierre.
La [verificación final](docs/backend-final/verificacion-final.md) registra las
pruebas medidas y la preservación de la base habitual.

## 2. Stack

| Componente | Versión/configuración del proyecto |
|---|---|
| Java | JDK 21 |
| Spring Boot | 4.1.1 |
| Gradle Wrapper | 9.7.1, Kotlin DSL |
| Persistencia | Spring Data JPA / Hibernate y PostgreSQL |
| Migraciones | Flyway, V1–V9 |
| Seguridad | Spring Security, BCrypt, JJWT 0.13.0 |
| Mapeo | MapStruct 1.6.3, Lombok, lombok-mapstruct-binding 0.2.0 |
| Validación y pruebas | Jakarta Validation, JUnit 5, Spring Boot Test |

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
[ocho flujos principales](docs/backend-final/flujos-principales.md).

## 4. Modelo

**10 entidades, 76 columnas y 13 relaciones FK**, sin cambios de esquema en
Sprint 7. `flyway_schema_history` es infraestructura y no integra ese conteo.

| Área del modelo | Entidades |
|---|---|
| Organización | Sede → Área → Laboratorio |
| Clasificación | Categoría → Subcategoría |
| Identidad y autorización | Rol, Usuario, UsuarioLaboratorio |
| Inventario | Equipo |
| Trazabilidad | MovimientoEquipo |

UsuarioLaboratorio resuelve Usuario N:M Laboratorio con PK compuesta. Equipo
tiene Laboratorio y Subcategoría obligatorios y responsable opcional. Movimiento
tiene Equipo, destino y actor obligatorios; origen permite null para datos legacy.
Los traslados nuevos siempre toman el origen real del Equipo.

Los diez modelos están integrados en Java/API; Rol y Usuario participan en
JWT/autorización y esto no significa que tengan CRUD administrativo completo.
Consulta el [ERD lógico](docs/Erd_actual/erd-logico-v2.md),
[ERD físico](docs/Erd_actual/erd-fisico-v2.md) y
[cotejo Entity/Flyway](docs/backend-final/auditoria-entity-flyway.md).

## 5. Roles

| Operación | ADMIN | GESTOR | LECTOR |
|---|---|---|---|
| Leer catálogos | Global | Global | Global |
| Escribir catálogos/asignaciones | Sí | No | No |
| Leer Equipos | Global | Alcance vigente | Alcance vigente |
| Crear, editar o dar de baja Equipos | Global | Alcance vigente | No |
| Trasladar | Global | Origen y destino autorizados | No |
| Consultar historia | Global | Origen o destino autorizado | Origen o destino autorizado |

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
- Errores seguros, DTOs públicos y bloqueos para proteger operaciones concurrentes.

PUT de Equipo rechaza código interno, laboratorio y otros campos inmutables.
BAJA no se edita ni traslada. El traslado exige destino activo/diferente y motivo
no blanco de máximo 500 caracteres; ubicación omitida/null/blanca se limpia.
Los movimientos históricos no bloquean por sí solos la baja lógica de Laboratorio.
Consulta [RN-01–45 y su evidencia](docs/reglas-negocio.md).

## 8. Endpoints

Se cuentan **42 combinaciones método+ruta**; los filtros no agregan endpoints.
Las consultas jerárquicas se agrupan con el padre de la ruta.

| Grupo | Cantidad |
|---|---:|
| AUTH | 3 |
| CATEGORIA | 6 |
| SUBCATEGORIA | 5 |
| SEDE | 6 |
| AREA | 6 |
| LABORATORIO | 5 |
| USUARIO_LABORATORIO | 2 |
| EQUIPO | 6 |
| MOVIMIENTO | 3 |

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

No se agrega V10 ni se modifica una migración aplicada. Las 13 FK usan RESTRICT
para operaciones físicas; las restricciones de baja lógica pertenecen a Services.
Las fechas son TIMESTAMPTZ. La fecha de Movimiento viene del default PostgreSQL;
`fechaActualizacion` de Equipo se actualiza en Java UTC, sin trigger.

La base habitual es `inventario_laboratorios`. Si preparas una instalación nueva,
crea una base vacía con tu usuario local antes del arranque, por ejemplo desde
pgAdmin conectado a `postgres`:

```sql
CREATE DATABASE inventario_laboratorios;
```

Ejecuta esa sentencia solo si no existe. No uses `repair`, `baseline` ni SQL
manual para ocultar diferencias de un esquema ya existente. El arranque aplica
V1–V9 en una base nueva o valida el historial de una ya preparada.

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

Las variables pertenecen a esa terminal. `.env` no se carga automáticamente.
Configúralas también en Run/Debug si arrancas desde el IDE. No publiques secretos
ni tokens. Cambiar JWT_SECRET invalida tokens anteriores.

El perfil dev crea marko/ADMIN, aldo/GESTOR y romel/LECTOR con una contraseña
externa; no restablece cuentas existentes ni crea asignaciones, Equipos o
Movimientos demo. Las cuentas existentes funcionan sin dev.

## 11. Ejecución

<a id="arranque-rápido-en-windows"></a>

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

Base anterior de Sprint 6: **233 pruebas**. Sprint 7 reejecutó las **233 y todas
aprobaron**, sin fallos, errores ni omitidas; no se agregaron pruebas ni se
modificaron las anteriores. compileJava y bootJar finalizaron correctamente.
El JAR arrancó en 8,32 segundos y las 28 solicitudes HTTP de comprobación
obtuvieron el estado esperado. La instancia se detuvo y la base temporal se
eliminó después de la revisión; la base habitual quedó idéntica. El detalle está
en [verificación final](docs/backend-final/verificacion-final.md).

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
| [Resumen del backend](docs/backend-final/resumen-backend.md) | Lectura general del cierre |
| [Inventario de auditoría](docs/backend-final/inventario-auditoria.md) | Estado inicial y hallazgos |
| [Arquitectura](docs/backend-final/arquitectura-backend.md) | Capas y responsabilidades |
| [Auditoría Entity/Flyway](docs/backend-final/auditoria-entity-flyway.md) | Correspondencia con PostgreSQL |
| [Flujos principales](docs/backend-final/flujos-principales.md) | Ocho recorridos de una solicitud |
| [Endpoints](docs/backend-final/endpoints.md) | Las 42 operaciones y Postman |
| [Códigos HTTP](docs/backend-final/codigos-http.md) | Éxitos y errores reales |
| [Reglas](docs/reglas-negocio.md) / [permisos](docs/matriz-permisos.md) | RN-01–45 y autorización |
| [JWT](docs/autenticacion-jwt.md) | Login, variables y comprobaciones |
| [Verificación final](docs/backend-final/verificacion-final.md) | Evidencia de tests, arranque y preservación |
| [Checklist](docs/backend-final/checklist-entrega.md) | Criterios de entrega |
| [Sprint 7](docs/sprints_realizados-backend/sprint-7.md) | Reporte del cierre técnico |
| [ERD lógico](docs/Erd_actual/erd-logico-v2.md) / [físico](docs/Erd_actual/erd-fisico-v2.md) | Mermaid editable y SVG |
| [Cambios de ERD](docs/Erd_actual/erd-v2-cambios.md) | Comparación con los PDF iniciales |

Los documentos de [Sprint 3](docs/sprints_realizados-backend/sprint-3-categorias.md),
[Sprint 4](docs/sprints_realizados-backend/sprint-4.md), [Sprint 5](docs/sprints_realizados-backend/sprint-5.md) y
[Sprint 6](docs/sprints_realizados-backend/sprint-6.md) conservan resultados y pendientes de cada
cierre. Sus cifras históricas no sustituyen la verificación final. Los PDF
`docs/erd-logico.pdf` y `docs/erd-fisico.pdf` son diseños anteriores; el ERD v2 es
el modelo vigente. En Sprint 7 solo se reparan enlaces rotos de las guías antiguas.

## 15. Backlog

Quedan fuera del backend cerrado: administración completa de usuarios, gestión
de mantenimiento como Entity, auditoría general, frontend, Docker, refresh token
y permisos dinámicos. El estado MANTENIMIENTO y el filtro de Equipo ya existen;
no constituyen un módulo de órdenes de mantenimiento.

El [backlog](docs/backend-final/backlog.md) separa esas ampliaciones de mejoras
opcionales como paginación y uniformidad futura de IDs inválidos. No hay rutas,
tablas ni funcionalidades nuevas de negocio agregadas para cerrar Sprint 7.
