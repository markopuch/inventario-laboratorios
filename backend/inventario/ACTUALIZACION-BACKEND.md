# Ampliación del backend — 3 de octubre de 2026

**Estado de integración posterior:** esta ampliación ya está conectada al frontend
Jason y desplegada en Docker local con las imágenes `sha-e1ce75a`.
Flyway V1–V13 tiene `success=true`; los checksums V1–V9 y los registros habituales
se conservaron. El [reporte vigente de Docker y Actions](../../docs/despliegue/verificacion-docker-actions-2026-10-03.md)
documenta ambas publicaciones, 94 comprobaciones HTTP, 30 aserciones funcionales,
siete controles SQL y la eliminación del entorno temporal. Esas cifras no son
una nueva ejecución de la suite JUnit. La actualización de documentación no
modificó código, SQL ni configuración del backend.

Implementación de las características solicitadas para acompañar al frontend.
Los cambios están dentro de `backend/inventario`; no se edita el frontend.
Se conservan Spring Security/JWT, DTO → Mapper → Domain → Service → Repository
→ Entity, MapStruct, Lombok y PostgreSQL administrado mediante Flyway.

Los documentos de cierre Sprint 7 describen la entrega histórica V9/233 tests.
Esta ampliación posterior añade capacidades solicitadas expresamente; no
reemplaza ni renumera ese cierre.

## 1. Catálogos y estado operativo

Los GET normales de Categoría, Subcategoría, Sede, Área y Laboratorio conservan
la consulta de activos. Los siguientes GET son ADMIN y permiten consultar
todos los registros; `?activo=true` o `?activo=false` filtra su estado.

| Método | Ruta | Request | Éxito |
|---|---|---|---|
| GET | `/api/admin/categorias` | Filtro `activo` opcional | 200 lista |
| GET | `/api/admin/subcategorias` | Filtro `activo` opcional | 200 lista |
| GET | `/api/admin/sedes` | Filtro `activo` opcional | 200 lista |
| GET | `/api/admin/areas` | Filtro `activo` opcional | 200 lista |
| GET | `/api/admin/laboratorios` | Filtro `activo` opcional | 200 lista |
| PATCH | `/api/categorias/{id}/estado` | `{"activo":true}` | 200 recurso |
| PATCH | `/api/subcategorias/{id}/estado` | `{"activo":true}` | 200 recurso |
| PATCH | `/api/sedes/{id}/estado` | `{"activo":true}` | 200 recurso |
| PATCH | `/api/areas/{id}/estado` | `{"activo":true}` | 200 recurso |
| PATCH | `/api/laboratorios/{id}/estado` | `{"activo":true}` | 200 recurso |

Todos los PATCH son ADMIN. `false` aplica las mismas restricciones que DELETE;
`true` reactiva y exige padre activo para Subcategoría, Área y Laboratorio.
Repetir el mismo estado es idempotente. Desactivar padres con hijos activos,
subcategorías con equipos no BAJA, o laboratorios con equipos no BAJA/asignaciones
activas produce 409. La fila del recurso y el padre necesario se bloquean para
serializar reactivación frente a baja de su padre. DELETE continúa siendo baja lógica.

Laboratorio incorpora `estadoOperativo`: `OPERATIVO` o `MANTENIMIENTO`.
Es independiente de `activo`:

| activo | estadoOperativo | Presentación |
|---|---|---|
| true | OPERATIVO | Activo |
| true | MANTENIMIENTO | En mantenimiento |
| false | Cualquiera | Inactivo |

Se recibe en POST/PUT de Laboratorio y aparece en su respuesta completa.
POST sin ese campo usa OPERATIVO; PUT sin él conserva el valor anterior.
Cambiarlo no altera automáticamente equipos ni crea restricciones nuevas
sobre traslados: las reglas vigentes continúan usando `activo`.

Ejemplo PUT de laboratorio existente:

```json
{
  "codigo": "{{codigoLaboratorio}}",
  "nombre": "{{nombreLaboratorio}}",
  "ubicacion": "{{ubicacionLaboratorio}}",
  "idArea": {{idArea}},
  "estadoOperativo": "MANTENIMIENTO"
}
```

## 2. Usuarios administrativos

Todas las operaciones requieren ADMIN. No se crea registro público.

| Método | Ruta | Request principal | Éxito |
|---|---|---|---|
| GET | `/api/admin/usuarios` | — | 200 lista de activos e inactivos |
| GET | `/api/admin/usuarios/{id}` | — | 200 usuario |
| POST | `/api/admin/usuarios` | userName,nombre,apellido,email,cargo,password,rol,activo | 201 usuario + Location |
| PUT | `/api/admin/usuarios/{id}` | nombre,apellido,email,cargo | 200 usuario |
| PATCH | `/api/admin/usuarios/{id}/estado` | `{"activo":false}` | 200 usuario |
| PATCH | `/api/admin/usuarios/{id}/rol` | `{"rol":"GESTOR"}` | 200 usuario |
| PUT | `/api/admin/usuarios/{id}/password` | `{"password":"{{nuevaPassword}}"}` | 204 sin cuerpo |

La respuesta administrativa incluye cargo y datos públicos; no incluye password
ni passwordHash. El perfil/login mantiene su DTO anterior. `cargo` ya existía
en SQL/Entity, por lo que no se añade de nuevo a la tabla.

- Username se normaliza con trim/minúsculas y permanece inmutable.
- Email se normaliza con trim/minúsculas. Los duplicados, incluso inactivos,
  producen 409, sin distinguir mayúsculas/minúsculas.
- Rol admite ADMIN/GESTOR/LECTOR y debe existir y estar activo.
- Cargo es opcional, máximo 100. Nombre/apellido: obligatorios, máximo 100;
  username: máximo 50; email: obligatorio, válido y máximo 150.
- Password: obligatoria, entre 4 y 72 caracteres y máximo 72 bytes UTF-8.
  No se recorta. Se utiliza el PasswordEncoder existente (BCrypt).
- Los DTO rechazan hashes, IDs, username en edición y campos ajenos al contrato.
- La fila estable `rol.ADMIN` serializa las escrituras y la comprobación del
  último ADMIN activo. No se permite dejar el sistema sin ADMIN por desactivación
  o degradación, incluso con solicitudes concurrentes.
- El actor de la API se revalida bajo ese bloqueo: inactivo produce 401;
  degradado produce 403. Un autocambio válido no se rechaza posteriormente.
- Las asignaciones se conservan al desactivar; no conceden acceso al usuario inactivo.

Los endpoints existentes GET/PUT `/api/admin/usuarios/{idUsuario}/laboratorios`
continúan funcionando. JWT recarga usuario, rol y actividad desde la base:
un token existente de usuario inactivo deja de autorizar; un cambio de rol
se aplica a las siguientes solicitudes. Restablecer password no implementa
revocación adicional de tokens: permanece su validación/vencimiento vigente.

Ejemplo POST para Postman; las variables deben resolverse localmente:

```json
{
  "userName": "{{nuevoUserName}}",
  "nombre": "{{nombreUsuario}}",
  "apellido": "{{apellidoUsuario}}",
  "email": "{{emailUsuario}}",
  "cargo": "Responsable de inventario",
  "password": "{{passwordUsuario}}",
  "rol": "GESTOR",
  "activo": true
}
```

## 3. Mantenimientos

Nueva entidad Mantenimiento, distinta del campo estado de Equipo.
Tipos: PREVENTIVO, CORRECTIVO, CALIBRACION, OTRO.
Estados: PROGRAMADO, EN_PROCESO, COMPLETADO, CANCELADO.

| Método | Ruta | Request principal | Éxito |
|---|---|---|---|
| GET | `/api/mantenimientos` | Filtros opcionales | 200 lista |
| GET | `/api/mantenimientos/{id}` | — | 200 mantenimiento |
| POST | `/api/mantenimientos` | idEquipo,tipo,descripcion,fechaProgramada,idResponsable?,observaciones? | 201 + Location |
| PUT | `/api/mantenimientos/{id}` | tipo,descripcion,fechaProgramada,idResponsable?,observaciones? | 200 mantenimiento |
| PATCH | `/api/mantenimientos/{id}/estado` | estado,observaciones? | 200 mantenimiento |

ADMIN tiene alcance global; GESTOR consulta/escribe en sus laboratorios activos
asignados; LECTOR solo consulta dentro de ese alcance. Las consultas usan el
laboratorio actual del equipo. Ser responsable no concede permisos.

Filtros: `idEquipo`, `idLaboratorio`, `estado`, `tipo`, `fechaDesde`, `fechaHasta`.
Las fechas son `YYYY-MM-DD` e incluyen ambos extremos de `fechaProgramada`.
Un rango invertido es 400; un laboratorio explícito ajeno al alcance es 403.

Ejemplo POST:

```json
{
  "idEquipo": {{idEquipo}},
  "tipo": "PREVENTIVO",
  "descripcion": "Revisión y ajuste programados",
  "fechaProgramada": "{{fechaProgramada}}",
  "idResponsable": {{idResponsable}},
  "observaciones": "Revisión previa al siguiente taller"
}
```

Responsable es opcional, pero debe estar activo al asignarlo. Descripción es
obligatoria, máximo 2000; observaciones, máximo 4000. No se acepta equipo nuevo
en PUT, ni fechas de ejecución, estado inicial o actor del cliente.

```text
PROGRAMADO ──→ EN_PROCESO ──→ COMPLETADO
    │              │
    └─→ CANCELADO ←┘
```

- Programar no cambia el estado del equipo. Solo PROGRAMADO permite PUT.
- Iniciar exige equipo no BAJA y laboratorio activo; guarda el estado previo,
  fija fechaInicio y cambia Equipo a MANTENIMIENTO en la misma transacción.
- Solo puede existir un mantenimiento EN_PROCESO por equipo; hay bloqueo de
  Equipo y un índice UNIQUE parcial como protección adicional.
- Completar/cancelar uno en proceso fija fechaFin y restaura el estado anterior:
  OPERATIVO vuelve a OPERATIVO; INOPERATIVO permanece INOPERATIVO; un
  MANTENIMIENTO previo manual se conserva. No se supone que finalizar una
  tarea arregla automáticamente un equipo previamente inoperativo.
- Cancelar PROGRAMADO no modifica el equipo. Puede cerrarse esa programación
  aunque el equipo haya sido dado de baja después; no inicia trabajo en BAJA.
- COMPLETADO/CANCELADO son finales. Las transiciones no válidas producen 409.
- Con mantenimiento EN_PROCESO se rechazan PUT, baja y traslado de Equipo (409).
- La bandera requiereMantenimiento permanece como dato del inventario.

## 4. Reportes reales

Los tres roles tienen lectura, siempre con su alcance efectivo. No hay datos
de demostración ni cifras calculadas a partir de registros no autorizados.
Las agregaciones se ejecutan en PostgreSQL mediante Repository y parámetros
SQL enlazados; no se cargan hashes ni todas las entidades para contar.

| GET | Respuesta |
|---|---|
| `/api/reportes/resumen` | totalEquipos,totalMovimientos,totalMantenimientos,equiposPorEstado,mantenimientosPorEstado |
| `/api/reportes/equipos/por-estado` | `[{valor,total}]` |
| `/api/reportes/equipos/por-laboratorio` | `[{idLaboratorio,codigo,nombre,total}]` |
| `/api/reportes/movimientos` | `{total,porTipo:[{valor,total}]}` |
| `/api/reportes/mantenimientos` | `{total,porEstado:[{valor,total}],porTipo:[{valor,total}]}` |

Todas devuelven 200. Filtros comunes: `idSede`, `idArea`, `idLaboratorio`,
`estado` (estado actual de Equipo), `fechaDesde`, `fechaHasta`. Para el componente
de mantenimientos: `estadoMantenimiento` y `tipoMantenimiento`.

- Fechas de equipos: fechaCreacion; movimientos: fechaMovimiento;
  mantenimientos: fechaProgramada. Los timestamps se filtran por días UTC,
  desde inclusivo hasta el inicio del día posterior exclusivo.
- Movimiento es visible por origen O destino autorizado. Se cuenta una sola
  vez aunque ambos estén asignados; jerarquía/alcance deben coincidir en un
  mismo extremo autorizado. Origen histórico null se conserva.
- ADMIN incluye historia de laboratorios inactivos; los demás solo su alcance activo.
- Sin alcance/datos: totales cero y listas vacías. Los grupos sin registros se omiten.
- Un laboratorio explícito inexistente para ADMIN es 404; ajeno a GESTOR/LECTOR
  es 403. Filtros sin coincidencias devuelven cero, sin conceder alcance.

Ejemplo: `/api/reportes/resumen?idLaboratorio={{idLaboratorio}}&fechaDesde={{fechaDesde}}&fechaHasta={{fechaHasta}}`.

## 5. Auditoría

Nueva tabla/Entity Auditoria. Las acciones exitosas de la API autenticada guardan
actor del JWT, username público, acción, entidad, ID, fecha y cambios seleccionados.
Se registra creación/edición/estado de catálogos, usuarios, rol, restablecimiento
de contraseña, asignaciones, equipos, traslados y ciclo de mantenimiento.

La auditoría comparte la transacción: un rollback no deja evento. Un PATCH de
estado sin cambio no duplica eventos. No se registran cuerpos HTTP, contraseñas,
hashes, JWT, secretos, ni textos libres completos. El restablecimiento solo
indica «Contraseña restablecida». La actividad anterior a esta ampliación no
se inventa ni se rellena retroactivamente.

Solo ADMIN puede consultar `GET /api/admin/auditoria` (200 lista).
Filtros: `entidad`, `idEntidad`, `idUsuario`, `accion`, `fechaDesde`, `fechaHasta`.
La respuesta contiene id,idUsuarioActor,userNameActor,accion,entidad,idEntidad,
cambios,fecha. No hay API pública de alta/edición/borrado de auditoría.

El FK del actor usa ON DELETE SET NULL y conserva su username histórico;
no existe DELETE administrativo de usuarios. Esto permite además eliminar
fixtures SQL propios en bases temporales sin borrar la historia generada.
Arranque y llamadas internas sin contexto autenticado no se presentan como
acciones realizadas por un usuario de la API.

## 6. Flyway y ejecución

V1–V9 se conservan íntegramente. Nuevas migraciones:

| Versión | Propósito |
|---|---|
| V10 | estado_operativo de laboratorio, default OPERATIVO, NOT NULL y CHECK |
| V11 | prevalidación de duplicados e índice único de email sin mayúsculas |
| V12 | mantenimiento, PK/FK, tipos/estados/ciclo/fechas, índices y único EN_PROCESO |
| V13 | auditoría, PK/FK, acciones/entidades válidas e índices |

El índice de username ya existía desde V6 y no se recrea. V11 falla sin modificar
usuarios si descubre duplicados históricos. No se corrigen datos automáticamente.
Se mantiene `ddl-auto=validate`, `open-in-view=false`, configuración por entorno
y el PasswordEncoder/JWT actuales. No se añaden dependencias ni secretos al repositorio.

Para ejecutar el backend local desde la raíz del repositorio:

```powershell
.\iniciar-backend.ps1
```

Flyway aplica las migraciones pendientes al arrancar. Si otra instancia ocupa
8080, iniciar en otro puerto configurando PORT o detener/reiniciar la instancia
que se desea sustituir. Una imagen Docker ya construida requiere reconstruirse
para incluir este código; editar el repositorio no actualiza automáticamente
un contenedor existente.

## 7. Verificación y comprobación manual

Evidencia medida el 3 de octubre de 2026:

| Comprobación | Resultado |
|---|---|
| compileJava / compileTestJava | Correctos |
| Suite anterior | 233 pruebas conservadas |
| Nuevas pruebas | 86: 21 catálogos, 29 usuarios, 25 mantenimiento, 11 reportes/auditoría |
| Suite final | 319 aprobadas; 0 fallos, 0 errores, 0 omitidas; 40 suites JUnit |
| bootJar | Correcto; JAR ejecutable generado |
| Arranque JAR / health | Correcto; puerto temporal 55989; health UP |
| Flyway / Hibernate | 13 migraciones validadas; ddl-auto=validate correcto |
| Smoke del JAR | Login ADMIN/GESTOR, catálogos, usuario/asignaciones, Equipo GET/PUT, mantenimiento inicio/cierre, conflicto 409, traslado/historial, baja, resumen y auditoría correctos |
| SQL de consistencia | Cero equipos vigentes bajo padres inactivos, asignaciones activas a laboratorio inactivo, movimientos huérfanos, traslados con origen=destino o mantenimiento en proceso con equipo incoherente |
| Limpieza | Instancias Java propias detenidas, conexiones propias cerradas, base temporal eliminada y ausencia confirmada |
| Frontend / V1–V9 | Sin modificaciones |

Base temporal utilizada: `inventario_verificacion_caracteristicas_20261003_4c449e35`.
Tras la suite, los fixtures de las tablas originales se comprobaron limpios y
sus conteos volvieron a los datos iniciales. El smoke escribió únicamente en
esa base de verificación; después se eliminó la base completa. Ninguna prueba
con escritura se ejecutó en la habitual.

La primera regresión ejecutó 315 pruebas y mostró seis fallos de mantenimiento:
el flush de Equipo enviaba fechas de Mantenimiento antes de asignar su estado,
violando el CHECK del ciclo. Se corrigió la coherencia antes del flush, conservando
el CHECK y todas las aserciones. La revisión también detectó una petición ADMIN
que podía esperar el bloqueo y continuar tras revocarse su actividad/rol; se
revalidó el actor dentro de la transacción y se añadieron cuatro pruebas críticas.
La suite completa final se reejecutó con `--rerun-tasks` y pasó las 319.

### Base habitual local

Flyway aplicó V10–V13 a `localhost:5432/inventario_laboratorios` mediante el JAR,
sin perfil dev y sin solicitudes API con escritura. Se comprobó arranque,
health y Hibernate validate, y se detuvo la instancia propia.

| Tabla | Antes | Después |
|---|---:|---:|
| usuario | 3 | 3 |
| usuario_laboratorio | 0 | 0 |
| categoria | 2 | 2 |
| subcategoria | 4 | 4 |
| sede | 1 | 1 |
| area | 2 | 2 |
| laboratorio | 2 | 2 |
| equipo | 0 | 0 |
| movimiento_equipo | 0 | 0 |

Las nuevas tablas mantenimiento y auditoría quedan vacías. Los dos laboratorios
existentes reciben OPERATIVO por el default de V10. No se cambiaron cuentas,
contraseñas, roles ni asignaciones. Los nueve checksums anteriores se conservan
y las trece migraciones tienen success=true.

| Nueva versión | Checksum aplicado local y temporal |
|---|---:|
| V10 | 642141636 |
| V11 | -697692969 |
| V12 | 1666773476 |
| V13 | -182052760 |

Hay **70 operaciones HTTP de aplicación**, 42 anteriores y 28 nuevas, sin rutas
duplicadas. Health de Actuator no se incluye en ese conteo.

La ejecución Docker existente usa una base dentro de su contenedor, diferente
del PostgreSQL local anterior. No se reconstruyeron ni reemplazaron contenedores.
Su backend necesita una imagen construida con este código; al arrancarla Flyway
aplicará las migraciones en la base configurada para ese despliegue. La imagen
anterior no incorpora estos endpoints por editar el repositorio.

La suite se ejecuta con `verificar-backend.ps1` y DB_URL hacia una base
`inventario_verificacion_*`, nunca hacia la habitual. No se eliminan ni
deshabilitan pruebas anteriores. CompileJava, test y bootJar deben terminar
correctamente y sin omitidas.

Recorrido manual en Postman (usar una base de verificación para escribir):

1. Login ADMIN; guardar accessToken como variable local, sin exportarlo.
2. GET de cada catálogo normal y ADMIN. Dar baja a un registro sin hijos,
   reactivarlo con PATCH y confirmar que reaparece en el GET normal.
3. Intentar reactivar un hijo cuyo padre esté inactivo: esperar 409.
4. PUT de laboratorio con estadoOperativo MANTENIMIENTO; GET conserva activo=true.
5. Crear usuario GESTOR: 201; repetir username/email cambiando mayúsculas: 409.
6. PUT datos públicos: 200; incluir username/passwordHash: 400.
7. Cambiar rol/actividad, consultar perfil con su token anterior y comprobar
   los permisos nuevos; inactivo recibe 401. Intentar retirar al último ADMIN: 409.
8. Restablecer password: 204; login con antigua falla y con nueva funciona.
9. Programar mantenimiento: 201; iniciar: 200 y Equipo MANTENIMIENTO;
   segundo inicio del mismo equipo: 409. PUT/baja/traslado durante proceso: 409.
10. Completar/cancelar: 200, fechaFin e historia; estado previo del equipo restaurado.
11. Consultar los cinco reportes, filtrar por laboratorio/fecha y contrastar
    con los registros persistidos de ese alcance.
12. Consultar auditoría como ADMIN; repetir como GESTOR/LECTOR: 403.
    Comprobar actor, acción y ausencia de secretos. Una operación rechazada
    no debe generar evento.
13. Probar los endpoints nuevos sin token: 401; con LECTOR las escrituras: 403;
    con GESTOR un mantenimiento de laboratorio ajeno: 403.

HTTP: 400 formato/validación; 401 autenticación; 403 rol/alcance; 404 inexistencia;
409 duplicado, último ADMIN, padre/hijo o ciclo; 500 mensaje seguro de fallo inesperado.
