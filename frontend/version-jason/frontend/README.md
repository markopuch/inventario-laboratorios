# Frontend Jason — Inventario de Laboratorios

Aplicación React/Vite conectada al backend Spring Boot del repositorio. Esta versión
está en `frontend/version-jason/frontend/`; la versión Marko se conserva por separado.

**Adaptación al backend ampliado — 3 de octubre de 2026:** esta versión consume
los contratos actuales de catálogos administrativos, usuarios, mantenimientos y
reportes. Requiere un backend que incorpore las migraciones V10–V13 y los nuevos
Controllers. Una imagen Docker anterior conserva su API anterior: hay que
reconstruirla para usar estos módulos. Esta adaptación modifica únicamente Jason;
no reconstruye ni reemplaza tu backend habitual.

## Ejecución local en Windows

Se necesitan Node.js con npm y Docker Desktop en ejecución. Los comandos siguientes
parten de la raíz del repositorio `inventario-laboratorios` y usan PowerShell.

En una terminal, usa el entorno Docker habitual del backend y su configuración local
existente:

```powershell
Set-Location backend/inventario
docker compose up -d db backend
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health | Select-Object status
```

El Compose contiene `db`, `backend` y `frontend` (Jason). Para desarrollar con Vite,
el comando anterior levanta solamente `db` y `backend`; el backend debe estar saludable
y la consulta de salud debe devolver `UP`. Si ya están levantados y saludables, basta
con comprobar ese estado. La configuración privada del backend permanece allí.

En otra terminal, de nuevo desde la raíz del repositorio:

```powershell
Set-Location frontend/version-jason/frontend
npm.cmd ci
npm.cmd run dev
```

Abre `http://localhost:5173`. Vite usa ese puerto con `strictPort`: si está ocupado,
el comando falla y muestra el conflicto, en lugar de cambiar de puerto silenciosamente.
El navegador solicita `/api` al mismo origen y Vite lo reenvía al backend en 8080.

## Frontend Jason en Docker

Se utiliza un solo archivo Compose: `backend/inventario/docker-compose.yml`.
Cada aplicación tiene su contenedor: PostgreSQL, Spring Boot y React servido por Nginx.
La versión Marko no se construye ni se publica con este flujo.

Desde la raíz del repositorio, en PowerShell:

```powershell
Set-Location backend/inventario
docker compose config --quiet
docker compose up -d --build frontend
docker compose ps
Invoke-RestMethod http://localhost:3000/health
Invoke-RestMethod http://localhost:8080/actuator/health | Select-Object status
```

Abre `http://localhost:3000`. El puerto 3000 del equipo apunta al 80 de Nginx;
el backend conserva 8080 y PostgreSQL no publica un puerto al equipo. El 5173
se reserva para el servidor de desarrollo Vite y no es necesario para este modo.
Las rutas de React siguen funcionando al recargar y `/api` se reenvía a
`backend:8080` dentro de la red Docker, conservando el JWT y la ruta completa.

El Dockerfile usa el lockfile con `npm ci` y ejecuta el build;
la imagen final contiene Nginx y los archivos de `dist`, no Node ni `node_modules`.
`.dockerignore` excluye archivos `.env*` y archivos generados. `VITE_API_URL=/api`
es una configuración pública de compilación, no una contraseña. La configuración
privada del backend permanece en su carpeta; no se copia al frontend.

Las pruebas se ejecutan con `npm.cmd test` desde esta carpeta del repositorio
completo y también en GitHub Actions antes de publicar Jason. Algunas verifican
los contratos leyendo los DTO Java del backend, por lo que no se ejecutan dentro
del contexto aislado del Dockerfile del frontend.

Después de cambiar el frontend, repite `docker compose up -d --build frontend`.
Para detener solamente este servicio usa `docker compose stop frontend`.
Se mantiene el proyecto `inventario-docker` y el volumen existente de PostgreSQL;
no uses `docker compose down -v` si quieres conservar sus datos.
`/health` verifica Nginx: no certifica por sí solo que la API o el login funcionen.

### Publicación de las imágenes

`.github/workflows/imagen.yml` construye y publica dos imágenes independientes
cuando se suben cambios a `main` en el backend, Jason o el propio workflow;
también permite ejecución manual desde GitHub Actions en `main`:

- `ghcr.io/markopuch/inventario-laboratorios-backend`
- `ghcr.io/markopuch/inventario-laboratorios-frontend-jason`

Cada imagen recibe `latest` y `sha-<7 caracteres del commit>`. GitHub Actions
usa `GITHUB_TOKEN`, sin incorporar credenciales a los archivos del proyecto.
El Compose local sigue construyendo las imágenes `:local`; publicar en GHCR
no sustituye automáticamente los contenedores que ya están ejecutándose.

La [ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900),
del 3 de octubre de 2026, comprobó ambos trabajos en verde sobre el commit
`99569392035fc975171d2df6929a1aa26111b139`. El trabajo de Jason aprobó **27/27 pruebas**
antes de publicar. Se verificaron las dos imágenes en GHCR con etiquetas `latest`
y `sha-9956939`; sus digests y enlaces de trabajos se conservan en la
[evidencia Docker y Actions](../../../docs/despliegue/verificacion-docker-actions-2026-10-03.md).
Esto acredita construcción y publicación; el despliegue en la nube sigue pendiente.

## Configuración de la conexión

Los valores por defecto ya permiten ejecutar la aplicación localmente; no es necesario
crear un archivo de entorno. [`.env.example`](.env.example) muestra estas opciones:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `VITE_API_URL` | `/api` | Base pública de las solicitudes del navegador. |
| `API_PROXY_TARGET` | `http://127.0.0.1:8080` | Destino del proxy del servidor de desarrollo Vite; no se incorpora al bundle del navegador. |

Si existe `.env` o `.env.local`, conserva su contenido y ajusta únicamente la opción
que necesites. No copies `.env.example` encima de `.env.local`. Reinicia Vite después
de cambiar la configuración. Para usar otro backend de desarrollo basta con mantener
`VITE_API_URL=/api` y cambiar `API_PROXY_TARGET` a su origen.

Las variables `VITE_*` son públicas en el navegador: no deben contener contraseñas,
credenciales de base de datos, JWT ni claves de firma. El frontend no necesita acceso
directo a PostgreSQL. El proxy configurado aquí corresponde a `npm.cmd run dev`; un
despliegue del build necesita que su servidor también enrute `/api` al backend.

## Sesión, roles y datos iniciales

El login utiliza **usuario**, no correo institucional. Se ingresa con una cuenta
existente del backend. No hay registro público; ADMIN dispone de un directorio
administrativo en `/usuarios`, con creación, edición, rol, estado, restablecimiento
de contraseña y gestión de laboratorios mediante endpoints independientes.
No se documentan ni precargan contraseñas.

El JWT y el usuario se conservan únicamente en memoria. Recargar la página, cerrar
la pestaña o cerrar sesión requiere iniciar sesión otra vez. Se eliminan las claves
antiguas de almacenamiento local; no existe Recordarme ni recuperación de contraseña
implementados. Un 401 de una solicitud de la sesión vigente cierra esa sesión.

| Rol | Recorridos de interfaz |
|---|---|
| `ADMIN` | Alcance global; catálogos activos/inactivos, organización, usuarios/asignaciones, equipos, movimientos, mantenimientos y reportes. |
| `GESTOR` | Consulta global de catálogos; equipos, movimientos, mantenimientos y reportes según sus laboratorios autorizados. |
| `LECTOR` | Consulta de catálogos, equipos, movimientos, mantenimientos y reportes; sin acciones de escritura. |

Los tres roles pueden leer los catálogos y la organización global. El alcance por
laboratorio restringe equipos y movimientos, no la lectura de todos los laboratorios
del catálogo. El alcance de menú/filtros se obtiene al iniciar sesión; el contador
del dashboard vuelve a consultar `/auth/me/laboratorios` en cada entrada.
Los autocambios de rol o de asignaciones refrescan perfil y alcance; un autocambio
de actividad que desactive la cuenta cierra la sesión. El backend revalida siempre
los permisos de cada solicitud, aunque una sesión ajena conserve datos visuales antiguos.

El backend autoriza cada operación. La lectura de catálogos y organización es global para los tres roles; el alcance por laboratorio restringe equipos, movimientos, mantenimientos y reportes. Los movimientos pueden seguir siendo visibles por su origen aunque el equipo haya salido de ese laboratorio. Las rutas `/asignaciones` y `/usuarios`
y `/configuracion` además están protegidas por rol en el frontend.

En la comprobación del entorno habitual del 3 de octubre de 2026 existían `marko`
(`ADMIN`), `aldo` (`GESTOR`) y `romel` (`LECTOR`), con cero asignaciones y cero equipos.
Es una fotografía de ese entorno: una pantalla vacía puede ser el resultado correcto.
Las asignaciones y equipos creados para verificar escrituras se probaron en un
entorno aislado, separado de ese inventario habitual.

## Alcance y pendientes

La integración implementa login/alcance, dashboard, equipos, categorías/subcategorías,
sedes/áreas/laboratorios, historial de movimientos, usuarios administrativos,
mantenimientos y reportes. ADMIN puede consultar activos/inactivos y cambiar su
estado real. Laboratorio distingue `activo` de `estadoOperativo`.
Asignaciones permite consultar por ID y seleccionar laboratorios activos, también
desde la acción Laboratorios de cada usuario;
guardar una lista vacía retira las asignaciones del usuario consultado.

Configuración permanece como Próximamente: no tiene contrato API para preferencias
ni seguridad configurable. No se inventa una pantalla de Auditoría. Los reportes
consumen agregados reales; no se implementa exportación sin contrato acordado.
El mantenimiento de Laboratorio es un estado operativo independiente del ciclo
de Mantenimiento de Equipo. Los Movimientos siguen siendo traslados confirmados,
sin estados pendientes/en proceso inventados.

La aplicación conserva el lenguaje visual Jason, pero no es una reproducción 1:1 de
los mockups. La [auditoría de integración](../../../docs/sprints_realizados-frontend-Jason/auditoria-integracion.md)
separa las diferencias de diseño, el trabajo pendiente y la evidencia de pruebas.

## Comprobaciones

Desde esta carpeta:

```powershell
npm.cmd test
npm.cmd run build
```

### Resultado de la adaptación al backend actual

Verificación del 3 de octubre de 2026: **67/67 pruebas aprobadas**, cero fallos,
cero cancelaciones y cero omitidas. Se conservan las 27 pruebas anteriores y se
añaden 40 sobre contratos, estados de catálogos, usuarios, sesión, mantenimientos
y reportes. El build de producción pasó con **1677 módulos**.

Se ejecutaron 13 comprobaciones con los servicios reales del frontend contra el
JAR existente del backend ampliado: estados de los cinco catálogos, laboratorio
operativo, usuarios, asignaciones, último ADMIN, conflictos, mantenimiento,
LECTOR y cinco reportes filtrados. En el navegador se comprobaron además creación
y edición de usuario, rol, actividad, asignaciones, estados de los cinco catálogos,
ciclo de mantenimiento, cancelación por GESTOR, filtros, reportes y lectura LECTOR.
Un autocambio ADMIN → LECTOR actualizó perfil/alcance y retiró Usuarios sin recargar.
El restablecimiento de contraseña se verificó mediante el servicio HTTP del
frontend; su diálogo se revisó vacío, sin automatizar la entrada de una contraseña
nueva en ese formulario. El recorrido manual de abajo permite corroborarlo.

Todas las escrituras se realizaron en **`inventario_verificacion_front_actual_7f93e1c4`**,
con PostgreSQL aislado, backend **49860** y Vite **49861**. Flyway aplicó V1–V13
correctamente y las siete consultas de inconsistencias devolvieron cero.
Al terminar se confirmaron cero conexiones, eliminación de la base, contenedor y
volumen temporales y ausencia de procesos escuchando en esos dos puertos. No se
escribió en el inventario habitual ni se reconstruyeron sus contenedores.

La [evidencia de esta adaptación](evidencias/adaptacion-backend-2026-10-03.json)
incluye resultados, escenarios y límites; las capturas muestran
[el conflicto del último ADMIN](evidencias/usuarios-conflicto-admin-2026-10-03.png),
[mantenimientos completados](evidencias/mantenimientos-2026-10-03.png) y
[reportes filtrados](evidencias/reportes-2026-10-03.png).
No se reejecutó la suite JUnit del backend ni se publicó un nuevo build en Actions.

**Comentario para el backend/despliegue:** no se encontró un cambio de código
backend necesario para esta adaptación. Para usarla con Docker, la imagen del
backend debe contener sus cambios V10–V13; arrancar una imagen anterior no los
incorpora automáticamente. La edición de registros inactivos requiere reactivarlos
primero según el contrato actual, y la interfaz muestra errores de cualquier paso
sin presentar operaciones parciales como una transacción completada.

Las dos verificaciones históricas siguientes corresponden a la versión anterior
a la ampliación V10–V13. Se conservan como antecedentes; no describen los módulos
que se conectaron después ni sustituyen las comprobaciones de la adaptación actual.

Resultado de la auditoría inicial del 3 de octubre de 2026: **27/27 pruebas automatizadas aprobadas**
(10 de autenticación/sesión, 5 de contratos de equipos/catálogos, 5 de movimientos
y 7 de asignaciones). Son pruebas de lógica y contratos; no certifican todos los
recorridos de navegador. El build final también pasó: **1670 módulos, 10.85 s**.

Se comprobaron **42 solicitudes HTTP con aserciones a través del proxy**; no son 42
endpoints distintos. Cubrieron autenticación/roles/alcance, CRUD de cinco catálogos,
asignaciones, equipos e historial, incluidos rechazos 400/401/403/409. En navegador
se verificaron login de los tres roles, asignaciones, creación/edición/traslado/baja
de equipos, filtros, catálogos, laboratorios, errores, logout, recarga y vista móvil.

Las escrituras usaron exclusivamente `inventario_verificacion_jason_91d7ac79`, backend
18080 y frontend 5174. Ese entorno temporal, su base, contenedor y credencial temporal
ya se eliminaron. Durante esa verificación, el frontend en **5173** apuntó al Docker habitual en **8080**;
los dos contenedores de base/backend estaban saludables y el dashboard mostró 2 laboratorios,
0 equipos y 0 movimientos. Los conteos del inventario habitual permanecieron iguales.

Conclusión de aquella auditoría: **integración local operativa en las funcionalidades implementadas**.
En ese momento seguían pendientes FE-08–FE-10, la fidelidad visual 1:1 y la revisión exhaustiva de
accesibilidad. La [evidencia final](../../../docs/sprints_realizados-frontend-Jason/evidencias/verificacion-2026-10-03.json)
y la auditoría detallan los escenarios comprobados; no se declara una reejecución
completa de la suite backend histórica.

### Verificación posterior del flujo completo en Docker

El 3 de octubre de 2026 se validó el frontend servido por Nginx y el backend con
las mismas imágenes Docker del entorno habitual, en un stack temporal aislado:
frontend **3001**, backend **18081** y base **`inventario_verificacion_docker_26e35749`**.
Se aprobaron **33/33 comprobaciones HTTP adicionales** y **24/24 aserciones de
flujo y consistencia**; son evidencias del recorrido, no nuevas pruebas JUnit ni
una suma a las 27 pruebas automatizadas de Jason.

Se comprobó login ADMIN/GESTOR/LECTOR, crear/consultar/editar equipo, traslado
del laboratorio 201 al 206, historial con actor/origen/destino/motivo, baja lógica
sin acciones disponibles, filtros, logout, recarga a Login y persistencia de datos
al reingresar. LECTOR recibió 403 para el equipo fuera de alcance y conservó la
lectura del historial por origen. La consola registró 0 errores y 0 advertencias.

Flyway mantuvo V1–V9 exitosas y las cinco consultas de inconsistencia devolvieron
0. Los conteos antes/después de las nueve tablas habituales fueron iguales. Se
eliminaron la base, contenedores, volumen y red temporales. El entorno habitual
final tiene sus tres contenedores saludables y utiliza frontend **3000** y backend
**8080**. El [informe consolidado](../../../docs/despliegue/verificacion-docker-actions-2026-10-03.md)
separa esta evidencia de la auditoría Vite anterior y de la publicación en GHCR.
En aquella comprobación FE-08–FE-10 y los límites visuales y de accesibilidad
conservaban su estado pendiente. Mantenimientos y Reportes ahora consumen sus APIs;
Configuración y las revisiones visuales adicionales conservan sus límites indicados.

Versiones verificadas: Node 24.16.0, npm 11.13.0, Docker 29.8.0, React 18.3.1,
Vite 6.4.3, Axios 1.20.0, React Router 7.18.4, plugin React 4.7.0 y Lucide 0.468.0.

## Recorrido manual reproducible

Los pasos de escritura son acciones explícitas sobre un entorno propio destinado
a pruebas. Usa cuentas y credenciales autorizadas; esta documentación no las expone.
El entorno temporal empleado en la verificación ya no existe.

1. Levanta el backend y Jason con los comandos anteriores, comprueba salud `UP` y
   abre **3000** para el modo Docker completo o **5173** para desarrollo con Vite.
   Identifica los datos iniciales del entorno que vas a utilizar.
2. Inicia sesión como ADMIN y comprueba dashboard, laboratorios y ausencia de errores.
   Las cifras deben corresponder a los datos reales, incluso cuando sean cero.
3. En Usuarios, abre Laboratorios para un GESTOR; también puedes usar `/asignaciones`
   para consultar su ID. Selecciona explícitamente los
   laboratorios de prueba y pulsa Guardar asignaciones para modificar ese usuario.
   Repite con un LECTOR si deseas preparar esa cuenta. Comprueba la respuesta mostrada.
4. Tras consultar un usuario, cambia el ID: su resumen y Guardar deben desaparecer.
   Consulta nuevamente antes de guardar. Para retirar asignaciones de tu usuario de
   prueba, desmarca todas y guarda la lista vacía de forma explícita.
5. Crea y edita una categoría/subcategoría y un laboratorio de prueba con relaciones
   válidas. Comprueba que la sede y el árbol correspondan al área elegida.
6. Crea un equipo con código único y datos de prueba, edítalo y trasládalo entre
   laboratorios autorizados. Comprueba historial con fecha, tipo, actor y motivo;
   después realiza su baja explícita y verifica que deje de ofrecer acciones.
7. Prueba filtros de estado y laboratorio. Un INOPERATIVO no debe sumar al KPI
   Operativos; un filtro sin coincidencias debe mostrar vacío sin inventar datos.
8. Cierra sesión e ingresa como GESTOR y LECTOR: verifica sus laboratorios de alcance,
   equipos/movimientos autorizados y ausencia de escritura en LECTOR. Los catálogos
   globales siguen siendo consultables. Recarga: debes volver a Login.
9. Comprueba mensaje ante credenciales incorrectas y, en tu entorno de pruebas,
   fallo/reintento de conexión sin convertir errores en ceros. Revisa móvil a
   390 × 844, menú y tablas; documenta cualquier diferencia adicional encontrada.

Después de cambiar asignaciones, vuelve a iniciar sesión con el usuario afectado para actualizar las selecciones de la interfaz. El backend verifica los permisos vigentes en cada solicitud; el contador de laboratorios del Dashboard consulta el alcance actualizado al entrar.

### Recorrido de los contratos ampliados

Utiliza registros propios de prueba. Los mensajes 409 son resultados de negocio
esperados, no una indicación de que React haya cambiado el dato localmente.

1. Como ADMIN, crea Categoría y Subcategoría sin Equipos asociados. En Editar cambia
   Estado a Inactivo, guarda y verifica su permanencia en el listado administrativo.
   Reactiva y comprueba nuevamente el resultado. Intenta desactivar una Categoría
   con Subcategorías activas: debe mostrarse el mensaje 409 de la API.
2. Repite con una Sede y un Área sin dependencias. Para reactivar Área, su Sede debe
   estar activa. El backend requiere reactivar antes de editar campos de una fila
   inactiva; la interfaz no presenta ambos pasos como una sola transacción.
3. En Laboratorios, cambia Activo → En mantenimiento → Activo. Comprueba el badge
   y que `activo` permanezca true. Inactivo usa el endpoint de estado y conserva el
   registro; asignaciones/equipos vigentes pueden impedirlo con 409.
4. En Usuarios, crea una cuenta de prueba con username y email únicos. Edita nombre,
   apellido, email y cargo; username permanece bloqueado. Cambia GESTOR ↔ LECTOR,
   desactiva/reactiva y comprueba que las cuentas inactivas siguen en el listado.
5. Intenta desactivar o degradar al último ADMIN activo: debe mostrarse 409 y la
   cuenta debe conservar su rol/actividad. Un autocambio permitido refresca sesión
   y navegación; desactivar la cuenta propia cierra su sesión.
6. Abre Restablecer contraseña: escribe y confirma una contraseña de prueba de
   4–72 caracteres y máximo 72 bytes UTF-8. El campo queda vacío al cerrar/guardar;
   la contraseña solo se envía a su endpoint. Comprueba un nuevo login. El backend
   no revoca automáticamente todos los JWT existentes al restablecer password.
7. En Laboratorios de un usuario, selecciona los disponibles, guarda y vuelve a
   consultar. La operación reemplaza la lista; vaciarla retira sus asignaciones.
8. En Mantenimientos, programa una tarea sobre un Equipo no BAJA. Edita su tipo,
   descripción, fecha y opcionales mientras está PROGRAMADO. Inicia y comprueba
   Equipo MANTENIMIENTO; mientras está EN_PROCESO el backend impide edición,
   baja y traslado del Equipo con 409. Completa o cancela y revisa fechas/historia
   y la restauración del estado previo del Equipo.
9. Prueba filtros reales de mantenimiento (Equipo, Laboratorio, Estado, Tipo,
   fecha desde/hasta). Un rango invertido debe mostrar validación y no enviarse.
10. En Reportes, filtra laboratorio, organización, estados y fechas. Contrasta
    totales con los registros de ese alcance. Equipos usa fechaCreacion, Movimientos
    fechaMovimiento y Mantenimientos fechaProgramada; los timestamps usan días UTC.
11. Repite con GESTOR: solo gestiona Equipos/Mantenimientos autorizados. Con LECTOR
    no deben aparecer acciones de escritura ni Usuarios. Intenta una ruta ADMIN
    directa y comprueba su redirección; la API también conserva 403.
12. Configuración sigue como Próximamente. No se presentan éxitos ni preferencias
    guardadas sin un endpoint que las persista.

La auditoría Vite anterior utilizó 5173/8080. La verificación posterior dejó el
entorno habitual Docker en 3000/8080, con sus tres contenedores saludables.
Para este modo, abre 3000 y sigue la sección Frontend Jason en Docker.
Para detener Vite usa Ctrl+C en su terminal; `docker compose stop` desde
backend/inventario detiene los servicios Docker y conserva los datos de PostgreSQL.

## Estructura

| Carpeta | Responsabilidad |
|---|---|
| `src/componentes/` | Layout, navegación, protección, tablas, tarjetas y controles comunes. |
| `src/contextos/` | Estado de la sesión y permisos de interfaz. |
| `src/pantallas/` | Recorridos por módulo. |
| `src/servicios/` | Cliente Axios, contratos HTTP y coordinación de sesión. |
| `src/utilidades/` | Payloads, validación, selección, fechas y pruebas de lógica. |

Documentación: [sprints Jason](../../../docs/sprints_realizados-frontend-Jason/README.md)
y [referencias visuales](../../../docs/frontend/mockup/).
