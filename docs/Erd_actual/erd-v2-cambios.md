# ERD v2: cambios frente al diseño inicial

Actualización documental del 21 de septiembre de 2026. El modelo vigente se
obtiene de Flyway V1–V9. Los PDF anteriores son propuestas de Sprint 0, anteriores
a las migraciones; se conservan como historia y no se sobrescriben.

Documentos vigentes: [ERD lógico v2](erd-logico-v2.md) y
[ERD físico PostgreSQL v2](erd-fisico-v2.md). Ambos conservan el fuente editable
en bloques Mermaid. El documento físico incluye una vista general y vistas
ampliadas para facilitar la lectura de las tablas.

## Fuentes y alcance de la comprobación

En la revisión original se inspeccionaron las migraciones, las siete Entities JPA de aquel cierre, el
README, el cierre de Sprint 4 y los PDF [lógico anterior](../erd-logico.pdf) y
[físico anterior](../erd-fisico.pdf). Se revisaron el texto y la representación
visual de ambos PDF, de una página cada uno.

| Migración revisada | Aporte al modelo vigente |
|---|---|
| [V1__crear_organizacion_y_catalogos.sql](../../backend/inventario/src/main/resources/db/migration/V1__crear_organizacion_y_catalogos.sql) | Sede, Área, Laboratorio, Categoría y Subcategoría; PK, FK, UNIQUE, defaults e índices |
| [V2__crear_usuarios_y_seguridad.sql](../../backend/inventario/src/main/resources/db/migration/V2__crear_usuarios_y_seguridad.sql) | Rol, Usuario y la tabla puente UsuarioLaboratorio |
| [V3__crear_equipos_y_movimientos.sql](../../backend/inventario/src/main/resources/db/migration/V3__crear_equipos_y_movimientos.sql) | Equipo y MovimientoEquipo, checks y relaciones de custodia y trazabilidad |
| [V4__insertar_datos_iniciales.sql](../../backend/inventario/src/main/resources/db/migration/V4__insertar_datos_iniciales.sql) | Datos iniciales; no agrega columnas ni restricciones |
| [V5__categoria_nombre_unico_sin_mayusculas.sql](../../backend/inventario/src/main/resources/db/migration/V5__categoria_nombre_unico_sin_mayusculas.sql) | Índice UNIQUE sobre `UPPER(categoria.nombre)` |
| [V6__agregar_username_usuario.sql](../../backend/inventario/src/main/resources/db/migration/V6__agregar_username_usuario.sql) | `usuario.username VARCHAR(50) NOT NULL` e índice UNIQUE sobre `UPPER(username)` |
| [V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql](../../backend/inventario/src/main/resources/db/migration/V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql) | Unicidad de Subcategoría por categoría y nombre sin distinguir mayúsculas |
| [V8__area_nombre_unico_por_sede_sin_mayusculas.sql](../../backend/inventario/src/main/resources/db/migration/V8__area_nombre_unico_por_sede_sin_mayusculas.sql) | Unicidad de Área por sede y nombre sin distinguir mayúsculas |
| [V9__laboratorio_codigo_unico_sin_mayusculas.sql](../../backend/inventario/src/main/resources/db/migration/V9__laboratorio_codigo_unico_sin_mayusculas.sql) | Unicidad global de código de Laboratorio sin distinguir mayúsculas |

Esta verificación es documental y estática: **no se abrió ninguna conexión a
PostgreSQL, no se ejecutaron migraciones ni tests Java**. El esquema descrito es
el generado por los archivos V1–V9, no una nueva inspección de una base en vivo.
La comprobación de Flyway en la base habitual registrada en el cierre de
[Sprint 4](../sprints/sprint-4.md) es evidencia histórica de aquel sprint.

## Comparación v1 → v2

La columna «ERD anterior» se refiere al PDF físico salvo que indique «lógico».
Que un atributo ya existiera en el diseño anterior no significa que su vertical
Java esté implementada.

| Tema | ERD anterior | ERD v2 | Motivo / fuente |
|---|---|---|---|
| `usuario.username` | No figuraba | `VARCHAR(50) NOT NULL`; UNIQUE por `UPPER(username)`, sin default | V6 agrega la columna, rellena las filas existentes y luego aplica NOT NULL; el relleno no es un default |
| UsuarioLaboratorio | Ya figuraba como puente con PK compuesta | Se conserva; ambas FK son NN, más `activo` y `fecha_asignacion` | V2; no se presenta como una tabla nueva de v2 |
| Custodio de Equipo | Ya figuraba `id_responsable`, nullable | Se conserva la FK opcional a Usuario; un equipo tiene 0..1 custodio y un usuario 0..N equipos | V3; la custodia no concede permisos |
| `equipo.ubicacion_interna` | Ya figuraba, `VARCHAR(150)` | `VARCHAR(200)`, nullable, sin default | V3; cambia la longitud documentada, no se inventa una columna nueva |
| Fechas | `TIMESTAMP` en el físico | Las 11 columnas de fecha del dominio son `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | V1, V2 y V3 |
| Sede | Incluía `telefono`; no mostraba `activo` | No tiene `telefono`; sí `activo BOOLEAN NOT NULL DEFAULT TRUE` | V1; nombre no tiene UNIQUE |
| Área | No mostraba `activo` | Incluye `activo BOOLEAN NOT NULL DEFAULT TRUE` | V1 |
| Laboratorio: campos | `codigo VARCHAR(20)`, `anexo VARCHAR(20)`, `ubicacion VARCHAR(150)` | `codigo VARCHAR(30)`, sin `anexo`, `ubicacion VARCHAR(200)` | V1; el lógico antiguo también mostraba `anexo` |
| Código de Laboratorio | `UNIQUE(id_area, codigo)` | UNIQUE **global** sobre `codigo`, más índice UNIQUE sobre `UPPER(codigo)` | V1 y V9; no se agrupa por Área |
| Categoría: nombre | UNIQUE sensible a mayúsculas | Se conserva, junto al índice UNIQUE sobre `UPPER(nombre)` | V1 y V5; incluye filas inactivas |
| Subcategoría: nombre | UNIQUE por categoría y nombre | Se conserva `UNIQUE(nombre,id_categoria)` y se agrega el índice sobre `(id_categoria,UPPER(nombre))` | V1 y V7; otro padre puede reutilizar el nombre |
| Área: nombre | UNIQUE por sede y nombre | Se conserva `UNIQUE(nombre,id_sede)` y se agrega el índice sobre `(id_sede,UPPER(nombre))` | V1 y V8; otro padre puede reutilizar el nombre |
| Reserva tras baja lógica | No explicitaba el alcance de los índices posteriores | Categoría, Subcategoría, Área y Laboratorio conservan la reserva al quedar inactivos | V5, V7, V8 y V9 no tienen condición `WHERE activo` |
| Rol | `nombre VARCHAR(30)` y CHECK con tres nombres; sin estado ni fecha | `nombre VARCHAR(50)` UNIQUE; `activo` y `fecha_creacion`; **sin CHECK de nombres** | V2; los tres roles insertados por V4 no equivalen a una restricción CHECK |
| Usuario: fechas | Incluía `fecha_actualizacion` | Solo `fecha_creacion`; V6 agrega `username` | V2 y V6; no se representa una columna inexistente |
| Equipo: identificadores | `serie_utec VARCHAR(50)` | `serie_utec VARCHAR(100)`; `codigo_interno`, `serie_utec` y `numero_serie` mantienen UNIQUE individuales | V3; los dos últimos admiten NULL |
| Equipo: estado | `VARCHAR(20)` con default y cuatro estados | `VARCHAR(30) NOT NULL DEFAULT 'OPERATIVO'`; CHECK de los mismos cuatro estados | V3; no hay columna `activo` en Equipo |
| Equipo: año | `SMALLINT`, sin CHECK mostrado | `INTEGER`, nullable; CHECK `anio IS NULL OR anio BETWEEN 1900 AND 2100` | V3 |
| Equipo: fecha de actualización | Mostraba un default de fecha | Se documenta el default real; no se afirma actualización automática en cada UPDATE | V3 no crea un trigger de actualización |
| Movimiento: origen | `id_laboratorio_origen INTEGER NOT NULL` | FK nullable: 0..1 laboratorio de origen por movimiento | V3 |
| Movimiento: destino y actor | Ambos obligatorios | Ambos siguen obligatorios: exactamente un destino y un actor por movimiento | V3 |
| Movimiento: tipo | `DEFAULT 'TRASLADO'` | `VARCHAR(30) NOT NULL`, **sin DEFAULT**, CHECK `BTRIM(tipo_movimiento) <> ''` | V3; no existe enum ni lista cerrada de tipos |
| Movimiento: motivo | `TEXT NOT NULL` | `VARCHAR(500) NOT NULL`, CHECK `BTRIM(motivo) <> ''` | V3 |
| Movimiento: origen distinto de destino | CHECK mostrado en el PDF | No se documenta como restricción vigente: no existe ese CHECK en Flyway | V3; v2 conserva el esquema sin rediseñarlo |
| Cardinalidades | Predominaban etiquetas `1:N`; origen/destino se resumían juntos | Se explicitan `1`, `0..1` y `0..N`; origen y destino tienen relaciones separadas | Nullability y ausencia de UNIQUE sobre las FK en V1–V3 |
| Acciones referenciales | Nota pendiente para definirlas al implementar | Las 13 FK tienen `ON UPDATE RESTRICT ON DELETE RESTRICT` | V1–V3 |
| Índices | Nota general de crear índices para FK | Catálogo de 12 índices de FK explícitos y 5 índices UNIQUE por expresión, además de índices implícitos de PK/UQ | V1–V3 y V5–V9; la PK del puente ya cubre su primera columna |
| Estado de implementación | Propuesta de diseño sin estado actual de verticales | Ocho Entities JPA implementadas desde Sprint 4E; Equipo y MovimientoEquipo aún sin vertical Java completa | Entities actuales, README y cierre de Sprint 4 |

## Implementación actual y diseño futuro

| Estado | Entidades | Alcance |
|---|---|---|
| Implementadas en Java / integradas con API | Rol, Usuario, Categoria, Subcategoria, Sede, Area, Laboratorio | Rol y Usuario participan en JPA/JWT/login; esto no afirma que exista un CRUD administrativo completo de usuarios o roles. Los otros cinco catálogos tienen CRUD |
| Implementada en Java/API desde Sprint 4E | UsuarioLaboratorio | Administración de asignaciones explícitas por ADMIN y cálculo del alcance propio; tabla V2 sin cambios |
| Solo esquema BD / diseño futuro | Equipo, MovimientoEquipo | Sus tablas y FK existen en Flyway; sus verticales y la aplicación del alcance siguen pendientes |

Sprint 4E actualiza únicamente el estado de implementación en estos ERD: ocho
entidades integradas en Java/API y dos futuras. No necesita V10 ni cambios en
columnas, claves, restricciones o diagramas físicos. El lógico cambia de color
UsuarioLaboratorio. La clasificación no cambia nombres de tablas ni relaciones. Las descripciones
de roles sembradas en V4 expresan un alcance previsto, pero no activan por sí
solas autorización por laboratorio. Los catálogos actuales mantienen el alcance
global descrito en README y la matriz de permisos.

## Decisiones conservadas

- Código de Laboratorio único globalmente, también al comparar mayúsculas.
- Sede sin unicidad de nombre; Área y Subcategoría con unicidad dentro de su padre.
- Los UNIQUE antiguos y los índices UNIQUE por expresión coexisten; v2 los enumera.
- Las FK físicas se conservan mediante RESTRICT. Las reglas de baja lógica y
  padre activo aplicadas por Services son distintas de las restricciones SQL.
- El responsable indica custodia; los permisos dependen de la autorización.
- No se agrega `activo` a Equipo ni se inventan restricciones de negocio futuras.

## Observaciones para una versión futura

Son observaciones, no cambios ejecutados ni decisiones ya aprobadas:

1. Evaluar los índices redundantes antes de plantear cualquier retiro; esta
   versión conserva todos los de V1–V9.
2. Definir con el usuario si se necesita unicidad de nombre de Sede. Hoy no existe.
3. Resolver con la futura vertical Equipo la baja por estado, las restricciones
   a bajas de Subcategoría/Laboratorio y la actualización de `fecha_actualizacion`.
4. Definir las reglas de MovimientoEquipo antes de añadir listas cerradas de
   tipos, exigir origen distinto de destino o relacionar movimientos con el
   laboratorio actual de Equipo. La BD actual no impone esas reglas.
5. Aplicar a Equipo el servicio de alcance implementado en Sprint 4E. La tabla
   UsuarioLaboratorio ya tiene vertical Java/API; no se cambió su estructura.

## Cobertura documental

El inventario físico contiene **10 tablas, 76 columnas, 10 PK, 13 FK, 9
constraints UNIQUE, 4 CHECK y 17 índices explícitos** (12 sobre FK y 5 UNIQUE
por expresión). Nueve PK proceden de SERIAL y una es compuesta. Las PK y los
UNIQUE también crean índices implícitos, enumerados aparte en el documento físico.

`flyway_schema_history` es infraestructura de Flyway y queda fuera del dominio.
Las columnas, nullability, tipos, defaults y restricciones se contrastan con
V1–V9; las 13 relaciones del lógico se contrastan con las mismas FK. La revisión
no ejecuta SQL, no modifica Java ni altera el estado de los sprints.

## Exportaciones reales y reproducción

Se generaron y revisaron visualmente estos **cuatro SVG** con Mermaid CLI
**11.17.0** y Chrome en modo headless:

- [ERD lógico v2](erd-logico-v2.svg).
- [ERD físico completo](erd-fisico-v2.svg).
- [Ampliación: catálogos e identidad](erd-fisico-v2-catalogos.svg).
- [Ampliación: inventario, asignaciones y trazabilidad](erd-fisico-v2-inventario.svg).

No se generaron PDF nuevos. Los dos PDF de v1 permanecen intactos. Los SVG
conservan texto y gráficos vectoriales: pueden ampliarse en un navegador. La
leyenda, los checks completos y los índices acompañan a las imágenes en los
Markdown; no es necesario comprimir toda esa información en las cajas.

La fuente de cada exportación es su bloque Mermaid, incluida la configuración
visual. Se usa **la misma herramienta** en ambos modelos: flowchart para agrupar
el lógico y erDiagram para las columnas y claves del físico. La revisión final
de los cuatro SVG no encontró etiquetas recortadas.

Para regenerarlos en Windows desde la raíz del proyecto, con Node.js y Chrome
instalados, puede ejecutarse este bloque de PowerShell. Solo exporta
documentación; no inicia Java ni consulta PostgreSQL. La primera ejecución de
`npx` puede descargar la herramienta en la caché de npm, fuera del proyecto.
Si Chrome está instalado en otra ruta, ajusta `$erdChrome`.

```powershell
$erdChrome = Join-Path $env:ProgramFiles 'Google\Chrome\Application\chrome.exe'
if (-not (Test-Path -LiteralPath $erdChrome)) { throw 'Configura la ruta de Chrome.' }
$erdScratch = Join-Path ([IO.Path]::GetTempPath()) ('inventario-erd-export-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $erdScratch | Out-Null
$erdEncoding = [Text.UTF8Encoding]::new($false)
$erdBrowserConfig = Join-Path $erdScratch 'puppeteer.json'
$erdConfigJson = @{ executablePath = $erdChrome } | ConvertTo-Json
[IO.File]::WriteAllText($erdBrowserConfig, $erdConfigJson, $erdEncoding)
$erdJobs = @(
    @{ File = 'docs/Erd_actual/erd-logico-v2.md'; Names = @('erd-logico-v2') },
    @{ File = 'docs/Erd_actual/erd-fisico-v2.md'; Names = @('erd-fisico-v2', 'erd-fisico-v2-catalogos', 'erd-fisico-v2-inventario') }
)
$erdPreviousDownload = $env:PUPPETEER_SKIP_DOWNLOAD
try {
    $env:PUPPETEER_SKIP_DOWNLOAD = 'true'
    foreach ($erdJob in $erdJobs) {
        $erdMarkdown = [IO.File]::ReadAllText((Join-Path (Get-Location).Path $erdJob.File))
        $erdBlocks = [regex]::Matches($erdMarkdown, '(?s)```mermaid\s*\r?\n(.*?)\r?\n```')
        if ($erdBlocks.Count -ne $erdJob.Names.Count) { throw 'Revisa los bloques Mermaid.' }
        for ($erdIndex = 0; $erdIndex -lt $erdBlocks.Count; $erdIndex++) {
            $erdName = $erdJob.Names[$erdIndex]
            $erdInput = Join-Path $erdScratch ($erdName + '.mmd')
            [IO.File]::WriteAllText($erdInput, $erdBlocks[$erdIndex].Groups[1].Value, $erdEncoding)
            $erdOutput = Join-Path 'docs/Erd_actual' ($erdName + '.svg')
            npx.cmd --yes --package @mermaid-js/mermaid-cli@11.17.0 mmdc -i $erdInput -o $erdOutput -p $erdBrowserConfig -b white -w 2400
            if ($LASTEXITCODE -ne 0) { throw ('Error al exportar ' + $erdName) }
        }
    }
} finally {
    $env:PUPPETEER_SKIP_DOWNLOAD = $erdPreviousDownload
}
```

Los `.mmd` temporales son copias de los bloques; los Markdown siguen siendo la
fuente que se versiona. Al cambiar el modelo en otra tarea, primero se contrasta
con sus migraciones y después se regeneran las exportaciones.
