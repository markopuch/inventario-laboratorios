# Registro de verificación — 26 de octubre de 2026

Zona horaria de referencia: **America/Lima, UTC-05:00**. Revisión de estado y
creación de documentación local; no hubo cambios a código de aplicación,
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
