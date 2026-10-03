# Verificación final — Sprint 7 (registro histórico)

## Resultado medido

Verificación realizada el 21 de septiembre de 2026 (America/Lima).
Compilación y regresión: **233 aprobadas de 233**, cero fallos, errores u
omitidas, cero pruebas nuevas. Arranque real y smoke test correctos.
No se modificó código Java, migraciones, dependencias ni reglas de negocio.

**Referencia posterior, 3 de octubre de 2026:** el
[reporte Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
documenta la última versión e1ce75a integrada con Jason: 94/94 comprobaciones HTTP, 30 aserciones funcionales, siete controles SQL y ambas imágenes GHCR confirmadas. Flyway habitual quedó en V13. No se deben mezclar esos datos con la verificación anterior 9956939 (33 HTTP/24 aserciones). Las cifras siguientes conservan
la ejecución histórica de Sprint 7: no representan una nueva suite Gradle
durante la validación Docker ni durante la actualización documental.

La fase previa consta en [inventario de auditoría](inventario-auditoria.md).
El [reporte Sprint 7](../sprints_realizados-backend/sprint-7.md) reúne el cierre y los archivos.

## 1. Compilación y suite

Se ejecutó desde la raíz `verificar-backend.ps1`, que invoca:

```powershell
.\gradlew.bat compileJava test bootJar --rerun-tasks --no-daemon --console=plain
```

La invocación Gradle se realiza dentro de `backend/inventario`.
Resultado: **BUILD SUCCESSFUL en 1 min 34 s**, seis tareas realmente ejecutadas.
No se usó caché como sustituto de una ejecución de pruebas.

| Medición | Resultado |
|---|---:|
| Pruebas anteriores conservadas | 233 |
| Pruebas nuevas | 0 |
| Total ejecutado / aprobado | 233 / 233 |
| Fallos | 0 |
| Errores | 0 |
| Omitidas | 0 |
| Suites JUnit | 33 |
| JAR ejecutable | 61 012 323 bytes |

Se analizaron los 33 XML de `backend/inventario/build/test-results/test/`.
El informe navegable queda en
[reporte JUnit](../../backend/inventario/build/reports/tests/test/index.html).
Estos archivos de build son locales e ignorados por Git; se regeneran con el script.
JAR: `backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar`.

Los avisos de Java sobre class sharing y compilación unchecked no impidieron
compilar ni ejecutar las pruebas. No se deshabilitó ni excluyó ningún test.

## 2. Protección del script

El script requiere una base local ya existente `inventario_verificacion_*`,
sin parámetros JDBC adicionales; rechaza la habitual antes de llamar a Gradle.
Se comprobaron dos rechazos: URL de inventario_laboratorios y URL de prueba con
parámetros adicionales. También se validó su sintaxis con el parser PowerShell.

Exige DB_USER, DB_PASSWORD y DEMO_USER_PASSWORD externas. Genera una clave JWT
temporal si no existe y restaura las variables de entorno que modifica.
Fija tanto el datasource JPA como el de Flyway a la misma URL de verificación,
usa perfil dev y validate, y rechaza configuraciones externas que puedan
sobrescribir esos destinos. No crea ni elimina bases y no contiene contraseñas.

Si Windows bloquea scripts, puede invocarse solo para ese proceso:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\verificar-backend.ps1
```

No cambia permanentemente la política de ejecución. Si falla una prueba o hay
omitidas, el script falla; no presenta una regresión parcial como cierre.

## 3. Flyway y esquema

Base de prueba: `inventario_verificacion_s7_cierre_20260921_a73f`.
Fue creada vacía; Flyway aplicó V1–V9. Al arrancar el JAR después de la suite,
Flyway validó nueve migraciones y confirmó versión 9 sin nuevas migraciones.

Los checksums de las nueve filas coinciden con los archivos SQL y con la base
habitual; todos tienen success=true. Los valores están en el inventario inicial.
Los hashes de archivos se comparan además con la copia tomada antes del sprint:
no se modificó ninguna migración aplicada.

Diez tablas del dominio, 76 columnas, diez PK, trece FK, nueve restricciones
UNIQUE y cuatro CHECK, además de los índices únicos funcionales de V5–V9.
Se cotejaron las diez Entities en [auditoría Entity/Flyway](auditoria-entity-flyway.md).
No se requirió V10. Se mantiene ddl-auto=validate y open-in-view=false.

## 4. Arranque del JAR

Se arrancó el JAR generado con Java 21, configuración por entorno, perfil dev
sobre la base temporal y `--server.address=127.0.0.1 --server.port=0`.
El puerto asignado fue **56448**. Spring Boot inició en **8,32 segundos**.

Evidencias del log:

- Successfully validated 9 migrations.
- Current version of schema "public": 9.
- Schema "public" is up to date. No migration necessary.
- Initialized JPA EntityManagerFactory for persistence unit 'default'.
- Tomcat started on port 56448.
- Started InventarioApplication.

Al terminar se detuvo exclusivamente el proceso iniciado para la verificación.
Se comprobó que no quedaba el proceso ni un listener en ese puerto.
No se dejó un backend oculto ejecutándose.

## 5. Smoke test de extremo a extremo

Se realizaron **28 solicitudes HTTP reales** sobre el JAR, distintas de la suite
JUnit. Los usuarios provienen de los fixtures dev existentes en la base temporal;
no se creó un endpoint de administración de usuarios. Tokens y contraseñas solo
se mantuvieron en memoria.

| # | Comprobación | Método | Esperado / obtenido |
|---|---|---|---|
| 1 | Login Admin | POST | 200 / 200 |
| 2 | Login Gestor | POST | 200 / 200 |
| 3 | Login Lector | POST | 200 / 200 |
| 4 | Crear categoria | POST | 201 / 201 |
| 5 | Crear subcategoria | POST | 201 / 201 |
| 6 | Crear sede | POST | 201 / 201 |
| 7 | Crear area | POST | 201 / 201 |
| 8 | Crear laboratorio A | POST | 201 / 201 |
| 9 | Crear laboratorio B | POST | 201 / 201 |
| 10 | Asignar laboratorios Gestor | PUT | 200 / 200 |
| 11 | Asignar laboratorios Lector | PUT | 200 / 200 |
| 12 | Consultar alcance GESTOR | GET | 200 / 200 |
| 13 | Crear Equipo con GESTOR | POST | 201 / 201 |
| 14 | Consultar Equipo con LECTOR | GET | 200 / 200 |
| 15 | Editar Equipo con GESTOR | PUT | 200 / 200 |
| 16 | Filtros combinados | GET | 200 / 200 |
| 17 | Traslado transaccional GESTOR A-B | POST | 200 / 200 |
| 18 | Equipo ahora fuera de alcance LECTOR | GET | 403 / 403 |
| 19 | Historial visible por origen LECTOR | GET | 200 / 200 |
| 20 | Movimientos por laboratorio | GET | 200 / 200 |
| 21 | Sin JWT | GET | 401 / 401 |
| 22 | LECTOR no traslada | POST | 403 / 403 |
| 23 | Mismo destino | POST | 409 / 409 |
| 24 | Campo de actor ajeno | POST | 400 / 400 |
| 25 | Baja logica Equipo | DELETE | 204 / 204 |
| 26 | Consultar Equipo BAJA | GET | 200 / 200 |
| 27 | Historia conservada tras BAJA | GET | 200 / 200 |
| 28 | BAJA no permite traslado | POST | 409 / 409 |

Se crearon padres propios para el flujo. Equipo terminó en BAJA dentro del
destino y conservó su único Movimiento TRASLADO, con origen/destino y actor
correctos. El LECTOR asignado solo al origen pudo consultar ese evento aunque
el detalle del Equipo, ya ubicado fuera de su alcance, devolviera 403.

Se comprobaron código interno preservado, ubicación limpiada al omitirla en
traslado, respuestas públicas sin campos privados y Location en las altas.
La revisión SQL posterior corroboró el mismo Equipo y Movimiento.

Esta demostración no afirma haber ejecutado la aplicación gráfica Postman.
La collection importable permite reproducir solicitudes de forma manual.

## 6. Consistencia SQL

Las consultas iniciales sobre la base habitual y posteriores al smoke sobre
la base temporal dieron:

| Comprobación | Incidencias |
|---|---:|
| Equipo no BAJA bajo Subcategoría inactiva | 0 |
| Equipo no BAJA bajo Laboratorio inactivo | 0 |
| UsuarioLaboratorio activo hacia Laboratorio inactivo | 0 |
| Movimiento sin Equipo/actor/destino u origen referenciado válido | 0 |
| TRASLADO con origen igual a destino | 0 |

No se modificaron filas para convertir un resultado inconsistente en correcto.
Los constraints principales se verificaron en pg_constraint y pg_indexes.
Las [consultas manuales de Sprint 6](../sprints_realizados-backend/sprint-6-movimientos.md#41-sql-de-verificación)
permiten revisar equipos, movimientos, actores, esquema y migraciones.

## 7. Limpieza de la base temporal

Después de la suite, Equipo, Movimiento y UsuarioLaboratorio estaban vacíos.
El smoke creó únicamente fixtures identificados por sus IDs reales.
Tras comprobar su comportamiento se eliminaron esos fixtures en orden de FK:
Movimiento, Equipo, asignaciones, Laboratorios, Área, Sede, Subcategoría y Categoría.
La limpieza incluyó una comprobación explícita del nombre de la base temporal.

Se recuperaron los conteos base: Usuario 3, UsuarioLaboratorio 0, Categoría 2,
Subcategoría 4, Sede 1, Área 2, Laboratorio 2, Equipo 0, Movimiento 0, Rol 3.
Las secuencias de esta base descartable no se reiniciaron.

Con cero conexiones se ejecutó DROP únicamente sobre
`inventario_verificacion_s7_cierre_20260921_a73f`, sin FORCE.
La consulta a pg_database confirmó **cero bases con ese nombre**.

## 8. Preservación de la base habitual

`inventario_laboratorios` se consultó exclusivamente en modo read-only.
La comparación antes/después incluyó conteos y huellas de todos los campos
públicos; password_hash quedó fuera de esa lectura.

| Tabla | Antes | Después | Huella pública |
|---|---:|---:|---|
| area | 2 | 2 | Idéntica |
| categoria | 2 | 2 | Idéntica |
| equipo | 0 | 0 | Idéntica |
| laboratorio | 2 | 2 | Idéntica |
| movimiento_equipo | 0 | 0 | Idéntica |
| sede | 1 | 1 | Idéntica |
| subcategoria | 4 | 4 | Idéntica |
| usuario | 3 | 3 | Idéntica |
| usuario_laboratorio | 0 | 0 | Idéntica |

No se ejecutaron pruebas, seeds, correcciones de filas ni migraciones nuevas
sobre la base habitual. Su historial Flyway sigue en V9.

## 9. Auditoría documental y de entrega

El catálogo final identifica 42 combinaciones método+ruta, agrupadas por
Controllers; variantes de filtros o roles no aumentan ese número.
El ERD conserva diez entidades implementadas, trece relaciones y el esquema
físico existente. Las reglas mantienen RN-01–45 y clasifican lo aplicable.
El backlog queda separado de los defectos.

La collection Postman contiene siete carpetas y 65 solicitudes para esas mismas
42 operaciones, incluidas variantes por rol y filtro. Se validó contra el esquema
oficial v2.1 y se compararon las rutas de Java, Markdown y JSON: ningún endpoint
faltante o inventado. Se verificaron variables, cuerpos JSON y sintaxis de 129
scripts; la simulación local de captura/escape de los tres login pasó 3/3.
Tokens, contraseñas e IDs se entregan vacíos. Estas comprobaciones estáticas no
se suman a las 233 pruebas JUnit ni a las 28 solicitudes HTTP reales del smoke.

La revisión final abarcó 27 Markdown y 357 enlaces locales: cero enlaces o
fragmentos rotos y cero marcadores de verificación pendiente. Se comprobaron
15 secciones del README, 24 del reporte Sprint 7 y 26 requisitos verificados
del checklist. El escaneo de archivos versionados y nuevos no detectó la
contraseña real de PostgreSQL ni tokens JWT completos; los secretos e IDs del
environment Postman están vacíos. git diff --check terminó sin errores.

La comparación de hashes con el inicio confirmó: 14 archivos nuevos, ocho
modificados y once .gitkeep eliminados. Los 119 Java, las nueve migraciones y
todos los documentos/SVG del modelo lógico y físico permanecen intactos;
únicamente cambió una frase de estado en erd-v2-cambios. Las guías de sprints
anteriores conservan sus hechos: solo se repararon siete enlaces históricos.
