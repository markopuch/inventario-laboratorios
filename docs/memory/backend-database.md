# Memoria técnica: backend y base de datos

Fecha de auditoría: **2026-10-26, America/Lima**. Documento local para continuidad de trabajo. Ruta del módulo: `backend/inventario`; paquete raíz `com.utec.inventario`. Se revisaron fuentes, DDL, configuración y artefactos existentes sin iniciar la aplicación, ejecutar migraciones ni repetir pruebas que escriben en PostgreSQL. No se incluyen valores de secretos ni registros personales.

## Arquitectura y estado comprobado

El backend es un **monolito REST por capas**, compilado como JAR ejecutable de Spring Boot con servidor Tomcat embebido. Spring Security autentica JWT y consulta usuarios/roles en PostgreSQL. Los controladores transforman DTO de entrada/salida mediante MapStruct; los servicios concentran transacciones y reglas; los repositorios JPA persisten entidades. Los reportes usan SQL parametrizado con `NamedParameterJdbcTemplate` para agregar datos en PostgreSQL. Flyway mantiene el esquema y Hibernate lo valida, sin generarlo.

Flujo habitual: `HTTP /api → JwtAuthFilter/SecurityConfig → controller → mapper/DTO/domain → service → repository → entity/PostgreSQL`. La autorización de recursos también se verifica dentro de servicios; no depende únicamente del frontend ni de un rol enviado por el cliente.

Interpretación de evidencia:

- **Declarado/verificado en fuente:** existe en el código o DDL actual revisado. No significa que una instancia desplegada lo ejecute.
- **Verificado en archivo local histórico:** se pudo leer el JAR o los reportes presentes; no se acredita que provengan del mismo commit que todo el árbol actual.
- **Verificado en sistema local hoy:** servicio PostgreSQL de Windows activo y puerto 5432 escuchando; no hubo autenticación SQL exitosa.
- **No verificado:** versión exacta del servidor PostgreSQL vivo, base seleccionada, migraciones aplicadas, filas actuales, estado de un backend en ejecución y correspondencia del JAR con un despliegue.

Hay **12 tablas del dominio, 16 FK y 98 columnas** en el modelo acumulado V1–V13; `flyway_schema_history` es infraestructura adicional. El [modelo vigente V13](../Erd_actual/modelo-vigente-v13.md) y los [ERD lógico](../Erd_actual/erd-logico-v3.md) y [físico](../Erd_actual/erd-fisico-v3.md) proporcionan detalle gráfico. La autoridad para cambios sigue siendo [DDL Flyway](../../backend/inventario/src/main/resources/db/migration/) y [entidades JPA](../../backend/inventario/src/main/java/com/utec/inventario/entity/).

## Versiones y dependencias

| Componente | Versión/evidencia | Alcance |
|---|---|---|
| Aplicación | `com.utec.inventario`, `0.0.1-SNAPSHOT` | Declarado en [build.gradle.kts](../../backend/inventario/build.gradle.kts) y manifest del JAR |
| Java | toolchain **21**; manifest `Build-Jdk-Spec: 21` | Requisito de compilación y JAR histórico; no acredita el JDK del proceso actual |
| Gradle Wrapper | **9.7.1** | [gradle-wrapper.properties](../../backend/inventario/gradle/wrapper/gradle-wrapper.properties) |
| Spring Boot | **4.1.1** | Plugin declarado y bibliotecas del JAR |
| Dependency Management plugin | **1.1.7** | Plugin declarado |
| MapStruct | **1.6.3** | Declarado y en JAR |
| JJWT | **0.13.0** (`api`, `impl`, `jackson`) | Declarado y en JAR |
| Lombok / binding | Lombok gestionado por dependencias Spring; binding **0.2.0** | `compileOnly`/annotation processors; no se asigna versión Lombok a partir de cachés |
| Spring Framework | **7.0.9** | `spring-core`, `spring-webmvc` del JAR histórico |
| Spring Security | **7.1.1** | `spring-security-core` del JAR histórico |
| Spring Data JPA | **4.1.1** | JAR histórico |
| Hibernate ORM / Validator | **7.4.5.Final / 9.1.3.Final** | JAR histórico |
| Jakarta Persistence API | **3.2.0** | JAR histórico |
| Flyway core / PostgreSQL extension | **12.4.0** | JAR histórico |
| JDBC PostgreSQL | **42.7.13** | JAR histórico; distinta de la versión del servidor |
| HikariCP | **7.0.2** | JAR histórico |
| Tomcat embebido | **11.0.24** | JAR histórico |
| Jackson | core/databind **3.1.5** y **2.21.5**; annotations **2.21** | Coexisten ambas líneas en el JAR; no reducirlo a una única versión |
| Micrometer | **1.17.1** | `micrometer-core` del JAR histórico |
| Cliente `psql` nativo | **18.6** | Ejecutado `psql --version` el 2026-10-26; no es prueba de versión del servidor |

Dependencias funcionales declaradas: starters de JPA, Flyway, Security, Validation, Web MVC y Actuator; driver PostgreSQL; MapStruct; Lombok; DevTools solo desarrollo. Pruebas con starters específicos de Spring Boot para JPA/Flyway/Security/Validation/Web MVC y plataforma JUnit; Gradle usa `useJUnitPlatform()`.

Artefacto leído: `backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar`, **63 246 614 bytes**, modificación registrada el **2026-10-03**. SHA-256: `001e0d7d0ed4003f89854dd5d79f86f616c5eec3813cf49e5c150560631f93e5`. Su manifest identifica `InventarioApplication` como clase de arranque. Las versiones transitivas anteriores corresponden a ese archivo, no a una resolución Gradle repetida hoy. El directorio `build/` puede estar ignorado y no existir en otra copia del repositorio.

## Organización interna

| Paquete | Responsabilidad |
|---|---|
| [controller](../../backend/inventario/src/main/java/com/utec/inventario/controller/) | REST, validación Jakarta, principal autenticado, códigos HTTP y `Location` en altas |
| [dto/request](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/) / [dto/response](../../backend/inventario/src/main/java/com/utec/inventario/dto/response/) | Contratos de API separados de entidades; filtros y comandos explícitos |
| [domain](../../backend/inventario/src/main/java/com/utec/inventario/domain/) | Objetos de dominio, enums de estados/roles, filtros y proyecciones de reportes |
| [mapper](../../backend/inventario/src/main/java/com/utec/inventario/mapper/) | MapStruct entre DTO, dominio y entidades; fuentes generadas en `build/generated/` |
| [service](../../backend/inventario/src/main/java/com/utec/inventario/service/) | Reglas, alcance de laboratorios, transacciones, bloqueos y auditoría |
| [repository](../../backend/inventario/src/main/java/com/utec/inventario/repository/) | Spring Data JPA, consultas/proyecciones y locks; `ReporteRepository` usa JDBC |
| [entity](../../backend/inventario/src/main/java/com/utec/inventario/entity/) | Modelo JPA del esquema PostgreSQL; PK compuesta para asignaciones |
| [security](../../backend/inventario/src/main/java/com/utec/inventario/security/) | JWT, `UserDetails`, validación de encabezado y errores de autenticación/autorización |
| [config](../../backend/inventario/src/main/java/com/utec/inventario/config/) | Cadena de seguridad, BCrypt/proveedor DAO y cuentas demo del perfil `dev` |
| [exception](../../backend/inventario/src/main/java/com/utec/inventario/exception/) | Error JSON uniforme y traducción de fallos de dominio/constraints |

Servicios principales: `CategoriaService`, `SubcategoriaService`, `SedeService`, `AreaService`, `LaboratorioService`, `UsuarioService`, `AdminUsuarioService`, `UsuarioLaboratorioService`, `AlcanceLaboratorioService`, `EquipoService`, `MovimientoEquipoService`, `MantenimientoService`, `ReporteService` y `AuditoriaService`. No hay una tabla/entidad independiente `reporte`.

## Configuración y seguridad

En [application.properties](../../backend/inventario/src/main/resources/application.properties): `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` son necesarios. `PORT` usa **8080** por defecto y `JWT_EXPIRATION_SECONDS` usa **1800 s**. `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false` y Flyway habilitado. Solo Actuator `health` está expuesto, con probes habilitadas y sin detalles públicos. La propiedad `DEMO_USER_PASSWORD` se requiere al activar el perfil `dev` mediante [application-dev.properties](../../backend/inventario/src/main/resources/application-dev.properties).

En [SecurityConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java): sesión `STATELESS`, sin formulario ni HTTP Basic; autenticación por `Authorization: Bearer ...`; CSRF deshabilitado porque la API no autentica mediante cookies. Las rutas se autorizan por método y patrón y se cierra con `anyRequest().denyAll()`. El filtro se registra solo en la cadena de Spring Security, no dos veces como filtro servlet. No se encontró configuración CORS habilitada en esta clase; las topologías que sirven frontend/API en orígenes distintos requieren comprobar el proxy y la política efectiva.

[JwtService](../../backend/inventario/src/main/java/com/utec/inventario/security/JwtService.java) firma **HS256**, valida issuer `inventario-laboratorios`, sujeto, ID, emisión y expiración; exige secreto Base64 con al menos 32 bytes. El JWT contiene ID y nombre de usuario, no constituye fuente de verdad del rol. [JwtAuthFilter](../../backend/inventario/src/main/java/com/utec/inventario/security/JwtAuthFilter.java) rechaza encabezados Authorization duplicados, esquema inválido y tokens con espacios; carga rol/estado desde PostgreSQL en cada solicitud y borra las credenciales del principal.

[UsuarioService](../../backend/inventario/src/main/java/com/utec/inventario/service/UsuarioService.java) normaliza el acceso y exige usuario y rol activos. [AppConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/AppConfig.java) usa BCrypt y `DaoAuthenticationProvider`. El login devuelve `accessToken`, `tokenType=Bearer`, `expiresIn` y `usuario`; contraseñas de más de 72 bytes UTF-8 se rechazan. La administración de contraseñas acepta 4–72 caracteres y como máximo 72 bytes UTF-8. No se observan endpoints de refresh token, recuperación de contraseña o revocación por lista de tokens.

Cambiar el rol o desactivar usuario/rol afecta solicitudes posteriores por la recarga de BD. **Restablecer la contraseña no invalida por sí mismo un JWT ya emitido**, porque el validador no incluye versión de contraseña/token; sigue sujeto a expiración, identidad y estado. Esto describe la implementación, no una decisión de producto nueva.

[DemoUsuariosConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/DemoUsuariosConfig.java) crea tres cuentas de demostración, una por rol, únicamente bajo `dev` y cuando no existen; no restablece contraseñas ni reactiva cuentas existentes. V4 no contiene usuarios ni credenciales. No activar `dev` indiscriminadamente sobre una base nueva de producción.

Roles y alcance efectivo, definidos por [AlcanceLaboratorioService](../../backend/inventario/src/main/java/com/utec/inventario/service/AlcanceLaboratorioService.java):

| Rol | Permisos y alcance |
|---|---|
| `ADMIN` | Administra usuarios, organización y catálogos; alcance global. Equipos/reportes mantienen consulta histórica incluso para laboratorios inactivos cuando corresponde. |
| `GESTOR` | Consulta y modifica equipos, traslados y mantenimientos dentro de laboratorios asignados activos. No administra organización/catálogos/usuarios. |
| `LECTOR` | Consulta equipos, movimientos, mantenimientos y reportes dentro de laboratorios asignados activos. Sin mutaciones. |

El alcance efectivo no administrativo necesita asignación activa y laboratorio activo. `estadoOperativo=MANTENIMIENTO` del laboratorio no elimina el alcance: es independiente de `activo`. Los catálogos generales consultables devuelven registros activos y no equivalen al alcance operativo; usar `/api/auth/me/laboratorios` para este último.

## Contrato HTTP por grupos

Las rutas llevan prefijo `/api` salvo salud. La matriz proviene de [controladores](../../backend/inventario/src/main/java/com/utec/inventario/controller/) y [SecurityConfig](../../backend/inventario/src/main/java/com/utec/inventario/config/SecurityConfig.java).

| Ruta | Métodos/operaciones | Acceso |
|---|---|---|
| `/actuator/health`, `/actuator/health/**` | GET salud/probes | Público |
| `/api/auth/login` | POST credenciales | Público |
| `/api/auth/me`, `/api/auth/me/laboratorios` | GET identidad / alcance efectivo | Tres roles |
| `/api/categorias`, `/api/subcategorias`, `/api/sedes`, `/api/areas`, `/api/laboratorios` | GET lista y GET `/{id}`; POST alta; PUT `/{id}`; PATCH `/{id}/estado`; DELETE `/{id}` baja lógica | GET tres roles; escrituras ADMIN |
| `/api/categorias/{idCategoria}/subcategorias`, `/api/sedes/{idSede}/areas`, `/api/areas/{idArea}/laboratorios` | GET hijos activos | Tres roles |
| `/api/admin/categorias`, `/subcategorias`, `/sedes`, `/areas`, `/laboratorios` bajo `/api/admin` | GET, filtro opcional `activo` para consultar también inactivos | ADMIN |
| `/api/admin/usuarios` y `/{id}` | GET lista/detalle; POST alta; PUT `/{id}`; PATCH `/{id}/estado`; PATCH `/{id}/rol`; PUT `/{id}/password` | ADMIN |
| `/api/admin/usuarios/{idUsuario}/laboratorios` | GET asignaciones; PUT reemplazo completo de IDs asignados | ADMIN |
| `/api/equipos`, `/api/equipos/{id}` | GET lista/detalle; POST alta; PUT actualización; DELETE baja | GET tres roles; escrituras ADMIN/GESTOR, con alcance |
| `/api/admin/equipos` | GET con los mismos filtros de equipos y consulta administrativa global | ADMIN |
| `/api/equipos/{idEquipo}/traslados` | POST traslado | ADMIN/GESTOR, con alcance de origen y destino para gestor |
| `/api/equipos/{idEquipo}/movimientos`, `/api/movimientos` | GET historial/lista | Tres roles, con visibilidad por origen o destino |
| `/api/mantenimientos`, `/api/mantenimientos/{id}` | GET lista/detalle; POST alta; PUT edición; PATCH `/{id}/estado` | GET tres roles; escrituras ADMIN/GESTOR, con alcance |
| `/api/reportes/resumen`, `/api/reportes/equipos/por-estado`, `/api/reportes/equipos/por-laboratorio`, `/api/reportes/movimientos`, `/api/reportes/mantenimientos` | GET agregados | Tres roles, filtrados por alcance |
| `/api/admin/auditoria` | GET eventos filtrados | ADMIN |

Filtros de equipos: `estado`, `idLaboratorio`, `idSubcategoria`, `requiereMantenimiento`. Movimientos: `idLaboratorio`. Mantenimientos: `idEquipo`, `idLaboratorio`, `estado`, `tipo`, `fechaDesde`, `fechaHasta`. Reportes: `idSede`, `idArea`, `idLaboratorio`, `estado`, `estadoMantenimiento`, `tipoMantenimiento`, `fechaDesde`, `fechaHasta`. Auditoría: `entidad`, `idEntidad`, `idUsuario`, `accion`, `fechaDesde`, `fechaHasta`. Las fechas de filtro usan ISO `YYYY-MM-DD` y validan orden del rango.

Éxitos habituales: GET/PUT/PATCH 200, altas 201 con `Location`, DELETE 204. [GlobalExceptionHandler](../../backend/inventario/src/main/java/com/utec/inventario/exception/GlobalExceptionHandler.java) devuelve `ApiError` con `timestamp` UTC, `status`, `error`, `message`, `path` y `errors` por campo cuando corresponde: validación/JSON 400, autenticación 401, permiso 403, inexistencia 404, conflicto 409, fallo inesperado 500 genérico. Traduce restricciones UNIQUE conocidas a 409, incluidas carreras concurrentes; no expone stacktrace al cliente.

## Modelo de datos acumulado

Las PK simples usan `SERIAL/INTEGER` y secuencia; las fechas con hora son `TIMESTAMPTZ`/`OffsetDateTime`. La asociación usa PK compuesta. Las FK tienen RESTRICT, excepto actor de auditoría con `ON DELETE SET NULL`. Las bajas funcionales conservan filas y relaciones.

| Tabla / Entity | Columnas principales y relaciones |
|---|---|
| `sede` / `SedeEntity` | `id_sede`, nombre, dirección, distrito, departamento, activo, fecha_creacion |
| `area` / `AreaEntity` | `id_area`, nombre, descripción, `id_sede`, activo, fecha_creacion; N:1 sede |
| `laboratorio` / `LaboratorioEntity` | `id_laboratorio`, nombre, código, ubicación, `id_area`, activo, fecha_creacion, estado_operativo; N:1 área |
| `categoria` / `CategoriaEntity` | `id_categoria`, nombre, descripción, activo, fecha_creacion |
| `subcategoria` / `SubcategoriaEntity` | `id_subcategoria`, nombre, descripción, `id_categoria`, activo, fecha_creacion; N:1 categoría |
| `rol` / `RolEntity` | `id_rol`, nombre, descripción, activo, fecha_creacion |
| `usuario` / `UsuarioEntity` | `id_usuario`, username, nombre, apellido, email, password_hash, cargo, activo, `id_rol`, fecha_creacion; N:1 rol. El hash se usa en autenticación y no se publica en DTO. |
| `usuario_laboratorio` / `UsuarioLaboratorioEntity` | PK `id_usuario + id_laboratorio`, activo, fecha_asignacion; materializa N:M usuario/laboratorio |
| `equipo` / `EquipoEntity` | `id_equipo`, código interno, serie UTEC, número de serie, nombre, marca, modelo, estado, año, orden de compra, ubicación interna, comentario, requiere_mantenimiento, `id_subcategoria`, `id_laboratorio`, `id_responsable` opcional, fechas de creación/actualización |
| `movimiento_equipo` / `MovimientoEquipoEntity` | `id_movimiento`, `id_equipo`, laboratorio origen opcional, laboratorio destino, usuario actor, tipo_movimiento, motivo, fecha_movimiento |
| `mantenimiento` / `MantenimientoEntity` | `id_mantenimiento`, `id_equipo`, tipo, descripción, fecha_programada DATE, fecha_inicio/fin, responsable opcional, estado, observaciones, estado_equipo_anterior, fechas de creación/actualización |
| `auditoria` / `AuditoriaEntity` | `id_auditoria`, usuario actor opcional, username_actor, acción, entidad, id_entidad, cambios, fecha. `entidad + id_entidad` es referencia lógica y no FK polimórfica. |

Unicidad relevante: nombre de categoría global IgnoreCase; nombre de subcategoría por categoría IgnoreCase; nombre de área por sede IgnoreCase; código de laboratorio global IgnoreCase; username y email IgnoreCase, todos incluidos los registros inactivos. Código interno, serie UTEC y número de serie de equipo tienen UNIQUE del DDL; no extrapolar IgnoreCase a estos últimos. Las dos series admiten NULL. Año de equipo está entre 1900–2100 o NULL.

### Historial Flyway en fuente

| Versión | Archivo | Efecto |
|---|---|---|
| V1 | [crear_organizacion_y_catalogos](../../backend/inventario/src/main/resources/db/migration/V1__crear_organizacion_y_catalogos.sql) | Crea sede, área, laboratorio, categoría, subcategoría; índices/FK iniciales |
| V2 | [crear_usuarios_y_seguridad](../../backend/inventario/src/main/resources/db/migration/V2__crear_usuarios_y_seguridad.sql) | Rol, usuario, usuario_laboratorio; asignación N:M con activo |
| V3 | [crear_equipos_y_movimientos](../../backend/inventario/src/main/resources/db/migration/V3__crear_equipos_y_movimientos.sql) | Equipo, movimiento_equipo, estados, UNIQUE, checks e índices |
| V4 | [insertar_datos_iniciales](../../backend/inventario/src/main/resources/db/migration/V4__insertar_datos_iniciales.sql) | Roles y catálogos ficticios de ejemplo; no usuarios ni credenciales |
| V5 | [categoria_nombre_unico_sin_mayusculas](../../backend/inventario/src/main/resources/db/migration/V5__categoria_nombre_unico_sin_mayusculas.sql) | Índice UNIQUE `UPPER(nombre)` de categoría |
| V6 | [agregar_username_usuario](../../backend/inventario/src/main/resources/db/migration/V6__agregar_username_usuario.sql) | Username obligatorio; backfill `usuario_` + ID; UNIQUE `UPPER(username)` |
| V7 | [subcategoria_nombre_unico_por_categoria](../../backend/inventario/src/main/resources/db/migration/V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql) | Detecta duplicados históricos y UNIQUE de categoría + UPPER(nombre) |
| V8 | [area_nombre_unico_por_sede](../../backend/inventario/src/main/resources/db/migration/V8__area_nombre_unico_por_sede_sin_mayusculas.sql) | Detecta duplicados y UNIQUE de sede + UPPER(nombre) |
| V9 | [laboratorio_codigo_unico](../../backend/inventario/src/main/resources/db/migration/V9__laboratorio_codigo_unico_sin_mayusculas.sql) | Detecta duplicados y UNIQUE global `UPPER(codigo)` |
| V10 | [estado_operativo_laboratorio](../../backend/inventario/src/main/resources/db/migration/V10__estado_operativo_laboratorio.sql) | estado_operativo OPERATIVO/MANTENIMIENTO, separado de activo |
| V11 | [administracion_usuarios](../../backend/inventario/src/main/resources/db/migration/V11__administracion_usuarios.sql) | Revisión de duplicados username/email e índice UNIQUE UPPER(email) |
| V12 | [crear_mantenimientos](../../backend/inventario/src/main/resources/db/migration/V12__crear_mantenimientos.sql) | Mantenimiento, ciclo/fechas/checks e índice UNIQUE parcial de un EN_PROCESO por equipo |
| V13 | [crear_auditoria](../../backend/inventario/src/main/resources/db/migration/V13__crear_auditoria.sql) | Auditoría, acciones/entidades válidas, FK opcional actor e índices |

No reescribir migraciones aplicadas. V7–V9 y V11 detienen la migración ante duplicados que requieren revisión; no borran ni fusionan automáticamente. V5 también depende de que no existan duplicados para poder crear su índice. No ejecutar `repair` para ocultar una discrepancia. La presencia de los archivos V1–V13 **no confirma su aplicación en la base viva actual**.

## Reglas de negocio que deben preservarse

**Organización/catálogos.** `activo` representa baja lógica; PATCH permite activar/desactivar. Los listados comunes/detalles trabajan con activos; listados `/api/admin/...` permiten ver ambos estados. Se exige padre activo para nuevas relaciones/activación. No se desactiva categoría con subcategorías activas, sede con áreas activas, área con laboratorios activos, subcategoría con equipos distintos de BAJA, ni laboratorio con asignaciones activas o equipos distintos de BAJA. Los nombres/códigos inactivos siguen reservados. Fuentes: [CategoriaService](../../backend/inventario/src/main/java/com/utec/inventario/service/CategoriaService.java), [SubcategoriaService](../../backend/inventario/src/main/java/com/utec/inventario/service/SubcategoriaService.java), [SedeService](../../backend/inventario/src/main/java/com/utec/inventario/service/SedeService.java), [AreaService](../../backend/inventario/src/main/java/com/utec/inventario/service/AreaService.java), [LaboratorioService](../../backend/inventario/src/main/java/com/utec/inventario/service/LaboratorioService.java).

**Usuarios.** Username/email se normalizan y son únicos incluso para usuarios inactivos; rol activo obligatorio. No se puede desactivar o degradar al último ADMIN activo. Las mutaciones administrativas serializan sobre el rol ADMIN y reconsultan permisos del actor tras obtener el bloqueo. El contrato no ofrece DELETE físico de usuario. [AdminUsuarioService](../../backend/inventario/src/main/java/com/utec/inventario/service/AdminUsuarioService.java).

**Asignaciones.** PUT reemplaza el conjunto completo, valida todos los destinos antes de modificar filas, reactiva/desactiva asociaciones existentes y conserva su historia; una lista vacía quita el alcance asignado. Bloquea primero usuario y después laboratorios ordenados por ID. Puede administrar asignaciones de usuario inactivo; estas no conceden acceso hasta que usuario/rol estén vigentes. [UsuarioLaboratorioService](../../backend/inventario/src/main/java/com/utec/inventario/service/UsuarioLaboratorioService.java).

**Equipos.** Estados OPERATIVO, MANTENIMIENTO, INOPERATIVO y BAJA. BAJA se establece solo por DELETE lógico y evita modificaciones, una segunda baja, traslado o nuevo/inicio de mantenimiento. El código interno y laboratorio no se editan por PUT; mover laboratorio requiere traslado. [UpdateEquipoRequest](../../backend/inventario/src/main/java/com/utec/inventario/dto/request/UpdateEquipoRequest.java) rechaza campos adicionales/protegidos en vez de ignorarlos. Alta requiere subcategoría y laboratorio activos. Responsable opcional; no se permite asignar un nuevo responsable inactivo aunque se puede conservar uno original que después quedó inactivo. Un mantenimiento EN_PROCESO bloquea edición, baja y traslado del equipo. `requiereMantenimiento` es bandera independiente; no asumir sincronización automática con todas las etapas del mantenimiento. [EquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/EquipoService.java).

**Traslados.** Destino distinto y activo, motivo obligatorio, actor real tomado de autenticación. Un gestor necesita acceso a origen y destino. Se bloquea actor → equipo → laboratorios por ID; se obtiene el origen real después de bloquear el equipo. Actualizar equipo e insertar movimiento ocurren en una transacción; si falla el historial se revierte la ubicación. Se permite al ADMIN rescatar historial/mover desde laboratorio inactivo; no asumir que todos los orígenes deben estar activos. Historial visible a no administradores cuando origen o destino pertenecen al alcance, incluso si el equipo ya salió del laboratorio; no se usa únicamente su ubicación actual. [MovimientoEquipoService](../../backend/inventario/src/main/java/com/utec/inventario/service/MovimientoEquipoService.java).

**Mantenimientos.** Tipos PREVENTIVO, CORRECTIVO, CALIBRACION y OTRO; estados PROGRAMADO, EN_PROCESO, COMPLETADO, CANCELADO. Alta siempre PROGRAMADO; edición de datos solo PROGRAMADO; transiciones permitidas PROGRAMADO → EN_PROCESO/CANCELADO y EN_PROCESO → COMPLETADO/CANCELADO. Al iniciar guarda estado previo del equipo y lo coloca en MANTENIMIENTO; al completar/cancelar desde EN_PROCESO restaura el estado previo. Una cancelación sin inicio guarda fecha_fin sin inventar fecha_inicio/estado previo. Solo un mantenimiento EN_PROCESO por equipo, reforzado por índice parcial. Fechas, observaciones ≤4000 y coherencia del ciclo tienen checks PostgreSQL. Bloquea equipo antes de mantenimiento, en el mismo protocolo que otras mutaciones. [MantenimientoService](../../backend/inventario/src/main/java/com/utec/inventario/service/MantenimientoService.java), [V12](../../backend/inventario/src/main/resources/db/migration/V12__crear_mantenimientos.sql).

**Reportes.** Agregan en SQL sin descargar todas las entidades; filtros parametrizados y alcance efectivo. Un usuario sin asignaciones obtiene ceros/listas vacías. Incluyen BAJA si no se filtra otro estado. Las fechas filtran creación de equipo, movimiento y fecha_programada de mantenimiento respectivamente; las fechas con hora se limitan en UTC, con día final inclusivo mediante límite exclusivo al día siguiente. Las agrupaciones de movimientos usan origen O destino; mantenimientos usan ubicación actual del equipo. Un filtro de estado de equipo afecta el estado **actual**, incluso al contar movimientos/mantenimientos históricos; no hay snapshot de todos los atributos históricos. [ReporteService](../../backend/inventario/src/main/java/com/utec/inventario/service/ReporteService.java), [ReporteRepository](../../backend/inventario/src/main/java/com/utec/inventario/repository/ReporteRepository.java).

**Auditoría.** Registro dentro de la misma transacción del cambio (`Propagation.MANDATORY`); un rollback no deja evento. Registra actor autenticado y resumen seleccionado; no recibe cuerpos HTTP completos ni contraseñas. Operaciones internas de arranque/fixtures sin principal autenticado no crean evento. Eventos ordenados por fecha e ID descendentes, filtrables por entidad/ID/actor/acción/fechas. No hay endpoint para insertar/borrar libremente auditoría. [AuditoriaService](../../backend/inventario/src/main/java/com/utec/inventario/service/AuditoriaService.java).

**Concurrencia.** Se combinan bloqueos pesimistas JPA, orden de adquisición estable y restricciones PostgreSQL. Mutaciones de equipos/traslados/mantenimientos toman un bloqueo de lectura sobre actor para coordinar revocación de asignaciones; modificaciones de asignaciones bloquean usuario en escritura. La baja de padres comparte bloqueos con creación de hijos. Conservar estos protocolos al refactorizar; una validación previa sin lock/constraint no reemplaza la protección concurrente.

## Pruebas y evidencia disponible

Los **40 XML locales** de `backend/inventario/build/test-results/test/` suman exactamente **319 pruebas, 0 fallos, 0 errores y 0 omitidas**. Esa ruta contiene artefactos locales ignorados por Git, ausentes en un clon nuevo. Las marcas internas de las suites son del **2026-10-03**, aproximadamente 18:04–18:05 UTC; no constituyen una ejecución nueva el 2026-10-26. No se observó en esos reportes una identificación que vincule automáticamente toda la ejecución al commit actual.

[Fuentes de pruebas](../../backend/inventario/src/test/java/com/utec/inventario/): integración de auth, administración de usuarios, estado de catálogos, organización, equipos, movimientos, mantenimientos, reportes/auditoría y asignaciones; concurrencia de categorías/subcategorías, organización, equipos, movimientos y usuario-laboratorio; unitarias de servicios, mappers, JWT, handler de errores e ID compuesto. [ReporteAuditoriaIntegrationTests](../../backend/inventario/src/test/java/com/utec/inventario/ReporteAuditoriaIntegrationTests.java) cubre agregación real, alcance, filtros, auditoría transaccional y ausencia de secretos en respuestas/eventos.

Las suites de integración/concurrencia usan PostgreSQL real, perfil `dev`, puerto HTTP aleatorio y una condición `DB_URL` que exige una base con prefijo `inventario_verificacion_`. Crean y eliminan fixtures mediante SQL; **sí escriben en BD**. El test `InventarioApplicationTests.contextLoads` carga el contexto y no tiene ese mismo guard de nombre. Por ello no se ejecutó `gradlew test` durante esta auditoría. Para repetirlas, usar una base de verificación dedicada y revisar primero las condiciones de cada suite y las variables; el simple nombre del task Gradle no garantiza lectura solamente.

La documentación [modelo V13](../Erd_actual/modelo-vigente-v13.md) conserva como evidencia histórica las 13 migraciones exitosas y checksums preservados de una comprobación Docker previa; eso no sustituye consultar `flyway_schema_history` en la instancia actual. Las fuentes no contienen marcadores TODO/FIXME/UnsupportedOperationException detectados en esta búsqueda; la ausencia de marcadores no equivale a ausencia de trabajo pendiente.

## Base de datos viva: comprobación del 2026-10-26 y pendientes

Se verificó servicio Windows **`postgresql-x64-18` en ejecución**, procesos `postgres` y escucha nativa en **5432**. El ejecutable `C:\Program Files\PostgreSQL\18\bin\psql.exe` informa cliente **18.6**. El nombre del servicio sugiere una instalación principal 18, pero no se presenta como `SELECT version()` exitoso.

Las variables `DB_URL`, `DB_USER`, `DB_PASSWORD` y `PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD` no estaban definidas en el entorno de la sesión auditora. El `.env` del backend contiene claves orientadas a Docker; se revisaron nombres de claves, sin volcar sus valores. Un intento único de conexión local sin prompt/contraseña disponible falló con código 2, antes de poder ejecutar la consulta de versión en transacción READ ONLY. No se insistió ni se probaron contraseñas.

Pendientes de comprobación, sin tratarlos como fallos confirmados del producto:

1. Identificar la base nativa realmente utilizada por desarrollo y consultar de forma autenticada y solo lectura versión de servidor, `flyway_schema_history`, esquema e indicadores agregados. Hoy no se certifican aplicación V13, checksums ni cantidad de registros vivos.
2. Vincular una próxima ejecución de pruebas y un JAR a un commit concreto; el JAR y los XML existentes son evidencia histórica del 3 de octubre.
3. Validar despliegue efectivo, variables de entorno y salud del backend en su plataforma; revisar la [memoria de infraestructura](./docker-deployment.md).
4. Revisar políticas de producto si se requieren invalidación inmediata de JWT al restablecer contraseña, refresh token o recuperaciones: no están implementadas en los endpoints revisados.
5. Si crecen datos, evaluar paginación: los listados revisados, incluidos movimientos/auditoría, devuelven listas completas. No atribuir paginación o exportación de reportes a la API actual.
6. Al trabajar con rangos de fechas desde interfaz peruana, preservar conscientemente la interpretación UTC de filtros con timestamp o cambiarla mediante una decisión y verificación explícitas.

Este documento no es un respaldo SQL ni exportación de datos. Es una memoria técnica reproducible desde fuentes y evidencia local. No incorpora valores de configuración privados, hashes de contraseñas, JWT ni contenido de filas de usuarios.
