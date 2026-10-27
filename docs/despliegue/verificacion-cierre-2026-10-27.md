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
| Cuentas nuevas | Jason ADMIN, Jorge GESTOR y Gabriela ADMIN, con datos de demostración. Marko y Aldo mantienen sus claves previas. |
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

El login conserva el acceso con claves existentes. No se restablecieron claves,
no se migraron hashes y BCrypt continúa como mecanismo de almacenamiento.
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
o ejecución manual sobre `main` pueden publicar imágenes. El resultado
remoto y los enlaces de la ejecución se incorporarán después del push; la
configuración por sí sola no acredita una ejecución aprobada.

| Entorno | Estado y límite |
|---|---|
| GitHub / Actions | Workflow actualizado en fuente y pendiente de publicación; commit, run y resultado remoto pendientes de incorporar. |
| GHCR | Se mantienen imágenes separadas backend/Jason, etiquetas `latest` y `sha-<commit>`. No se acredita una imagen nueva hasta finalizar la publicación. |
| Docker local | Motor inaccesible en esta sesión. No se certifica ejecución local de las imágenes nuevas ni se reemplazó el volumen existente. |
| Render / Vercel | Sin despliegue manual en este cierre ni verificación autenticada de nube. Un posible despliegue automático debe confirmarse en el proveedor. |
| PostgreSQL | Windows, Compose y Render son bases distintas. Código e imágenes no incluyen el inventario ni sus credenciales. |

La [memoria de Docker/CI](../memory/docker-deployment.md) distingue esta etapa de
la [evidencia histórica del 3 de octubre](verificacion-docker-actions-2026-10-03.md).
La variante y los sprints Marko siguen solo locales e ignorados por Git.

La revisión previa a publicar examinó **421 archivos versionados o nuevos no
ignorados**: no encontró coincidencias con las tres contraseñas demo generadas.
Se confirmó la exclusión de las carpetas Marko y la ausencia de cambios a las
migraciones V1–V13. Las credenciales, respaldos y manifiestos privados permanecen
fuera del repositorio.

## Pendientes delimitados

1. Registrar commit/push y ejecución real de GitHub Actions con sus imágenes.
2. Verificar Docker local cuando esté disponible el motor, conservando su volumen.
3. Acreditar posteriormente el release y recorrido autenticado en Render/Vercel;
   los datos locales no se transfieren por publicar el código.
4. Completar los 31 estados pendientes solo con información confirmada.

La PPT continúa aplazada. Los demás elementos del
[backlog](../backend-final/backlog.md) requieren priorización independiente.
