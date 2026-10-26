# Memoria técnica del frontend

Fecha de auditoría: **2026-10-26, America/Lima**. Alcance: lectura del código, configuración, lockfiles y documentación de ambas versiones locales; pruebas indicadas abajo. No se instalaron dependencias, no se arrancaron servidores y no se modificaron datos ni código de aplicación.

## Arquitectura y decisión de versión

La aplicación integrada es una SPA **React + JavaScript + Vite** de **Jason**. En desarrollo, Vite sirve la SPA en `localhost:5173` y reenvía `/api` al backend en 8080. En Docker, una construcción con Node produce `dist`; **Nginx** sirve esos archivos en el puerto interno 80, expuesto localmente como 3000, y reenvía `/api` a `backend:8080`. Spring Boot conserva la autorización y el acceso a PostgreSQL. El navegador no conecta directamente con la base.

La versión **Marko** es una aplicación independiente de tutoría, pausada en etapa FE-00/FE-01. En la revisión local del 26 de octubre, su `App.jsx` solo mostraba la pantalla «Preparación FE-00» y el texto que anuncia la futura construcción del login. No debe confundirse su React/Vite más nuevo con la aplicación funcional actualmente integrada.

**Conservación local, 26 de octubre de 2026:** por decisión del usuario, `frontend/version-marko/` y `docs/sprints_realizados-frontend-Marko/` se retiran del seguimiento Git y quedan ignorados, conservando sus carpetas y contenido en este equipo. Los clones nuevos del árbol posterior a este cambio no incluyen la variante Marko ni sus sprints. La retirada no borra el historial Git; las referencias Marko de este anexo apuntan a fuentes históricas del commit `0344b3301c93f5b50894fbf454a21c7b129af37b`. Las versiones y comprobaciones locales que siguen se conservan como antecedentes, no como componentes distribuidos con la aplicación.

**Verificado por configuración:** [Compose][compose] construye `frontend/version-jason/frontend`; [GitHub Actions][ci] prueba y publica esa misma versión. Marko no participa en ese flujo. El [README general][repo-readme] y el [índice frontend][frontend-readme] lo declaran explícitamente.

## Versiones declaradas, resueltas e instaladas

Fuentes: [package Jason][j-package], [lock Jason][j-lock], [package Marko histórico][m-package], [lock Marko histórico][m-lock]. Ambos lockfiles revisados usan formato **3**. «Resuelta» significa versión exacta del lock consultado; no significa la versión más reciente disponible en Internet. La tabla Marko documenta la copia local auditada antes de retirarla del seguimiento; no es una segunda aplicación incluida en nuevos clones.

| Jason, paquete `frontend-inventario-laboratorios` 1.0.0 | Declarado en package.json | Resuelto en package-lock.json |
|---|---|---|
| React | `^18.3.1` | 18.3.1 |
| React DOM | `^18.3.1` | 18.3.1 |
| React Router DOM | `^7.1.1` | 7.18.4 |
| Axios | `^1.12.2` | 1.20.0 |
| lucide-react | `^0.468.0` | 0.468.0 |
| Vite, desarrollo | `^6.0.7` | 6.4.3 |
| @vitejs/plugin-react, desarrollo | `^4.3.4` | 4.7.0 |

| Marko histórico/local, paquete `inventario-frontend` 0.0.0 | Declarado en package.json | Resuelto en package-lock.json |
|---|---|---|
| React | `^19.2.8` | 19.3.0 |
| React DOM | `^19.2.8` | 19.3.0 |
| Vite | `^8.3.0` | 8.3.2 |
| @vitejs/plugin-react | `^6.1.1` | 6.1.1 |
| @types/react | `^19.2.18` | 19.3.0 |
| @types/react-dom | `^19.2.7` | 19.3.0 |
| @eslint/js | `10.0.1` | 10.0.1 |
| ESLint | `10.10.0` | 10.10.0 |
| eslint-plugin-react-hooks | `7.1.1` | 7.1.1 |
| eslint-plugin-react-refresh | `0.5.7` | 0.5.7 |
| globals | `17.12.0` | 17.12.0 |
| Oxlint | `^1.81.0` | 1.86.0 |

En Marko, solo React y React DOM son dependencias de ejecución; las demás son de desarrollo. Oxlint está instalado/declarado, pero el script `lint` ejecuta **ESLint**, según [su configuración][m-eslint]. Las definiciones `@types/*` no convierten el código JavaScript/JSX en TypeScript.

**Verificado en el equipo el 2026-10-26:** Node **v24.16.0**, npm **11.13.0**. Se encontraron `node_modules` y `dist` en ambas variantes. Las versiones instaladas consultadas coinciden con el lock: Jason React 18.3.1, Vite 6.4.3, Axios 1.20.0, Router DOM 7.18.4; Marko React 19.3.0 y Vite 8.3.2. La existencia de `dist` no certifica que represente todo el código actual.

**Declarado para contenedor/CI:** Jason compila con imagen `node:22-alpine` y sirve con `nginx:stable-alpine`; CI configura Node `22`. Son etiquetas móviles, no versiones patch exactas. El Dockerfile no fija npm ni la versión exacta de Nginx. El lockfile conserva el árbol completo de dependencias transitivas; las tablas anteriores cubren las dependencias directas.

## Arquitectura interna de Jason

```text
index.html → src/main.jsx → App.jsx
                              ├─ BrowserRouter
                              └─ AuthProvider
                                   ├─ /login → Login
                                   └─ Protegida → Layout
                                        ├─ BarraLateral
                                        ├─ Encabezado
                                        ├─ Routes → pantallas/*
                                        └─ PiePagina

pantallas → servicios/api.js → Axios → /api → backend
          ↘ utilidades/*: contratos, payloads y validación
          ↘ componentes/*: formularios, tarjetas, filtros y tablas
```

[App][j-app] concentra las rutas y el layout. [AuthContext][j-auth] publica el estado de sesión y permisos; [Protegida][j-protegida] exige sesión coherente y comprueba roles. El estado de cada pantalla usa hooks React y se mantiene dentro del módulo. No se declara una biblioteca de estado global adicional. `utilidades/` separa construcción de payloads, validación y coordinación de asignaciones de la presentación. Los estilos son CSS propios (`index.css`, `App.css` y algunos CSS de pantalla); los iconos vienen de Lucide.

Los componentes compartidos incluyen `Boton`, `TarjetaEquipo`, `FiltroEquipo`, `FilaMovimiento`, `ControlCantidad`, `ResumenSeleccion` y `TarjetaBienvenida`. El layout incluye menú móvil, cierre por Escape y fondo pulsable. La presencia de atributos ARIA y adaptación CSS no acredita una auditoría completa de accesibilidad o fidelidad al mockup.

## Rutas y funcionalidades reales de Jason

| Ruta | Acceso de interfaz | Estado observado en código |
|---|---|---|
| `/login` | Pública | Formulario de acceso real, validación, envío y errores; sesión ya abierta redirige al inicio. |
| `/` | Sesión válida | Dashboard con inventario visible, cantidad de laboratorios autorizados, últimos movimientos y resumen de mantenimiento consultados por API; diferencia errores de conteos cero. |
| `/equipos` | ADMIN/GESTOR/LECTOR | Lista y búsqueda local; cuatro filtros de API; alta, edición, baja lógica y traslado para quienes gestionan. BAJA oculta acciones de modificación. |
| `/categorias`, `/catalogo` | Sesión válida | Mismo componente: categorías/subcategorías. ADMIN crea/edita, consulta inactivos y cambia actividad; otros roles consultan catálogos comunes. |
| `/laboratorios` | Sesión válida | Gestión/consulta de sedes, áreas y laboratorios. Administración restringida a ADMIN. Separa `activo` de `estadoOperativo`. |
| `/usuarios` | ADMIN | Directorio, creación, edición de datos públicos, cambio de rol, actividad, contraseña y asignaciones. |
| `/asignaciones` | ADMIN | Pantalla alternativa de asignaciones a usuarios existentes; no aparece como enlace independiente en la barra lateral actual. |
| `/movimientos` | Sesión válida | Historial real, búsqueda, filtro de laboratorio, origen/destino y actor. El traslado se inicia desde Equipos. |
| `/mantenimientos` | Sesión válida | Listado, seis filtros, creación, edición y transiciones de estado; lectura según alcance, escritura ADMIN/GESTOR. |
| `/reportes` | Sesión válida | Cinco consultas agregadas del servidor y ocho filtros; presenta conteos/tablas con estados de carga, error y vacío. |
| `/configuracion` | ADMIN | **Placeholder**, sin persistencia ni operaciones. La barra lateral lo muestra deshabilitado como «Próximamente»; la ruta directa existe. |
| Cualquier otra | — | Redirección a `/`. Sin sesión, el guard redirige a `/login`. |

Fuentes principales: [rutas][j-app], [barra lateral][j-sidebar], [pantallas][j-screens], [utilidades][j-utils], [API][j-api].

Detalles útiles para continuidad:

- **Equipos:** filtros `estado`, `idLaboratorio`, `idSubcategoria`, `requiereMantenimiento`. En edición, el payload omite código interno, laboratorio y metadatos protegidos. El laboratorio solo cambia por traslado. El responsable se introduce como ID numérico opcional, sin selector de directorio. La baja usa confirmación nativa `window.confirm` y después `DELETE`.
- **Catálogos/organización:** la actividad se cambia con `PATCH .../estado`; un cambio de actividad y edición puede requerir varias llamadas. Las utilidades propagan errores parciales, por lo que no debe suponerse una transacción única desde la interfaz. Un laboratorio activo en mantenimiento sigue activo y tiene `estadoOperativo=MANTENIMIENTO`.
- **Usuarios:** username, rol, actividad y contraseña tienen contratos separados; editar datos públicos no envía esos campos protegidos. Los autocambios administrativos refrescan perfil y alcance o cierran la sesión cuando corresponde. Las asignaciones se reemplazan mediante `{ idsLaboratorio: [...] }`, incluida la lista vacía; no son adiciones incrementales.
- **Mantenimientos:** tipos `PREVENTIVO`, `CORRECTIVO`, `CALIBRACION`, `OTRO`; estados `PROGRAMADO`, `EN_PROCESO`, `COMPLETADO`, `CANCELADO`. La interfaz permite PROGRAMADO → EN_PROCESO/CANCELADO y EN_PROCESO → COMPLETADO/CANCELADO; no reabre terminales. Fechas programadas se tratan como fecha de calendario y no como instante convertido de zona horaria.
- **Reportes:** resumen, equipos por estado, equipos por laboratorio, movimientos y mantenimientos. Filtros `idSede`, `idArea`, `idLaboratorio`, `estado`, `fechaDesde`, `fechaHasta`, `estadoMantenimiento`, `tipoMantenimiento`. Los conteos proceden del servidor y las respuestas parciales o inválidas no se sustituyen por ceros. La UI documenta fechas de movimientos por días UTC; no confundirlo con la presentación local de instantes.

## Sesión, autenticación y autorización

Implementación vigente: [API/control de sesión][j-api], [AuthContext][j-auth], [guard][j-protegida].

1. `POST /api/auth/login` envía `{ userName, password }`; se recorta el username y se conserva la contraseña tal como se escribió.
2. Valida `accessToken`, `tokenType=Bearer`, `expiresIn>0` y el usuario devuelto por login: ID válido, activo y rol ADMIN/GESTOR/LECTOR.
3. Con el token provisional, consulta `GET /api/auth/me/laboratorios` y valida `{ alcanceGlobal, laboratorios }`. Solo publica sesión completa cuando usuario y alcance son coherentes. El login actual toma el perfil de la respuesta del POST; **no llama `/auth/me` durante ese acceso inicial**.
4. `refrescarSesion()` consulta `GET /api/auth/me` y después `/auth/me/laboratorios`, conservando el JWT y actualizando perfil/alcance.
5. JWT vive exclusivamente en memoria React/módulo. Al recargar se pierde la sesión. Se eliminan las claves legacy `inventario_token`/`inventario_usuario` de localStorage y sessionStorage; no se recuperan tokens desde allí.
6. Axios agrega Bearer únicamente a solicitudes autenticadas; login nunca lleva Authorization. Cada petición guarda una versión de sesión. Un 401 de la sesión vigente la invalida; un 401 obsoleto no cierra una nueva sesión. Un 403 conserva la sesión.
7. AbortController y números de operación impiden que un login o refresco tardío restaure una sesión cerrada. Logout es local; no hay endpoint de revocación ni refresh token en el cliente.

`esAdmin` exige token, rol ADMIN y alcance global; `puedeGestionar` exige sesión/alcance y rol ADMIN o GESTOR. Un alcance vacío es válido y no implica alcance global. Los guards y botones ayudan a la navegación; **el backend es la autoridad de permisos y alcance vigentes**. La propiedad de responsable de equipo no concede permisos. No se observó un temporizador de renovación automática; la expiración se trata al recibir 401.

El cliente central tiene timeout de **15 segundos**. `mensajeError` distingue red, timeout, credenciales, sesión expirada, cancelación y conflictos de negocio. Para estados 400/403/404/409/422 expone un subconjunto de mensajes cortos saneados; para 500 muestra un mensaje general.

## API, ejecución y despliegue configurado

| Modo | Configuración comprobada | Límite |
|---|---|---|
| Vite Jason | `localhost:5173`, `strictPort`, `VITE_API_URL` o `/api`; proxy a `API_PROXY_TARGET` o `http://127.0.0.1:8080`. | Las variables Vite se incorporan en compilación; `API_PROXY_TARGET` es para el servidor dev. |
| Docker Jason | Node 22 Alpine → `npm ci` → `npm run build`; Nginx sirve `dist`, fallback a index y `/api` a backend:8080. | El Dockerfile no ejecuta la suite de contratos; necesita fuentes Java del repo completo. |
| CI Jason | Node 22, `npm ci`, `npm test`, build/push Docker, etiquetas `latest` y SHA en GHCR. | Configuración existente no acredita por sí sola la última ejecución remota. |
| Vercel Jason | `vercel.json` reescribe `/api/:path*` al backend Render `https://inventario-laboratorios-tyni.onrender.com/api/:path*` y el resto a `index.html`. | **Declarado en archivo**; este informe frontend no visitó ni validó esas URLs. Revisar memoria de deployment para el estado remoto. |
| Vite Marko, copia local histórica | 5173, `strictPort`, `/api` hacia `http://localhost:8080`; `.env.example` declara `VITE_API_URL=/api`. | Variante ignorada y ausente en nuevos clones; no tenía login funcional montado ni Dockerfile propio. |

Fuentes: [Vite Jason][j-vite], [Dockerfile][j-docker], [Nginx][j-nginx], [Vercel][j-vercel], [Compose][compose], [Actions][ci], [Vite Marko][m-vite]. Nginx devuelve 404 para assets inexistentes, cachea assets con hash y evita cachear `index.html`. Resuelve de nuevo el nombre interno `backend` mediante DNS Docker y conserva ruta, query y Authorization. `/health` solo devuelve salud de Nginx; no prueba API, PostgreSQL ni login.

Scripts disponibles: Jason `dev`, `build`, `preview`, `test`; no declara `lint`. La copia local histórica de Marko conserva `dev`, `build`, `lint`, `preview`; no declara `test` y no se obtiene con un clon nuevo. Para usar los comandos hay que entrar en la carpeta de la variante adecuada; ambas usan el mismo puerto Vite. Las pruebas Jason se lanzan desde el **repositorio completo**, porque algunas leen controladores/DTO Java mediante rutas relativas.

## Antecedente y alcance de la copia local de Marko

La [App Marko histórica][m-app] no importa login, router ni cliente HTTP. [Su cliente histórico][m-api] sí implementa `solicitarApi(ruta, opciones)`: `fetch`, URL de entorno obligatoria, JSON, Bearer opcional proporcionado por el llamador, `credentials: 'omit'`, respuesta 204 y errores estructurados (`status`, `errors`, `tipo`). Eso es una base reutilizable y no una autenticación completa. Esta descripción conserva el valor educativo de la copia local ignorada; Jason no depende de ella.

El [registro histórico de tutoría][m-sprints] conserva FE-00 con cliente y proxy y FE-01 pendiente de composición visual/login/sesión. La [guía FE-01 histórica][m-fe01] describe arquitectura futura, no archivos existentes. Su «alcance confirmado» es un compromiso de trabajo, no implementación. Históricamente, el usuario aportó una consulta `/api/auth/me` sin token que devolvió 401 y un build de 16 módulos; no son evidencia de login completado.

## Verificación de esta auditoría y evidencia histórica

**Ejecutado el 2026-10-26:** `npm test -- --test-reporter=spec` en Jason: **67 tests, 67 aprobados, 0 fallos, 0 cancelados, 0 omitidos, 0 pendientes**, salida 0; duración reportada por Node **6740.0941 ms**. Se utilizaron Node/npm y dependencias locales existentes. Los tests usan adaptadores Axios/objetos simulados y lectura de fuentes Java; no iniciaron la aplicación ni escribieron en una base de datos.

Para reproducir, usar `npm test` o `node --test --test-reporter=spec src/servicios/*.test.js src/utilidades/*.test.js`. En el comando ejecutado, el sufijo colocado después de los patrones no configuró el reporter: Node lo interpretó como patrón adicional; la suite sí encontró y ejecutó los nueve archivos reales y reportó 67/67 con salida 0. El formato spec observado era el predeterminado.

**Ejecutado el 2026-10-26:** `npm run lint` en Marko terminó con salida **0**, sin diagnósticos. Es una comprobación estática de su base actual; no acredita que FE-01 esté implementado. No se encontraron instrucciones `AGENTS.md` preexistentes en los directorios ascendentes consultados ni dentro del árbol `v2` durante la inspección inicial.

**Revisión documental de la conservación local, 2026-10-26:** se comprobó que el `package.json` local de Marko y el índice local de sus sprints seguían presentes. Se releyeron [Compose][compose] y [Actions][ci]: ambos seleccionan Jason para construir, probar o publicar el frontend, sin referenciar Marko ni sus sprints. Esta revisión no repitió las suites ni acredita un despliegue nuevo; los resultados de pruebas anteriores mantienen su fecha y alcance.

Cobertura presente: control asíncrono de sesión, 401/403, almacenamiento legacy, mensajes de error, contratos HTTP, asignaciones y respuestas obsoletas, payloads protegidos, catálogos y actividad, alcance de laboratorios, mantenimiento/fechas/transiciones, movimientos y validación de los cinco reportes, usuarios y contraseña UTF-8. Son pruebas de lógica/contratos; **no una suite de interacción JSX, accesibilidad, navegador completo ni backend en ejecución**.

No se volvió a compilar `dist` ni se arrancó navegador/servidor durante esta auditoría frontend. Por tanto, el build y la experiencia visual actual no se certifican con esta nueva ejecución de tests.

**Histórico, 2026-10-03:** [sprints Jason][j-sprints] y [evidencia de adaptación][j-adaptacion] documentan 67/67 tests y build con 1677 módulos. [Evidencia Docker][j-evidencia-docker] registra el despliegue local de imágenes SHA `e1ce75a`, smoke aislado 94/94 HTTP, 30 aserciones y siete controles SQL sin inconsistencias. Son ejecuciones diferentes y no se suman a los 67 tests ni a los tests JUnit del backend. La baja del equipo se comprobó por API en ese smoke porque la confirmación nativa bloqueó la automatización del navegador. Las capturas son evidencia histórica, no una inspección visual nueva.

La documentación histórica dice «nube pendiente»; la presencia actual de `vercel.json` es evidencia de configuración posterior o adicional y requiere comprobar deployment antes de afirmar el estado de la nube. Algunas secciones antiguas de [FE-11][j-fe11] aún mencionan localStorage: su propia introducción las marca históricas; el código vigente usa memoria.

## Pendientes y recomendaciones para la siguiente IA

- Configuración persistida y seguridad configurable: no implementadas. La pantalla actual es placeholder.
- Pantalla de auditoría administrativa: no existe en rutas ni cliente Axios, aunque el backend tenga su API.
- Reportes: exportación y gráficos completos de mockup pendientes; actualmente tablas/conteos reales.
- Mantenimientos: calendario fuera del alcance actual.
- Refresh tokens, recuperación de contraseña y revocación de sesión no implementados en frontend.
- No hay evidencia nueva de build, E2E, revisión visual completa, accesibilidad exhaustiva o comportamiento bajo carga. La prueba de lógica aprobada no elimina esos límites.
- Marko: continuar la tutoría local desde composición visual de login/FE-01 solo si el usuario retoma esa variante. Sus carpetas permanecen ignoradas y no forman parte de nuevos clones; no volver a añadirlas al seguimiento sin una petición que cambie esta decisión. No portar automáticamente paquetes/código de Jason ni afirmar completados los sprints de Marko.
- Confirmar variante y modo de ejecución antes de cualquier cambio; no ejecutar ambos Vite en 5173. Usar lockfile y `npm ci` al preparar un entorno nuevo y no actualizar versiones para documentar.
- Mantener secretos fuera del bundle. `VITE_*` es configuración pública de compilación. No incluir contraseñas, JWT ni archivos `.env` privados en memorias o commits.

[repo-readme]: ../../README.md
[frontend-readme]: ../../frontend/readme.md
[compose]: ../../backend/inventario/docker-compose.yml
[ci]: ../../.github/workflows/imagen.yml
[j-package]: ../../frontend/version-jason/frontend/package.json
[j-lock]: ../../frontend/version-jason/frontend/package-lock.json
[j-app]: ../../frontend/version-jason/frontend/src/App.jsx
[j-auth]: ../../frontend/version-jason/frontend/src/contextos/AuthContext.jsx
[j-protegida]: ../../frontend/version-jason/frontend/src/componentes/Protegida.jsx
[j-api]: ../../frontend/version-jason/frontend/src/servicios/api.js
[j-sidebar]: ../../frontend/version-jason/frontend/src/componentes/BarraLateral.jsx
[j-screens]: ../../frontend/version-jason/frontend/src/pantallas
[j-utils]: ../../frontend/version-jason/frontend/src/utilidades
[j-vite]: ../../frontend/version-jason/frontend/vite.config.js
[j-docker]: ../../frontend/version-jason/frontend/Dockerfile
[j-nginx]: ../../frontend/version-jason/frontend/nginx.conf
[j-vercel]: ../../frontend/version-jason/frontend/vercel.json
[j-sprints]: ../../docs/sprints_realizados-frontend-Jason/README.md
[j-fe11]: ../../docs/sprints_realizados-frontend-Jason/fe-11-arquitectura-frontend.md
[j-adaptacion]: ../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json
[j-evidencia-docker]: ../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json
[m-package]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/package.json
[m-lock]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/package-lock.json
[m-app]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/src/App.jsx
[m-api]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/src/servicios/api.js
[m-vite]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/vite.config.js
[m-eslint]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/frontend/version-marko/inventario-frontend/eslint.config.js
[m-sprints]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/docs/sprints_realizados-frontend-Marko/README.md
[m-fe01]: https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/docs/sprints_realizados-frontend-Marko/fe-01-login-sesion.md
