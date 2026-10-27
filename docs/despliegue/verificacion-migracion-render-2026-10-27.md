# Copia de PostgreSQL Windows a Render

**Fecha:** 27 de octubre de 2026, America/Lima. **Resultado SQL:** copia completa
confirmada, con las trece tablas iguales al respaldo actual de origen.
La restauración se ejecutó de **11:21:14 a 11:22:05 Lima**
(`16:21:14–16:22:05 UTC`). La salud, los logins con credenciales locales
existentes y las consultas autenticadas posteriores aprobaron; la salud de
Render quedó comprobada desde las **11:27:23 Lima**.

## Autorización, origen y destino

El usuario autorizó copiar la base local con sus equipos y usuarios a Render,
respaldar ambas instancias primero y sustituir los datos remotos, que confirmó
como prescindibles.

| Elemento | Instancia comprobada |
|---|---|
| Origen | PostgreSQL nativo de Windows, versión **18.6**, base `inventario_laboratorios`. Contiene la carga Excel y el mantenimiento posterior de las cuentas locales. |
| Destino | Servicio PostgreSQL de Render llamado **`miproyecto-db`**; el nombre SQL real de la base es **`inventario_laboratorios`**. Servidor **PostgreSQL 18.6, Debian**. |
| Host de destino | `dpg-db53rjd9fdbs73bh3u1g-a.oregon-postgres.render.com`. |
| Backend observado | Servicio web `srv-db60s7id0e5s73900jr0`; despliegue **`58ac8f6`**, estado Live, rama `main`, Auto-Deploy habilitado y Root Directory `backend/inventario`. |
| Docker local | Instancia independiente; no fue origen ni destino de esta copia. Conservó sus datos. |

Esta operación copia el estado de la base nativa respaldada. La
[carga inicial del Excel](../importacion-inventario-demo.md) fue una operación
anterior por API; su respaldo de precarga no se utilizó como origen actual.
Los 31 candidatos con estado sin confirmar continúan pendientes en el plan del
importador y no forman parte de los 258 equipos existentes.

## Respaldos anteriores al cambio

Ambos archivos se guardan fuera del repositorio, en la carpeta privada
`C:/Users/marko/Desktop/fullstackweb/RespaldosRender/20261027-6455357f/`.
Son archivos PostgreSQL **Custom**; contienen datos privados y no se incorporan
a Git. Los SHA-256 siguientes identifican archivos de respaldo, no contraseñas.

| Respaldo | Momento Lima | Tamaño | SHA-256 del archivo |
|---|---|---:|---|
| `render-before.backup` | 27 de octubre, **10:58:34** | 58 053 bytes | `3a94fa606dc98470ef5a3d89910c944d3d47c65d199714c29c2daa2beee37d2a` |
| `local-current.backup` | 27 de octubre, **11:07:07**; obtenido mediante pgAdmin | 74 236 bytes | `8c54d78bac6ddfe1c04d436b2866c7c3d732d5112c79f91f14d7fac0f7d3d7a9` |

Los dos respaldos se restauraron correctamente en un PostgreSQL **18.6**
temporal, exclusivo de verificación, con conexión loopback en **55433** y
autenticación SCRAM. Se inspeccionaron sus esquemas, conteos, restricciones,
secuencias e historial Flyway antes de modificar Render. Ese clúster temporal
quedó detenido y el puerto 55433 está libre. La revisión automática de ejecución
bloqueó el borrado recursivo de la carpeta de trabajo y el posterior intento
acotado de eliminar los archivos de conexión y SQL generado. Por ello se
conservan el clúster detenido y los archivos temporales privados fuera de Git;
su directorio tiene la herencia desactivada y acceso únicamente para el usuario
Windows actual, comprobado sin reglas adicionales. La limpieza sigue pendiente;
los dos respaldos originales deben conservarse.

## Procedimiento aplicado y primera guarda

Se pausó el servicio web del backend para evitar escrituras o peticiones en
espera sobre identificadores que iban a sustituirse. La restauración utilizó
`psql --single-transaction` y `ON_ERROR_STOP`, con esta secuencia:

1. Bloquear las trece tablas y comprobar que las huellas de los datos remotos
   seguían coincidiendo con el respaldo `render-before.backup`.
2. Ejecutar el SQL de restauración producido con `pg_restore --clean --if-exists
   --no-owner --no-acl`, sin crear otra base ni otro esquema. Las tablas y las
   once secuencias se eliminaron y recrearon dentro de la misma transacción;
   se evitaron propietarios y permisos técnicos locales incompatibles con Render.
3. Antes del COMMIT, exigir igualdad de las huellas completas de las trece
   tablas frente al respaldo actual de Windows y validar las secuencias.
4. Confirmar la transacción tras aprobar las aserciones y reanudar el backend.

El primer intento se detuvo **en la guarda anterior al DDL** por diferencias
de ordenación de cadenas entre la verificación temporal y Render. Se comprobó
que Render seguía coincidiendo con su respaldo anterior. Después se normalizó
la ordenación utilizada para calcular/comparar las huellas con `COLLATE "C"`;
el segundo intento completó las comprobaciones y confirmó la copia. Esta
normalización afecta al método de comparación; no cambia las reglas de
ordenación del esquema ni las migraciones aplicadas.

## Conteos acreditados por SQL

La columna final corresponde tanto al respaldo actual de origen como a Render
después de la restauración; sus huellas completas también coincidieron.

| Tabla | Render antes | Windows respaldado / Render después |
|---|---:|---:|
| `area` | 2 | 3 |
| `auditoria` | 5 | 351 |
| `categoria` | 3 | 25 |
| `equipo` | 1 | **258** |
| `flyway_schema_history` | 13 | 13 |
| `laboratorio` | 2 | 5 |
| `mantenimiento` | 0 | 0 |
| `movimiento_equipo` | 0 | 0 |
| `rol` | 3 | 3 |
| `sede` | 1 | 2 |
| `subcategoria` | 4 | 53 |
| `usuario` | 3 | **6** |
| `usuario_laboratorio` | 0 | **4** |

Las seis cuentas están activas: **tres ADMIN, dos GESTOR y un LECTOR**.
Se copiaron los datos existentes de usuarios y sus credenciales BCrypt, sin
restablecer contraseñas ni publicar sus valores. La comparación interna de
`usuario` incluyó también el contenido privado de las credenciales; este
documento no contiene hashes de contraseñas ni huellas de filas de usuarios.

## Integridad y alcance de la comprobación

| Comprobación | Resultado |
|---|---|
| Tablas | **13**: doce del dominio y `flyway_schema_history`. |
| Columnas del dominio | **98**, sin contar las columnas técnicas de Flyway. |
| Claves foráneas | **16**, válidas. |
| Secuencias | **11**, con próximo identificador válido respecto al máximo existente. |
| Flyway | **V1–V13**, trece entradas; mismos checksums que el origen. |
| Índices inválidos | **0**. |
| Datos de las trece tablas | Huellas completas iguales al respaldo actual de origen antes de confirmar. |
| Historia y relaciones | Se conservaron IDs, fechas, asignaciones y los **351 eventos de auditoría** existentes. |

El origen Windows y la base Docker permanecieron sin modificaciones por esta
operación. No se editaron las migraciones Flyway, los contratos de la API ni
el código de aplicación para realizar la copia.

## Salud, login y datos a través de la API publicada

Después de reanudar el backend, el arranque fue lento. Los logs confirmaron la
validación de las **trece migraciones** y que el esquema estaba actualizado.
Desde las **11:27:23 Lima** se comprobó salud **UP** en Render y se completaron
las siguientes solicitudes reales:

| Comprobación | Resultado |
|---|---|
| Login Jason ADMIN | **HTTP 200**, con su contraseña local existente. |
| Login Jorge GESTOR | **HTTP 200**, con su contraseña local existente. |
| Login Gabriela ADMIN | **HTTP 200**, con su contraseña local existente. |
| Alcance ADMIN | Global. |
| Alcance de Jorge | Un laboratorio, **L409**, y **56 equipos** visibles. |
| Usuarios consultados por ADMIN | **6**. |
| Equipos consultados por ADMIN | **258**. |
| Auditorías consultadas por ADMIN | **351**. |
| Reporte del inventario | Total de **258 equipos**. |
| GET autenticado vía Vercel `/api/admin/equipos` | **258 equipos**, confirmando consulta por el proxy publicado. |
| GET anónimo vía Vercel `/api/auth/me` | **HTTP 401**. |

Estas comprobaciones conservaron las credenciales copiadas y respetaron el
alcance del gestor. Se realizaron logins y consultas, **sin escrituras de
aplicación** ni creación de actividad para completar las pruebas.

Esta evidencia acredita **la restauración SQL y el acceso autenticado a sus
datos por la API publicada**, incluida la ruta del proxy Vercel. No se ejecutaron
Gradle, npm ni sus suites durante la transferencia; los resultados del
[cierre del 27](verificacion-cierre-2026-10-27.md) conservan su fecha y alcance.

Fuentes de continuidad: [memoria canónica](../../memory.md),
[backend y base de datos](../memory/backend-database.md),
[infraestructura](../memory/docker-deployment.md) y
[modelo V13](../Erd_actual/modelo-vigente-v13.md). El detalle operativo procede
de los respaldos privados, sus restauraciones de verificación, la sesión SQL
de transferencia y los metadatos del servicio Render observados durante esta tarea.

La [evidencia estructurada](evidencias/migracion-render-2026-10-27.json) conserva
conteos, comprobaciones, hashes de los archivos de respaldo y el estado real de
limpieza, sin contraseñas, JWT ni hashes de contraseñas. La captura del backend
reactivado se conserva junto a los respaldos privados.
