# Plan de sprints del frontend — Inventario de Laboratorios

**Versión:** propuesta v1 para implementación incremental.

**Base documental:** `estructura-frontend-y-adaptacion-inventario.md`, los diez mockups adjuntos y los contratos del backend documentados hasta Sprint 6. Antes de programar, contrastar estos contratos con el repositorio vigente y con `docs/backend-final/endpoints.md` si existe. Este plan no certifica que se haya ejecutado el cierre del backend ni que ya exista una aplicación frontend.

**Identificación:** FE-00 a FE-08, independiente de la numeración de los sprints del backend.

**Actualización de estado — 3 de octubre de 2026:** este documento conserva el
plan inicial basado en Tech Store. La implementación activa es
[frontend Jason](../../frontend/version-jason/frontend/README.md), que usa
React Router y Axios y dispone de dashboard. Su
[índice de sprints](../sprints_realizados-frontend-Jason/README.md) y la
[evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
registran el avance real: Jason adaptado a V13, 67 pruebas frontend aprobadas,
flujo Docker `e1ce75a` validado y ambas imágenes publicadas.
Los identificadores FE de este plan no equivalen a los FE de Jason; por ejemplo,
FE-08 aquí propone consolidación y FE-08 de Jason describe Mantenimientos,
actualmente integrado. Usuarios administrativos y Reportes también están
implementados en Jason; Configuración persistida continúa fuera del alcance.
Las decisiones propuestas abajo no deben interpretarse como el estado actual
ni como una obligación de reemplazar la arquitectura de Jason.

## 1. Objetivo y límites

Construir una primera versión funcional que conserve el diseño de los mockups y consuma el backend existente. No ampliar automáticamente el backend para reproducir controles que todavía no tienen soporte.

La referencia Tech Store aporta React, Vite, JavaScript/JSX, pantallas, componentes, estado, props, formularios controlados, `fetch` y CSS normal. No trasladar al inventario su login simulado, datos comerciales, carrito, API de productos o reglas de cantidad.

Primera versión funcional: login, panel común, consulta/detalle/alta/edición/baja de equipos, clasificación, organización, traslado, historial y asignación de laboratorios a un usuario existente identificado.

Quedan fuera: CRUD/listado general de usuarios, recuperación de contraseña, sesión persistente con refresh token, fotos/adjuntos, agenda de mantenimiento, reportes históricos guardados, ajustes globales, notificaciones, doble factor y permisos dinámicos. El dashboard independiente no tiene un mockup propio: inicialmente se entra a Equipos.

## 2. Fuentes y decisiones

| Fuente | Define |
|---|---|
| Mockups 01–10 | Composición, identidad visual, jerarquía, navegación y aspecto de componentes |
| Guía de estructura frontend | Organización técnica y adaptación del ejercicio React al inventario |
| Contratos actuales del backend | Campos, endpoints, permisos, validaciones y persistencia disponible |
| Este plan | Incrementos de trabajo, decisiones propuestas, entregables y aceptación |

Ante una diferencia, conservar la intención visual y ajustar la operación al contrato real. Registrar la adaptación; no inventar campos ni simular una persistencia inexistente.

### Decisiones iniciales propuestas

- React + Vite, JavaScript/JSX, CSS normal y `fetch`, siguiendo el taller. Conservar versiones compatibles y verificarlas durante FE-00; no interpretar versiones de la guía como obligación de actualización.
- Navegación inicial por estado y props. URLs por pantalla, enlaces profundos e historial del navegador quedan como evolución explícita, no como una biblioteca añadida sin decisión.
- Sesión en memoria para la primera versión. Recargar exige iniciar sesión nuevamente, pero los datos guardados se conservan en el backend. No implementar Recordarme ni almacenar contraseñas.
- Frontend ejecutable en `frontend/`; planificación y evidencias en `docs/frontend/`.
- Equipos será la primera pantalla tras autenticar. El dashboard independiente queda pendiente de diseño.
- Componentes y estilos reutilizables se crean al necesitarlos, no como decenas de archivos vacíos.
- Cada sprint termina con un incremento integrado, no solo una maqueta con datos ficticios.

## 3. Adaptación de los diez mockups

| Mockup | Entrega inicial | Diferencias que se deben respetar |
|---|---|---|
| 01 Login | FE-01 | Pedir Usuario y enviar `userName`. Omitir acciones no disponibles: recuperación, Recordarme y acceso de prueba sin autenticación real. Ajustar el texto que anuncia mantenimiento como módulo. |
| 02 Equipos | FE-02/FE-03 | Estados reales, datos autorizados y filtros existentes. Sin fotografías ni último mantenimiento inventados. Búsqueda y páginas locales deben identificarse como tales. |
| 03 Nuevo equipo | FE-03 | Subcategoría obligatoria, responsable opcional, campos actuales y resumen lateral. Sin subida de archivos o fechas inexistentes. |
| 04 Clasificación | FE-04 | Categorías y subcategorías activas; consulta para tres roles y escritura ADMIN. Sin listado simulado de inactivos. |
| 05 Organización | FE-05 | Sede → Área → Laboratorio. Sin capacidad, responsable de laboratorio o estado de mantenimiento no definidos. |
| 06 Usuarios | FE-07 parcial | Asignaciones a un usuario identificado por ID y validado mediante GET administrativo. Sin lista completa, métricas, alta/edición de cuentas o último acceso. |
| 07 Movimientos | FE-06 | Traslados confirmados, actor real e historial autorizado. Sin estados pendientes/en proceso o creación de otros tipos. |
| 08 Mantenimiento | Backlog | No confundir bandera/estado del equipo con programación de órdenes o calendario. |
| 09 Reportes | Backlog; contadores simples en FE-02 | Estadísticas presentes solo sobre datos autorizados descargados. Sin series mensuales de mantenimiento, tendencias o historial de exportaciones ficticios. |
| 10 Configuración | Backlog | Sin ajustes globales o controles de seguridad que aparenten estar activos. Preferencias locales, si se acuerdan, se rotulan como locales. |

El menú funcional conserva el orden relativo de los módulos disponibles. En una presentación visual se pueden enseñar los demás accesos como «Próximamente», pero no deben llevar a una falsa pantalla operativa. La versión de trabajo puede ocultarlos. Las imágenes de referencia no se utilizan como un fondo que simule toda la aplicación: los controles deben ser componentes reales.

## 4. Resumen de sprints

| Sprint | Nombre | Dependencia | Resultado |
|---|---|---|---|
| FE-00 | Preparación, contratos y base técnica | Guía y backend documentado | React ejecutable, conexión preparada y límites acordados |
| FE-01 | Identidad visual, login y panel | FE-00 | Inicio real de sesión, navegación y layout común |
| FE-02 | Listado, filtros y detalle de equipos | FE-01 | Primera consulta funcional del inventario |
| FE-03 | Alta, edición y baja de equipos | FE-02 | Gestión de equipos según rol y alcance |
| FE-04 | Categorías y subcategorías | FE-03 | Clasificación administrable |
| FE-05 | Sedes, áreas y laboratorios | FE-04 | Organización navegable y administrable |
| FE-06 | Traslado e historial | FE-05 | Flujo de ubicación y trazabilidad completo |
| FE-07 | Asignaciones de laboratorios | FE-06 | ADMIN configura alcance de un usuario existente |
| FE-08 | Consolidación y cierre frontend | FE-00–FE-07 | Entrega integrada y documentada |

La consulta de catálogos necesaria para formularios se implementa antes que sus pantallas administrativas. Por ejemplo, FE-03 puede utilizar GET de subcategorías aunque su CRUD visual corresponda a FE-04.

## 5. FE-00 — Preparación, contratos y base técnica

**Objetivo:** disponer de una aplicación ejecutable y de un alcance que Codex pueda implementar sin rediseñar el backend.

### Historias y tareas

- **FE00-01 — Inspección:** revisar `frontend/`, la guía, imágenes y contratos actuales. La guía indica que inicialmente solo existe un README; confirmar antes de crear el proyecto. No sobrescribir una aplicación que pudiera haberse creado después.
- **FE00-02 — Configuración:** preparar React/Vite en JavaScript y JSX; conservar README, configurar ESLint, scripts dev/build/lint/preview y lock reproducible. No copiar node_modules ni dist del ejercicio.
- **FE00-03 — Contratos:** crear una matriz Pantalla → operación → endpoint → request → response → rol → adaptación del mockup. Confirmar que no haya endpoints de cierre posteriores que cambien lo documentado.
- **FE00-04 — HTTP:** preparar `src/servicios/api.js` y `.env.example` con dirección pública de API. Usar la propuesta `VITE_API_URL=/api` y proxy local a `http://localhost:8080` sin duplicar el prefijo `/api`. Puerto frontend 5173 con `strictPort`, según la guía.
- **FE00-05 — Evidencias:** definir cómo se ejecutarán comprobaciones funcionales de componentes y navegador. La referencia no tiene script test; cualquier herramienta y script nuevos deben configurarse explícitamente y documentarse.

### Responsabilidades técnicas

`api.js` centraliza URL, cabeceras, token cuando corresponda y errores HTTP. Debe distinguir JSON de respuesta vacía, especialmente 204. No interpretar un error como `[]`. Los servicios por módulo no deciden qué pantalla se dibuja.

No colocar DB_PASSWORD, credenciales PostgreSQL, JWT_SECRET o contraseñas demo en variables VITE. La dirección de API es pública; los secretos siguen en el backend. El proxy de desarrollo no es una solución de despliegue.

### Archivos iniciales

`package.json`, `package-lock.json`, `vite.config.js`, `eslint.config.js`, `index.html`, `.gitignore`, `.env.example`, `src/main.jsx`, `src/App.jsx`, `src/index.css` y `src/servicios/api.js`.

Documentación propuesta: `docs/frontend/plan-sprints-frontend.md`, `docs/frontend/matriz-mockup-api.md`, `docs/frontend/decisiones-frontend.md` y `docs/frontend/sprints/fe-00-base.md`.

### Aceptación

La aplicación inicia en el puerto previsto; lint y build se ejecutan; el proxy alcanza un endpoint real y permite observar una respuesta HTTP del backend. Un 401 de un recurso protegido confirma conectividad, pero no un login correcto. No hay secretos ni cambios de negocio en el backend. Los límites de cada pantalla están registrados.

## 6. FE-01 — Identidad visual, login y panel

**Objetivo:** entrar al inventario con autenticación real y una estructura visual compartida.

**Referencia:** mockup 01 y estructura común de 02–10.

### Historias y tareas

- **FE01-01 — Diseño común:** construir fondo claro/celeste, paneles blancos, títulos oscuros, menú seleccionado celeste, acciones destacadas rojas, campos y etiquetas consistentes. Definir variables CSS y verificar contraste. Mantener identidad de Inventario; no incorporar logo institucional UTEC.
- **FE01-02 — Layout:** crear `PanelInventario`, `Encabezado`, `MenuLateral`, `TituloPagina` y controles compartidos. Mostrar nombre/rol del usuario real, no Marko/Administrador hardcodeados.
- **FE01-03 — Login:** crear `Login`, `CampoContrasena` y `authService`. Enviar `userName` y `password` a POST `/api/auth/login`; no recortar la contraseña.
- **FE01-04 — Sesión:** conservar token/usuario en memoria, consultar perfil y alcance, navegar inicialmente a Equipos y limpiar sesión/datos privados al cerrar sesión.
- **FE01-05 — Errores:** diferenciar credenciales incorrectas en login, sesión inválida en recursos protegidos, permisos insuficientes y conexión fallida.

### Endpoints

POST `/api/auth/login`, GET `/api/auth/me`, GET `/api/auth/me/laboratorios`.

### Adaptaciones

«Correo institucional» cambia a «Usuario». No activar Recordarme, recuperación ni acceso de prueba. El logout local limpia la sesión del cliente: no inventar endpoint de revocación. La campana sin servicio de notificaciones no muestra alertas ficticias. El buscador global no aparenta consultar usuarios/equipos/laboratorios sin una implementación acordada.

La navegación inicial por estado no ofrece enlaces profundos ni historial de pantalla en la URL. Esa limitación debe quedar documentada.

### Aceptación

Login válido abre el panel; inválido mantiene el formulario con error. Cerrar sesión impide volver a consultar datos privados desde la interfaz; los datos del usuario anterior no quedan en caché compartida. Un 403 no se trata como logout automático. Recargar solicita login conforme a la sesión en memoria. Menú utilizable con teclado y adaptable a móvil.

## 7. FE-02 — Listado, filtros y detalle de equipos

**Objetivo:** convertir el mockup 02 en una vista conectada a datos reales.

### Historias y tareas

- **FE02-01 — Listado:** crear `Equipos`, `FilaEquipo`, `EtiquetaEstado`, `IndicadorResumen` y `equiposService`.
- **FE02-02 — Filtros:** conectar estado, `idLaboratorio`, `idSubcategoria` y `requiereMantenimiento` a los filtros reales del servidor.
- **FE02-03 — Detalle:** crear `DetalleEquipo` derivado de la estética del formulario, sin inventar un mockup adicional aprobado.
- **FE02-04 — Búsqueda/páginas:** si se incluyen, hacerlas locales sobre la lista autorizada ya recibida. Identificar claramente que no son búsquedas o paginación del servidor. Una respuesta antigua no debe sobrescribir el resultado de filtros más recientes.
- **FE02-05 — Contadores:** calcular cifras del resultado autorizado y declarar si corresponden al conjunto consultado, filtrado o completo del alcance. No inventar tendencias mensuales.

### Endpoints

GET `/api/equipos`, GET `/api/equipos/{id}` y consultas de catálogos/alcance para preparar filtros. GET `/api/admin/equipos` queda reservado a una vista ADMIN si resulta necesario; no se necesita duplicar pantallas.

### Adaptaciones

Mostrar Subcategoría en lugar de afirmar que el resumen ya contiene Categoría. La categoría puede enriquecerse con datos realmente disponibles, sin perder los equipos BAJA cuyo catálogo esté inactivo. Sustituir imágenes inexistentes por un icono genérico, y último mantenimiento por un dato real pertinente, como fecha de actualización. No mostrar las cifras de ejemplo 128/96/12 ni variaciones porcentuales como datos.

BAJA permanece visible cuando el backend lo devuelve. No habilitar edición/baja/traslado de BAJA. La tabla no debe fingir que el icono de papelera realiza borrado físico.

### Aceptación

ADMIN ve el conjunto global permitido; GESTOR/LECTOR ven sus equipos. Sin asignaciones se presenta un vacío válido. Los cuatro filtros se combinan y no amplían el alcance. Se distinguen carga, vacío, sin coincidencias locales y error. El detalle refleja la respuesta del backend.

## 8. FE-03 — Alta, edición y baja de equipos

**Objetivo:** completar el flujo operativo del inventario usando el mockup 03.

### Historias y tareas

- **FE03-01 — Formulario compartido:** crear `FormularioEquipo`, `NuevoEquipo`, `EditarEquipo`, `SelectorLaboratorio` y validaciones compartidas reales.
- **FE03-02 — Selección:** cargar categorías/subcategorías mediante servicios de consulta, y laboratorios permitidos mediante alcance. Categoría es una ayuda; el request guarda `idSubcategoria`.
- **FE03-03 — Alta:** construir expresamente CreateEquipoRequest, con IDs numéricos y Boolean real para mantenimiento.
- **FE03-04 — Edición:** cargar detalle, preparar formulario y construir exclusivamente UpdateEquipoRequest. No enviar indiscriminadamente todo EquipoResponse.
- **FE03-05 — Baja:** añadir `ConfirmacionAccion` con identificación del equipo y explicación de BAJA; tratar 204 sin intentar leer JSON.

### Contratos

POST `/api/equipos`, PUT `/api/equipos/{id}`, DELETE `/api/equipos/{id}`.

Alta exige código interno, nombre, laboratorio, subcategoría, estado y bandera de mantenimiento. Estados editables: OPERATIVO, MANTENIMIENTO e INOPERATIVO. BAJA se solicita exclusivamente mediante DELETE.

La edición no envía código interno, laboratorio, ID ni fechas del servidor. Conserva como lectura los datos inmutables. Los opcionales se limpian solo conforme a una decisión explícita del usuario/formulario, pues PUT reemplaza campos editables.

### Responsable sin directorio disponible

No inventar una lista de usuarios ni extraerla de responsables de equipos como si fuera un directorio completo. Primera versión propuesta: nuevo equipo sin asignación opcional de responsable; en edición, mostrar el actual en lectura y preservar su ID en el request. Ocultar el selector NO significa omitir el valor existente y borrar accidentalmente la asignación. La selección de otro responsable queda documentada como dependencia de un origen de datos acordado.

### Adaptaciones visuales

Subcategoría pasa a obligatoria y Responsable deja de mostrar asterisco. Eliminar fecha de compra/último mantenimiento y añadir campos reales como año, orden de compra, serie UTEC y bandera de mantenimiento. Observaciones representa `comentario`, sin heredar un límite de 500 inexistente para ese campo.

La columna derecha conserva el resumen/vista previa; no muestra un cargador de fotos/documentos funcional sin backend. Se puede dedicar ese espacio a la lectura de la selección actual y a ayuda contextual.

### Aceptación

ADMIN/GESTOR autorizado registran, editan y dan de baja. LECTOR no tiene acciones de escritura. Un conflicto 409 conserva el formulario y explica la causa. Tras éxito se usa la respuesta confirmada o se recarga la consulta. La edición preserva responsable no editado, código y laboratorio. Los envíos duplicados se bloquean mientras una solicitud está en curso.

## 9. FE-04 — Categorías y subcategorías

**Objetivo:** implementar la composición maestro–detalle del mockup 04.

### Historias y tareas

- **FE04-01:** lista de categorías y selección que carga subcategorías del padre real.
- **FE04-02:** formularios/modales de creación y edición para ambos recursos.
- **FE04-03:** confirmación de baja y presentación del conflicto cuando existen hijas/equipos que la impiden.
- **FE04-04:** lectura para los tres roles; creación/edición/baja exclusivamente ADMIN.

### Endpoints

CRUD de `/api/categorias` y `/api/subcategorias`; GET `/api/categorias/{idCategoria}/subcategorias`.

### Archivos

`Categorias.jsx`, componentes de lista/panel creados según reutilización, `ModalFormulario`, `catalogosService`.

### Aceptación

Cambiar categoría cambia sus hijas visibles. Los nombres y IDs corresponden al backend. La baja solo desaparece de la lista después de confirmación exitosa. Los listados normales muestran activos: no se fabrica una tabla de inactivos. Modales accesibles con foco, cerrar/cancelar y mensajes de validación.

## 10. FE-05 — Sedes, áreas y laboratorios

**Objetivo:** implementar árbol, tabla y detalle del mockup 05.

### Historias y tareas

- **FE05-01:** construir `ArbolOrganizacion` con sedes, áreas y laboratorios realmente recibidos.
- **FE05-02:** sincronizar selección del árbol, tabla y panel de detalle.
- **FE05-03:** formularios de alta/edición/baja para ADMIN; consultas para los tres roles.
- **FE05-04:** controlar cambios de padre y conflictos por hijos, equipos o asignaciones activas.

### Endpoints

CRUD de `/api/sedes`, `/api/areas`, `/api/laboratorios`; GET `/api/sedes/{idSede}/areas`; GET `/api/areas/{idArea}/laboratorios`.

### Adaptaciones

Laboratorio no tiene capacidad, responsable, descripción ni estado «En mantenimiento» en el contrato documentado. Mostrar código, nombre, ubicación y jerarquía reales. No deducir un responsable por ser el primer usuario asignado. Los catálogos son globales, pero el inventario de cada laboratorio no lo es: no mostrar cero equipos como conclusión de no tener permiso para consultarlos.

Si se añade un contador, debe proceder de datos autorizados y expresar su alcance. Los registros inactivos no se listan como si hubiese una API que los suministre.

### Aceptación

El árbol y la tabla se mantienen sincronizados después de operaciones confirmadas. Padres inactivos/no disponibles no se ofrecen como nuevos destinos. Un 409 conserva la selección y explica la restricción. GESTOR/LECTOR no pueden abrir formularios administrativos operativos.

## 11. FE-06 — Traslado e historial

**Objetivo:** conectar el flujo de traslado y adaptar el mockup 07 al historial real.

### Historias y tareas

- **FE06-01:** crear `FormularioTraslado`, desde detalle de un equipo autorizado no BAJA.
- **FE06-02:** ofrecer destinos permitidos y distintos del origen actual; para GESTOR exigir alcance en ambos extremos.
- **FE06-03:** crear `Movimientos`, `TablaMovimientos`, historial por equipo y actividad reciente basada en los eventos devueltos.
- **FE06-04:** filtro de laboratorio del servidor. Texto/fechas/tipo, si se incorporan, son filtros locales declarados.
- **FE06-05:** actualizar vistas solo con traslado confirmado; manejar datos de permisos que hayan cambiado desde que se abrió el formulario.

### Endpoints

POST `/api/equipos/{idEquipo}/traslados`; GET `/api/equipos/{idEquipo}/movimientos`; GET `/api/movimientos?idLaboratorio=...`.

El request contiene destino, motivo y ubicación interna destino opcional. No contiene origen, actor, tipo, fecha, responsable ni estado. Motivo: máximo 500; ubicación: máximo 200, según contrato vigente. Advertir que omitir ubicación destino limpia la ubicación interna anterior.

### Adaptaciones

La columna debe decir Actor del traslado, no confundirlo con responsable/custodio. No inventar Pendientes, En proceso o una agenda de movimientos. Los eventos de este flujo están confirmados; el estado «Enviando» pertenece únicamente al formulario.

El frontend conserva la política de historia parcial: un movimiento puede ser visible por uno de sus extremos aunque el detalle actual del equipo esté fuera de alcance. No descartar esos eventos por anticipado. Un enlace al detalle puede responder 403 y se debe explicar sin tratarlo como fallo de sesión.

No deducir la ubicación actual del equipo exclusivamente de la primera fila del historial; utilizar el EquipoResponse confirmado. Origen histórico null debe presentarse sin romper la tabla.

### Aceptación

Un traslado confirmado actualiza equipo e historial; el frontend nunca intenta fabricar las dos escrituras separadamente. Un rechazo no cambia la ubicación local como si hubiera éxito. LECTOR no traslada. Los eventos, filtros y actividad reciente respetan los resultados autorizados del backend.

## 12. FE-07 — Asignaciones de laboratorios a usuarios existentes

**Objetivo:** implementar únicamente la parte soportada del mockup 06.

### Historias y tareas

- **FE07-01:** pantalla `AsignacionesLaboratorio` exclusiva de ADMIN.
- **FE07-02:** permitir introducir un ID público de usuario y consultar GET administrativo; mostrar nombre/rol devueltos antes de permitir cambios.
- **FE07-03:** preparar panel de selección de laboratorios activos, con búsqueda local opcional.
- **FE07-04:** guardar el conjunto completo mediante PUT, permitir cancelar y advertir cuando quede vacío.
- **FE07-05:** mostrar alcance propio en perfil/panel sin confundirlo con el catálogo global.

### Endpoints

GET/PUT `/api/admin/usuarios/{idUsuario}/laboratorios`; GET `/api/auth/me/laboratorios`; GET `/api/laboratorios`.

### Límites y cuidado de datos

No hay listado/CRUD de usuarios: no inventar filas, métricas, último acceso ni botones de alta/cambio de rol. No enumerar IDs para construir un listado oculto. Introducir el ID es una solución inicial explícita, no la UX final de un directorio.

PUT reemplaza todas las asignaciones. Filtrar la lista visual de checkboxes no debe eliminar las selecciones ocultas. Enviar siempre el conjunto completo. `[]` significa retirar todas las asignaciones activas; confirmar esa intención.

Una asignación explícita vacía de ADMIN no significa alcance global vacío. Ante 409 por un laboratorio ahora inactivo, conservar selección y ofrecer refrescar la lista.

### Aceptación

ADMIN identifica al destinatario, consulta, modifica y verifica el conjunto. GESTOR/LECTOR no acceden a administración. No se crean usuarios ficticios para simular el mockup. Se explica la limitación de identificación por ID y se conserva el alcance efectivo del principal como concepto separado.

## 13. FE-08 — Consolidación y cierre frontend

**Objetivo:** cerrar una primera versión funcional, adaptable y reproducible.

### Historias y tareas

- **FE08-01 — Revisión visual:** comparar cada pantalla implementada con su referencia y con la matriz de adaptaciones aprobada.
- **FE08-02 — Responsive:** verificar escritorio, tablet y móvil; menú colapsable, formularios en una columna y tablas legibles. No hay diseños móviles suministrados: registrar estas adaptaciones como decisiones de implementación.
- **FE08-03 — Accesibilidad:** etiquetas, foco visible, navegación por teclado, control del foco en modales, mensajes y estados no comunicados solo mediante color.
- **FE08-04 — Recorridos:** login, equipos, edición, baja, traslado, historial, catálogos y asignaciones con ADMIN/GESTOR/LECTOR.
- **FE08-05 — Errores:** 400 por campo, 401 de sesión, 403 de permiso, 404, 409, backend apagado, listas vacías y respuestas tardías.
- **FE08-06 — Entrega:** lint, build, pruebas configuradas, revisión de la compilación, README, contratos y limitaciones. Documentar cómo la compilación encuentra su API; no confundir el proxy dev con despliegue.

### Pruebas y evidencia

Las pruebas del backend no sustituyen las del frontend. Codex debe ejecutar las verificaciones que su entorno permita y adjuntar resultados reales, no solo escribir que pasó. Si no pudo ejecutar navegador o integración, marcarlo como pendiente verificable. No excluir fallos para obtener un conteo verde.

Integraciones que escriban utilizarán exclusivamente una base de verificación del patrón `inventario_verificacion_*`. No ejecutar borrados, asignaciones demo o traslados de prueba sobre la base habitual. Los datos simulados de tests se mantienen aislados y nunca se presentan como respuesta real de la API.

### Aceptación final

Se puede completar desde el navegador un recorrido autorizado de principio a fin. Los datos confirmados persisten al volver a iniciar sesión y consultarlos. No hay acciones ficticias, secretos, pantallas de producción alimentadas con mockups, ni promesas de funciones fuera de alcance. Se conocen las limitaciones de sesión, navegación, búsquedas locales y selección de usuarios.

## 14. Definición de terminado de cada sprint

Cada incremento debe entregar:

1. Código integrado y archivos creados/modificados con propósito.
2. Pantallas/componentes construidos y mockups asociados.
3. Endpoints realmente consumidos y roles comprobados.
4. Diferencias respecto del diseño registradas en la matriz.
5. Estados de carga, envío, vacío y error implementados.
6. Resultado real de lint/build y de las pruebas configuradas.
7. Evidencia visual cuando pueda ejecutarse el navegador.
8. Documentación `docs/frontend/sprints/fe-XX-*.md` y pendientes específicos.

No hacer todos los sprints en una sola orden a Codex. Mantener este plan como contexto común y ejecutar un incremento, revisar su reporte y continuar con el siguiente.

## 15. Funcionalidades futuras y sus dependencias

| Función deseada | Dependencia antes de activarla |
|---|---|
| Directorio y CRUD de usuarios | Contratos de listado, consulta y administración de cuentas |
| Selector completo de responsables | Fuente de usuarios públicos autorizada para esa selección |
| Fotos/documentos | Subida, almacenamiento y acceso a adjuntos definidos |
| Mantenimiento y calendario | Modelo/operaciones de programación y seguimiento |
| Reportes históricos guardados | Definición de métricas, datos y almacenamiento de exportaciones |
| Exportar datos visibles | Función adicional de cliente acordada, con alcance y formato declarados |
| Dashboard independiente | Diseño y definición de indicadores reales, sin porcentajes ficticios |
| Recordarme/recuperación | Estrategia de sesión o API correspondiente |
| Notificaciones y ajustes globales | Contratos funcionales y persistencia |
| Doble factor y permisos dinámicos | Ampliación de seguridad fuera del backend actual |
| Navegación por URL | Decisión explícita de routing y configuración asociada |

Los mockups futuros siguen siendo referencia del producto deseado. Este cierre entrega la primera versión funcional compatible con el backend, no afirma que las diez imágenes estén reproducidas íntegramente como módulos operativos.

## 16. Próxima acción

Ejecutar únicamente **FE-00**: inspección, matriz mockup–API, base React/Vite y conexión local. No rehacer el backend ni implementar a la vez login, CRUD, reportes y mantenimiento.
