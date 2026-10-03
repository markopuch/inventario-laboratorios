# FE-01 — Diseño común, login y sesión

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Login y sesión integrados; tres roles comprobados con las imágenes Docker actuales. JWT y usuario permanecen en memoria.**

## Implementación y verificación actuales

El login usa `{ userName, password }`, carga el alcance autorizado y agrega Bearer a las solicitudes. Recarga y logout requieren nuevo login; un 401 de la sesión vigente la limpia. La adaptación añadió refresco de perfil/alcance ante autocambios administrativos permitidos. Recordarme y recuperación no están implementados. Las 14 pruebas actuales de `api.test.js` forman parte de las 67 del conjunto, no se añaden otra vez a su total.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes describen la inspección inicial, anterior a las
correcciones. Las menciones a `localStorage`, acceso de prueba y base URL directa
no describen el estado vigente; se conservan para explicar el cambio auditado.

El usuario implementó el frontend y aportó el resultado para revisión.
Este documento registra el avance real del bloque y conserva la inspección inicial
como historia; no convierte por sí solo las comprobaciones estáticas en certificación
de comportamiento de backend.

## Avance después del punto de pausa

Revisión sobre la versión entregada en `frontend/version-jason/frontend/`; todas las
rutas `src/` de esta guía se entienden relativas a esa carpeta.

| Elemento | Estado comprobado en archivos |
|---|---|
| React + JavaScript + Vite | IMPLEMENTADO; dependencias React 18, React DOM, Vite, React Router DOM y Axios presentes |
| `src/App.jsx` | IMPLEMENTADO: BrowserRouter, AuthProvider, Login público y Layout protegido |
| `src/main.jsx`, `src/index.css`, `src/App.css` | IMPLEMENTADOS; montaje y estilos globales del inventario |
| `src/contextos/AuthContext.jsx` | IMPLEMENTADO: login, token, usuario, alcance de laboratorios, logout y permisos por rol |
| `src/servicios/api.js` | IMPLEMENTADO: Axios, URL configurable, Bearer y APIs de autenticación/catálogos/equipos/movimientos/usuarios |
| `src/pantallas/Login.jsx` | IMPLEMENTADO: formulario, validación HTML, mostrar/ocultar contraseña, error, carga y acceso de prueba |
| `src/pantallas/Login.css` | IMPLEMENTADO: composición en dos columnas y adaptación visual |
| Persistencia de sesión | IMPLEMENTADA mediante `localStorage` para token y usuario |
| Protección de navegación | IMPLEMENTADA mediante `Protegida` y redirección de Login cuando existe token |
| 401 de API | IMPLEMENTADO en interceptor: limpia sesión local y redirige a `/login` salvo en el propio login |
| Login, sesión y panel | IMPLEMENTADOS; faltan comprobaciones de cierre y escenarios de error exhaustivos |

El Login actual conserva visualmente «Recordarme» y «¿Olvidaste tu contraseña?»,
pero no implementa persistencia específica de Recordarme ni recuperación de contraseña.
El botón «Acceso de prueba» solamente precarga `usuario` / `usuario`.

## Alcance confirmado

- Login con usuario y contraseña contra `POST /api/auth/login`.
- Mostrar y ocultar contraseña.
- Mensaje de error cuando el login falla.
- Estado de carga mientras se valida el acceso.
- Obtención posterior de perfil y laboratorios mediante `/auth/me` y `/auth/me/laboratorios`.
- Sesión persistida en `localStorage`.
- Logout local y limpieza de token/usuario.
- Protección de la aplicación mediante `Protegida`.
- Redirección a `/login` cuando un recurso protegido responde 401.
- Presentación adaptable del login y uso de controles nativos/teclado.

No incluye recuperación de contraseña ni lógica real de «Recordarme».
No se modifican backend ni PostgreSQL.

## Referencia visual revisada

Se inspeccionó la referencia de Login entregada para el proyecto. El frontend
actual conserva el formulario a la izquierda y un bloque visual a la derecha,
con fondos claros, tarjeta blanca, acciones rojas y elementos gráficos de
laboratorio.

La implementación usa `lucide-react` y CSS para la ilustración provisional:
monitor, matraz, microscopio y recipientes. No se utiliza la captura completa
como interfaz.

## Contratos comprobados mediante lectura

| Operación | Solicitud | Respuesta principal | Uso en frontend |
|---|---|---|---|
| `POST /api/auth/login` | `{ userName, password }` | token y usuario | `authApi.login()` |
| `GET /api/auth/me` | Bearer | perfil del usuario | restauración/validación de sesión |
| `GET /api/auth/me/laboratorios` | Bearer | alcance y laboratorios | carga de alcance posterior al login |

El cliente Axios toma `VITE_API_URL` y, si no está definida, usa
`http://localhost:8080/api`. El interceptor agrega `Authorization: Bearer ...`
cuando existe token.

## Arquitectura implementada

```text
main.jsx → App
            ├─ /login → Login
            └─ * → Protegida → Layout
                              ├─ BarraLateral
                              ├─ Encabezado
                              ├─ pantalla seleccionada
                              └─ PiePagina

Login → AuthContext → authApi → Axios → backend
                         ↓
                  Bearer en solicitudes
```

`AuthContext` coordina usuario, token, alcance, login y logout.
Los servicios concentran las llamadas HTTP. `App` decide la composición
de las rutas y `Protegida` evita mostrar el panel cuando no hay sesión.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Base mínima React/Vite y montaje | Implementado |
| 2 | Cliente HTTP y conexión con backend | Implementado |
| 3 | Estilos compartidos y composición del login | Implementado |
| 4 | Formulario controlado y visibilidad de contraseña | Implementado |
| 5 | Login real → perfil → alcance | Implementado |
| 6 | Panel, encabezado, menú y contenido inicial | Implementado |
| 7 | Logout, 401 y tratamiento básico de errores | Implementado |
| 8 | Revisión móvil, teclado y estados visuales | Implementado en código; verificación manual pendiente |
| 9 | Comprobaciones técnicas y funcionales | Pendiente de evidencia completa |
| 10 | Documentación de resultados y cierre | En actualización |

## Evidencia de implementación

- `Login.jsx` contiene campos controlados, `submit`, estado de carga, error y visibilidad de contraseña.
- `AuthContext.jsx` realiza login, consulta el alcance y conserva token/usuario.
- `api.js` agrega Bearer y procesa 401.
- `App.jsx` separa `/login` del resto de la aplicación protegida.
- `Protegida.jsx` participa en la protección de la aplicación.

Al cerrar FE-01 se recomienda comprobar login válido/inválido, recarga con sesión,
logout, 401, backend caído, doble envío y navegación por teclado.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- [Antecedente de FE-01 en la versión Marko](../sprints_realizados-frontend-Marko/fe-01-login-sesion.md), conservado como historia; sus resultados no certifican la versión Jason.
- `src/pantallas/Login.jsx`, `src/contextos/AuthContext.jsx`, `src/servicios/api.js`.
- Referencias visuales del proyecto en `docs/frontend/mockup/`.

El documento actualiza el estado histórico aportado para FE-01 con el frontend
realizado posteriormente. No implica que todas las pruebas manuales estén cerradas.
