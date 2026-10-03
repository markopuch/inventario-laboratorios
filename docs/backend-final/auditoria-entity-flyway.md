# Auditoría de Entity, Flyway y limpieza — Sprint 7

**Alcance histórico: Sprint 7, 21 de septiembre de 2026 (V1–V9).** Los conteos, referencias a Entities y conclusiones del cuerpo siguiente describen aquel corte; no son el inventario actual. El estado vigente e1ce75a es V1–V13, 12 entidades y 70 rutas. Consultar [modelo V13](../Erd_actual/modelo-vigente-v13.md), [endpoints actuales](endpoints.md) y [evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md). Esta actualización documental no repite la auditoría dinámica ni modifica migraciones.

## 1. Resultado y fuentes

El cotejo estático de las diez Entities con V1–V9 es conforme: **10 tablas,
76 columnas, 10 PK y 13 FK**. No se encontró una diferencia que requiera cambiar
Java o crear otra migración. La inspección de PostgreSQL real registrada en el
[inventario inicial](inventario-auditoria.md) coincide con estos conteos.
El resultado de arrancar, compilar y probar el cierre se registra por separado
en [verificación final](verificacion-final.md).

Fuentes de verdad:

- [V1: organización y catálogos](../../backend/inventario/src/main/resources/db/migration/V1__crear_organizacion_y_catalogos.sql).
- [V2: usuarios y seguridad](../../backend/inventario/src/main/resources/db/migration/V2__crear_usuarios_y_seguridad.sql).
- [V3: equipos y movimientos](../../backend/inventario/src/main/resources/db/migration/V3__crear_equipos_y_movimientos.sql).
- [V4: datos iniciales](../../backend/inventario/src/main/resources/db/migration/V4__insertar_datos_iniciales.sql).
- [V5: nombre de categoría](../../backend/inventario/src/main/resources/db/migration/V5__categoria_nombre_unico_sin_mayusculas.sql),
  [V6: username](../../backend/inventario/src/main/resources/db/migration/V6__agregar_username_usuario.sql),
  [V7: subcategoría](../../backend/inventario/src/main/resources/db/migration/V7__subcategoria_nombre_unico_por_categoria_sin_mayusculas.sql),
  [V8: área](../../backend/inventario/src/main/resources/db/migration/V8__area_nombre_unico_por_sede_sin_mayusculas.sql) y
  [V9: código de laboratorio](../../backend/inventario/src/main/resources/db/migration/V9__laboratorio_codigo_unico_sin_mayusculas.sql).

`flyway_schema_history` es infraestructura y no integra las diez entidades del
dominio. Los ERD son representaciones del modelo; no sustituyen estas fuentes.

## 2. Convenciones del cotejo

`NN` significa `NOT NULL`; `NULL`, columna opcional. `PK`, `FK` y `UQ`
describen restricciones SQL existentes. `Integer` representa `INTEGER`; `String`
representa `VARCHAR(n)` o `TEXT`; `boolean`, `BOOLEAN`; y `OffsetDateTime`,
`TIMESTAMPTZ`. La longitud se comprueba contra `@Column(length = n)`.

Los nueve identificadores simples usan `SERIAL`: en PostgreSQL son columnas
`INTEGER` con secuencia y `DEFAULT nextval(...)`, no columnas declaradas con
`GENERATED ... AS IDENTITY`. JPA usa `@GeneratedValue(strategy = IDENTITY)` para
recuperar el identificador producido por el INSERT; esa estrategia es compatible
con el esquema existente y no lo modifica.

Las once columnas de fecha tienen `DEFAULT CURRENT_TIMESTAMP`. Las diez fechas
de creación/asignación/movimiento son `insertable = false, updatable = false`;
`Equipo.fechaActualizacion` es `insertable = false` y permanece actualizable.
Todas tienen `@Generated(event = INSERT)` para recuperar el valor inicial de la
base. No existe un trigger de actualización de Equipo: PUT, baja y traslado
establecen su nueva fecha desde el Service con `OffsetDateTime.now(UTC)`.

Todas las relaciones `ManyToOne` son LAZY y no tienen cascada de borrado ni
colecciones bidireccionales. En las tablas las trece FK usan tanto
`ON UPDATE RESTRICT` como `ON DELETE RESTRICT`. Flyway define las restricciones;
Hibernate se limita a `ddl-auto=validate`. Los UNIQUE compuestos, índices
funcionales y CHECK no necesitan duplicarse como anotaciones que generen DDL.

## 3. Las diez entidades

### 3.1 Sede — V1, siete columnas

[SedeEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/SedeEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_sede | INTEGER NN | `idSede: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String` |
| direccion | VARCHAR(200) NULL | `direccion: String` |
| distrito | VARCHAR(100) NULL | `distrito: String` |
| departamento | VARCHAR(100) NULL | `departamento: String` |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

Sin FK ni unicidad del nombre. No se añade una regla de nombre único inexistente.

### 3.2 Area — V1 y V8, seis columnas

[AreaEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/AreaEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_area | INTEGER NN | `idArea: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String`, parte de UQ por sede |
| descripcion | VARCHAR(255) NULL | `descripcion: String` |
| id_sede | INTEGER NN | `sede: SedeEntity`, FK a sede.id_sede |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

V1 conserva `UNIQUE(nombre, id_sede)`; V8 añade el índice UNIQUE sobre
`(id_sede, UPPER(nombre))`. Se valida la nulabilidad física mediante
`@JoinColumn(nullable = false)`.

### 3.3 Laboratorio — V1 y V9, siete columnas

[LaboratorioEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/LaboratorioEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_laboratorio | INTEGER NN | `idLaboratorio: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String` |
| codigo | VARCHAR(30) NN | `codigo: String`, UQ global |
| ubicacion | VARCHAR(200) NULL | `ubicacion: String` |
| id_area | INTEGER NN | `area: AreaEntity`, FK a area.id_area |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

V1 conserva `UNIQUE(codigo)` y V9 añade `UNIQUE(UPPER(codigo))`. Las referencias
históricas a un laboratorio inactivo siguen siendo válidas.

### 3.4 Categoria — V1 y V5, cinco columnas

[CategoriaEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/CategoriaEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_categoria | INTEGER NN | `idCategoria: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String`, UQ global |
| descripcion | VARCHAR(255) NULL | `descripcion: String` |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

V1 conserva `UNIQUE(nombre)` y V5 añade `UNIQUE(UPPER(nombre))`. Los nombres
de categorías inactivas también permanecen reservados.

### 3.5 Subcategoria — V1 y V7, seis columnas

[SubcategoriaEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/SubcategoriaEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_subcategoria | INTEGER NN | `idSubcategoria: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String`, parte de UQ por categoría |
| descripcion | VARCHAR(255) NULL | `descripcion: String` |
| id_categoria | INTEGER NN | `categoria: CategoriaEntity`, FK a categoria.id_categoria |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

V1 conserva `UNIQUE(nombre, id_categoria)`; V7 añade
`UNIQUE(id_categoria, UPPER(nombre))`.

### 3.6 Rol — V2, cinco columnas

[RolEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/RolEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_rol | INTEGER NN | `idRol: Integer`, PK, SERIAL |
| nombre | VARCHAR(50) NN | `nombre: String`, UQ |
| descripcion | VARCHAR(255) NULL | `descripcion: String` |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

V4 inserta ADMIN, GESTOR y LECTOR. Esos seeds no equivalen a un CHECK o ENUM
cerrado en PostgreSQL. La aplicación autoriza los tres roles conocidos.

### 3.7 Usuario — V2 y V6, diez columnas

[UsuarioEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/UsuarioEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_usuario | INTEGER NN | `idUsuario: Integer`, PK, SERIAL |
| nombre | VARCHAR(100) NN | `nombre: String` |
| apellido | VARCHAR(100) NN | `apellido: String` |
| email | VARCHAR(150) NN | `email: String`, UQ |
| password_hash | VARCHAR(255) NN | `passwordHash: String`, solo persistencia/autenticación |
| cargo | VARCHAR(100) NULL | `cargo: String` |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| id_rol | INTEGER NN | `rol: RolEntity`, FK a rol.id_rol |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |
| username | VARCHAR(50) NN | `userName: String`, índice UNIQUE de V6 sobre UPPER(username) |

El dominio público y los Response DTO no contienen `passwordHash`. Rol y
Usuario están implementados para JPA/JWT; eso no implica un CRUD administrativo
completo de usuarios o roles.

### 3.8 UsuarioLaboratorio — V2, cuatro columnas

[UsuarioLaboratorioEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/UsuarioLaboratorioEntity.java)
y [UsuarioLaboratorioId.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/UsuarioLaboratorioId.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_usuario | INTEGER NN | `id.idUsuario`, FK `usuario: UsuarioEntity`, parte de PK |
| id_laboratorio | INTEGER NN | `id.idLaboratorio`, FK `laboratorio: LaboratorioEntity`, parte de PK |
| activo | BOOLEAN NN | `activo: boolean`, DEFAULT TRUE |
| fecha_asignacion | TIMESTAMPTZ NN | `fechaAsignacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

`@EmbeddedId` y los dos `@MapsId` representan una sola PK compuesta
`(id_usuario, id_laboratorio)`; no duplican columnas. El ID implementa
`Serializable` y `equals/hashCode`. Reactivar una relación conserva la fila y
su fecha original. La PK ya cubre la búsqueda por su primera columna.

### 3.9 Equipo — V3, dieciocho columnas

[EquipoEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/EquipoEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_equipo | INTEGER NN | `idEquipo: Integer`, PK, SERIAL |
| codigo_interno | VARCHAR(50) NN | `codigoInterno: String`, UQ, updatable=false |
| serie_utec | VARCHAR(100) NULL | `serieUtec: String`, UQ |
| numero_serie | VARCHAR(100) NULL | `numeroSerie: String`, UQ |
| nombre | VARCHAR(150) NN | `nombre: String` |
| marca | VARCHAR(100) NULL | `marca: String` |
| modelo | VARCHAR(100) NULL | `modelo: String` |
| estado | VARCHAR(30) NN | `estado: EstadoEquipo`, EnumType.STRING, DEFAULT 'OPERATIVO' |
| anio | INTEGER NULL | `anio: Integer`, CHECK 1900–2100 o NULL |
| orden_compra | VARCHAR(50) NULL | `ordenCompra: String` |
| ubicacion_interna | VARCHAR(200) NULL | `ubicacionInterna: String` |
| comentario | TEXT NULL | `comentario: String`, columnDefinition=TEXT |
| requiere_mantenimiento | BOOLEAN NN | `requiereMantenimiento: boolean`, DEFAULT FALSE |
| id_subcategoria | INTEGER NN | `subcategoria: SubcategoriaEntity`, FK |
| id_laboratorio | INTEGER NN | `laboratorio: LaboratorioEntity`, FK |
| id_responsable | INTEGER NULL | `responsable: UsuarioEntity`, FK opcional |
| fecha_creacion | TIMESTAMPTZ NN | `fechaCreacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |
| fecha_actualizacion | TIMESTAMPTZ NN | `fechaActualizacion: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

El CHECK de estado admite OPERATIVO, MANTENIMIENTO, INOPERATIVO y BAJA,
coincidiendo con `EstadoEquipo`. No existe columna `activo` en Equipo.
Los tres UNIQUE son sensibles a mayúsculas y reservan valores también en BAJA;
las columnas opcionales admiten múltiples NULL según PostgreSQL.

La relación laboratorio debe ser actualizable por JPA para ejecutar un traslado.
Su protección en PUT reside en `UpdateEquipoRequest` y `EquipoMapper.copy`, que
rechazan/ignoran los campos protegidos. Solo `MovimientoEquipoService` modifica
el laboratorio de un Equipo existente desde la API.

### 3.10 MovimientoEquipo — V3, ocho columnas

[MovimientoEquipoEntity.java](../../backend/inventario/src/main/java/com/utec/inventario/entity/MovimientoEquipoEntity.java)

| Columna PostgreSQL | Tipo / nulabilidad | Campo Java y restricción/default |
|---|---|---|
| id_movimiento | INTEGER NN | `idMovimiento: Integer`, PK, SERIAL |
| id_equipo | INTEGER NN | `equipo: EquipoEntity`, FK |
| id_laboratorio_origen | INTEGER NULL | `laboratorioOrigen: LaboratorioEntity`, FK opcional |
| id_laboratorio_destino | INTEGER NN | `laboratorioDestino: LaboratorioEntity`, FK |
| id_usuario_actor | INTEGER NN | `usuarioActor: UsuarioEntity`, FK |
| tipo_movimiento | VARCHAR(30) NN | `tipoMovimiento: String`, CHECK BTRIM <> '' |
| motivo | VARCHAR(500) NN | `motivo: String`, CHECK BTRIM <> '' |
| fecha_movimiento | TIMESTAMPTZ NN | `fechaMovimiento: OffsetDateTime`, DEFAULT CURRENT_TIMESTAMP |

No hay DEFAULT de tipo ni ENUM SQL cerrado. `TipoMovimientoEquipo.TRASLADO`
define lo que escribe el caso de uso actual; la Entity y el Domain conservan
`String` para leer valores históricos válidos en V3. El origen nullable se
preserva en JPA, Mapper y consultas mediante LEFT JOIN. Motivo es VARCHAR(500),
no TEXT; el request tiene `@NotBlank` y `@Size(max = 500)`.

Todas las columnas de contenido del movimiento son `updatable = false`; no hay
endpoints para crear movimientos libremente, editarlos o borrarlos. La fecha
proviene de la base. `CURRENT_TIMESTAMP` representa el inicio de la transacción:
el orden del historial `fecha DESC, id DESC` no debe describirse como orden de
confirmación de transacciones concurrentes.

## 4. Relaciones, restricciones e índices

| Origen | Referencias FK reales |
|---|---|
| area | id_sede → sede.id_sede |
| laboratorio | id_area → area.id_area |
| subcategoria | id_categoria → categoria.id_categoria |
| usuario | id_rol → rol.id_rol |
| usuario_laboratorio | id_usuario → usuario.id_usuario; id_laboratorio → laboratorio.id_laboratorio |
| equipo | id_subcategoria → subcategoria.id_subcategoria; id_laboratorio → laboratorio.id_laboratorio; id_responsable → usuario.id_usuario |
| movimiento_equipo | id_equipo → equipo.id_equipo; id_laboratorio_origen → laboratorio.id_laboratorio; id_laboratorio_destino → laboratorio.id_laboratorio; id_usuario_actor → usuario.id_usuario |

Total: trece FK, todas RESTRICT en actualización y borrado físico. Hay doce
índices explícitos para FK: tres en V1, dos en V2 y siete en V3. La primera
columna de la PK de UsuarioLaboratorio cubre la FK restante.

Las nueve restricciones UNIQUE originales son: area(nombre,id_sede),
laboratorio(codigo), categoria(nombre), subcategoria(nombre,id_categoria),
rol(nombre), usuario(email) y los tres identificadores de Equipo. Los cinco
índices UNIQUE funcionales de V5–V9 se añaden a ellas, no las sustituyen.
Los cuatro CHECK están en Equipo (estado, año) y MovimientoEquipo (tipo y
motivo no vacíos). Consultar el [ERD físico](../Erd_actual/erd-fisico-v2.md) para nombres
SQL completos e índices individuales.

## 5. Arquitectura y reglas revisadas

Se revisaron 119 archivos Java: 14 Controllers, 10 Services, 10 Repositories,
9 Mappers, 14 tipos de Domain, 15 requests, 22 responses, 10 Entities y su ID
compuesto, además de configuración, seguridad, aplicación y excepciones.

Los DTO y Domain no importan Entities ni JPA; los responses no contienen hashes.
Los Mappers transforman datos sin consultar repositorios. Los Services concentran
alcance, reglas y transacciones. Los Controllers delegan estas reglas y forman
la respuesta HTTP; AuthController conserva la coordinación de autenticación y
la protección del límite de BCrypt. No se reorganizó ese patrón por estilo.

Se verificó el protocolo de escritura Usuario → Equipo → padres, con actor
FOR SHARE y laboratorios por ID ascendente en traslados. El reemplazo de
asignaciones adquiere Usuario FOR UPDATE antes de los laboratorios. Las bajas
de padres consultan existencia de hijos sin bloquearlos, evitando invertir el
orden. La lectura del historial aplica alcance en SQL sobre origen O destino,
con proyección pública de actores en lote. La atomicidad se conserva mediante
una transacción para UPDATE Equipo e INSERT Movimiento. La evidencia dinámica
de estas reglas se referencia en [verificación final](verificacion-final.md).

## 6. Limpieza realizada y evidencia

Después de guardar el inventario de solo lectura, se comprobó con `rg --files`
que cada paquete tenía archivos Java y con `rg -n --hidden --fixed-strings` que
ninguna ruta concreta de `.gitkeep` tenía consumidores en el repositorio.
La búsqueda general de `.gitkeep` solo encontró la descripción genérica del
README y el acta de inventario. Los once archivos tenían un byte.

Se resolvió cada ruta absoluta y se verificó que comenzaba dentro de la raíz
del workspace. Se eliminaron únicamente estos archivos mediante
`Remove-Item -LiteralPath`, sin borrado recursivo ni eliminación de carpetas:

| Ruta relativa eliminada | Archivos Java conservados en el paquete |
|---|---:|
| backend/inventario/src/main/java/com/utec/inventario/config/.gitkeep | 3 |
| backend/inventario/src/main/java/com/utec/inventario/controller/.gitkeep | 14 |
| backend/inventario/src/main/java/com/utec/inventario/domain/.gitkeep | 14 |
| backend/inventario/src/main/java/com/utec/inventario/dto/request/.gitkeep | 15 |
| backend/inventario/src/main/java/com/utec/inventario/dto/response/.gitkeep | 22 |
| backend/inventario/src/main/java/com/utec/inventario/entity/.gitkeep | 11 |
| backend/inventario/src/main/java/com/utec/inventario/exception/.gitkeep | 6 |
| backend/inventario/src/main/java/com/utec/inventario/mapper/.gitkeep | 9 |
| backend/inventario/src/main/java/com/utec/inventario/repository/.gitkeep | 10 |
| backend/inventario/src/main/java/com/utec/inventario/security/.gitkeep | 4 |
| backend/inventario/src/main/java/com/utec/inventario/service/.gitkeep | 10 |

No se detectaron imports sin uso, TODO/FIXME/HACK/XXX ni clases o DTO muertos
demostrables. Los DTO resumen tienen consumidores como campos de otros DTO;
MapStruct genera sus conversiones. Se conservan dos consultas sin consumidores
actuales de `UsuarioLaboratorioRepository` (líneas 31 y 34): listar asignaciones
por laboratorio y comprobar una asignación activa sin filtrar el laboratorio.
No causan un defecto y no se elimina API interna solo por estilo.

Esta limpieza no modificó código Java, dependencias, configuración, migraciones
ni datos. Las guías históricas se conservan y la documentación de cierre enlaza
la evidencia actual sin convertir este trabajo en otro sprint funcional.
