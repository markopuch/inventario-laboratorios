# Publicación y verificación cloud de paginación y login

**27 de octubre de 2026, America/Lima.** Se publicaron los cambios autorizados
en `main` y se comprobaron los servicios reales. Las consultas y los logins de
verificación no crean ni modifican usuarios, equipos o eventos de auditoría.

## Publicación por etapas

| Etapa | Commit y comprobación |
|---|---|
| Backend compatible y Postman | `34d8033ed4e9e79b1f8767f9e277240b7159891b`; Render Auto-Deploy `dep-dbgf7bdg1s2s739dull0`, **Live en 4m16s**, startup 181.6s. Flyway validó las 13 migraciones, sin aplicar cambios de esquema. Salud UP y OpenAPI con page/size/q y respuesta 429 comprobados. |
| Frontend paginado | `74128312fe30a8ac85280bf9c5a6f749ce4d364b`; estado Vercel **success** para ese SHA, deployment `nBQ59JBg7CcodNPtqu2y7NGYCpTs`. La web pública muestra los controles y los datos nuevos. |
| Corrección definitiva del login | `ef31d659983493e9ca9eeea2e063caf0b5e63029`; Render **Live**, despliegue limpio `dep-dbgfo1mi0phs73dt8020`, **4m52s**, startup **171.8s**. Vercel success para el mismo SHA. Contrato público nuevo y bloqueo visual comprobados. |

El backend se publicó primero para conservar arrays en los clientes antiguos;
el frontend nuevo se publicó después de confirmar la API paginada.
Git/gh locales rechazaron una cadena emitida por una CA Fortinet de la red.
Se usó el conector GitHub manteniendo TLS; los árboles publicados coinciden
exactamente con los árboles del índice local. Las referencias de `main` se
actualizaron con comprobación del SHA anterior, sin force-push.

## CI y pruebas de la primera publicación

- [Actions 40564481861](https://github.com/markopuch/inventario-laboratorios/actions/runs/40564481861): cinco trabajos aprobados; backend **406**, frontend anterior **93**, importador **28** y publicación de ambas imágenes.
- [Actions 40565271628](https://github.com/markopuch/inventario-laboratorios/actions/runs/40565271628): completed/success; backend **406**, frontend nuevo **107**, build **1681 módulos**, importador **28** y publicación de imágenes.
- [Actions 40567596491](https://github.com/markopuch/inventario-laboratorios/actions/runs/40567596491), corrección `ef31d65`: **success, cinco trabajos**; backend **410**, frontend **107**, build **1681 módulos**, importador **28** y ambas imágenes publicadas. Finalizó a las 14:20:24 Lima.
- Postman conserva **70 operaciones / 75 solicitudes**; su sandbox no hace HTTP real.
- La [verificación local previa](verificacion-paginacion-login-2026-10-27.md) conserva el recorrido temporal con 53 fixtures y móvil de 390 px; se distingue de estos datos cloud.

## API y navegador reales

En [Render](https://inventario-laboratorios-tyni.onrender.com) y mediante el
proxy de [Vercel](https://inventario-laboratorios.vercel.app) se comprobaron
páginas de 20/50/100, defaults, alias ADMIN, arrays compatibles, orden, búsqueda
fuera de la primera página, parámetros inválidos (400) y páginas fuera de rango vacías.
Jorge conserva **56 equipos** dentro de su laboratorio autorizado.
Los conteos antes y después fueron **258 equipos, seis usuarios y 351 eventos**.

El navegador público comprobó login válido, Dashboard con 258 equipos, Equipos 1–20/258,
21–40/258, cambio a 50/100 con vuelta a página 1, búsqueda de un código fuera
de la primera página y Auditoría de 20/100 sobre 351 eventos. Las capturas que
contienen inventario y auditoría reales se conservan localmente en
`%LOCALAPPDATA%/inventario-laboratorios/evidencias-cloud-2026-10-27/`;
la evidencia versionable registra resultados y conteos sin copiar esos datos.

## Incidencia detectada y corrección de login

La [prueba inicial](evidencias/paginacion-login-cloud-inicial-2026-10-27.json)
conserva **53/54** comprobaciones aprobadas y el fallo explícito: seis
solicitudes con conexiones independientes y un nombre inexistente devolvieron
seis respuestas 401. No se consideró suficiente que la regla pasara solo localmente.

Se cambió el presupuesto de fallos a **username normalizado**, independiente
del peer de la conexión. Se conserva la reserva concurrente antes de BCrypt,
la expiración de 900s, el reinicio al éxito, los mapas acotados y la cuota
general de 60/60s por peer. No se confía en Forwarded/X-Forwarded-For del cliente.
No cambia contraseñas, JWT, permisos, tablas ni migraciones.

La corrección se publicó en `ef31d659983493e9ca9eeea2e063caf0b5e63029`.
Vercel confirmó **success** para ese mismo SHA mediante el estado de GitHub:
[deployment E4o5pumzx8Z9hPRVXSsvtQKYysTu](https://vercel.com/marko-e1c9/inventario-laboratorios/E4o5pumzx8Z9hPRVXSsvtQKYysTu).
Regresión local posterior: **410/410**, cero fallos, errores u omitidas;
compileJava/test/bootJar correctos, BUILD SUCCESSFUL en 2m17s. Las pruebas del
límite comprenden 11 unitarias y 8 HTTP, con peers TCP reales 127.0.0.2–22,
nombres independientes, éxito desde otro peer, expiración de 900s y 20 solicitudes
simultáneas compartiendo solo cinco reservas. La primera ejecución falló por
overrides vacíos del helper; retirarlos realmente del entorno permitió ejecutar
la suite completa, sin cambiar las fuentes del patch.

El primer Auto-Deploy del patch, `dep-dbgfh18473hc739j058g`, llegó a figurar
Live tras 11m46s, pero cuatro GET públicos con nonce y no-cache todavía
devolvían la descripción anterior. La configuración apuntaba al repositorio,
rama y carpeta correctos. Se ejecutó **Clear build cache & deploy** del mismo
SHA: los logs confirmaron checkout ef31d65, COPY src y bootJar correcto en 26s.
El nuevo despliegue limpio llegó a Live en 4m52s y la API pública confirmó el
texto nuevo «agregados por nombre normalizado». No se atribuye una causa exacta
a la discrepancia previa ni se cambió configuración de BD/Flyway.

La [prueba de navegador final](../frontend/evidencias/cloud-paginacion-login-2026-10-27.json)
combinó cinco POST fallidos desde conexiones independientes a Render (**cinco respuestas 401**)
con un sexto desde la UI de Vercel para el mismo nombre inexistente: apareció
la espera de **868s**, el botón Reintentar quedó deshabilitado y la cuenta
regresiva bajó a 839s. No se agotó el presupuesto de ninguna cuenta real.
[Captura de login con contraseña vacía](../frontend/evidencias/cloud-login-limite-2026-10-27.png)
y [panel Render con revisión/despliegue activos](evidencias/render-paginacion-login-2026-10-27.png).

La [verificación API final](evidencias/paginacion-login-cloud-2026-10-27.json)
aprobó **59/59** entre 14:37:40 y 14:38:42 Lima: cinco respuestas 401 y un sexto 429 desde
conexiones independientes, `Retry-After: 893`, `Cache-Control: no-store` y
ApiError correcto. Un login real ADMIN posterior devolvió 200. Se repitieron
paginación, búsqueda, Auditoría, alcance Jorge de 56 equipos y proxy Vercel; los conteos
finales se mantuvieron en **258 equipos, seis usuarios y 351 auditorías**.
La evidencia inicial fallida permanece intacta y separada.

## Límites y conservación

Los contadores pertenecen a cada proceso y se reinician con él; no coordinan
réplicas. Agotar el presupuesto de un username puede causar una espera temporal
a su dueño, aunque cambie de conexión; no es un bloqueo persistido en BD.
La cuota por peer puede compartirse entre clientes detrás de un proxy y no
equivale a identificar su IP pública. No se forzó la cuota global en producción.

Windows, el stack Docker principal y los datos de Render se conservaron.
Las pruebas de integración usan PostgreSQL temporal aislado. La revisión
automática rechazó un comando que incluía limpieza recursiva con el único
mensaje `rejected: blocked by policy`; se continuó sin ese borrado.
El clúster quedó **detenido**, puerto 56829 libre y postmaster.pid ausente;
la carpeta `verificacion-login-username-c80d2fe874c341479aa2989ec019b770`
permanece bajo `%LOCALAPPDATA%/inventario-laboratorios/`, fuera de Git y con
ACL exclusiva del usuario Windows. No se reintentó su eliminación por otro
mecanismo. Los residuos históricos del bloque local conservan su registro
en el informe previo; no se incluyeron secretos de ningún entorno en Git.
