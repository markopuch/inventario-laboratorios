# Registro de verificación — 26 de octubre de 2026

Zona horaria de referencia: **America/Lima, UTC-05:00**. Revisión de estado y
creación de documentación local; en esa auditoría inicial no hubo cambios a código de aplicación,
dependencias declaradas, migraciones, configuración operativa ni datos.

La tabla siguiente corresponde a la auditoría inicial. La retirada del seguimiento
Git autorizada después se registra en la sección «Conservación local de Marko»;
sus comprobaciones son de organización de archivos, sin repetir las suites de
aplicación ni renovar las observaciones operativas anteriores.

## Comprobaciones nuevas

| Área | Método | Resultado y límite |
|---|---|---|
| Git | `git status`, `git log`, remoto/configuración locales | Inicio limpio, rama main, HEAD `0344b3301c93f5b50894fbf454a21c7b129af37b`. Sin fetch/pull. Al terminar solo archivos nuevos de memoria/AGENTS. |
| Instrucciones | Búsqueda de `AGENTS.md` en workspace/ascendentes y árbol del proyecto | No se encontraron instrucciones preexistentes; se añadieron dos archivos para descubrir esta memoria. |
| Arquitectura | Fuentes Java/React, Gradle, propiedades, Dockerfiles, Compose, workflow y Vercel | Monorepo, SPA Jason, backend por capas, PostgreSQL, tres topologías. |
| API | Conteo de anotaciones Get/Post/Put/Patch/DeleteMapping en controllers | **19 clases controller y 70 operaciones**. No incluye endpoints del framework Actuator. |
| Esquema fuente | Lectura de migraciones V1–V13, Entities y ERD v3 | Modelo de 12 tablas/98 columnas/16 FK. No equivale a una consulta de esquema vivo. |
| Versiones | Comandos de versión, metadatos de instalación, package.json/lock, JAR existente | Se distinguen equipo, build declarado, lockfile y artefacto histórico en los anexos. Sin actualización de paquetes. |
| Tests Jason | `npm test -- --test-reporter=spec` desde su carpeta con dependencias existentes | **67/67 aprobados**, cero fallos/cancelados/omitidos/pendientes, salida 0. Tests de lógica, adaptadores simulados y lectura de contratos Java; sin BD. |
| Lint Marko | `npm run lint` | Salida **0**, sin diagnósticos. No acredita login/FE-01 terminado. |
| Compose | `docker compose config --quiet` y `--services` | Configuración válida; servicios db/backend/frontend. No se volcó configuración expandida con secretos. |
| Docker | CLI/contexto/versión e intento de consultar daemon | CLI 29.8.0, Compose 5.5.1, Desktop 4.92.0; motor Linux inaccesible por pipe inexistente. No se iniciaron servicios. |
| Puertos locales | Listeners y GET de salud | PostgreSQL nativo en 5432; 3000/8080 rechazaron conexión; sin listener 5173 observado. Estado instantáneo de esta sesión. |
| PostgreSQL nativo | Servicio/procesos/binarios e intento único de conexión sin contraseña/prompt | Servicio principal 18 activo, binarios 18.6. Conexión SQL falló antes de consulta; versión real, migraciones y filas sin certificar. |
| HTTP público | GET `/login` en Vercel | HTTP **200**, HTML. No acredita login, carga completa del navegador o flujos de módulos. |
| HTTP público | GET Render `/actuator/health` y Vercel `/api/auth/me` sin token | Intentos agotaron **35 y 50 segundos**; salud y proxy actuales indeterminados. Sin atribuir causa ni afirmar baja permanente. |
| GitHub Actions | API de lectura de runs/jobs mediante conector GitHub | Último run visible **38183985364**, commit **b164c44**, ambos jobs success, 11 de octubre, 18:23:14 Lima. Sin descargar manifiestos GHCR ni verificar despliegue efectivo. |
| Historia | `list_threads`, `list_archived_threads`, índice local y `read_thread` paginado | 6 chats principales completos y 5 auxiliares completos; **238 registros de turnos con historia repetida por forks**. Síntesis sin secretos, cobertura documentada en history-decisions.md. |
| Documentación nueva | Comprobación de enlaces locales, formato y estado Git | Verificación final de archivos de memoria y fuentes enlazadas; no se ejecutaron nuevas suites de aplicación para validar Markdown. |

Para reproducir la suite Jason basta `npm test`. El sufijo reporter utilizado
en la ejecución registrada quedó después de los patrones y no configuró el
reporter; los nueve archivos reales sí ejecutaron sus 67 tests. Ver aclaración
en el [anexo frontend](frontend.md). Una sonda posterior de parseo con nombres
inexistentes confirmó esa interpretación, sin crear archivos ni ejecutar tests;
su salida 1 intencional no es un fallo de la suite.

Consultas operativas principales realizadas aproximadamente entre **10:17 y
10:26 Lima**. El reloj de referencia devolvió `2026-10-26 15:25:56 UTC`,
equivalente a 10:25:56 Lima; comprobaciones finales de documentación posteriores.

## Evidencia histórica inspeccionada, sin repetirla

| Fuente | Resultado conservado | Interpretación |
|---|---|---|
| `backend/inventario/build/test-results/test/TEST-*.xml` | 40 suites, **319 pruebas**, 0 fallos/errores/omitidos, 3oct2026 | Reportes locales existentes. No se ejecutó Gradle test hoy ni se vincula automáticamente a HEAD. |
| `backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar` | Java21/Boot4.1.1 y librerías exactas enumeradas en anexo backend | Artefacto de 3oct, SHA-256 documentado en backend-database.md. No acredita runtime remoto. |
| Evidencia Docker3oct | `e1ce75a`, V1–V13, **94 HTTP**, **30 aserciones**, **7 controles SQL** | Base temporal documentada como eliminada; no hubo nuevas escrituras o limpieza de BD en esta tarea. |
| Cierre Sprint7 | V1–V9, 10 tablas, 42 operaciones, 233 tests | Corte anterior a usuarios administrativos/mantenimiento/reportes/auditoría. |
| Chat Etapa final11oct | Vercel200, RenderUP, proxy401 sin token | Hubo conectividad en esa fecha; no certifica estado actual ni login autenticado. |
| Chat de deployment11oct | Usuario confirmó publicar Jason y crear BD nueva Render sin importar datos | Falta confirmar cuentas y login; no trasladar automáticamente datos/contraseñas locales a esa BD. |

El [cierre posterior del 27 de octubre](../despliegue/verificacion-cierre-2026-10-27.md)
registra la importación aplicada, ocho recorridos con API/BD reales y refuerzo de
contraseñas, además de los resultados de JUnit/GitHub Actions al completarse.
Las secciones siguientes conservan el momento de sus comprobaciones anteriores.

Las cifras de pruebas pertenecen a ejecuciones y alcances distintos. No sumar
JUnit, tests frontend, smoke HTTP y aserciones como una sola suite.

## Fuentes y seguimiento

- [Memoria canónica](../../memory.md).
- [Backend y BD](backend-database.md), [frontend](frontend.md), [deployment](docker-deployment.md), [historia](history-decisions.md).
- [Snapshot técnico](snapshot-2026-10-26.json), con rutas relativas y hashes de la auditoría inicial, sin `.env` privados ni datos. Conserva cinco rutas Marko ahora solo locales; su ausencia en un clon nuevo es deliberada y no indica pérdida de la copia de esta computadora.
- [Evidencia Docker histórica](../despliegue/verificacion-docker-actions-2026-10-03.md).
- [Última ejecución CI consultada](https://github.com/markopuch/inventario-laboratorios/actions/runs/38183985364).

No se dispone de sesión autenticada en los paneles Render/Vercel ni de credenciales
SQL utilizables para la base nativa en este proceso. Eso limita la comprobación
de datos vivos, release efectivo y login cloud; no impidió documentar el código,
configuración, versiones y estado observado. Los resultados deben renovarse cuando
se obtenga acceso o cambie el entorno.

## Conservación local de Marko — reorganización posterior del 26 de octubre

El usuario autorizó retirar de GitHub la variante y sus sprints conservando ambas
carpetas en esta computadora, y actualizar todos los Markdown afectados.

- Se añadieron reglas ancladas a la raíz en `.gitignore` para
  `frontend/version-marko/` y `docs/sprints_realizados-frontend-Marko/`.
- `git rm -r --cached -- frontend/version-marko/ docs/sprints_realizados-frontend-Marko/`
  retiró **21 archivos del índice**: 19 del frontend y 2 de sprints. No se usó
  borrado de archivos ni de directorios locales.
- Se compararon los SHA-256 de los 21 archivos antes/después: **sin diferencias**.
  El inventario de las dos carpetas mantuvo **4512 archivos locales**, sin rutas
  añadidas ni desaparecidas; ambas carpetas siguen presentes.
- `git ls-files` para las dos carpetas devolvió **0 archivos** y
  `git check-ignore -v` identificó las dos reglas nuevas.
- Se revisaron Compose, Actions, Dockerfiles y contratos del frontend: seleccionan
  Jason/backend y no dependen del código ni de los sprints Marko. El usuario demo
  `marko` es independiente y no se modificó.
- Los enlaces afectados apuntan a fuentes del commit histórico
  `0344b3301c93f5b50894fbf454a21c7b129af37b`; los clones nuevos no requieren las
  carpetas ignoradas para navegar la documentación. El historial anterior sigue
  disponible. El snapshot JSON conserva su carácter de evidencia inicial.
- Se incorporan las memorias y las instrucciones de lectura a la documentación
  versionada, para que los enlaces relativos del conjunto también estén disponibles
  en GitHub. La memoria del workspace superior se actualiza solo en el equipo.
- La revisión de los 16 Markdown preparados comprueba sus enlaces contra el árbol
  del índice, sin depender de archivos ignorados: sin enlaces locales rotos tras
  corregir también la referencia a reportes de build locales. Las ocho fuentes
  históricas únicas de Marko existen en el commit fijado. `git diff --cached --check`
  no presenta errores. No hay cambios de código activo fuera de las retiradas.

Esta reorganización no modifica fuentes activas, dependencias, migraciones,
datos ni configuración de servicios. No se ejecutaron nuevas pruebas de aplicación,
builds o despliegues para comprobar una retirada de seguimiento y cambios Markdown.

## Cierre funcional posterior: Reportes CSV, navegación y Auditoría ADMIN

El usuario autorizó implementar la retirada de Configuración, el acceso de menú
a Asignaciones, exportación CSV y la pantalla de Auditoría exclusiva para ADMIN.

| Comprobación nueva | Resultado y alcance |
|---|---|
| Código | Retirada de `/configuracion` y `ModuloVisual`, `/auditoria` protegida con `roles={['ADMIN']}`, menú administrativo y cliente GET de auditoría con seis filtros permitidos. |
| Permiso servidor | Lectura de SecurityConfig: `GET /api/admin/auditoria` ya exige ADMIN. No se cambió backend, contrato ni migraciones. |
| Suite frontend | `npm test`: **90/90 aprobadas**, cero fallos/canceladas/omitidas/pendientes, salida 0. Son 23 casos adicionales: 12 Auditoría, 10 CSV, 1 contrato HTTP. |
| Build frontend | `npm run build`: salida 0, **1679 módulos**. Se repitió el build tras el ajuste visual final de CSS; los artefactos `dist` continúan ignorados. |
| Navegador | **10 comprobaciones aprobadas**, cero errores de página; Chrome headless con Playwright sobre Vite local `127.0.0.1:5173` y API simulada. |
| Navegación | ADMIN ve Asignaciones/Auditoría; Configuración no aparece y su ruta antigua vuelve al inicio. Menú móvil cierra al navegar. |
| Auditoría | Campos y hora de Lima, actor histórico, detalle de texto largo, seis filtros, rango inválido sin solicitud, 403, reintento y vacío. GESTOR/LECTOR no tienen enlace, no pueden abrir la ruta y no lanzan consultas de auditoría. |
| CSV | Se descargaron los cinco archivos desde la UI; BOM UTF-8 y sin token. Filtros aplicados presentes y borradores bloqueados; carga/error/vacío deshabilitan el bloque correspondiente. GESTOR/LECTOR exportan con alcance descriptivo de laboratorios asignados. |
| Presentación | Capturas inspeccionadas en escritorio y 390 px. Se corrigió el ancho del resumen de Auditoría; sin desborde horizontal de página, la tabla se desplaza dentro de su contenedor. |
| Documentación | Guía CSV/Auditoría, matriz de roles con Mermaid, README, backlog, plan y sprints distinguen este cierre de las pruebas históricas. **20 Markdown** revisados, **322 enlaces locales válidos**, sin enlaces rotos ni errores de whitespace. |

El smoke utiliza respuestas y credenciales ficticias interceptadas en el navegador.
Comprueba interfaz, rutas, descargas y tratamiento de respuestas, pero **no es E2E
contra backend/BD reales ni certifica el funcionamiento de la nube**. No se repitió
JUnit, no se escribieron datos de aplicación ni se actualizaron dependencias,
Docker, Render o Vercel. El JWT permanece en memoria. La guía de uso es
[Reportes CSV y Auditoría](../frontend/guia-reportes-auditoria.md).

Los artefactos de comprobación de navegador son temporales locales, con datos
simulados; no se incluyen como evidencia de información real del inventario.
El servidor Vite de comprobación se detuvo al finalizar. Las carpetas locales de
Marko siguen excluidas de Git y sus 21 archivos originales conservan sus hashes.
Los cambios de este cierre permanecen locales, sin commit/push ni publicación.

## Preparación posterior de importación Excel y cuentas demo

El usuario autorizó cuentas de demostración Jason ADMIN, Jorge GESTOR y Gabriela
ADMIN; confirmó Marko ADMIN y Aldo GESTOR, reparto de un laboratorio por los
cuatro primeros, Gabriela sin asignación nueva y participación de todos mediante
altas reales que produzcan auditoría. Seleccionó L201 hasta la fila 128 incluida,
L409, L413 y L414; la fila 126 de L201 debe tratarse como L201 aunque diga L209.

| Comprobación | Resultado y límites |
|---|---|
| Archivo fuente | Lectura con Python/openpyxl en modo solo lectura. **288 candidatos**: L201 127, L409 60, L413 61, L414 40. Original sin cambios. |
| Preparación | **257 listos**: 230 OPERATIVO, 24 INOPERATIVO, 3 MANTENIMIENTO; **31 pendientes** con estado nulo en el plan. No se cambió el contrato/NOT NULL para enviarlos a la API. |
| Identificadores | Códigos deterministas con laboratorio/fila, originales en comentario, series duplicadas vacías con valor original conservado y caracteres invisibles normalizados. |
| Reparto | Permutación reproducible: Marko L414, Aldo L413, Jason L201, Jorge L409. Gabriela participa mediante operaciones ADMIN, sin asignación nueva. |
| PowerShell | `importar-inventario-demo.ps1` pasó análisis sintáctico; el modo predeterminado se ejecutó con Windows PowerShell y preparó el plan privado sin HTTP/BD. |
| Pruebas nuevas | Ejecución conjunta `python -B -m unittest discover -s scripts -p 'test_*.py' -v`: **25/25 aprobadas**, 16,420 s, salida 0. Son 12 de preparación y 13 de API simulada. |
| Casos del ejecutor | Cinco actores con CREAR de recursos relacionados, HTTP 403, rol incorrecto, conflictos de código/serie, cuentas preexistentes sin reset, asignación exacta y recuperación de timeout tras commit en servidor simulado. |
| Privacidad | Carpeta privada fuera de Git, restringida al usuario Windows; en la ejecución real de preparación solo existe `plan.json`, sin claves, tokens ni respaldos. |
| Entorno de esta etapa | PostgreSQL escucha en 5432; no había backend en 8080 ni frontend en 5173. No hay credenciales en la sesión del agente. El usuario había confirmado anteriormente que el arranque manual funcionó; son momentos distintos. |

**En esa fase de preparación no se aplicó la importación:** no se crearon usuarios, equipos ni auditorías
reales, no hubo conexión SQL autenticada ni respaldo real. La aplicación requiere
backend local y entrada oculta de credenciales en una terminal interactiva. El
wrapper exige respaldo custom listado con `pg_restore`, verifica su hash y lo
conserva al reanudar. Las escrituras se realizan mediante los contratos existentes,
sin editar backend/migraciones, restablecer claves existentes o inventar historial.
Guía: [carga local del inventario demo](../importacion-inventario-demo.md).

La suite frontend de 90 casos y sus diez recorridos anteriores no se repitieron:
esta etapa añadió herramientas de carga y documentación, sin cambiar la aplicación
React/Spring. Los cambios permanecen locales, sin commit/push ni despliegue.

## Carga aplicada y cierre posterior del 27 de octubre

La preparación anterior quedó seguida por una carga local real mediante API:
**257 equipos importados**, 31 estados vacíos pendientes y 258 equipos totales
al conservar un equipo previo BAJA. El conflicto de serie `1` de L201, fila 57,
se resolvió con serie `null` y referencia original en comentario, sin editar el
registro anterior. El manifiesto privado acredita 337 altas de recursos y eventos
reales de creación de Marko, Aldo, Jason, Jorge y Gabriela.

El recorrido del **26 de octubre, 23:55 Lima**, aprobó **8/8 comprobaciones con
API/BD reales**, cero errores de página: ADMIN/GESTOR/LECTOR, alcance, Auditoría,
CSV y móvil de 390 px. Tres CSV con datos se compararon con la API; Movimientos
y Mantenimientos vacíos mantuvieron descarga deshabilitada. Jorge se usó como
LECTOR temporalmente y se restauró a GESTOR al finalizar.

El **27 de octubre** se reforzaron altas/restablecimientos/demo nuevas a mínimo
15 caracteres Unicode y máximo 72 bytes UTF-8, sin modificar login ni hashes
anteriores. Frontend **93/93** y build **1679 módulos** aprobados; la API real
verificó **11 casos** válidos/inválidos, incluido el rechazo de 14 caracteres con
400, sin modificar cuentas ni auditoría. La UI real comprobó mínimo 15 y cinco
casos Unicode/espacios sin errores ni escrituras. Importador: **28/28** aprobadas,
12 de preparación y 16 de aplicación con datos/API simulados. La regresión
backend aislada aprobó **365/365 pruebas**, cero fallos/errores/omitidas, con
`compileJava test bootJar --rerun-tasks`, salida 0 y build correcto en 2 min 23 s.
El PostgreSQL 18 temporal en 55432 usó builtin `C.UTF-8`, tras resolver un fallo
del primer intento con locale `C`; el clúster propio se detuvo y eliminó al
terminar. El backend habitual conservó su base y salud UP. El commit 58ac8f6
se publicó en main; Actions 40454293249 aprobó cinco trabajos y repitió las
pruebas backend/frontend/importador antes de publicar ambas imágenes. Las cuatro
etiquetas GHCR se consultaron independientemente y coincidieron con los digests
de publicación. Ver [anexo del cierre](../despliegue/verificacion-cierre-2026-10-27.md).

El daemon Docker local continúa inaccesible; no se validó ejecución de nuevas
imágenes localmente ni hubo despliegue manual en Render/Vercel. Esas limitaciones
no invalidan la comprobación del modo nativo ni trasladan sus datos a otros entornos.
