# Sprint 5 — Gestión de Equipos

Guía del CRUD de Equipo, sus filtros y la aplicación del alcance de laboratorio.
Continúa la base de **168 pruebas** de Sprint 4E. Reutiliza la tabla de V3 y el
servicio de alcance existente: **Flyway permanece en V9, sin V10**.
El [reporte de Sprint 5](sprint-5.md) registra los archivos y la evidencia final.

## 1. Objetivo

Registrar, consultar, editar y dar de baja Equipos respetando relaciones,
roles, alcance y concurrencia. ADMIN tiene gestión global, GESTOR opera dentro
de sus laboratorios asignados y LECTOR solo consulta dentro de ese alcance.
No se implementan MovimientoEquipo, traslados ni historial de movimientos.

## 2. Equipo como recurso central

Equipo une la clasificación técnica, la ubicación organizacional y un custodio
opcional. Su clasificación pasa por Subcategoría → Categoría y su ubicación por
Laboratorio → Área → Sede. UsuarioLaboratorio define dónde puede operar un
usuario; el responsable del bien no define ese permiso.

```text
Categoría
   ↓
Subcategoría
   ↓
Equipo ← Laboratorio ← Área ← Sede
   ↑
Usuario responsable (opcional)

Usuario → UsuarioLaboratorio → Laboratorio → alcance sobre Equipo
```

## 3. Esquema real de V3

La inspección de [V3](../../backend/inventario/src/main/resources/db/migration/V3__crear_equipos_y_movimientos.sql)
y del catálogo PostgreSQL confirmó estas **18 columnas**. No se añaden columnas.

| Columna | Tipo | Nullability | Clave / default |
|---|---|---|---|
| `id_equipo` | SERIAL, almacenado como INTEGER | NOT NULL | PK; secuencia propia |
| `codigo_interno` | VARCHAR(50) | NOT NULL | UNIQUE |
| `serie_utec` | VARCHAR(100) | NULL | UNIQUE |
| `numero_serie` | VARCHAR(100) | NULL | UNIQUE |
| `nombre` | VARCHAR(150) | NOT NULL | — |
| `marca` | VARCHAR(100) | NULL | — |
| `modelo` | VARCHAR(100) | NULL | — |
| `estado` | VARCHAR(30) | NOT NULL | DEFAULT 'OPERATIVO'; CHECK de cuatro estados |
| `anio` | INTEGER | NULL | CHECK 1900–2100 o NULL |
| `orden_compra` | VARCHAR(50) | NULL | — |
| `ubicacion_interna` | VARCHAR(200) | NULL | — |
| `comentario` | TEXT | NULL | Sin límite de longitud declarado por V3 |
| `requiere_mantenimiento` | BOOLEAN | NOT NULL | DEFAULT FALSE |
| `id_subcategoria` | INTEGER | NOT NULL | FK Subcategoría |
| `id_laboratorio` | INTEGER | NOT NULL | FK Laboratorio |
| `id_responsable` | INTEGER | NULL | FK Usuario |
| `fecha_creacion` | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP |
| `fecha_actualizacion` | TIMESTAMPTZ | NOT NULL | DEFAULT CURRENT_TIMESTAMP |

Las tres FK usan `ON UPDATE RESTRICT ON DELETE RESTRICT` y tienen índices
`idx_equipo_id_subcategoria`, `idx_equipo_id_laboratorio` e
`idx_equipo_id_responsable`. Los tres UNIQUE comparan los valores directamente,
sin UPPER ni filtro por estado. V3 define dos CHECK: estados y año. No existe
columna `activo`, trigger de actualización ni índice de unicidad por laboratorio.

## 4. EquipoEntity

`EquipoEntity` mapea `equipo`, su ID generado con `GenerationType.IDENTITY`,
las columnas reales y tres relaciones LAZY. Usa getters/setters, Builder y
constructores Lombok, sin `@Data`. Las fechas se representan con OffsetDateTime.
La Entity no se devuelve directamente por HTTP.

## 5. Domain

`Equipo` representa el negocio sin anotaciones JPA. Mantiene la separación
Domain ≠ Entity ≠ DTO; permite que el Service y el Mapper trabajen sin convertir
los contratos HTTP en entidades administradas por Hibernate.

## 6. EstadoEquipo

El enum contiene exactamente `OPERATIVO`, `MANTENIMIENTO`, `INOPERATIVO` y `BAJA`,
como el CHECK de V3. JSON utiliza estos nombres y un estado desconocido produce
400. El estado es obligatorio en Create/Update, aunque PostgreSQL tenga un
default para inserciones SQL. BAJA existe en el modelo, pero solo DELETE puede
llevar un Equipo a ese estado dentro de esta API.

## 7. Relaciones ManyToOne

Equipo tiene muchas filas posibles para una misma Subcategoría, Laboratorio o
Usuario responsable. Cada Equipo tiene exactamente una Subcategoría y un
Laboratorio, y cero o un responsable. Se emplean `@ManyToOne(fetch=LAZY)` y
`@JoinColumn` con la nullability real. No se agregan colecciones bidireccionales.

## 8. Subcategoría

POST exige una Subcategoría existente y activa. PUT puede cambiarla y valida
el destino del mismo modo: inexistente 404, inactiva 409. El Service resuelve y
bloquea el padre; el Mapper no consulta repositorios. Una Subcategoría con
Equipos no BAJA no puede darse de baja.

## 9. Laboratorio

POST exige un Laboratorio existente y activo y, para GESTOR, dentro del alcance.
El laboratorio se fija al crear y no cambia con PUT. Mantenerlo evita un traslado
sin registro de movimiento. La relación persistida permanece incluso si, tras
dar de baja todos sus equipos y quitar asignaciones, se desactiva el laboratorio.

## 10. Responsable

`idResponsable` es opcional. Si se proporciona uno nuevo, debe existir y estar
activo: 404 si falta, 409 si está inactivo. Si el responsable actual se desactiva
después, PUT puede conservar ese mismo ID o retirarlo con null. Cambiar a otro
responsable vuelve a exigir que el nuevo esté activo.

No se exige una fila UsuarioLaboratorio para ser responsable. Ser custodio no
concede acceso ni cambia el rol. Se consultan datos públicos para las respuestas,
sin cargar su contraseña/hash ni devolver UsuarioEntity.

## 11. CreateRequest

`CreateEquipoRequest` admite código interno, series, nombre, marca, modelo,
estado, año, orden de compra, ubicación interna, comentario, indicador de
mantenimiento y los tres IDs de referencia. No admite controlar el ID generado
ni las fechas del servidor. El JSON completo se encuentra en la sección 37.

Obligatorios: `codigoInterno`, `nombre`, `estado`, `requiereMantenimiento`,
`idSubcategoria` e `idLaboratorio`. El resto es opcional. Un boolean obligatorio
se modela con Boolean y `@NotNull` para distinguir false de ausencia.

## 12. UpdateRequest

PUT reemplaza **todos los campos editables**: `serieUtec`, `numeroSerie`, `nombre`,
`marca`, `modelo`, `estado`, `anio`, `ordenCompra`, `ubicacionInterna`, `comentario`,
`requiereMantenimiento`, `idSubcategoria` e `idResponsable`. Omitir un opcional
o enviarlo como null lo limpia; no es una actualización parcial PATCH.

No recibe ID, código interno, laboratorio ni fechas. Los campos desconocidos o
inmutables se rechazan con 400 mediante una validación específica del request,
sin cambiar el comportamiento JSON de los otros módulos. El mensaje explica:

> El PUT solo admite los campos editables del equipo. El código interno es inmutable y el cambio de laboratorio se realiza mediante el flujo de traslado.

PUT sobre BAJA o con `estado=BAJA` devuelve 409. Una entrada rechazada conserva
los datos y fechas anteriores.

## 13. Código interno inmutable

El código identifica al Equipo durante su vida útil. Se normaliza al crear y
se conserva en todas las ediciones y bajas. No se permite eludir su identidad
mediante un campo adicional en PUT. Su UNIQUE sigue reservado después de BAJA.

## 14. Laboratorio inmutable mediante PUT

`idLaboratorio` solo existe en CreateEquipoRequest. En PUT su presencia produce
400 y explica que el cambio necesita el flujo de traslado. No se cambia a otro
laboratorio por Mapper ni por un campo desconocido del JSON.

## 15. Editar frente a trasladar

Editar cambia datos técnicos, Subcategoría o responsable. Trasladar cambia la
ubicación a otro laboratorio y tendrá que registrar MovimientoEquipo y validar
origen/destino. El futuro `POST /api/equipos/{id}/traslados` no se implementa en
Sprint 5. Esta separación conserva las reglas de trazabilidad del proyecto.

## 16. Responses resumidos

EquipoResponse incluye `id`, `codigoInterno`, `serieUtec`, `numeroSerie`, `nombre`,
`marca`, `modelo`, `estado`, `anio`, `ordenCompra`, `ubicacionInterna`, `comentario`,
`requiereMantenimiento`, `fechaCreacion` y `fechaActualizacion`, además de:

| Resumen | Campos públicos |
|---|---|
| `subcategoria` | `id`, `nombre` |
| `laboratorio` | `id`, `codigo`, `nombre` |
| `responsable` | `id`, `userName`, `nombre`, `apellido`, o null |

No se incluyen Entities, contraseñas, hashes ni árboles JPA completos. El Service
obtiene resúmenes públicos de responsables en lote, evitando una consulta por
equipo y sin recorrer su información sensible.

## 17. Mapper

EquipoMapper sigue MapStruct con `componentModel="spring"` y
`ReportingPolicy.ERROR`. Convierte Create/Update → Domain, Entity → Domain,
Domain → Entity y Domain → Response, incluidas listas y resúmenes. La copia de
edición conserva campos del servidor e inmutables.

El Mapper transforma datos; no resuelve relaciones, consulta repositorios,
comprueba duplicados, decide roles ni valida alcance. Esas responsabilidades
permanecen en el Service.

## 18. Repository

EquipoRepository extiende JpaRepository. Tiene consultas de duplicados,
existencia de Equipos no BAJA por padre, búsqueda bloqueante y dos listados
JPQL: global y restringido mediante IDs de laboratorio permitidos. Los filtros
se añaden como condiciones a esas consultas y el orden es `id_equipo` ascendente.
Las relaciones requeridas se cargan de forma controlada; no se utiliza
`findAll()` para descargar la tabla y recortarla en memoria.

## 19. Cómo se combinan los filtros

`estado`, `idLaboratorio`, `idSubcategoria` y `requiereMantenimiento` son opcionales.
Un parámetro ausente no restringe; los presentes se combinan con AND. La consulta
restringida añade además los laboratorios del alcance, por lo que filtrar nunca
amplía los permisos. Se conserva JPQL, sin introducir Specifications ni una
abstracción adicional solo para estos cuatro filtros.

## 20. Service

EquipoService coordina autorización, normalización, duplicados, referencias,
transacciones y fechas. Al crear/editar valida todos los datos antes de guardar.
POST devuelve 201 con Location; PUT devuelve el estado actualizado con 200 y
DELETE devuelve 204 sin cuerpo. Las excepciones reutilizan ApiError y el handler
global, con mensajes de negocio y sin detalles internos de PostgreSQL.

## 21. AlcanceLaboratorioService

Se reutiliza el servicio de Sprint 4E; no se confía en un rol o ID de usuario
enviados en el body. El principal identifica al actor y el backend verifica su
estado vigente. GESTOR/LECTOR obtienen sus laboratorios activos asignados;
EquipoRepository aplica sus IDs en PostgreSQL. El usuario sin alcance obtiene
lista vacía al consultar sin filtro explícito de laboratorio.

## 22. ADMIN

ADMIN consulta y administra cualquier Equipo existente, con las restricciones
de estado/referencias que correspondan. Su listado y detalle incluyen históricos
BAJA incluso si el laboratorio fue desactivado posteriormente. Un filtro de
laboratorio para ADMIN exige que ese laboratorio exista, pero acepta inactivos.
POST siempre exige laboratorio activo.

Esta consulta histórica no cambia `/api/auth/me/laboratorios`, cuyo contrato
sigue siendo todos los laboratorios **activos** para ADMIN. No necesita
asignaciones explícitas para operar Equipos.

## 23. GESTOR

Consulta y escribe solo en laboratorios de su alcance vigente. Un detalle
existente fuera de alcance devuelve 403; un Equipo inexistente devuelve 404.
Un filtro explícito `idLaboratorio` fuera del alcance devuelve 403, incluso si
GET sin filtro habría devuelto `[]`. Revocar la asignación afecta a las siguientes
peticiones sin necesidad de emitir otro JWT válido.

## 24. LECTOR

Puede listar y consultar Equipos de su alcance, incluidos BAJA. POST, PUT y
DELETE siempre dan 403. Tener asignaciones o ser responsable no convierte a
LECTOR en editor. El listado administrativo también es exclusivo de ADMIN.

## 25. Baja lógica

DELETE bloquea el Equipo, comprueba el alcance y cambia su estado a BAJA.
Actualiza `fechaActualizacion` y conserva ID, código, referencias, fecha de
creación y demás datos. La fila permanece y sus identificadores siguen reservados.
No se utiliza `repository.delete()` para esta operación.

## 26. BAJA consultable

BAJA es un estado del bien, no ocultamiento absoluto como `activo=false` en los
catálogos. Por eso GET detalle y listado siguen devolviéndolo cuando hay acceso,
y `estado=BAJA` es un filtro válido. PUT y segundo DELETE dan 409; no existe
reactivación en este sprint.

## 27. Ejemplos de filtros

| Consulta relativa | Resultado autorizado |
|---|---|
| `/api/equipos` | Todos los estados dentro del alcance, incluido BAJA |
| `/api/equipos?estado=OPERATIVO` | Solo ese estado |
| `/api/equipos?idLaboratorio={{laboratorioId}}` | Solo ese laboratorio si está permitido |
| `/api/equipos?idSubcategoria={{subcategoriaId}}` | Solo esa Subcategoría dentro del alcance |
| `/api/equipos?requiereMantenimiento=true` | Solo los marcados para mantenimiento |
| `/api/equipos?estado=OPERATIVO&idLaboratorio={{laboratorioId}}&requiereMantenimiento=false` | Intersección de las tres condiciones |

La ruta `/api/admin/equipos` acepta los mismos filtros y reutiliza la misma
lógica global. Enum, boolean o identificador con formato inválido producen 400.
Los IDs suministrados como filtros deben ser positivos.

## 28. Validaciones y normalización

| Campo | Regla |
|---|---|
| `codigoInterno` | Obligatorio en POST, no blanco, máximo 50; inmutable en PUT |
| `nombre` | Obligatorio, no blanco, máximo 150 |
| `serieUtec`, `numeroSerie` | Opcionales, máximo 100; trim y blanco → null |
| `marca`, `modelo` | Opcionales, máximo 100; trim y blanco → null |
| `ordenCompra` | Opcional, máximo 50; trim y blanco → null |
| `ubicacionInterna` | Opcional, máximo 200; trim y blanco → null |
| `comentario` | Opcional, TEXT; trim y blanco → null, sin límite SQL inventado |
| `estado` | Obligatorio; enum real; POST/PUT hacia BAJA → 409 |
| `anio` | Opcional; de 1900 a 2100 inclusive |
| `requiereMantenimiento` | Obligatorio; true o false |
| `idSubcategoria` | Obligatorio y positivo |
| `idLaboratorio` | Obligatorio y positivo en POST; inmutable en PUT |
| `idResponsable` | Opcional; positivo si se informa |

Bean Validation controla formato y longitudes; el Service aplica reglas de
negocio. Código y nombre se recortan y siguen siendo obligatorios. Un campo
opcional ausente representa ausencia, especialmente en las dos series UNIQUE.
El indicador de mantenimiento no obliga a un estado concreto: son datos distintos.

## 29. Duplicados

Los tres identificadores conservan la comparación sensible a mayúsculas de V3.
El Service anticipa duplicados y PostgreSQL aporta la última defensa concurrente.
La API devuelve 409 para código interno, serie UTEC o número de serie repetidos,
incluidos los reservados por Equipos BAJA. En PUT se excluye el propio ID para
permitir conservar las series existentes. Varias filas pueden tener series null.

Una posible unicidad futura sin distinguir mayúsculas requeriría una decisión
de negocio y migración; no se introduce en Sprint 5.

## 30. Fecha de creación

En INSERT la genera PostgreSQL mediante el DEFAULT de V3 y Hibernate recupera
el valor generado. Se utiliza `TIMESTAMPTZ`/OffsetDateTime. No se acepta del
cliente ni se reemplaza durante PUT o DELETE lógico.

## 31. Fecha de actualización

Al insertar también procede del DEFAULT PostgreSQL. En cada PUT correcto y
DELETE lógico, EquipoService establece `OffsetDateTime.now(ZoneOffset.UTC)`;
Hibernate persiste el nuevo valor. No se agrega un trigger. Los errores no
deben cambiarla. La serialización puede mostrar un offset, pero representa
el instante guardado, no una cadena local sin zona.

## 32. Subcategoría con Equipos

SubcategoriaService bloquea el padre y comprueba si existe algún Equipo cuyo
estado sea distinto de BAJA. En ese caso devuelve 409 y conserva la Subcategoría
activa. Si todos sus Equipos están en BAJA, la baja de la Subcategoría puede
continuar con 204. Los registros históricos siguen referenciándola.

## 33. Laboratorio con Equipos

LaboratorioService conserva la regla de Sprint 4E: una asignación activa de
UsuarioLaboratorio bloquea su baja, aunque el usuario esté inactivo. Sprint 5
agrega otra condición: tampoco puede tener Equipos no BAJA. Ambas deben permitir
la baja. La regla usa EquipoRepository y UsuarioLaboratorioRepository, sin
colecciones bidireccionales ni lógica en Controller.

## 34. Seguridad y errores

| Código | Situación |
|---|---|
| 400 | JSON, formato, enum, Bean Validation, IDs inválidos o campos no editables en PUT |
| 401 | JWT ausente, inválido o usuario sin autenticación vigente |
| 403 | Rol insuficiente o laboratorio fuera del alcance |
| 404 | Equipo o referencia requerida inexistente |
| 409 | Duplicados, referencia inactiva, BAJA incompatible o baja de padre bloqueada |
| 500 | Error interno genérico, sin SQL ni trazas en la respuesta |

Se mantienen login, firma, expiración JWT y BCrypt. Los roles se comprueban en
Spring Security y el Service aplica el alcance. Los listados no exponen datos
de otro laboratorio por combinar filtros.

## 35. Concurrencia

Las escrituras bloquean primero al usuario actor con lectura pesimista, compatible
con referencias al custodio y coordinada con el PUT de asignaciones de Sprint 4E.
PUT/DELETE bloquean después al Equipo. Crear/editar coordina los padres en orden
Subcategoría → Laboratorio para impedir que una baja concurrente deje un Equipo
no BAJA bajo un padre inactivo.

Si gana CREATE, DELETE padre encuentra el Equipo y da 409. Si gana la baja del
padre, CREATE detecta inactividad y da 409. PUT y DELETE del mismo Equipo usan
su bloqueo: si DELETE gana, PUT detecta BAJA y no puede reactivarlo. Los UNIQUE
resuelven la carrera de dos creaciones con el mismo código. Las nueve invocaciones
concurrentes verifican CREATE frente a cada padre en ambos órdenes (4), PUT
frente a DELETE (2), código duplicado (1) y revocación de alcance frente a POST
de GESTOR en ambos órdenes (2).

## 36. Los seis endpoints

| Método | Ruta | Roles | Éxito |
|---|---|---|---|
| GET | `/api/equipos` | ADMIN, GESTOR, LECTOR; alcance aplicado | 200 |
| GET | `/api/equipos/{id}` | ADMIN, GESTOR, LECTOR; alcance aplicado | 200 |
| POST | `/api/equipos` | ADMIN, GESTOR dentro de alcance | 201 y Location |
| PUT | `/api/equipos/{id}` | ADMIN, GESTOR dentro de alcance | 200 |
| DELETE | `/api/equipos/{id}` | ADMIN, GESTOR dentro de alcance | 204 |
| GET | `/api/admin/equipos` | Solo ADMIN | 200 |

EquipoController representa el recurso normal. AdminEquipoController ofrece la
vista global explícita del curso, reutilizando el mismo Service y filtros.

## 37. Postman: preparación y secuencia manual

### Preparar variables y conservar datos

Inicia el backend siguiendo el [README](../../README.md#arranque-rápido-en-windows).
Usa `baseUrl=http://localhost:8080`, una `corrida` corta y única por ejecución
(por ejemplo, fecha/hora compacta), y `demoPassword` como valor local secreto.
No exportes tokens ni contraseñas. Los códigos del ejemplo deben mantenerse
dentro de los límites de la sección 28.

Inicia sesión mediante `POST /api/auth/login`, No Auth, Body raw JSON:

```json
{"userName":"marko","password":"{{demoPassword}}"}
```

Guarda `accessToken` local como `tokenAdmin`. Repite con aldo y romel para
`tokenGestor` y `tokenLector`. Obtén sus IDs mediante la consulta segura de la
sección 38. Los nombres demo solo sirven si esas cuentas ya existen.

Ejecuta `GET /api/subcategorias` y `GET /api/laboratorios` para conocer IDs reales.
Para casos que dan de baja padres, crea una organización de prueba aislada con
ADMIN. La tabla siguiente presenta cada body; guarda el `id` de cada respuesta
201 en la variable indicada, sin asumir números:

| POST | Body raw JSON | Variable de respuesta |
|---|---|---|
| `/api/categorias` | `{"nombre":"Cat S5 {{corrida}}","descripcion":"Prueba"}` | `categoriaPruebaId` |
| `/api/subcategorias` | `{"nombre":"Sub S5 {{corrida}}","descripcion":"Prueba","idCategoria":{{categoriaPruebaId}}}` | `subcategoriaId` |
| `/api/subcategorias` | `{"nombre":"Sub alternativa {{corrida}}","descripcion":null,"idCategoria":{{categoriaPruebaId}}}` | `subcategoriaAlternaId` |
| `/api/sedes` | `{"nombre":"Sede S5 {{corrida}}","direccion":null,"distrito":null,"departamento":null}` | `sedePruebaId` |
| `/api/areas` | `{"nombre":"Área S5 {{corrida}}","descripcion":"Prueba","idSede":{{sedePruebaId}}}` | `areaPruebaId` |
| `/api/laboratorios` | `{"nombre":"Laboratorio A S5","codigo":"S5-A-{{corrida}}","ubicacion":null,"idArea":{{areaPruebaId}}}` | `laboratorioId` |
| `/api/laboratorios` | `{"nombre":"Laboratorio B S5","codigo":"S5-B-{{corrida}}","ubicacion":null,"idArea":{{areaPruebaId}}}` | `laboratorioFueraId` |

Obtén `aldoId`, `romelId` y un `responsableId` activo con SQL; puede ser cualquiera
de los usuarios activos reales. Antes de modificar asignaciones de Aldo/Romel,
guarda sus conjuntos originales con GET ADMIN. En una base de prueba configura
ambos usuarios con `{"idsLaboratorio":[{{laboratorioId}}]}` mediante PUT
`/api/admin/usuarios/{idUsuario}/laboratorios`. Consulta sus alcances para
confirmarlo. El laboratorio B queda fuera de su alcance.

Las pruebas manuales siguientes escriben si ejecutas POST/PUT/DELETE. La ejecución
automática del sprint no crea equipos ni asignaciones de demostración en la
base habitual. Una baja lógica conserva filas; usa una base personal de prueba
si necesitas mantener intactos tus datos de uso habitual.

### Caso 1 — Crear Equipo como ADMIN

`POST {{baseUrl}}/api/equipos`, Bearer `{{tokenAdmin}}`, JSON:

```json
{
  "codigoInterno":"EQ-S5-{{corrida}}",
  "serieUtec":"UTEC-S5-{{corrida}}",
  "numeroSerie":"SN-S5-{{corrida}}",
  "nombre":"Osciloscopio Sprint 5",
  "marca":"  Rigol  ",
  "modelo":"DS1054Z",
  "estado":"OPERATIVO",
  "anio":2025,
  "ordenCompra":"OC-S5-{{corrida}}",
  "ubicacionInterna":"Armario A",
  "comentario":"Prueba Sprint 5",
  "requiereMantenimiento":false,
  "idSubcategoria":{{subcategoriaId}},
  "idLaboratorio":{{laboratorioId}},
  "idResponsable":{{responsableId}}
}
```

Esperado: 201, Location y response público. Guarda `id` como `equipoId`, sus dos
fechas y código/laboratorio iniciales. `marca` debe ser `Rigol`, sin espacios.

### Caso 2 — Listado, detalle y vista ADMIN

Consulta con ADMIN `/api/equipos`, `/api/equipos/{{equipoId}}` y
`/api/admin/equipos`: 200. GESTOR/LECTOR pueden usar las dos primeras dentro
del alcance; `/api/admin/equipos` devuelve 403 con sus tokens.

### Caso 3 — Cuatro filtros y combinación

Ejecuta cada consulta de la sección 27 con los tres tokens. El Equipo creado
aparece en OPERATIVO, su laboratorio, su Subcategoría y mantenimiento=false.
Mantenimiento=true no debe incluirlo todavía. Compara los IDs de cada respuesta:
ningún filtro de GESTOR/LECTOR puede exponer el laboratorio B.

### Caso 4 — Crear como GESTOR, custodio opcional y blancos

Copia el POST, utiliza otro código como `EQ-G-S5-{{corrida}}`, cambia ambas
series a `"   "` y `""`, y `idResponsable` a null. Envía con tokenGestor al
laboratorio A: 201. Guarda `equipoGestorId`. Series y responsable deben aparecer
como null. Un segundo equipo con series null también es válido si su código es
nuevo. Guarda todos los IDs creados para la limpieza lógica final.

### Caso 5 — Fuera de alcance y responsable sin permiso

Con ADMIN crea otro equipo en `laboratorioFueraId`, código/series nuevos y
Subcategoría alternativa; guarda `equipoFueraId`. Puedes usar Aldo como
responsable aunque no tenga asignado B. GET detalle de ese equipo con
tokenGestor o tokenLector da 403. POST de un código nuevo en B con GESTOR da 403.
GET con filtro `idLaboratorio={{laboratorioFueraId}}` también da 403 a ambos.
ADMIN puede consultarlo con 200.

### Caso 6 — Alcance vacío y cambio sin renovar JWT

Después de guardar la configuración, ADMIN envía PUT vacío para Aldo. Con el
mismo tokenGestor válido, GET `/api/equipos` devuelve `[]`; el detalle de
`equipoId` devuelve 403. Restablece la asignación A y vuelve a consultarlo: 200.
No confundas `/api/laboratorios`, que sigue siendo catálogo global.

### Caso 7 — PUT completo válido

`PUT {{baseUrl}}/api/equipos/{{equipoId}}`, Bearer ADMIN o GESTOR autorizado:

```json
{
  "serieUtec":"UTEC-S5-{{corrida}}",
  "numeroSerie":"SN-S5-{{corrida}}",
  "nombre":"Osciloscopio editado Sprint 5",
  "marca":"Rigol",
  "modelo":"DS1054Z",
  "estado":"MANTENIMIENTO",
  "anio":2025,
  "ordenCompra":null,
  "ubicacionInterna":"Mesa de revisión",
  "comentario":null,
  "requiereMantenimiento":true,
  "idSubcategoria":{{subcategoriaAlternaId}},
  "idResponsable":null
}
```

Esperado: 200. Código, ID, laboratorio y fechaCreacion se conservan;
fechaActualizacion cambia, la Subcategoría cambia, el responsable se retira y
los opcionales nulos se limpian. Vuelve a usar `subcategoriaId` si continuarás
la prueba de bloqueo del padre original. Comprueba filtros MANTENIMIENTO y
requiereMantenimiento=true después de la edición.

### Caso 8 — Campos inmutables y traslado diferido

Añade por separado `codigoInterno`, `idLaboratorio`, `id`, `fechaCreacion` o
`fechaActualizacion` al PUT válido: 400 sin cambios. También un campo desconocido
da 400. El mensaje explica que el código es inmutable y el cambio de laboratorio
requiere el flujo de traslado. No existe todavía la ruta de traslado.

### Caso 9 — Duplicados de los tres identificadores

Copia el POST inicial y prueba, por separado:

| Código/series enviados | Esperado |
|---|---|
| Mismo codigoInterno; series nuevas o null | 409 |
| Código nuevo; misma serieUtec; numeroSerie nuevo o null | 409 |
| Código nuevo; serieUtec nueva o null; mismo numeroSerie | 409 |

En PUT de otro Equipo, intenta utilizar las series de `equipoId`: 409 y ninguna
modificación parcial. Conservar las series del propio Equipo es válido. Los
valores diferentes solo en mayúsculas no colisionan por una regla UPPER: V3 no
contiene esa protección. No confundas esta decisión con la de Laboratorio.

### Caso 10 — Validaciones y estado BAJA

| Cambio al body válido | Esperado |
|---|---|
| Estado desconocido, por ejemplo `OTRO` | 400 |
| Falta estado o requiereMantenimiento | 400 |
| Año 1899 o 2101 | 400 |
| Código/nombre blanco o longitud superior a su límite | 400 |
| ID requerido null, cero o negativo | 400 |
| Responsable cero o negativo | 400 |
| POST con estado BAJA | 409 |
| PUT de Equipo vigente con estado BAJA | 409 |
| Query con enum/boolean/ID inválido | 400 |

Parte siempre de un JSON válido para aislar la regla que quieres verificar.

### Caso 11 — Referencias inexistentes

Obtén mediante las consultas de la sección 38 IDs positivos válidos que no
existan. Sustituye una referencia cada vez en un POST con código/series nuevos:
Subcategoría, Laboratorio y Responsable inexistentes dan 404. PUT también
rechaza Subcategoría/Responsable nuevos inexistentes sin cambiar el Equipo.
No uses un número supuesto que podría corresponder a un registro real.

### Caso 12 — Referencias inactivas

Crea una Subcategoría y un Laboratorio adicionales exclusivos de prueba,
sin equipos/asignaciones; dales baja con ADMIN. Intenta usarlos en un POST nuevo:
409. La Subcategoría inactiva también da 409 como destino del PUT. Guarda sus
IDs y no reutilices nombres/códigos reservados por las bajas.

Si existe un usuario inactivo en una base de prueba, asignarlo como responsable
nuevo produce 409; no es necesario que tenga alcance. Este sprint no incluye
un endpoint para desactivar usuarios. No modifiques usuarios habituales solo
para preparar ese escenario: los tests aislados cubren responsable nuevo
inactivo, conservar uno actual posteriormente inactivo y retirarlo.

### Caso 13 — Padres bloqueados por Equipo no BAJA

Con un Equipo no BAJA bajo `subcategoriaId`, DELETE de esa Subcategoría con
ADMIN debe dar 409. DELETE del laboratorio A también da 409. Para aislar la
regla de Equipos de la regla de asignaciones, usa el laboratorio B sin usuarios
asignados: su Equipo no BAJA también impide DELETE con 409. Los padres siguen
activos y sus GET devuelven 200.

### Caso 14 — Baja lógica y consulta posterior

DELETE `/api/equipos/{{equipoId}}` con ADMIN o GESTOR autorizado: 204. GET detalle
con token autorizado: 200 y `estado="BAJA"`. Comprueba SQL: fila presente, mismas
referencias/fechaCreacion y fechaActualizacion nueva. GET `?estado=BAJA` lo
incluye. Segundo DELETE y PUT válido sobre ese Equipo: 409. Un POST que intenta
reutilizar su código o sus series aún informadas también da 409.

### Caso 15 — Padres con solo Equipos BAJA

Da de baja todos los Equipos de prueba que aún usen las Subcategorías/laboratorios
que vas a desactivar; incluye `equipoGestorId`, `equipoFueraId` y cualquier
creación adicional. Con todos BAJA, una Subcategoría de prueba puede dar 204 al
DELETE. El laboratorio A sigue dando 409 mientras conserve las asignaciones
activas de Aldo/Romel; B puede dar 204 si no tiene ninguna asignación activa.

Después de desactivar B, ADMIN sigue consultando `equipoFueraId` con 200 y puede
filtrar por `laboratorioFueraId`; el registro BAJA no desaparece del inventario
histórico. GESTOR/LECTOR siguen sin acceso a ese laboratorio.

### Caso 16 — Seguridad HTTP

Con tokenLector, POST/PUT/DELETE de Equipos devuelve 403, incluso si está
asignado o es custodio. Con tokenGestor fuera del alcance, PUT/DELETE devuelve
403. Los seis endpoints sin JWT válido devuelven 401. GET administrativo con
GESTOR/LECTOR devuelve 403; con ADMIN devuelve 200. Una solicitud rechazada no
cambia estado ni fecha.

### Caso 17 — Concurrencia y cierre manual

Las regresiones automáticas coordinan crear frente a baja de Subcategoría o
Laboratorio, PUT frente a DELETE y dos códigos duplicados. Pulsar Send en dos
pestañas no garantiza que las transacciones se solapen; úsalo como exploración,
no como sustituto de esas pruebas coordinadas.

Restaura las asignaciones originales guardadas de Aldo/Romel mediante PUT ADMIN.
Retira cualquier asignación de prueba antes de dar de baja los laboratorios.
Da de baja los Equipos adicionales y luego los padres creados para la prueba,
en orden Laboratorio → Área → Sede y Subcategoría → Categoría. No desactives
padres preexistentes. Los Equipos BAJA y los pares inactivos de asignación
permanecen físicamente; la API no ofrece borrar esa historia. Comprueba los
resultados con SQL y conserva solamente los IDs públicos en tus notas.

## 38. SQL de verificación

Consultas de lectura para pgAdmin, sin contraseñas, hashes ni tokens. Ejecuta
sobre la misma base de tu prueba manual.

```sql
-- 1. IDs reales de usuarios y rol; responsable activo y actores de Postman.
SELECT u.id_usuario, u.username, u.nombre, u.apellido, u.activo, r.nombre AS rol
FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ORDER BY u.id_usuario;

-- 2. Equipo con toda la jerarquía y custodio opcional.
SELECT e.id_equipo, e.codigo_interno, e.serie_utec, e.numero_serie,
       e.nombre, e.marca, e.modelo, e.estado, e.anio, e.orden_compra,
       e.ubicacion_interna, e.comentario, e.requiere_mantenimiento,
       sc.id_subcategoria, sc.nombre AS subcategoria, c.nombre AS categoria,
       l.id_laboratorio, l.codigo AS laboratorio_codigo, l.nombre AS laboratorio,
       a.nombre AS area, s.nombre AS sede,
       u.id_usuario AS responsable_id, u.username AS responsable_username,
       e.fecha_creacion, e.fecha_actualizacion
FROM equipo e
JOIN subcategoria sc ON sc.id_subcategoria = e.id_subcategoria
JOIN categoria c ON c.id_categoria = sc.id_categoria
JOIN laboratorio l ON l.id_laboratorio = e.id_laboratorio
JOIN area a ON a.id_area = l.id_area
JOIN sede s ON s.id_sede = a.id_sede
LEFT JOIN usuario u ON u.id_usuario = e.id_responsable
ORDER BY e.id_equipo;

-- 3. Equipos no dados de baja.
SELECT id_equipo, codigo_interno, estado, id_subcategoria, id_laboratorio
FROM equipo WHERE estado <> 'BAJA' ORDER BY id_equipo;

-- 4. BAJA permanece, con fechas conservadas/actualizadas.
SELECT id_equipo, codigo_interno, estado, fecha_creacion, fecha_actualizacion
FROM equipo WHERE estado = 'BAJA' ORDER BY id_equipo;

-- 5. Equipos por laboratorio, incluyendo los que ya están en BAJA.
SELECT l.id_laboratorio, l.codigo, l.activo AS laboratorio_activo,
       e.id_equipo, e.codigo_interno, e.estado
FROM laboratorio l JOIN equipo e ON e.id_laboratorio = l.id_laboratorio
ORDER BY l.id_laboratorio, e.id_equipo;

-- 6. Equipos por Subcategoría y condición que bloquea su baja.
SELECT sc.id_subcategoria, sc.nombre, sc.activo,
       COUNT(e.id_equipo) AS total,
       COUNT(e.id_equipo) FILTER (WHERE e.estado <> 'BAJA') AS no_baja
FROM subcategoria sc LEFT JOIN equipo e ON e.id_subcategoria = sc.id_subcategoria
GROUP BY sc.id_subcategoria, sc.nombre, sc.activo ORDER BY sc.id_subcategoria;

-- 7. Marca de mantenimiento independiente del estado.
SELECT id_equipo, codigo_interno, estado, requiere_mantenimiento
FROM equipo WHERE requiere_mantenimiento = TRUE ORDER BY id_equipo;

-- 8. Duplicados reales de los tres UNIQUE: cero filas en cada consulta.
SELECT codigo_interno, COUNT(*) FROM equipo
GROUP BY codigo_interno HAVING COUNT(*) > 1;
SELECT serie_utec, COUNT(*) FROM equipo WHERE serie_utec IS NOT NULL
GROUP BY serie_utec HAVING COUNT(*) > 1;
SELECT numero_serie, COUNT(*) FROM equipo WHERE numero_serie IS NOT NULL
GROUP BY numero_serie HAVING COUNT(*) > 1;

-- 9. Series vacías que la API debe normalizar a NULL: cero tras sus altas.
SELECT id_equipo FROM equipo
WHERE BTRIM(serie_utec) = '' OR BTRIM(numero_serie) = '';

-- 10. Equipos no BAJA bajo padres inactivos: cero tras operar por la API.
SELECT e.id_equipo, e.estado, sc.activo AS subcategoria_activa,
       l.activo AS laboratorio_activo
FROM equipo e
JOIN subcategoria sc ON sc.id_subcategoria = e.id_subcategoria
JOIN laboratorio l ON l.id_laboratorio = e.id_laboratorio
WHERE e.estado <> 'BAJA' AND (NOT sc.activo OR NOT l.activo);

-- 11. Asignaciones vigentes para contrastar el alcance de GESTOR/LECTOR.
SELECT u.id_usuario, u.username, r.nombre AS rol,
       ul.id_laboratorio, l.codigo
FROM usuario u JOIN rol r ON r.id_rol = u.id_rol
JOIN usuario_laboratorio ul ON ul.id_usuario = u.id_usuario
JOIN laboratorio l ON l.id_laboratorio = ul.id_laboratorio
WHERE u.activo AND r.activo AND ul.activo AND l.activo
ORDER BY u.id_usuario, ul.id_laboratorio;

-- 12. Candidatos positivos ausentes: verificar antes de usar en pruebas 404.
SELECT COALESCE(MAX(id_equipo)::bigint,0)+1 AS equipo_inexistente_id FROM equipo;
SELECT COALESCE(MAX(id_subcategoria)::bigint,0)+1 AS subcategoria_inexistente_id FROM subcategoria;
SELECT COALESCE(MAX(id_laboratorio)::bigint,0)+1 AS laboratorio_inexistente_id FROM laboratorio;
SELECT COALESCE(MAX(id_usuario)::bigint,0)+1 AS usuario_inexistente_id FROM usuario;

-- 13. Esquema real: 18 columnas, tres FK, tres UNIQUE y dos CHECK.
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema='public' AND table_name='equipo' ORDER BY ordinal_position;
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint WHERE conrelid='public.equipo'::regclass ORDER BY conname;
SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname='public' AND tablename='equipo' ORDER BY indexname;

-- 14. Flyway se conserva en V1–V9, sin nueva migración.
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 15. Conteos públicos antes/después.
SELECT 'equipo' AS tabla, COUNT(*) FROM equipo
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'usuario_laboratorio', COUNT(*) FROM usuario_laboratorio
UNION ALL SELECT 'laboratorio', COUNT(*) FROM laboratorio
UNION ALL SELECT 'subcategoria', COUNT(*) FROM subcategoria;
```

Un candidato ausente debe caber en el entero positivo aceptado por la API y
seguir sin existir al hacer la solicitud. Para comparar un caso específico,
usa su ID real en un WHERE o identifica la fila por el código de tu corrida.
Las consultas de duplicados no usan UPPER, porque V3 conserva comparación exacta.

## 39. Tests y verificación

Regresión anterior: **168 pruebas**. Resultado Sprint 5:
**203 aprobadas de 203: 168 anteriores y 35 nuevas; cero fallos, errores y omitidas**. Base temporal: `inventario_verificacion_s5_equipo_20260921_b8f4`.
Compilación, JAR y limpieza: **compileJava, suite completa y bootJar correctos**. Después de verificar
los fixtures y comprobar cero conexiones, se eliminó la base temporal y se
confirmó su ausencia en el catálogo. V1–V9 conservaron sus huellas SHA-256.

El desglose nuevo es Mapper 3, Service 7, integración 14, concurrencia 9 y una
regresión adicional en cada Service padre (2). Tras la suite quedaron solamente
3 roles, 3 usuarios, 2 categorías, 4 subcategorías, 1 sede, 2 áreas y 2
laboratorios: 0 Equipos, 0 movimientos y 0 asignaciones. Se comprobó que no
existían Equipos no BAJA bajo padres inactivos.

La cobertura nueva comprueba mappers, campos del servidor, responsables públicos,
CRUD, estados, filtros combinados, roles/alcance, inmutabilidad, referencias,
timestamps, bajas de padres y concurrencia. Toda integración que escribe utiliza
exclusivamente `inventario_verificacion_*`, con fixtures propios e IDs devueltos,
sin alterar equipos o asignaciones reales de la base habitual.

Con una base temporal preparada y las variables DB/JWT del README configuradas
solo en memoria, desde `backend/inventario` se verifica compilación, tests y JAR:

```powershell
.\gradlew.bat compileJava --no-daemon --console=plain
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat bootJar --no-daemon --console=plain
```

No apuntes la suite de escritura a `inventario_laboratorios`. Los reportes se
generan en `backend/inventario/build/reports/tests/test/index.html`. La base
temporal se elimina solo tras verificar fixtures y cerrar sus conexiones.
Resultado de preservación habitual: **conteos y huellas de datos públicos idénticos**: 0 Equipos, 3 usuarios,
0 asignaciones, 2 laboratorios y 4 subcategorías; sin escrituras demo.

## 40. Archivos creados

Rutas principales relativas a
`backend/inventario/src/main/java/com/utec/inventario/`:

| Archivo | Propósito |
|---|---|
| `entity/EquipoEntity.java` | Las 18 columnas y tres relaciones LAZY |
| `domain/Equipo.java` | Modelo sin JPA |
| `domain/EstadoEquipo.java` | Cuatro valores reales del CHECK |
| `dto/request/CreateEquipoRequest.java` | Contrato de alta y validación |
| `dto/request/UpdateEquipoRequest.java` | Reemplazo de campos editables |
| `dto/response/EquipoResponse.java` | Recurso público con resúmenes |
| `dto/response/SubcategoriaEquipoResponse.java` | ID/nombre de clasificación |
| `dto/response/LaboratorioEquipoResponse.java` | ID/código/nombre de ubicación |
| `dto/response/ResponsableEquipoResponse.java` | Datos públicos del custodio |
| `mapper/EquipoMapper.java` | Conversiones y copia controlada |
| `repository/EquipoRepository.java` | Listados JPQL, filtros, duplicados y bloqueos |
| `service/EquipoService.java` | Reglas, transacciones, alcance y fechas |
| `controller/EquipoController.java` | Cinco endpoints del recurso |
| `controller/AdminEquipoController.java` | GET global que reutiliza lógica |
| `exception/CampoEquipoNoEditableException.java` | Error seguro de PUT no editable |

Pruebas nuevas, relativas a `backend/inventario/src/test/java/com/utec/inventario/`:

| Archivo | Cobertura |
|---|---|
| `mapper/EquipoMapperTest.java` | 3 pruebas: requests, copia, campos del servidor y resúmenes |
| `service/EquipoServiceTest.java` | 7 pruebas: reglas y alcance del servicio |
| `EquipoIntegrationTests.java` | 14 pruebas: HTTP, persistencia, filtros, roles y validaciones |
| `EquipoConcurrenciaTests.java` | 9 invocaciones: padres, baja, duplicados y revocación de alcance |

Documentos nuevos: `docs/sprints/sprint-5-equipos.md` y
`docs/sprints/sprint-5.md`.

## 41. Archivos modificados

SubcategoriaService y LaboratorioService incorporan la existencia de Equipos
no BAJA; SecurityConfig declara los seis endpoints; GlobalExceptionHandler
traduce los tres UNIQUE y el error específico de campos inmutables. Las consultas
de Usuario proveen datos públicos y coordinan bloqueos, sin contraseñas/hashes.
LaboratorioRepository agrega la búsqueda bloqueante del laboratorio existente
para PUT, sin cambiar su ubicación. Pruebas anteriores relacionadas se adaptan
manteniendo sus regresiones: SubcategoriaServiceTest y LaboratorioServiceTest.
Cada una incorpora una comprobación nueva de la regla de Equipos no BAJA.

README, reglas RN-37–41, matriz y los tres Markdown ERD reflejan el estado actual.
El lógico cambia el color de Equipo y su SVG se regenera; no cambian las
relaciones ni el modelo físico. Las guías históricas de Sprint 4 conservan los
resultados de su cierre. La lista exacta de rutas y motivos está en el
[reporte, sección 5](sprint-5.md#5-archivos-modificados). No se modifican
migraciones, DemoUsuariosConfig, lógica de login ni documentos históricos.

## 42. Pendientes de MovimientoEquipo

MovimientoEquipo, traslado, historial de movimientos, mantenimiento como entidad,
auditoría general, administración completa de usuarios, frontend y Docker.
El futuro traslado deberá autorizar origen/destino y registrar el movimiento
en la misma transacción; PUT de Equipo no puede saltarse ese flujo.
Sprint 5 termina con Equipo y no implementa esos endpoints futuros.
