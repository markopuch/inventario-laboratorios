# Endpoints del backend — catálogo vigente V13

Revisión documental del **3 de octubre de 2026**, commit `e1ce75a`. Inventario contrastado con **19 Controllers**, SecurityConfig, DTOs y Services: **70 combinaciones método y ruta de aplicación**. Las 42 operaciones base de Sprint 7 permanecen; las 28 extensiones posteriores se documentan en secciones 10–14. Filtros y variantes de roles no aumentan el total. Actuator es infraestructura y no integra esas 70. [Evidencia vigente](../despliegue/verificacion-docker-actions-2026-10-03.md).

Base backend local: `http://localhost:8080`; en Docker, el frontend de `http://localhost:3000` envía las mismas rutas `/api` mediante Nginx. Los cuerpos son JSON y requieren `Content-Type: application/json`. Todas las rutas salvo el login requieren `Authorization: Bearer <token>`.

## Cómo leer los contratos

- A = ADMIN; G = GESTOR; L = LECTOR. El rol, usuario activo y alcance se comprueban en el servidor; no los concede un campo enviado por el cliente.
- `—` significa sin cuerpo. `Lista<X>` es un arreglo JSON, no un objeto paginado. Una lista válida sin resultados devuelve `200 []`.
- Los POST de catálogos y equipos devuelven `201`, su respuesta JSON y la cabecera `Location`. Los DELETE son bajas lógicas con `204` sin cuerpo.
- Los PUT reemplazan los campos editables: un opcional omitido o nulo se limpia. No son PATCH.
- Errores comunes a las rutas protegidas: **401** por ausencia de JWT, token inválido/vencido o usuario/rol inactivo; **403** por rol o alcance insuficiente; **400** por JSON, tipo de parámetro o validación incorrectos. Los errores de negocio propios de cada operación aparecen en las tablas.
- Un ID no entero produce 400. Los IDs de rutas modernas son positivos. Categoría conserva el comportamiento anterior: un ID entero inexistente, incluido 0 o negativo, produce 404.
- Las respuestas usan DTOs públicos; no devuelven entidades JPA, hashes ni contraseñas. `fechaCreacion`, `fechaActualizacion` y fechas de movimientos son valores del servidor en formato ISO 8601 con zona horaria.
- Existen alta administrativa de Usuario, cambio de rol/actividad/contraseña y reactivación de catálogos. No hay registro público, recuperación autónoma de contraseña, CRUD Rol, refresh token ni borrado físico de Equipo.

### Respuesta uniforme de error

```json
{
  "timestamp": "2026-09-21T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene campos inválidos.",
  "path": "/api/categorias",
  "errors": {"nombre": "El nombre es obligatorio"}
}
```

`errors` solo aparece cuando hay errores por campo. Los 409 explican el conflicto; los 500 usan un mensaje genérico sin SQL ni trazas. Los 405/415 se normalizan cuando la solicitud llega al controlador; una combinación método/ruta que Security no habilita puede ser rechazada antes con 401/403. No se debe interpretar cualquier ruta inexistente como un 404 garantizado.

## 1. AUTH — 3 operaciones

| Método | Ruta | Descripción y alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| POST | `/api/auth/login` | Autenticar y emitir JWT | Pública | `AuthRequest` | 200 `AuthResponse` | 400 validación; 401 credenciales o inactividad | IMPLEMENTADO |
| GET | `/api/auth/me` | Datos del principal autenticado | A/G/L | — | 200 `UsuarioResponse` | 404 si el usuario deja de existir/estar activo entre comprobaciones | IMPLEMENTADO |
| GET | `/api/auth/me/laboratorios` | Alcance efectivo del principal; no admite elegir otro usuario | A/G/L | — | 200 `AlcanceLaboratoriosResponse` | 403 si ya no hay acceso vigente al comprobar el servicio | IMPLEMENTADO |

`AuthRequest`: `userName` obligatorio, máximo 50; `password` obligatorio, máximo 72 caracteres. Además, el login rechaza contraseñas de más de 72 bytes UTF-8 con 401 para evitar el truncamiento de BCrypt. El nombre de usuario se normaliza con trim y minúsculas; la contraseña no se recorta.

```json
{"userName": "{{userNameAdmin}}", "password": "{{passwordAdmin}}"}
```

`AuthResponse`: `accessToken`, `tokenType` (`Bearer`), `expiresIn` (segundos), `usuario: UsuarioResponse`.

`UsuarioResponse`: `id`, `userName`, `nombre`, `apellido`, `email`, `rol`, `activo`, `fechaCreacion`.

`AlcanceLaboratoriosResponse`: `alcanceGlobal` y `laboratorios: [{id,codigo,nombre}]`. A recibe todos los laboratorios activos independientemente de sus asignaciones. G/L reciben únicamente asignaciones activas a laboratorios activos. Los cambios de rol, actividad o asignaciones se reflejan con el mismo token vigente.

## 2. CATEGORIA — 6 operaciones base; PATCH en sección 10

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/categorias` | Listar activas, global | A/G/L | — | 200 `Lista<CategoriaResponse>` | — | IMPLEMENTADO |
| GET | `/api/categorias/{id}` | Consultar una activa, global | A/G/L | — | 200 `CategoriaResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| POST | `/api/categorias` | Crear categoría | A | `CreateCategoriaRequest` | 201 `CategoriaResponse` | 409 nombre reservado | IMPLEMENTADO |
| PUT | `/api/categorias/{id}` | Reemplazar editables | A | `UpdateCategoriaRequest` | 200 `CategoriaResponse` | 404 inexistente/inactiva; 409 nombre reservado | IMPLEMENTADO |
| DELETE | `/api/categorias/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactiva; 409 subcategorías activas | IMPLEMENTADO |
| GET | `/api/categorias/{idCategoria}/subcategorias` | Hijas activas de padre activo, global | A/G/L | — | 200 `Lista<SubcategoriaResponse>` | 404 padre inexistente/inactivo | IMPLEMENTADO |

Create/Update: `nombre` obligatorio, máximo 100; `descripcion` opcional, máximo 255. Se recortan espacios. El nombre es único global sin distinguir mayúsculas, incluso después de la baja.

`CategoriaResponse`: `id`, `nombre`, `descripcion`, `activo`, `fechaCreacion`.

## 3. SUBCATEGORIA — 5 operaciones base; PATCH en sección 10

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/subcategorias` | Listar activas, global | A/G/L | — | 200 `Lista<SubcategoriaResponse>` | — | IMPLEMENTADO |
| GET | `/api/subcategorias/{id}` | Consultar activa, global | A/G/L | — | 200 `SubcategoriaResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| POST | `/api/subcategorias` | Crear bajo categoría activa | A | `CreateSubcategoriaRequest` | 201 `SubcategoriaResponse` | 404 padre inexistente; 409 padre inactivo/duplicado | IMPLEMENTADO |
| PUT | `/api/subcategorias/{id}` | Editar y, si se solicita, cambiar categoría | A | `UpdateSubcategoriaRequest` | 200 `SubcategoriaResponse` | 404 hija/padre inexistente o hija inactiva; 409 padre inactivo/duplicado | IMPLEMENTADO |
| DELETE | `/api/subcategorias/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactiva; 409 equipos no BAJA | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `descripcion` opcional hasta 255; `idCategoria` obligatorio y positivo. Nombre único dentro de la categoría sin distinguir mayúsculas, también para inactivas. El mismo nombre en otra categoría es válido.

`SubcategoriaResponse`: `id`, `nombre`, `descripcion`, `activo`, `fechaCreacion`, `categoria: {id,nombre}`.

## 4. SEDE — 6 operaciones base; PATCH en sección 10

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/sedes` | Listar activas, global | A/G/L | — | 200 `Lista<SedeResponse>` | — | IMPLEMENTADO |
| GET | `/api/sedes/{id}` | Consultar activa, global | A/G/L | — | 200 `SedeResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| POST | `/api/sedes` | Crear sede | A | `CreateSedeRequest` | 201 `SedeResponse` | — | IMPLEMENTADO |
| PUT | `/api/sedes/{id}` | Reemplazar editables | A | `UpdateSedeRequest` | 200 `SedeResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| DELETE | `/api/sedes/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactiva; 409 áreas activas | IMPLEMENTADO |
| GET | `/api/sedes/{idSede}/areas` | Hijas activas de sede activa, global | A/G/L | — | 200 `Lista<AreaResponse>` | 404 sede inexistente/inactiva | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `direccion` opcional hasta 200; `distrito` y `departamento` opcionales hasta 100. **Sede no tiene unicidad de nombre**.

`SedeResponse`: `id`, `nombre`, `direccion`, `distrito`, `departamento`, `activo`, `fechaCreacion`.

## 5. AREA — 6 operaciones base; PATCH en sección 10

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/areas` | Listar activas, global | A/G/L | — | 200 `Lista<AreaResponse>` | — | IMPLEMENTADO |
| GET | `/api/areas/{id}` | Consultar activa, global | A/G/L | — | 200 `AreaResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| POST | `/api/areas` | Crear bajo sede activa | A | `CreateAreaRequest` | 201 `AreaResponse` | 404 sede inexistente; 409 sede inactiva/duplicado | IMPLEMENTADO |
| PUT | `/api/areas/{id}` | Editar o cambiar sede | A | `UpdateAreaRequest` | 200 `AreaResponse` | 404 área/sede inexistente o área inactiva; 409 sede inactiva/duplicado | IMPLEMENTADO |
| DELETE | `/api/areas/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactiva; 409 laboratorios activos | IMPLEMENTADO |
| GET | `/api/areas/{idArea}/laboratorios` | Hijos activos de área activa, global | A/G/L | — | 200 `Lista<LaboratorioResponse>` | 404 área inexistente/inactiva | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `descripcion` opcional hasta 255; `idSede` obligatorio y positivo. Nombre único por sede sin distinguir mayúsculas, incluidas inactivas.

`AreaResponse`: `id`, `nombre`, `descripcion`, `activo`, `fechaCreacion`, `sede: {id,nombre}`.

## 6. LAB — 5 operaciones base; PATCH en sección 10

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/laboratorios` | Listar activos, global, incluso para G/L sin asignaciones | A/G/L | — | 200 `Lista<LaboratorioResponse>` | — | IMPLEMENTADO |
| GET | `/api/laboratorios/{id}` | Consultar activo, global | A/G/L | — | 200 `LaboratorioResponse` | 404 inexistente/inactivo | IMPLEMENTADO |
| POST | `/api/laboratorios` | Crear bajo área activa | A | `CreateLaboratorioRequest` | 201 `LaboratorioResponse` | 404 área inexistente; 409 área inactiva/código reservado | IMPLEMENTADO |
| PUT | `/api/laboratorios/{id}` | Editar o cambiar área | A | `UpdateLaboratorioRequest` | 200 `LaboratorioResponse` | 404 laboratorio/área inexistente o laboratorio inactivo; 409 área inactiva/código reservado | IMPLEMENTADO |
| DELETE | `/api/laboratorios/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactivo; 409 asignaciones activas o equipos no BAJA | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `codigo` obligatorio hasta 30; `ubicacion` opcional hasta 200; `idArea` obligatorio y positivo. El código es único global sin distinguir mayúsculas, incluso para inactivos. Una asignación activa bloquea la baja aunque su usuario esté inactivo. Los movimientos históricos, por sí solos, no la bloquean.

`LaboratorioResponse`: `id`, `nombre`, `codigo`, `ubicacion`, `activo`, `estadoOperativo`, `fechaCreacion`, `area: {id,nombre}`. Create admite estadoOperativo con valor predeterminado OPERATIVO; Update lo admite opcional y conserva el valor anterior si se omite. Valores: OPERATIVO o MANTENIMIENTO; es independiente de activo.

## 7. USUARIO_LABORATORIO — 2 operaciones administrativas

El GET del alcance propio está contado en AUTH; estas dos operaciones muestran/configuran **asignaciones explícitas**, no el alcance global especial de ADMIN.

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/admin/usuarios/{idUsuario}/laboratorios` | Asignaciones activas de un usuario existente, incluso inactivo | A | — | 200 `UsuarioLaboratoriosResponse` | 404 usuario inexistente | IMPLEMENTADO |
| PUT | `/api/admin/usuarios/{idUsuario}/laboratorios` | Reemplazar atómicamente todas sus asignaciones | A | `ActualizarLaboratoriosUsuarioRequest` | 200 `UsuarioLaboratoriosResponse` | 404 usuario/laboratorio inexistente; 409 laboratorio inactivo | IMPLEMENTADO |

```json
{"idsLaboratorio": [{{idLaboratorio}}, {{idLaboratorioDestino}}]}
```

La lista es obligatoria y cada elemento es entero positivo no nulo. `[]` quita todas las asignaciones; los duplicados se deduplican. La reactivación reutiliza la misma clave usuario/laboratorio y conserva la fecha original. Una referencia inválida no aplica un reemplazo parcial.

`UsuarioLaboratoriosResponse`: `usuario: {id,userName,nombre,apellido,rol}` y `laboratorios: [{id,codigo,nombre}]`.

## 8. EQUIPO — 6 operaciones

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/equipos` | Listar con filtros; A global, G/L según laboratorio actual autorizado | A/G/L | Filtros opcionales | 200 `Lista<EquipoResponse>` | 403 filtro laboratorio fuera de alcance; 404 laboratorio filtrado inexistente para A | IMPLEMENTADO |
| GET | `/api/equipos/{id}` | Detalle, incluido BAJA; mismo alcance | A/G/L | — | 200 `EquipoResponse` | 404 inexistente; 403 fuera de alcance | IMPLEMENTADO |
| POST | `/api/equipos` | Crear en laboratorio activo; G debe tener alcance | A/G | `CreateEquipoRequest` | 201 `EquipoResponse` | 404 referencias inexistentes; 409 referencias inactivas, duplicados o BAJA; 403 alcance | IMPLEMENTADO |
| PUT | `/api/equipos/{id}` | Editar sin cambiar código ni laboratorio | A/G | `UpdateEquipoRequest` | 200 `EquipoResponse` | 404 equipo/referencias inexistentes; 409 referencia inactiva, duplicados, BAJA o mantenimiento EN_PROCESO; 403 alcance | IMPLEMENTADO |
| DELETE | `/api/equipos/{id}` | Establecer BAJA sin borrar; comprobar alcance actual | A/G | — | 204 — | 404 inexistente; 409 ya BAJA o mantenimiento EN_PROCESO; 403 alcance | IMPLEMENTADO |
| GET | `/api/admin/equipos` | Alias de listado global con los mismos filtros | A | Filtros opcionales | 200 `Lista<EquipoResponse>` | 404 laboratorio filtrado inexistente | IMPLEMENTADO |

Filtros combinables: `estado` (`OPERATIVO`, `MANTENIMIENTO`, `INOPERATIVO`, `BAJA`), `idLaboratorio` positivo, `idSubcategoria` positivo y `requiereMantenimiento` booleano. Un filtro válido sin coincidencias devuelve `[]`; `idSubcategoria` inexistente no se valida como referencia, devuelve una lista vacía. G/L sin alcance obtienen `[]` si no fuerzan un laboratorio. ADMIN mantiene la lectura histórica aun cuando el laboratorio esté inactivo.

| Campo | Create | Update | Validación |
|---|---|---|---|
| `codigoInterno` | Obligatorio | No admitido | Texto no blanco, máximo 50; inmutable |
| `idLaboratorio` | Obligatorio | No admitido | Entero positivo; cambiar mediante traslado |
| `nombre` | Obligatorio | Obligatorio | Texto no blanco, máximo 150 |
| `estado` | Obligatorio | Obligatorio | Enum textual; BAJA solo mediante DELETE |
| `requiereMantenimiento` | Obligatorio | Obligatorio | Booleano no nulo |
| `idSubcategoria` | Obligatorio | Obligatorio | Entero positivo; subcategoría activa |
| `serieUtec`, `numeroSerie` | Opcionales | Opcionales | Máximo 100 cada uno |
| `marca`, `modelo` | Opcionales | Opcionales | Máximo 100 cada uno |
| `anio` | Opcional | Opcional | Entero entre 1900 y 2100 |
| `ordenCompra` | Opcional | Opcional | Máximo 50 |
| `ubicacionInterna` | Opcional | Opcional | Máximo 200 |
| `comentario` | Opcional | Opcional | Texto, sin límite `@Size` en DTO |
| `idResponsable` | Opcional | Opcional | Entero positivo si se envía; usuario existente |

Código, serie UTEC y número de serie son únicos **distinguiendo mayúsculas**, conforme a V3, incluidos equipos BAJA. Los textos opcionales vacíos se normalizan a null y admiten varios equipos sin serie. Un responsable nuevo debe estar activo; conservar el mismo responsable que luego quedó inactivo sí está permitido. Ser responsable no concede alcance.

PUT rechaza con 400 campos desconocidos o inmutables (`id`, `codigoInterno`, `idLaboratorio`, fechas, etc.). Un equipo BAJA sigue visible para quien tenga alcance, pero no acepta PUT, traslado ni otro DELETE.

`EquipoResponse`: `id`, `codigoInterno`, `serieUtec`, `numeroSerie`, `nombre`, `marca`, `modelo`, `estado`, `anio`, `ordenCompra`, `ubicacionInterna`, `comentario`, `requiereMantenimiento`, `fechaCreacion`, `fechaActualizacion`, `subcategoria: {id,nombre}`, `laboratorio: {id,codigo,nombre}`, `responsable: {id,userName,nombre,apellido}` o null.

## 9. MOVIMIENTO_EQUIPO — 3 operaciones

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| POST | `/api/equipos/{idEquipo}/traslados` | Cambiar laboratorio y registrar evento en una transacción; G requiere origen y destino autorizados | A/G | `TrasladarEquipoRequest` | 200 `TrasladoEquipoResponse` | 404 equipo/destino inexistente; 409 BAJA, mantenimiento EN_PROCESO, mismo destino o destino inactivo; 403 alcance | IMPLEMENTADO |
| GET | `/api/equipos/{idEquipo}/movimientos` | Historia de un equipo; A global, G/L según origen O destino autorizado | A/G/L | — | 200 `Lista<MovimientoEquipoResponse>` | 404 equipo inexistente; 403 fuera del alcance actual y sin historia visible | IMPLEMENTADO |
| GET | `/api/movimientos` | Historia global o filtrada; A global, G/L solo eventos visibles | A/G/L | `idLaboratorio` opcional positivo | 200 `Lista<MovimientoEquipoResponse>` | 403 laboratorio filtrado fuera de alcance; 404 filtro inexistente para A | IMPLEMENTADO |

```json
{
  "idLaboratorioDestino": {{idLaboratorioDestino}},
  "motivo": "Reubicación para práctica de laboratorio",
  "ubicacionInternaDestino": "Mesa 2"
}
```

Destino es obligatorio y positivo. Motivo es obligatorio, no blanco y máximo 500. Ubicación es opcional, máximo 200: omitida, nula o blanca limpia la ubicación anterior. Cualquier otro campo se rechaza con 400; equipo, origen real, actor, tipo `TRASLADO` y fecha los determina el servidor. Se conservan código, estado, subcategoría, responsable y fecha de creación. Si falla el evento, se revierte también la modificación del equipo.

`TrasladoEquipoResponse`: `equipo: EquipoResponse`, `movimiento: MovimientoEquipoResponse`.

`MovimientoEquipoResponse`: `id`, `tipoMovimiento`, `motivo`, `fechaMovimiento`, `equipo: {id,codigoInterno,nombre}`, `laboratorioOrigen: {id,codigo,nombre}` o null, `laboratorioDestino: {id,codigo,nombre}`, `actor: {id,userName,nombre,apellido}`. La lectura admite tipos históricos válidos de V3 además de TRASLADO y conserva registros legacy con origen nulo.

Reglas de historia:

- Se ordena por fecha descendente e ID descendente como desempate.
- El filtro de laboratorio compara **origen o destino** del evento; la visibilidad usa las asignaciones vigentes, no el rol/asignaciones que existían cuando ocurrió.
- Equipo actualmente dentro del alcance y sin historia visible: 200 `[]`.
- Equipo actualmente fuera pero con historia visible: 200, solo ese subconjunto. Fuera y sin historia visible: 403.
- ADMIN consulta historia incluso tras la baja del equipo/laboratorio o inactivación del actor. G/L necesitan laboratorios activos asignados para su alcance.
- No hay POST/PUT/DELETE directo de movimientos; el evento se genera únicamente al trasladar.

## Uso guiado de Postman — colección histórica Sprint 7

La colección JSON existente cubre los contratos del cierre Sprint 7. No fue ampliada en este paso de documentación y no se presenta como colección completa de V13. Para usuarios, PATCH de catálogos, mantenimiento, reportes y auditoría, usar las rutas/DTO vigentes de las secciones siguientes y credenciales privadas de una base de verificación.

### Importación de la colección base

Importar [la colección](postman/Inventario-Laboratorios.postman_collection.json) y [el environment](postman/Inventario-Laboratorios.postman_environment.json) mediante **Import** y seleccionar el environment `Inventario Laboratorios - Local`. La colección usa el [esquema oficial Postman Collection v2.1](https://schema.postman.com/collection/json/v2.1.0/draft-04/collection.json); el procedimiento de importación está descrito en la [documentación de Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/).

`baseUrl` comienza en `http://localhost:8080`; el backend y PostgreSQL deben estar iniciados. Los nombres iniciales son marko/aldo/romel. **Contraseñas, tokens e IDs se entregan vacíos**: completa las contraseñas de tu entorno local. No compartas/exportes después un environment con contraseñas o tokens completos. Las variables son el mecanismo estándar para [reutilizar valores entre solicitudes](https://learning.postman.com/docs/sending-requests/variables/variables/).

Los scripts guardan tokens e IDs solo cuando la respuesta tiene el código esperado. No imprimen secretos. `sufijo` se genera al primer envío y diferencia los nombres/códigos de las pruebas; para iniciar otro conjunto, vacía `sufijo` y los IDs antes de volver a crear registros. Si una creación devuelve 409, no supongas que su ID fue actualizado: corrige el nombre/código o inicia otro conjunto.

Secuencia sugerida, seleccionando solicitudes individuales:

1. **Auth:** ejecutar los tres login. Se guardan `tokenAdmin/Gestor/Lector` e `idUsuarioAdmin/Gestor/Lector`. Consultar `me` para comprobar cada rol.
2. Crear, sin ejecutar sus DELETE todavía: **Categoría → Subcategoría**; **Sede → Área → Laboratorio origen → Laboratorio destino**. Los POST guardan sus IDs.
3. **UsuarioLaboratorio:** asignar ambos laboratorios al Gestor y solo el origen al Lector. Esto **reemplaza** las asignaciones de esos usuarios: úsalo en tu entorno de pruebas y conserva su configuración previa si deseas restaurarla.
4. **Equipos:** crear equipo como Gestor en origen. Probar detalle, filtros, alias ADMIN y PUT. El ejemplo de escritura LECTOR debe devolver 403; la consulta sin JWT debe devolver 401.
5. **Movimientos:** trasladar como Gestor a destino. Consultar historial como Gestor y Lector, y filtrar por origen/destino. Lector conserva el evento visible por origen, aunque ya no pueda consultar el detalle actual del equipo en destino (403). Consultar `me/laboratorios` confirma que el traslado no cambió asignaciones.
6. Dar de baja el equipo **después** del traslado. Comprobar que ADMIN/GESTOR pueden consultarlo como BAJA y que el historial persiste. Para repetir un traslado se necesita otro equipo vigente o moverlo a un destino diferente antes de la baja.
7. Si deseas terminar el conjunto: vaciar asignaciones de Gestor y Lector, dar de baja Subcategoría y Categoría, ambos Laboratorios, Área y Sede. Los DELETE no borran físicamente: equipos y movimientos siguen registrados. Restaurar las asignaciones previas si se reemplazaron en el paso 3.

**No ejecutar el Runner completo sin seleccionar y ordenar los casos.** Los DELETE están cerca del CRUD de cada recurso y los casos negativos requieren estados concretos; ejecutar toda la colección en el orden visual destruye precondiciones. Esta entrega verifica estructura JSON, esquema y correspondencia estática con las 42 rutas. No afirma haber ejecutado la colección en la aplicación Postman.

## Trazabilidad del inventario

Fuentes locales: `backend/inventario/src/main/java/com/utec/inventario/controller`, `config/SecurityConfig.java`, `security`, `dto/request`, `dto/response`, `service` y `exception/GlobalExceptionHandler.java`. El detalle de verificación automatizada y manual se mantiene en los demás documentos de este cierre; este archivo describe el contrato implementado.


## 10. Extensiones de CATÁLOGOS — 10 operaciones

Cada fila es una combinación real de método y ruta; todas están IMPLEMENTADAS
y requieren ADMIN. La consulta normal sigue devolviendo solo activos.
Los GET administrativos sin activo incluyen todos; `activo=true` o
`activo=false` filtra por actividad.

| Método | Ruta | Request / alcance | Response y HTTP | Errores propios |
|---|---|---|---|---|
| PATCH | `/api/categorias/{id}/estado` | `CambiarEstadoCatalogoRequest`: activo; global | 200 CategoriaResponse | 404 inexistente; 409 hijos activos al desactivar |
| PATCH | `/api/subcategorias/{id}/estado` | activo; global | 200 SubcategoriaResponse | 404 inexistente; 409 padre inactivo al activar o equipos no BAJA al desactivar |
| PATCH | `/api/sedes/{id}/estado` | activo; global | 200 SedeResponse | 404 inexistente; 409 áreas activas al desactivar |
| PATCH | `/api/areas/{id}/estado` | activo; global | 200 AreaResponse | 404 inexistente; 409 padre inactivo al activar o laboratorios activos al desactivar |
| PATCH | `/api/laboratorios/{id}/estado` | activo; global | 200 LaboratorioResponse | 404 inexistente; 409 padre inactivo al activar; equipos/asignaciones activos al desactivar |
| GET | `/api/admin/categorias` | query activo opcional; global | 200 Lista<CategoriaResponse> | 400 filtro inválido |
| GET | `/api/admin/subcategorias` | query activo opcional; global | 200 Lista<SubcategoriaResponse> | 400 filtro inválido |
| GET | `/api/admin/sedes` | query activo opcional; global | 200 Lista<SedeResponse> | 400 filtro inválido |
| GET | `/api/admin/areas` | query activo opcional; global | 200 Lista<AreaResponse> | 400 filtro inválido |
| GET | `/api/admin/laboratorios` | query activo opcional; global | 200 Lista<LaboratorioResponse> | 400 filtro inválido |

El cuerpo PATCH es `{"activo": true}` o `{"activo": false}`. No necesita
volver a enviar todo el catálogo. Los nombres/códigos reservados por la baja
siguen reservados al reactivar.

## 11. USUARIO ADMIN — 7 operaciones

ADMIN exclusivamente; alcance global. GET puede incluir usuarios inactivos.
No hay DELETE ni registro público. Todas las filas están IMPLEMENTADAS.

| Método | Ruta | Request principal | Response y HTTP | Errores propios |
|---|---|---|---|---|
| GET | `/api/admin/usuarios` | — | 200 Lista<AdminUsuarioResponse> | — |
| GET | `/api/admin/usuarios/{id}` | — | 200 AdminUsuarioResponse | 404 inexistente |
| POST | `/api/admin/usuarios` | CreateUsuarioRequest | 201 AdminUsuarioResponse + Location | 400 validación; 409 username/email reservado o rol no disponible |
| PUT | `/api/admin/usuarios/{id}` | UpdateUsuarioRequest | 200 AdminUsuarioResponse | 404 inexistente; 400 campo protegido; 409 email reservado |
| PATCH | `/api/admin/usuarios/{id}/estado` | CambiarEstadoUsuarioRequest: activo | 200 AdminUsuarioResponse | 404 inexistente; 409 último ADMIN activo |
| PATCH | `/api/admin/usuarios/{id}/rol` | CambiarRolUsuarioRequest: rol | 200 AdminUsuarioResponse | 404 inexistente; 409 rol no disponible o último ADMIN activo |
| PUT | `/api/admin/usuarios/{id}/password` | CambiarPasswordUsuarioRequest: password | 204 sin cuerpo | 404 inexistente; 400 contraseña inválida |

Create: userName ≤50, nombre/apellido ≤100, email válido ≤150, cargo opcional
≤100, password de 4–72 caracteres y máximo 72 bytes UTF-8, rol ADMIN/GESTOR/LECTOR,
activo booleano (predeterminado true). Username y email se normalizan a
minúsculas y trim; su unicidad incluye inactivos. Password no se recorta.

Update admite solo nombre, apellido, email y cargo. No cambia userName,
activo, rol ni password: esos cambios usan sus rutas específicas. Se rechazan
campos desconocidos/protegidos. No se admite crear un catálogo nuevo de roles.

AdminUsuarioResponse: id, userName, nombre, apellido, email, cargo, rol, activo,
fechaCreacion. No incluye passwordHash/password. El perfil de AUTH conserva
UsuarioResponse, sin cargo en ese contrato. GET/PUT de laboratorios del usuario
se cuentan separadamente en sección 7.

## 12. MANTENIMIENTO — 5 operaciones

Lectura A/G/L; escritura A/G. A global; G/L por laboratorio **actual** del
Equipo, a diferencia de los movimientos históricos. Todas IMPLEMENTADAS.

| Método | Ruta | Request principal | Response y HTTP | Errores propios |
|---|---|---|---|---|
| GET | `/api/mantenimientos` | FiltroMantenimientoRequest (query) | 200 Lista<MantenimientoResponse> | 403 filtro fuera de alcance; 404 Equipo/laboratorio filtrado inexistente; 400 rango/enum inválido |
| GET | `/api/mantenimientos/{id}` | — | 200 MantenimientoResponse | 404 inexistente; 403 Equipo fuera de alcance |
| POST | `/api/mantenimientos` | CreateMantenimientoRequest | 201 MantenimientoResponse + Location | 404 referencias; 409 Equipo BAJA/laboratorio inactivo/responsable nuevo inactivo |
| PUT | `/api/mantenimientos/{id}` | UpdateMantenimientoRequest | 200 MantenimientoResponse | 404 inexistente; 409 no PROGRAMADO o Equipo BAJA; 403 alcance |
| PATCH | `/api/mantenimientos/{id}/estado` | CambiarEstadoMantenimientoRequest | 200 MantenimientoResponse | 404 inexistente; 409 transición inválida/otro EN_PROCESO/incoherencia Equipo; 403 alcance |

Create: idEquipo positivo, tipo, descripcion no blanca ≤2000,
fechaProgramada (YYYY-MM-DD), idResponsable opcional positivo, observaciones
opcionales ≤4000. Tipo: PREVENTIVO/CORRECTIVO/CALIBRACION/OTRO.
Servidor fija PROGRAMADO, ID y fechas. Update admite tipo, descripcion,
fechaProgramada, idResponsable y observaciones; no cambia Equipo ni estado.

PATCH admite estado y observaciones opcionales. Ciclo:
PROGRAMADO → EN_PROCESO o CANCELADO; EN_PROCESO → COMPLETADO o CANCELADO.
Finales no se reabren. Empezar guarda estado anterior del Equipo y lo pone
en MANTENIMIENTO; finalizar/cancelar en proceso restaura el estado previo.
Mientras exista EN_PROCESO, PUT/DELETE/traslado de Equipo devuelve 409.

Filtros: idEquipo, idLaboratorio, estado, tipo, fechaDesde, fechaHasta.
Fecha programada es DATE; fechas de inicio/fin/creación/actualización son
TIMESTAMPTZ. MantenimientoResponse contiene id, equipo público, tipo,
descripcion, fechaProgramada, fechaInicio/Fin, responsable público o null,
estado, observaciones, fechaCreacion/Actualizacion. Estado anterior es interno.

## 13. REPORTES — 5 operaciones

Solo lectura A/G/L; A global, G/L con alcance aplicado en servidor.
No crean otra tabla. Todos IMPLEMENTADOS y responden 200.

| Método | Ruta | Request principal | Response principal |
|---|---|---|---|
| GET | `/api/reportes/resumen` | FiltroReporteRequest (query) | ResumenReporteResponse |
| GET | `/api/reportes/equipos/por-estado` | FiltroReporteRequest | Lista<ConteoReporteResponse> |
| GET | `/api/reportes/equipos/por-laboratorio` | FiltroReporteRequest | Lista<EquiposLaboratorioReporteResponse> |
| GET | `/api/reportes/movimientos` | FiltroReporteRequest | MovimientosReporteResponse |
| GET | `/api/reportes/mantenimientos` | FiltroReporteRequest | MantenimientosReporteResponse |

Filtros: idSede, idArea, idLaboratorio positivos; estado de Equipo,
estadoMantenimiento, tipoMantenimiento, fechaDesde/fechaHasta ISO DATE.
Cada reporte usa los filtros pertinentes a su conjunto de datos; fechas de
Equipo corresponden a creación, de Movimiento a movimiento y de Mantenimiento
a programación. El rango invertido devuelve 400; no se amplía alcance al
filtrar. Filtro explícito de laboratorio no autorizado devuelve 403;
laboratorio inexistente filtrado por ADMIN, 404. idSede/idArea inexistentes se aplican como filtro SQL y pueden devolver conteos/listas vacíos. Sin resultados se devuelven
conteos/listas vacíos según DTO, no información de otros laboratorios.

## 14. AUDITORÍA — 1 operación

| Método | Ruta | Roles / alcance | Request principal | Response y HTTP | Errores propios |
|---|---|---|---|---|---|
| GET | `/api/admin/auditoria` | A, global | FiltroAuditoriaRequest (query) | 200 Lista<AuditoriaResponse>, IMPLEMENTADO | 400 filtros/rango inválidos; 403 G/L |

Filtros: entidad, idEntidad, idUsuario (actor), accion, fechaDesde/fechaHasta.
Respuesta: id, idUsuarioActor, userNameActor, accion, entidad, idEntidad,
cambios y fecha. Actor procede del servidor; no existe POST/PUT/DELETE libre.
Los resúmenes no registran contraseñas, hashes, tokens o credenciales.

## 15. Recuento y fuente del catálogo

| Grupo | Combinaciones método+ruta |
|---|---:|
| AUTH | 3 |
| Categoría (incluye hijas) | 7 |
| Subcategoría | 6 |
| Sede (incluye áreas) | 7 |
| Área (incluye laboratorios) | 7 |
| Laboratorio | 6 |
| Catálogos ADMIN | 5 |
| Usuario ADMIN | 7 |
| UsuarioLaboratorio | 2 |
| Equipo (incluye listado ADMIN) | 6 |
| Movimiento/traslado/historia | 3 |
| Mantenimiento | 5 |
| Reportes | 5 |
| Auditoría | 1 |
| **Total aplicación** | **70** |

Fuente estática: [Controllers](../../backend/inventario/src/main/java/com/utec/inventario/controller/),
[DTO request](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/),
[DTO response](../../backend/inventario/src/main/java/com/utec/inventario/dto/response/) y
[SecurityConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java).
Comportamiento observado: [smoke Docker](../despliegue/verificacion-docker-actions-2026-10-03.md).
Las 94 comprobaciones HTTP del smoke incluyen varios escenarios para las mismas
rutas; no son 94 endpoints ni una nueva ejecución JUnit.
