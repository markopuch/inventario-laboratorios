# Verificación del cierre local y publicación en GitHub

Actualización: **27 de octubre de 2026, America/Lima**. Este registro distingue
el recorrido real del 26 de octubre, las comprobaciones posteriores de seguridad
y los resultados de publicación. No contiene credenciales, JWT, respaldos ni
datos personales exportados.

## Inventario y auditoría reales

La carga usa el Excel autorizado: L201 hasta la fila 128 incluida, L409, L413 y
L414. La fila 126 de L201 se trató como L201 por decisión del usuario.

| Comprobación | Resultado acreditado |
|---|---|
| Selección | 288 candidatos; 257 con estado definido y 31 pendientes por estado vacío. |
| Importación local | 257 equipos importados; 258 equipos totales al conservar uno previo en BAJA. |
| Reparto | Marko L414, Aldo L413, Jason L201 y Jorge L409; Gabriela sin laboratorio asignado. ADMIN conserva alcance global. |
| Cuentas nuevas | Jason ADMIN, Jorge GESTOR y Gabriela ADMIN, con datos de demostración. Durante esa importación se conservaron las claves previas de Marko y Aldo. |
| Conflicto L201, fila 57 | La serie `1` ya pertenecía a un equipo previo en BAJA. Se creó el brazo OpenManipulator X con serie `null` y referencia original en comentario; el equipo previo no se modificó. Resolución trazable en el manifiesto privado. |
| Procedencia y auditoría | El manifiesto acredita 337 altas de recursos de esta carga. Se comprobó participación de los cinco usuarios en eventos reales de creación. No se insertaron eventos libremente ni se alteraron sus fechas. |
| Pendientes | Los 31 estados ambiguos permanecen vacíos en el plan y fuera de la API/BD, que exige estado. No se consideran equipos cargados. |

La [guía de importación](../importacion-inventario-demo.md) conserva
normalizaciones, respaldo, reanudación y ubicación privada del manifiesto.
La publicación del código no traslada esta base a Docker o Render.

## Navegador con backend y base reales

El **26 de octubre**, Chrome headless con Playwright sobre la web local y su
backend real aprobó **8/8 comprobaciones**, con **cero errores de página**.
La marca del resultado fue `2026-10-27T04:55:14.072Z`, equivalente al
26 de octubre a las 23:55:14 en Lima.

- ADMIN accede a Asignaciones y Auditoría; Configuración está retirada.
- Auditoría muestra actividad de Marko, Aldo, Jason, Jorge y Gabriela; sus filtros
  consultan eventos reales y sigue siendo de solo lectura ADMIN.
- GESTOR respeta el laboratorio asignado; LECTOR conserva consultas sin acciones
  de escritura. Ninguno obtiene acceso a Auditoría.
- Jorge se utilizó temporalmente como LECTOR para comprobar ese recorrido y se
  restauró a GESTOR al finalizar; no se dejó el rol de prueba.
- Se descargaron CSV de Resumen, Equipos por estado y Equipos por laboratorio,
  y se compararon con la API, incluido el filtro L201.
- Movimientos y Mantenimientos no tenían filas: sus descargas permanecieron
  deshabilitadas. Sus CSV con datos ya se habían comprobado con API simulada;
  no se inventó actividad para llenar esos dos bloques.
- La vista móvil de 390 px conservó navegación y acceso a las pantallas.

Las capturas, CSV y resultados detallados del navegador permanecen como archivos
temporales locales; no se publican datos de la base en el repositorio.

## Contraseñas y regresión

La política vigente para **crear usuarios, restablecer contraseñas y crear cuentas
demo ausentes** requiere al menos **15 caracteres Unicode** y como máximo
**72 bytes UTF-8**. No recorta ni transforma la contraseña; admite espacios y
rechaza claves compuestas únicamente por blancos. El límite de bytes conserva
la compatibilidad con BCrypt. No se añadió una regla de composición arbitraria.

El login conserva su compatibilidad con claves existentes. Durante este cierre,
hasta las 00:19 Lima del 27 de octubre, no se restablecieron claves ni se migraron
hashes. BCrypt continúa como mecanismo de almacenamiento.
Restablecer una contraseña sigue sin revocar automáticamente JWT ya emitidos.
La longitud toma como referencia [NIST SP 800-63B, verificación de contraseñas](https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver);
esto no acredita cumplimiento integral de la norma.

| Verificación | Resultado |
|---|---|
| Frontend | `npm test`: **93/93 aprobadas**. |
| Build frontend | `npm run build`: correcto, **1679 módulos**. |
| Validación API local | **11 casos** válidos/inválidos de política y límites, sin cambiar cuentas ni generar auditorías. El rechazo de 14 caracteres se comprobó con HTTP 400. |
| Formularios reales | Alta y restablecimiento muestran `minLength=15`; **cinco casos** de Unicode y espacios comprobados sin errores de página ni cambios de cuentas. |
| Suite backend completa | `compileJava test bootJar --rerun-tasks`: **365/365 aprobadas**, 0 fallos, 0 errores y 0 omitidas; XML JUnit comprobados. `BUILD SUCCESSFUL` en 2 min 23 s y salida 0. PostgreSQL 18 temporal en puerto 55432, con proveedor builtin y locale `C.UTF-8`; sin usar el inventario habitual. |
| Herramienta de importación | Regresión **28/28 aprobada**: 12 pruebas de preparación y 16 del ejecutor, con Excel sintético/HTTP simulado. Sustituye el total inicial de 25 de la primera etapa. |
| Esquema y datos | Sin migraciones nuevas ni edición de V1–V13; el inventario habitual conserva sus datos. |

Los scripts [iniciar-backend.ps1](../../iniciar-backend.ps1) y
[verificar-backend.ps1](../../verificar-backend.ps1) conservan la comprobación del
código de salida de Gradle y ajustan temporalmente `ErrorActionPreference`
al invocarlo: los avisos de `javac` escritos en stderr ya no abortan Windows
PowerShell antes de terminar. Después se restaura la preferencia anterior;
un fallo real de Gradle continúa deteniendo el script.

El primer intento de JUnit registró un fallo con el locale `C` del clúster
temporal. Tras comprobar `upper` con caracteres acentuados en `C.UTF-8`, la
regresión completa pasó. El clúster temporal propio se detuvo y eliminó al
finalizar; los logs detallados permanecen fuera del repositorio. El backend
habitual en 8080 conservó su base y respondió salud **UP** después del cierre.

Fuentes: [PasswordPolicy](../../backend/inventario/src/main/java/com/utec/inventario/security/PasswordPolicy.java),
[validación frontend](../../frontend/version-jason/frontend/src/utilidades/usuarios.js)
y [contratos administrativos](../backend-final/endpoints.md#11-usuario-admin--7-operaciones).

## GitHub Actions y Docker

El [workflow integrado](../../.github/workflows/imagen.yml) contiene tres
verificaciones: backend/JUnit con Java 21 y PostgreSQL 18 temporal del runner,
frontend con Node 22 y herramienta de importación con Python 3.14/openpyxl 3.1.5.
Los dos trabajos de publicación Docker dependen del éxito de las tres
verificaciones. Los pull requests ejecutan verificaciones; solo push a `main`
o ejecución manual sobre `main` pueden publicar imágenes. El código se publicó
en `main`, commit [`58ac8f6`](https://github.com/markopuch/inventario-laboratorios/commit/58ac8f6123849e7467663a53ac62b1c9a09318b2).
La [ejecución 40454293249](https://github.com/markopuch/inventario-laboratorios/actions/runs/40454293249)
terminó **completed/success, 5/5 trabajos aprobados**, el 27 de octubre entre
las **00:15:36 y 00:18:54 Lima** (`05:15:36Z–05:18:54Z`). Los logs de los trabajos
confirman pruebas y publicación de manifiestos. Una consulta independiente al
registro comprobó las cuatro etiquetas publicadas; no se infiere el resultado
solo de la configuración.

| Trabajo remoto | Resultado comprobado en logs |
|---|---|
| Backend/JUnit | **365 pruebas**, 0 fallos, errores u omitidas; `BUILD SUCCESSFUL` en **1 min 44 s**. |
| Jason | **93/93 pruebas**, 0 fallos; build de **1679 módulos**, **2,47 s**. |
| Importador | **28/28 pruebas**, **10,259 s**, `OK`. |
| Publicar backend | Aprobado; manifiesto publicado y digest confirmado por `containerimage.digest`. |
| Publicar frontend Jason | Aprobado; manifiesto publicado y digest confirmado por `containerimage.digest`. |

Las dos imágenes se publicaron con las etiquetas **`sha-58ac8f6` y `latest`**:

| Imagen | Digest publicado |
|---|---|
| `ghcr.io/markopuch/inventario-laboratorios-backend:sha-58ac8f6` | `sha256:ccde889fa75361e791c9375c031e0c63450c40cf02d34233a4c5af4a61de14b9` |
| `ghcr.io/markopuch/inventario-laboratorios-frontend-jason:sha-58ac8f6` | `sha256:183d4100c69eb95879b7ba13fd0e9c3ae7ea6c53118118ae1235c8361efa8a74` |

Desde Windows, `docker buildx imagetools inspect` consultó **las cuatro etiquetas**
(`sha-58ac8f6` y `latest` de ambas imágenes), con salida 0 y digests iguales a los
logs de publicación. Los índices OCI incluyen `linux/amd64` y atestación.
Esta inspección remota no necesita el daemon y **no acredita contenedores locales
arrancados** ni una actualización del volumen PostgreSQL.

El commit que identifica esas imágenes es **58ac8f6**. Una actualización posterior
solo de documentación no modifica esas imágenes ni reconstruye la aplicación:
el workflow filtra rutas de código, scripts y su propia configuración.

| Entorno | Estado y límite |
|---|---|
| GitHub / Actions | Código publicado en `main`, commit 58ac8f6; run 40454293249 terminado correctamente, **5/5 trabajos aprobados**. |
| GHCR | Backend y Jason publicados, etiquetas `sha-58ac8f6` y `latest`; logs e inspección independiente de las cuatro etiquetas acreditan los digests indicados arriba. |
| Docker local | Motor accesible; stack principal anterior con tres servicios saludables. Las imágenes exactas 58ac8f6 aprobaron después **22/22 comprobaciones en un entorno aislado**, incluidas siete de UI; no se actualizó el stack principal ni se copió su base. |
| Render / Vercel | Sin despliegue manual en este cierre ni verificación autenticada de nube. Un posible despliegue automático debe confirmarse en el proveedor. |
| PostgreSQL | Windows, Compose y Render son bases distintas. Código e imágenes no incluyen el inventario ni sus credenciales. |

La [memoria de Docker/CI](../memory/docker-deployment.md) distingue esta etapa de
la [evidencia histórica del 3 de octubre](verificacion-docker-actions-2026-10-03.md).
La variante y los sprints Marko siguen solo locales e ignorados por Git.

La revisión del árbol publicado examinó **420 archivos de `git ls-files`**:
no encontró coincidencias con las tres contraseñas demo generadas. El árbol de
GitHub contiene 420 blobs; las carpetas Marko, el Excel y los archivos privados
están ausentes, y las migraciones V1–V13 permanecen intactas. Las credenciales,
respaldos y manifiestos privados se conservan fuera del repositorio.

La comprobación global revisó **63 Markdown y 904 enlaces locales**: 903 están
disponibles en un clon nuevo; uno corresponde al reporte JUnit local histórico,
identificado como artefacto regenerable. Ese enlace a `build/reports` no implica
publicar los reportes de compilación ni cambia su carácter de evidencia local.

## Pendientes delimitados

1. Actualizar el stack Docker principal al cierre publicado solo si se autoriza, conservando su volumen y datos. Las imágenes 58ac8f6 ya están acreditadas en el smoke aislado, no en ese stack principal.
2. Acreditar posteriormente el release y recorrido autenticado en Render/Vercel;
   los datos locales no se transfieren por publicar el código.
3. Completar los 31 estados pendientes solo con información confirmada.

La PPT continúa aplazada. Los demás elementos del
[backlog](../backend-final/backlog.md) requieren priorización independiente.

## Comprobación posterior de Docker local — 27 de octubre

Después del cierre publicado, el usuario encendió Docker Desktop. La consulta
de solo lectura confirmó **motor accesible y tres servicios saludables**.
La web en `http://localhost:3000` respondió **HTTP 200**; la consulta anónima
de `/api/auth/me` por su proxy respondió **401**, como corresponde sin token.
Esta primera consulta no acreditaba login autenticado; la verificación posterior
del restablecimiento Docker se registra abajo. No acredita el código nuevo
58ac8f6 ejecutándose en ese entorno.

El stack local utiliza imágenes anteriores, sin revisión OCI que identifique
su commit. No se reconstruyeron ni reemplazaron por las imágenes `sha-58ac8f6`
durante esta consulta. Tampoco se copió la base nativa al volumen Docker.

| Instancia | Datos y cuentas comprobados |
|---|---|
| PostgreSQL nativo / aplicación de desarrollo | 258 equipos y seis cuentas: Marko ADMIN, Aldo GESTOR, Romel LECTOR, Jason ADMIN, Jorge GESTOR y Gabriela ADMIN. Conserva la carga Excel y su auditoría real. |
| PostgreSQL del stack Docker anterior | Tres equipos, dos laboratorios, cuatro asignaciones y tres cuentas: Marko ADMIN, Aldo LECTOR y Romel LECTOR. Auditoría: veinte eventos antes del restablecimiento, **23 después**. Jason, Jorge y Gabriela no forman parte de esa base observada. |

Son instancias independientes: las diferencias de usuarios, roles e inventario
no implican que se hayan perdido los datos importados en Windows. La inspección
no modificó roles ni trasladó datos entre bases.

El usuario autorizó después un **restablecimiento puntual de las cuentas demo
locales**. Es una operación de mantenimiento excepcional; no cambia la política
general de 15 caracteres Unicode y 72 bytes UTF-8, el contrato de login ni la
validación de la API. El valor solicitado no se guarda en este documento ni en Git.

### Restablecimiento Docker comprobado

El **27 de octubre a las 00:36:35 Lima** (`2026-10-27T05:36:35.685404Z`) se
actualizaron únicamente los tres hashes BCrypt de Marko, Aldo y Romel de la base
Docker, mediante un script privado en una transacción serializable de mantenimiento.
La operación se ejecutó fuera de la API y añadió tres eventos
`RESTABLECER_CONTRASENA`, actor Marko, con fecha real de la operación; no se
atribuyen a un formulario web ni alteran hechos anteriores.

Se comprobaron los datos de las doce tablas: se conservaron roles, los tres
equipos, dos laboratorios, cuatro asignaciones y los veinte eventos previos.
Los únicos cambios fueron los tres hashes y los tres eventos nuevos, que elevan
el total de auditoría a **23**. No se almacenan contraseñas ni hashes en el resumen
de esos eventos o en esta documentación.

Los tres logins por HTTP devolvieron **200**: Marko ADMIN, Aldo LECTOR y Romel
LECTOR. No se actualizaron las imágenes del stack principal, no se recreó el
volumen y no se trasladaron datos nativos a Docker. Las seis cuentas nativas
se comprobaron posteriormente, como se registra abajo.

### Smoke aislado de las imágenes exactas 58ac8f6

El **27 de octubre, de 00:44:35 a 00:45:44 Lima**, las imágenes backend y Jason
publicadas para 58ac8f6, con los digests registrados arriba, se ejecutaron en
un entorno Docker exclusivo de verificación. El resultado fue **22/22
comprobaciones aprobadas**, incluidas **7/7 de UI**, con **cero errores de página**.
La [evidencia publicable](evidencias/docker-smoke-58ac8f6-2026-10-27.json)
contiene revisiones OCI, digests, resultados y limpieza, sin credenciales ni rutas privadas.

| Área del smoke | Resultado |
|---|---|
| Aislamiento | Backend en `127.0.0.1:18080`, frontend en `127.0.0.1:13000`; red propia y PostgreSQL en `tmpfs`, sin volúmenes compartidos. |
| Base temporal | PostgreSQL **18.6**, cero equipos iniciales y tres cuentas seed ADMIN/GESTOR/LECTOR. Las trece migraciones V1–V13 se aplicaron correctamente. No utiliza la base nativa ni la base histórica Docker. |
| Salud y proxy | Backend y frontend saludables; salud HTTP 200 y anónimo `/api/auth/me` por el proxy HTTP 401. |
| Permisos y auditoría | Tres roles iniciaron sesión. GESTOR/LECTOR recibieron 403 al consultar Auditoría y al escribir catálogos; ADMIN creó una categoría temporal y consultó su evento real de creación. |
| Contraseñas | La imagen mantiene el mínimo general de 15 caracteres en restablecimientos; el mantenimiento puntual de las cuentas históricas no relajó esa API. |
| Reportes y UI | Login y navegación por rol, Auditoría ADMIN y descarga CSV UTF-8/BOM, 17 columnas, tres agregados válidos en cero y alcance global. |
| Limpieza | Tres contenedores propios y su red eliminados; tres archivos privados de credenciales borrados y puertos liberados. El stack principal conservó exactamente IDs, imágenes y volúmenes, con tres servicios saludables. |

Este smoke acredita **arranque y funcionamiento de las imágenes nuevas en un
entorno aislado**. El stack principal sigue con sus imágenes anteriores y su
propio inventario; no se actualizó ni recibió los datos Excel. Tampoco se
desplegó en Render/Vercel. Las cuentas seed de la base temporal son independientes
de las cuentas con el mismo nombre de las bases nativa y Docker principal.

### Restablecimiento nativo comprobado

El **27 de octubre a las 00:46:30 Lima** (`2026-10-27T05:46:30.823135900Z`)
finalizó el mantenimiento puntual de las **seis cuentas demo nativas**: Marko
ADMIN, Aldo GESTOR, Romel LECTOR, Jason ADMIN, Jorge GESTOR y Gabriela ADMIN.
Los seis logins devolvieron **HTTP 200**. Las tres claves anteriores de Jason,
Jorge y Gabriela se probaron y recibieron 401; no se incluyen sus valores.

La operación de mantenimiento fuera de la API generó seis eventos reales
`RESTABLECER_CONTRASENA`, actor Jason, uno por usuario (IDs 1–6), conservando
los eventos anteriores. Los datos públicos de los seis usuarios, sus roles,
asignaciones y resumen del inventario quedaron exactamente como antes:
**258 equipos totales, 257 procedentes del Excel y 31 pendientes de carga**.
El archivo local privado de las tres cuentas nuevas se actualizó; no se publica
el contenido de esas credenciales.

El restablecimiento fue una **excepción operativa de demostración solicitada por
el usuario**, no una modificación de la política general. Un PUT de
restablecimiento por API a un ID inexistente con una contraseña de ocho caracteres
recibió **400**, confirmando que sigue vigente el mínimo de 15 caracteres y
máximo 72 bytes UTF-8. El login conserva su compatibilidad con claves existentes;
no se añadió una regla nueva para aceptar contraseñas cortas en altas o cambios
ordinarios de la API.

En total se verificaron **nueve restablecimientos locales**, seis en PostgreSQL
nativo y tres en Docker principal, con sus nueve logins correctos. No incluye
cuentas de Render/Vercel ni traslada datos entre bases. El valor de la contraseña,
hashes, JWT y archivos privados permanecen fuera de Git.

La limpieza final eliminó el helper temporal de mantenimiento, su clase compilada,
el import provisional de arranque y los payloads privados de solicitud. El árbol
de código backend/frontend/scripts/Actions quedó **sin diferencias**. `compileJava`
aprobó, salida 0, en **19 s**; después se verificaron salud nativa **UP** y los
seis logins **HTTP 200** nuevamente. El Docker principal conservó sus tres
servicios saludables y volumen original. Solo quedan cambios de documentación
y la evidencia pública del smoke, sin valores de contraseña ni hashes privados.
