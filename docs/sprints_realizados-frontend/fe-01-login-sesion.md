# FE-01 — Diseño común, login y sesión

Fecha de inicio: 1 de octubre de 2026.

**Estado: tutoría en curso. Base y cliente HTTP implementados por el usuario; build previo y consulta protegida con 401 acreditados por salidas compartidas. Siguiente bloque: composición visual de Login. Autenticación y sesión pendientes.**

El usuario implementará cada bloque y aportará su resultado antes de continuar.
Este documento registra el avance y conserva la inspección inicial como historia;
no certifica un sprint terminado.

## Avance después del primer punto de pausa

Revisión del 1 de octubre de 2026. La aplicación está en
`frontend/inventario-frontend/`; todas las rutas `src/` de esta guía se entienden
ahora relativas a esa carpeta.

| Elemento | Estado comprobado en archivos |
|---|---|
| React + JavaScript + Vite | IMPLEMENTADO por el usuario; package y lock presentes |
| `src/App.jsx`, `src/index.css`, `index.html` | IMPLEMENTADO: pantalla base del inventario, estilos claros e idioma español |
| `src/main.jsx` | REVISADO ESTÁTICAMENTE: montaje de App con StrictMode y estilos |
| `vite.config.js` | IMPLEMENTADO: 5173, strictPort y proxy `/api` hacia `http://localhost:8080` |
| `.env.example` y `.env.local` | REVISADO ESTÁTICAMENTE: `VITE_API_URL=/api` en ambos |
| `.gitignore` | IMPLEMENTADO: excluye configuración local y permite `.env.example` |
| Lint | ESLint instalado y configurado; script corregido a `eslint .`. Fragmento de ejecución aportado sin diagnósticos, pero sin código de salida |
| `dist/` | Existe; el usuario aportó además build correcto: 16 módulos, finalización en 184 ms |
| `src/servicios/api.js` | IMPLEMENTADO por el usuario y REVISADO ESTÁTICAMENTE: URL, JSON, Bearer, 204 y errores HTTP/red |
| GET protegido mediante Vite y `solicitarApi` | COMPROBADO POR EJECUCIÓN aportada por el usuario: `/api/auth/me` sin token devuelve 401 y se conserva su mensaje |
| Login, sesión y panel | PENDIENTES |

El usuario indicó que terminó hasta el punto de pausa y luego adjuntó el
resultado correcto del build y la cabecera de ejecución de ESLint. La
finalización sin errores de lint depende de que haya vuelto al indicador de
PowerShell; ese retorno/código no está en el fragmento. No se observó la
pantalla en el navegador durante esta revisión. Se continúa la tutoría
distinguiendo los resultados aportados de la revisión estática.

El usuario aportó la siguiente comprobación desde la consola del navegador:

```text
GET http://localhost:5173/api/auth/me 401 (Unauthorized)
401 'Se requiere autenticación válida para acceder a este recurso.'
```

La llamada utilizó `solicitarApi('/auth/me')`. Quedan comprobados el recorrido
por el proxy y la propagación de status/message del error de autenticación.
Las respuestas correctas con JWT, 204, validación por campos, fallos de red y
otros estados HTTP todavía requieren sus comprobaciones correspondientes.

El script real `iniciar-backend.ps1` fue leído antes de indicar su uso. Ante
el bloqueo de ejecución de scripts se indicó al usuario iniciarlo en una
sesión temporal mediante `powershell.exe -NoProfile -ExecutionPolicy Bypass
-File ".\iniciar-backend.ps1"`, sin cambiar la política permanente. El agente
no arrancó servidores ni realizó consultas HTTP.

Próximo bloque: crear `src/pantallas/Login.jsx` y `Login.css`, adaptar los estilos
globales y mostrar Login desde App. Se propone una vista previa con campos
deshabilitados, dos columnas en escritorio y una columna en móvil; el bloque
visual de la derecha usará CSS provisional porque falta una ilustración
independiente. Todavía no se implementan estado del formulario, envío o sesión.
Estas instrucciones no significan que los archivos de Login ya existan.

## Inspección inicial de FE-00 (histórica)

| Elemento dentro de frontend/ | Clasificación | Evidencia |
|---|---|---|
| `readme.md` | REQUIERE AJUSTE | Existe, pero está vacío; se conserva |
| `package.json` | NO EXISTE | No hay dependencias ni scripts declarados |
| `package-lock.json` | NO EXISTE | Aún no hay instalación reproducible propia |
| `vite.config.js` | NO EXISTE | No hay servidor ni proxy configurados |
| `eslint.config.js` | NO EXISTE | No hay lint configurado |
| `index.html` | NO EXISTE | No hay entrada HTML |
| `src/main.jsx` | NO EXISTE | No hay montaje de React |
| `src/App.jsx` | NO EXISTE | No hay componente raíz |
| `src/index.css` | NO EXISTE | No hay estilos de la aplicación |
| `.gitignore` | NO EXISTE | Sí existe el de la raíz; falta cobertura local para artefactos frontend |
| `.env.example` | NO EXISTE | Falta ejemplo de URL pública |
| `src/servicios/api.js` | NO EXISTE | No existe cliente HTTP |
| Ejecución, lint, build y conexión al backend | NO VERIFICADO | No hay aplicación frontend que ejecutar |

Entorno consultado en solo lectura: Node `v24.16.0`, npm `11.13.0`.
Las versiones resueltas de la referencia incluyen React/React DOM `19.3.0`,
Vite `8.3.0`, plugin React `6.1.1` y ESLint `10.10.0`. Son datos del lock
local de Techstore, no una comprobación del registro npm ni una instalación
realizada en este frontend. No se utiliza `latest` para preparar el bloque.

## Alcance confirmado

- Login real con usuario y contraseña, validación obligatoria y mostrar/ocultar contraseña.
- Obtención del perfil y del alcance antes de confirmar una sesión completa.
- Sesión en memoria; recargar requiere autenticarse de nuevo.
- Panel con encabezado, nombre/rol reales, menú y sección inicial Equipos.
- Equipos muestra una explicación de que su listado corresponde a FE-02; no hay datos ficticios.
- Cierre local de sesión y tratamiento diferenciado de credenciales, sesión inválida, permisos y conexión.
- Presentación adaptable y uso con teclado.

No incluye CRUD de equipos, registro de usuarios, recuperación de contraseña,
Recordarme, refresh token, buscador global, notificaciones, dashboard ni módulos
de mantenimiento. No se cambian el backend ni PostgreSQL.

## Referencia visual revisada

Se inspeccionaron las imágenes 01 Login, 02 Equipos, 04 Clasificación y 05
Organización de `docs/frontend/mockup/`. Se conserva el formulario a la
izquierda, bloque visual a la derecha, fondos claros y tarjetas blancas;
en el panel, encabezado, menú lateral y área central. La selección es celeste
y las acciones destacadas son rojas. Los valores CSS serán una adaptación,
no valores oficiales deducidos de las capturas.

Adaptaciones acordadas:

- «Correo institucional» se convierte en «Usuario», de tipo texto, por el contrato `userName`.
- Se retiran Recordarme, recuperación, alta de cuenta y acceso que evite la autenticación.
- El texto promocional se limita a equipos, laboratorios, traslados y trazabilidad.
- No se encontró una ilustración independiente del login en los recursos del inventario revisados. Se propone un bloque visual provisional con CSS y texto; no se importa un archivo inexistente ni se usa la captura completa como interfaz.
- No se añade el logo UTEC. Los módulos futuros se omiten o se identifican como Próximamente y permanecen deshabilitados.

## Contratos comprobados mediante lectura

| Operación | Solicitud | Respuesta principal | Éxito |
|---|---|---|---|
| `POST /api/auth/login` | `{ userName, password }` | `{ accessToken, tokenType, expiresIn, usuario }` | 200 |
| `GET /api/auth/me` | Bearer; sin cuerpo | `{ id, userName, nombre, apellido, email, rol, activo, fechaCreacion }` | 200 |
| `GET /api/auth/me/laboratorios` | Bearer; sin cuerpo | `{ alcanceGlobal, laboratorios: [{ id, codigo, nombre }] }` | 200 |

Evidencia: `AuthController`, DTOs de autenticación/perfil/alcance y
`config/SecurityConfig.java`. Los dos GET admiten ADMIN/GESTOR/LECTOR.
El backend decide permisos y alcance. Un alcance vacío es válido y no
equivale a alcance global. Ser responsable de un equipo no concede permisos.

La contraseña se envía tal como se escribe; no se transforma ni se registra.
Los tokens se mantienen en memoria, no en almacenamiento persistente ni
variables de entorno. El logout local no revoca un JWT ya emitido.

## Arquitectura prevista, todavía no implementada

```text
main.jsx → App (sesión y selección de pantalla)
             ├─ Login → CampoContrasena
             └─ PanelInventario
                   ├─ Encabezado (perfil y cerrar sesión)
                   ├─ MenuLateral (selección y rol)
                   └─ contenido recibido por children

Login/App → authService → api.js → fetch → /api
                                             ↓ proxy Vite en desarrollo
                                      backend localhost:8080
```

Login mantiene sus campos, errores y envío. CampoContrasena recibe valor y
callback por props y mantiene únicamente la visibilidad. App coordina la
sesión y las funciones de acceso/salida, sin almacenar los campos del formulario.
Los servicios no deciden la navegación. La navegación condicional no cambia la
URL y no constituye rutas protegidas del navegador.

`VITE_API_URL=/api` será una variable pública. Los servicios usarán rutas
como `/auth/login` para evitar `/api/api`. El proxy conservará `/api` y
Vite usará 5173 con `strictPort`. No se modificará CORS en el backend.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Base mínima React/Vite y montaje | Implementada por el usuario y revisada en archivos; usuario reporta haber terminado el bloque |
| 2 | Completar ESLint, cliente HTTP y conexión de FE-00; matriz de contratos | ESLint y cliente HTTP implementados; consulta protegida sin token comprobada con 401 |
| 3 | Estilos compartidos y composición del login | Instrucciones del siguiente bloque; implementación y revisión visual pendientes |
| 4 | Formulario controlado y CampoContrasena | Pendiente |
| 5 | authService y acceso real: login → perfil → alcance | Pendiente |
| 6 | PanelInventario, encabezado, menú y contenido inicial | Pendiente |
| 7 | Logout, solicitudes pendientes, errores y presentación por rol | Pendiente |
| 8 | Revisión móvil, teclado y estados visuales | Pendiente |
| 9 | Comprobaciones técnicas y funcionales | Pendiente |
| 10 | Documentación de resultados y cierre | Pendiente |

Se adelanta el cliente HTTP respecto de la secuencia visual para terminar el
prerrequisito FE-00 antes de construir FE-01. Confirmar el primer bloque no
equivale por sí solo a cerrar FE-00.

## Evidencia de la inspección inicial (histórica)

- **IMPLEMENTADO:** registro documental del estado en esta carpeta. No hay código frontend implementado.
- **COMPROBADO POR EJECUCIÓN:** solamente consultas de versión de Node/npm, sin instalación ni ejecución de la aplicación.
- **REVISADO ESTÁTICAMENTE:** estructura vacía del frontend, guía, plan, cuatro mockups, referencia Techstore y contratos de autenticación.
- **PENDIENTE DE VERIFICAR:** instalación, lint, build, navegador, proxy, login por rol, perfil, alcance, logout, errores, diseño adaptable y teclado.

Al validar FE-01 se registrarán: login válido/inválido, campos requeridos,
visibilidad de contraseña, doble envío, perfil/alcance fallidos, alcance vacío,
403 sin cierre de sesión, 401 protegido con invalidación de la sesión adecuada,
red/500, salida durante solicitudes pendientes, recarga, roles y uso con teclado.
Lint/build no prueban por sí solos autenticación o permisos.

El primer punto de pausa requiere el resultado de lint/build y confirmar que
la pantalla mínima abre en `http://localhost:5173`. No se solicitan contraseñas,
JWT ni credenciales de PostgreSQL.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- [Guía de estructura](../frontend/estructura-frontend-y-adaptacion-inventario.md).
- [Plan frontend v1](../frontend/plan-sprints-frontend-inventario-v1.md).
- [Endpoints](../backend-final/endpoints.md), [códigos HTTP](../backend-final/codigos-http.md) y [matriz de permisos](../matriz-permisos.md).
- Referencia local: `C:\Users\marko\Desktop\fullstackweb\frontend-judith\actividad-techstore\v2\tech-store`.

Esta ubicación respeta la carpeta de sprints frontend solicitada por el usuario,
en lugar de abrir una segunda carpeta de seguimiento. Se actualizará según
evidencia aportada durante la tutoría. No se inicia FE-02.
