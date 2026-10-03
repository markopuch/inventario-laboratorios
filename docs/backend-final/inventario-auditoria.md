# Inventario inicial y auditoría — Sprint 7

**Alcance histórico: Sprint 7, 21 de septiembre de 2026 (V1–V9).** Los conteos, referencias a Entities y conclusiones del cuerpo siguiente describen aquel corte; no son el inventario actual. El estado vigente e1ce75a es V1–V13, 12 entidades y 70 rutas. Consultar [modelo V13](../Erd_actual/modelo-vigente-v13.md), [endpoints actuales](endpoints.md) y [evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md). Esta actualización documental no repite la auditoría dinámica ni modifica migraciones.

## 1. Alcance y punto de partida

Acta de la fase de solo lectura, registrada antes de corregir documentación,
limpiar archivos o ejecutar la regresión de cierre. Referencia Git inicial:
`e41b53d`, árbol limpio, 218 archivos versionados. Se capturaron hashes de los
archivos y conteos/huellas públicas de la base habitual. No se mostraron secretos.

El cierre de Sprint 6 consta de 233 pruebas aprobadas, cero fallos, errores u
omitidas, comprobados también en los 33 XML JUnit existentes. Esa evidencia es
el punto de partida; la ejecución nueva se documenta en
[verificación final](verificacion-final.md).

Se inspeccionaron backend/inventario (configuración, Gradle, Java y pruebas),
README, docs y el script iniciar-backend.ps1. No existe una collection Postman
ni un script de verificación final al iniciar Sprint 7. El alcance no incorpora
funcionalidades de inventario, tablas, administración completa de usuarios,
mantenimiento, auditoría genérica, frontend ni contenedores.

## 2. Inventario de código

| Elemento | Cantidad / estado |
|---|---|
| Controllers | 14 |
| Services | 10 |
| Repositories | 10 |
| Mappers | 9 |
| Domain | 14 |
| Request DTO | 15 |
| Response DTO | 22 |
| Entities JPA | 10, más UsuarioLaboratorioId para la clave compuesta |
| Configuración Java | 3 clases |
| Seguridad | 4 clases |
| Excepciones y contrato de error | 6 clases |
| Archivos de tests | 33 |
| Operaciones HTTP | 42 |
| Migraciones | V1–V9 |

La arquitectura conserva Controller, DTO, Mapper, Domain, Service, Repository y
Entity. Los Controllers delegan negocio; ningún Mapper consulta Repository;
ningún DTO devuelve Entity o passwordHash. El actor de traslado se obtiene del
principal y la custodia no otorga alcance. Se revisaron JWT, SecurityConfig y
GlobalExceptionHandler, incluidas respuestas seguras de error.

## 3. Endpoints iniciales reales

Se cuentan combinaciones método+ruta; filtros no agregan endpoints. Las
consultas jerárquicas se agrupan con el padre de la ruta y alcance propio con AUTH.

| Grupo | Operaciones |
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
| **Total** | **42** |

Los contratos se desarrollan en [endpoints](endpoints.md). Categoría conserva
su contrato histórico: un ID entero inexistente, incluso no positivo, produce
404; los módulos que declaran Positive rechazan esos valores con 400. No se
modifica ese comportamiento únicamente por uniformidad.

## 4. PostgreSQL y Flyway

PostgreSQL inspeccionado: 18.6. Diez tablas del dominio, 76 columnas, diez PK
(nueve simples y una compuesta), trece FK, nueve restricciones UNIQUE y cuatro
CHECK. Además existen los índices UNIQUE funcionales de V5–V9. La tabla
flyway_schema_history es infraestructura, fuera del modelo de diez entidades.

| Tabla | Columnas |
|---|---:|
| rol | 5 |
| usuario | 10 |
| usuario_laboratorio | 4 |
| sede | 7 |
| area | 6 |
| laboratorio | 7 |
| categoria | 5 |
| subcategoria | 6 |
| equipo | 18 |
| movimiento_equipo | 8 |

Los checksums calculados sobre el contenido SQL coinciden con los almacenados
en la base habitual; todas las filas tienen success=true.

| Versión | Checksum |
|---|---:|
| V1 | -1224790997 |
| V2 | 930089448 |
| V3 | -889347868 |
| V4 | -30934170 |
| V5 | 1571040130 |
| V6 | 1971342152 |
| V7 | 932004306 |
| V8 | 2043234542 |
| V9 | -204147188 |

El cotejo Entity/Flyway no encontró diferencias de columnas, longitudes,
nulabilidad, fechas o relaciones que requieran migración. Hibernate usa
ddl-auto=validate; Flyway está habilitado y open-in-view=false.

## 5. Base habitual: registro antes de trabajar

| Tabla | Antes |
|---|---:|
| usuario | 3 |
| usuario_laboratorio | 0 |
| categoria | 2 |
| subcategoria | 4 |
| sede | 1 |
| area | 2 |
| laboratorio | 2 |
| equipo | 0 |
| movimiento_equipo | 0 |

Las cinco comprobaciones iniciales de consistencia devolvieron cero incidencias:
Equipos no BAJA bajo Subcategoría inactiva, Equipos no BAJA bajo Laboratorio
inactivo, asignaciones activas hacia Laboratorio inactivo, movimientos huérfanos
y traslados con origen igual a destino. No se corrigieron filas por SQL.

## 6. Hallazgos y decisiones

**Bugs funcionales:** ninguno demostrado en la auditoría inicial. No se agregan
pruebas por alcanzar una cantidad ni se cambia el estilo del curso.

**Documentación:** siete rutas locales rotas en guías históricas; guía JWT que
todavía presenta asignaciones/Equipo como pendientes; resumen de permisos de
usuarios que mezcla diseño y funciones disponibles; precisiones necesarias en
RN-01, RN-04, RN-26 y RN-27; README con verificación antigua y descripción de
paquetes vacíos; una nota de ERD sobre Equipo aún descrita como «sin traslado».
Se conserva la numeración RN y los hechos históricos de cada sprint.

**Limpieza segura:** once .gitkeep en paquetes ya poblados. Solo se eliminan
después de comprobar los archivos hermanos y ausencia de consumidores. No se
detectaron imports sin uso, TODO/FIXME ni clases/DTO abandonados demostrables.
Dos consultas de UsuarioLaboratorioRepository no tienen consumidores actuales;
se conservan porque no causan un defecto y no hace falta cambiar la API interna.

**Mejoras opcionales:** uniformar IDs inválidos de Categoría, paginación futura
y los módulos del [backlog](backlog.md). No se implementan para cerrar el backend.

## 7. Configuración y seguridad de la verificación

DB_URL, DB_USER, DB_PASSWORD y JWT_SECRET se resuelven por variables. Las
cuentas demo se crean solo con perfil dev y contraseña externa; no se actualizan
contraseñas existentes. El escaneo de archivos versionados no encontró la
credencial real de PostgreSQL ni asignaciones de secretos reales en configuración.
.env y configuración local están ignorados. La revisión final vuelve a verificar
estos puntos y los archivos Postman con tokens/contraseñas vacíos.

Toda escritura del cierre se ejecutará en inventario_verificacion_s7_cierre_*,
con una instancia de JAR controlada que se detendrá al terminar. La base habitual
se consulta únicamente para auditoría y comparación de preservación.
