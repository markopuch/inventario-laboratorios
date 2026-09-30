# Endpoints del backend — cierre Sprint 7

Inventario contrastado con los 14 controladores, `SecurityConfig`, los DTOs y servicios del backend. Hay **42 operaciones implementadas** en nueve grupos. Las variantes de rol y filtros de Postman son ejemplos de estas mismas operaciones, no endpoints adicionales.

Base local: `http://localhost:8080`. Los cuerpos son JSON y requieren `Content-Type: application/json`. Todas las rutas salvo el login requieren `Authorization: Bearer <token>`.

## Cómo leer los contratos

- A = ADMIN; G = GESTOR; L = LECTOR. El rol, usuario activo y alcance se comprueban en el servidor; no los concede un campo enviado por el cliente.
- `—` significa sin cuerpo. `Lista<X>` es un arreglo JSON, no un objeto paginado. Una lista válida sin resultados devuelve `200 []`.
- Los POST de catálogos y equipos devuelven `201`, su respuesta JSON y la cabecera `Location`. Los DELETE son bajas lógicas con `204` sin cuerpo.
- Los PUT reemplazan los campos editables: un opcional omitido o nulo se limpia. No son PATCH.
- Errores comunes a las rutas protegidas: **401** por ausencia de JWT, token inválido/vencido o usuario/rol inactivo; **403** por rol o alcance insuficiente; **400** por JSON, tipo de parámetro o validación incorrectos. Los errores de negocio propios de cada operación aparecen en las tablas.
- Un ID no entero produce 400. Los IDs de rutas modernas son positivos. Categoría conserva el comportamiento anterior: un ID entero inexistente, incluido 0 o negativo, produce 404.
- Las respuestas usan DTOs públicos; no devuelven entidades JPA, hashes ni contraseñas. `fechaCreacion`, `fechaActualizacion` y fechas de movimientos son valores del servidor en formato ISO 8601 con zona horaria.
- No existen endpoints de registro de usuarios, cambio de rol, recuperación de contraseña, refresh token, reactivación de catálogos ni borrado físico.

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

## 2. CATEGORIA — 6 operaciones

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

## 3. SUBCATEGORIA — 5 operaciones

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/subcategorias` | Listar activas, global | A/G/L | — | 200 `Lista<SubcategoriaResponse>` | — | IMPLEMENTADO |
| GET | `/api/subcategorias/{id}` | Consultar activa, global | A/G/L | — | 200 `SubcategoriaResponse` | 404 inexistente/inactiva | IMPLEMENTADO |
| POST | `/api/subcategorias` | Crear bajo categoría activa | A | `CreateSubcategoriaRequest` | 201 `SubcategoriaResponse` | 404 padre inexistente; 409 padre inactivo/duplicado | IMPLEMENTADO |
| PUT | `/api/subcategorias/{id}` | Editar y, si se solicita, cambiar categoría | A | `UpdateSubcategoriaRequest` | 200 `SubcategoriaResponse` | 404 hija/padre inexistente o hija inactiva; 409 padre inactivo/duplicado | IMPLEMENTADO |
| DELETE | `/api/subcategorias/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactiva; 409 equipos no BAJA | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `descripcion` opcional hasta 255; `idCategoria` obligatorio y positivo. Nombre único dentro de la categoría sin distinguir mayúsculas, también para inactivas. El mismo nombre en otra categoría es válido.

`SubcategoriaResponse`: `id`, `nombre`, `descripcion`, `activo`, `fechaCreacion`, `categoria: {id,nombre}`.

## 4. SEDE — 6 operaciones

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

## 5. AREA — 6 operaciones

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

## 6. LAB — 5 operaciones

| Método | Ruta | Descripción / alcance | Roles | Request | Response / éxito | Errores propios | Estado |
|---|---|---|---|---|---|---|---|
| GET | `/api/laboratorios` | Listar activos, global, incluso para G/L sin asignaciones | A/G/L | — | 200 `Lista<LaboratorioResponse>` | — | IMPLEMENTADO |
| GET | `/api/laboratorios/{id}` | Consultar activo, global | A/G/L | — | 200 `LaboratorioResponse` | 404 inexistente/inactivo | IMPLEMENTADO |
| POST | `/api/laboratorios` | Crear bajo área activa | A | `CreateLaboratorioRequest` | 201 `LaboratorioResponse` | 404 área inexistente; 409 área inactiva/código reservado | IMPLEMENTADO |
| PUT | `/api/laboratorios/{id}` | Editar o cambiar área | A | `UpdateLaboratorioRequest` | 200 `LaboratorioResponse` | 404 laboratorio/área inexistente o laboratorio inactivo; 409 área inactiva/código reservado | IMPLEMENTADO |
| DELETE | `/api/laboratorios/{id}` | Baja lógica | A | — | 204 — | 404 inexistente/inactivo; 409 asignaciones activas o equipos no BAJA | IMPLEMENTADO |

Create/Update: `nombre` obligatorio hasta 100; `codigo` obligatorio hasta 30; `ubicacion` opcional hasta 200; `idArea` obligatorio y positivo. El código es único global sin distinguir mayúsculas, incluso para inactivos. Una asignación activa bloquea la baja aunque su usuario esté inactivo. Los movimientos históricos, por sí solos, no la bloquean.

`LaboratorioResponse`: `id`, `nombre`, `codigo`, `ubicacion`, `activo`, `fechaCreacion`, `area: {id,nombre}`.

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
| PUT | `/api/equipos/{id}` | Editar sin cambiar código ni laboratorio | A/G | `UpdateEquipoRequest` | 200 `EquipoResponse` | 404 equipo/referencias inexistentes; 409 referencia inactiva, duplicados o BAJA; 403 alcance | IMPLEMENTADO |
| DELETE | `/api/equipos/{id}` | Establecer BAJA sin borrar; comprobar alcance actual | A/G | — | 204 — | 404 inexistente; 409 ya BAJA; 403 alcance | IMPLEMENTADO |
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
| POST | `/api/equipos/{idEquipo}/traslados` | Cambiar laboratorio y registrar evento en una transacción; G requiere origen y destino autorizados | A/G | `TrasladarEquipoRequest` | 200 `TrasladoEquipoResponse` | 404 equipo/destino inexistente; 409 BAJA, mismo destino o destino inactivo; 403 alcance | IMPLEMENTADO |
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

## Uso guiado de Postman

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
