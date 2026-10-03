# Auditoría de integración — Frontend Jason

Fecha: 3 de octubre de 2026.

**Estado: integración local operativa y flujo completo Docker comprobado. La auditoría inicial conserva build, 27/27 pruebas y 42 comprobaciones HTTP; la validación posterior Docker registra 33/33 HTTP adicionales y 24/24 aserciones de flujo y consistencia. Actions publicó ambas imágenes con trabajos en verde. FE-08 a FE-10 continúan pendientes; no se declara fidelidad visual 1:1 ni cierre de los once módulos.**

## Actualización posterior: Docker y GitHub Actions

La [evidencia consolidada del 3 de octubre de 2026](../despliegue/verificacion-docker-actions-2026-10-03.md)
documenta dos comprobaciones posteriores a la auditoría Vite que se conserva abajo:

- **Flujo Docker:** mismas imágenes del entorno habitual, frontend Nginx temporal
  en 3001, backend en 18081 y base `inventario_verificacion_docker_26e35749`.
  Login ADMIN/GESTOR/LECTOR; alta, lectura, edición, traslado 201 → 206, historial
  con actor/origen/destino/motivo, baja sin acciones, filtros, logout, recarga a
  Login y datos conservados al reingresar. LECTOR recibió 403 fuera del laboratorio
  actual del equipo y 200 al consultar el historial permitido por origen.
- **Resultados Docker:** 33/33 HTTP adicionales y 24/24 aserciones de flujo y
  consistencia, sin fallos; consola sin errores ni advertencias. Son comprobaciones
  del recorrido, no nuevas pruebas JUnit ni una regresión completa del backend.
- **Base y limpieza:** Flyway V1–V9 exitosas, cinco consultas de inconsistencia con
  resultado 0 y conteos de nueve tablas habituales iguales antes/después. Base,
  contenedores, volumen y red temporales eliminados. Tres contenedores habituales
  saludables; frontend actual en 3000 y backend en 8080.
- **Actions:** [ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900)
  exitosa sobre `99569392035fc975171d2df6929a1aa26111b139`; trabajos backend
  `111240944686` y frontend `111240944823` exitosos. Jason ejecutó 27/27 pruebas
  antes de publicar.
- **GHCR:** imágenes backend y frontend-jason confirmadas con `latest` y
  `sha-9956939`; los digests están en el informe consolidado. Publicar no implica
  despliegue en la nube ni actualización automática del entorno local.

Esta actualización conserva el resultado y los límites de cada ejecución. No suma
las 42 HTTP anteriores y las 33 posteriores como una suite nueva, ni agrega las
24 aserciones a las 27 pruebas automatizadas o a las 233 históricas de Gradle.

## Alcance y criterio de evidencia

Se leyeron completos los doce Markdown originales de esta carpeta: `README.md` y
FE-01 a FE-11. Se contrastaron con `frontend/version-jason/frontend/src/`, sus
dependencias, los contratos relevantes del backend y las diez imágenes de
`docs/frontend/mockup/`. No se usó la versión Marko como evidencia de funcionamiento
de Jason. No se encontraron instrucciones `AGENTS.md` aplicables durante la revisión.

Los sprints conservan las afirmaciones de la entrega original. Los hallazgos de esta
auditoría las precisan cuando son incompletas o contradicen el código. Las referencias
de línea de la sección de hallazgos corresponden a la lectura inicial, anterior a las
correcciones; pueden desplazarse al editar los archivos.

- **Implementado por lectura:** existe código que expresa el recorrido; no equivale
  a una prueba exitosa con backend.
- **Corrección en validación:** existe una modificación del código inspeccionada,
  y su alcance solo se da por comprobado en los escenarios registrados.
- **Visual o pendiente:** existe un placeholder, un control decorativo o una parte
  de la referencia todavía sin recorrido funcional.
- **Comprobado por ejecución:** requiere indicar escenario, entorno y resultado;
  no se asigna este estado a partir de una lectura de código o de un mockup.

La severidad **P1** corresponde al riesgo de operar sobre un usuario distinto;
**P2**, a recorridos, datos o permisos de interfaz incorrectos; **P3**, a limitaciones
de presentación o documentación sin ese impacto inmediato. El backend conserva la
responsabilidad de autorizar cada operación.

## Matriz FE-01 a FE-11

| Sprint | Estado de integración después de la revisión | Diferencias o límites |
|---|---|---|
| [FE-01](fe-01-login-sesion.md) | Login de tres roles, logout, recarga a Login y error de credenciales comprobados. Sesión coherente en memoria, manejo de 401 y respuestas antiguas; 10 pruebas de lógica. | Sin Recordarme, recuperación ni persistencia de sesión. Escenarios de expiración/concurrencia cubiertos por lógica, no todos reproducidos en navegador. |
| [FE-02](fe-02-dashboard.md) | Resúmenes e historial comprobados, errores/reintento y valores indisponibles diferenciados. El contador consulta alcance actualizado al entrar, también para ADMIN. | No hay mockup independiente. No se certifica cada combinación de datos ni accesibilidad exhaustiva. |
| [FE-03](fe-03-equipos.md) | Alta, edición, traslado y baja por UI; filtros OPERATIVO/BAJA; permisos y campos inmutables; BAJA sin acciones. CRUD GESTOR, filtros y errores adicionales por HTTP. | Tarjetas y modal, en lugar de tabla/página de mockup. Sin ficha completa, adjuntos, paginación, exportación ni selector nominal de responsables. |
| [FE-04](fe-04-categorias-subcategorias.md) | Categoría creada/editada y subcategoría relacionada por UI. CRUD y conflicto de padre con hijas comprobados por HTTP. Se usa activo real. | Pestañas en lugar de maestro/detalle; no se certificó cada baja mediante navegador. |
| [FE-05](fe-05-laboratorios-sedes-areas.md) | Laboratorio creado/editado por UI, sede resuelta correctamente y árbol expandible. CRUD de sede/área/laboratorio probado por HTTP. | Sin ficha detallada, búsqueda del árbol ni filtros combinados del mockup. Mantenimiento de laboratorio no tiene contrato actual. |
| [FE-06](fe-06-usuarios-roles-asignacion.md) | Selector real; asignación de 2 laboratorios a GESTOR y 1 a LECTOR por UI. Cambiar ID invalida resultado. 7 pruebas de identidad, lista vacía, doble envío y respuestas tardías. | Consulta por ID; no existe directorio, creación ni cambio administrativo de roles. Vaciar asignaciones no se ejecutó por UI. |
| [FE-07](fe-07-movimientos.md) | Traslado e historial con fecha/tipo/origen/destino/actor/motivo comprobados. Filtro de laboratorio y lectura histórica por origen; 5 pruebas de fechas/orden/búsqueda. | Sin rango de fechas, filtro por tipo, exportación ni panel lateral. No existen estados Pendiente/Completado en el contrato. |
| [FE-08](fe-08-mantenimientos.md) | Identificado como Próximamente. | Placeholder: programación, calendario y persistencia no implementados. |
| [FE-09](fe-09-reportes.md) | Identificado como Próximamente. | Placeholder: gráficos, filtros y exportación no implementados. |
| [FE-10](fe-10-configuracion.md) | Placeholder y ruta con guardia ADMIN revisada. | Preferencias y configuración de seguridad no implementadas. |
| [FE-11](fe-11-arquitectura-frontend.md) | Proxy /api, rutas administrativas protegidas, controles honestos, build y 27 pruebas aprobados. Navegación y presentación revisadas en escritorio y móvil. | Sin certificación exhaustiva de accesibilidad ni reproducción visual al píxel. |

## Hallazgos de la entrega original

En esta sección, `src/` es relativo a `frontend/version-jason/frontend/` y `backend/`
designa `backend/inventario/src/main/java/com/utec/inventario/`. Se conserva la
descripción original del defecto aunque los defectos funcionales descritos ya fueron corregidos.

| ID | Prioridad / sprint | Evidencia inicial | Implicación y criterio de corrección |
|---|---|---|---|
| A01 | P1 · FE-06 | `src/pantallas/Asignaciones.jsx:2` mantenía `data/ids` al editar el ID y guardaba con `Number(id)` actual. | Consultar A, escribir B y guardar podía sustituir las asignaciones de B con la lista de A. Guardar debe quedar ligado al usuario consultado; cambiar el ID invalida el resultado anterior. |
| A02 | P2 · FE-06 | `Asignaciones.jsx:2` solo ejecutaba `setIds` al consultar y no ofrecía controles para editar esa lista; FE-06 declaraba actualización implementada. | La consulta existía, pero Guardar reenviaba las mismas asignaciones. Se necesita selección explícita y confirmación del resultado de la API. |
| A03 | P2 · FE-03, FE-10, FE-11 | `TarjetaEquipo.jsx:7` mostraba todas las acciones; `Equipos.jsx:11–14` solo condicionaba Nuevo equipo; `App.jsx:30–35` y `Protegida.jsx:3` no restringían rutas por rol. | Los permisos visuales estaban incompletos. Deben protegerse acciones y rutas administrativas además de ocultarse enlaces; esto no sustituye los permisos del backend. |
| A04 | P2 · FE-01 | `AuthContext.jsx:24–28` publicaba token antes de terminar alcance; `Login.jsx:12` redirigía al existir token. | Un fallo posterior podía ocurrir después de abandonar el login. La sesión debe quedar coherente ante éxito, error de perfil/alcance, restauración y logout. |
| A05 | P2 · FE-02 | `Home.jsx:10` usaba `catch(()=>{})` y dejaba ceros/vacío tras fallar una carga; FE-02 describía manejo de errores implementado. | Distinguir error, carga, ausencia de registros y cifras válidas; no interpretar una respuesta fallida como inventario vacío. |
| A06 | P2 · FE-03 | `Equipos.jsx:14` contaba operativos con `includes('OPERAT')`. | `INOPERATIVO` también coincidía. El KPI debe comparar el estado exacto y respetar el alcance del conjunto mostrado. |
| A07 | P2 · FE-04 | `Catalogo.jsx:27` eliminaba `payload.estado`; `:42` mostraba Activo fijo; `:45` permitía seleccionar Activa/Inactiva. | El selector era inoperante, no solo pendiente de prueba. `backend/dto/response/CategoriaResponse.java:19` devuelve `activo`; la UI debe reflejarlo sin ofrecer una actualización inexistente. |
| A08 | P2 · FE-05 | `Laboratorios.jsx:13,22–23,38,43–44` usaba `estado` y asumía Activo. Los DTO `UpdateSedeRequest`, `UpdateAreaRequest` y `UpdateLaboratorioRequest` no declaran ese campo; sus respuestas contienen `activo`. | La entrega no podía persistir Activo/Inactivo/Mantenimiento como describía FE-05. La interfaz debe respetar el contrato y no inventar mantenimiento de ubicaciones. |
| A09 | P2 · FE-07 | `Movimientos.jsx:2` ejecutaba `setLab` y `setTimeout(cargar,0)` con la función que conservaba el laboratorio anterior. | La selección y la solicitud podían diferir. Cada cambio debe cargar el laboratorio actualmente seleccionado. |
| A10 | P2 · FE-07 | `Movimientos.jsx:2` leía `fecha/createdAt` y `estado`; `FilaMovimiento.jsx:2` usaba `fechaMovimiento`. `backend/dto/response/MovimientoEquipoResponse.java:17–23` contiene `tipoMovimiento/fechaMovimiento`, sin `estado`. | El KPI del día usaba otro campo y Pendientes/Completados no provenían del contrato. El fallback que contaba todo lo no pendiente como completado fabricaba significado. Los indicadores deben derivarse de campos reales y de la fecha local acordada. |
| A11 | P3 · FE-11 | `Encabezado.jsx:8,10` tenía buscador y campana sin acciones; `BarraLateral.jsx:21` mostraba API local/8080 y punto verde fijo. | Eran elementos decorativos; no demostraban búsqueda, notificaciones ni disponibilidad de API. Implementarlos o identificarlos/retirarlos según el alcance. |
| A12 | P3 · FE-03, FE-04, FE-05, FE-11 | Modales en `Equipos.jsx:14`, `Catalogo.jsx:45`, `Laboratorios.jsx:38`; botones de icono y campos de búsqueda compartidos. | Revisar nombres accesibles, foco, Escape, cierre/restauración de foco y navegación por teclado. La inspección no constituye certificación de accesibilidad. |
| A13 | P2 documental · README, FE-01, FE-03 | FE-01 refería la raíz inexistente `frontend/inventario-frontend/` y un enlace histórico roto; FE-03 atribuía tarjetas al mockup tabular; README presentaba la adaptación de todas las referencias sin detallar pendientes. | Raíz e índice se corrigen en esta revisión. Los sprints conservan su historia, con esta auditoría como precisión del estado y las diferencias visuales. |

En la lectura inicial no se encontraron fixtures que reemplazaran las colecciones API. Sí se encontraron
valores o controles presentados sin respaldo funcional: estados Activo fijos,
clasificación artificial de movimientos, controles de Recordarme/recuperación,
búsqueda global, campana y punto verde de API. La ausencia de fixtures no permite
concluir que todos los indicadores fueran correctos. Las correcciones actuales
retiran esos controles engañosos o derivan sus valores del contrato real, según la
matriz anterior; la sección de hallazgos permanece como historia.

## Diferencias con las referencias visuales

Las diez imágenes se inspeccionaron directamente. Esta comparación describe la
estructura que falta o cambia; se complementa con las capturas de ejecución enlazadas abajo, sin constituir una aprobación de fidelidad al píxel. Los valores y personas ilustrados
en los mockups no son datos de prueba del backend.

| Referencia | Diferencia observada en la entrega Jason |
|---|---|
| [01 · Login](../frontend/mockup/01_inicio_sesion_inventario_laboratorios.png) | Conserva dos columnas y acciones principales. La ilustración se aproxima con CSS/Lucide; Recordarme y recuperación eran controles sin recorrido. |
| [02 · Equipos](../frontend/mockup/02_equipos_listado_general.png) | La referencia es una tabla con miniaturas, responsable, último mantenimiento, detalle, paginación y exportación; Jason usa tarjetas sin esas partes. |
| [03 · Nuevo equipo](../frontend/mockup/03_equipos_nuevo_registro.png) | La referencia es una página con categoría, responsable, fechas, adjuntos y resumen; Jason usa un modal con un subconjunto distinto de campos. |
| [04 · Categorías](../frontend/mockup/04_categorias_y_subcategorias.png) | La referencia muestra categoría seleccionada y subcategorías simultáneamente; Jason usa pestañas sin maestro/detalle ni conteo por categoría. |
| [05 · Ubicaciones](../frontend/mockup/05_laboratorios_sedes_y_areas.png) | Existe árbol, pero faltan búsqueda del árbol, selección/ficha de laboratorio y filtros por sede/área/estado. El retiro de la fecha decorativa es una diferencia consciente ya documentada. |
| [06 · Usuarios](../frontend/mockup/06_usuarios_roles_y_asignacion_laboratorios.png) | La referencia muestra listado, roles, KPIs y selección con casillas; la entrega solo consultaba por ID y mostraba las asignaciones recibidas. |
| [07 · Movimientos](../frontend/mockup/07_movimientos_historial_y_actividad.png) | Faltan rango de fechas, tipo de movimiento, exportación y actividad lateral. Los estados de proceso del dibujo no forman parte del DTO de movimientos inspeccionado. |
| [08 · Mantenimientos](../frontend/mockup/08_mantenimientos_programacion_y_calendario.png) | Lista, KPIs, programación y calendario no están maquetados; solo hay un placeholder genérico. |
| [09 · Reportes](../frontend/mockup/09_reportes_indicadores_y_exportacion.png) | No hay gráficos, filtro temporal, exportación, historial ni hallazgos; solo hay un placeholder genérico. |
| [10 · Configuración](../frontend/mockup/10_configuracion_general_y_seguridad.png) | No hay formulario general, secciones ni controles de seguridad funcionales; solo hay un placeholder genérico. |

No existe entre las referencias aportadas una imagen independiente de Dashboard.
Su evaluación visual se limita a la coherencia con el lenguaje compartido. La tabla
describe la entrega original: la corrección ya incorpora casillas de asignaciones,
ID de responsable en equipos y columna de tipo en movimientos, y retira controles
sin función. Estos ajustes no completan las diferencias estructurales de los mockups.

## Contratos y trabajo pendiente del backend

La revisión utiliza las operaciones existentes. No añade contratos de gestión
completa de usuarios, mantenimiento programado, exportación de reportes ni preferencias
de seguridad. Esos módulos necesitan definición e implementación de backend antes
de presentarse como funcionalidad integrada.

Categorías y ubicaciones devuelven `activo`; sus DTO de alta/edición no ofrecen un
estado libre ni Mantenimiento de laboratorio. Movimientos devuelve `tipoMovimiento`
y `fechaMovimiento`, sin estados Pendiente/Completado. Las diferencias de los mockups
en estos puntos se mantienen como pendientes de producto/contrato, sin simularlas.

## Auditoría inicial: entornos y resultados con Vite

La siguiente tabla es evidencia de la revisión inicial. El entorno habitual actual
posterior usa Docker completo en 3000/8080, como se documenta en la actualización.

| Comprobación | Resultado y alcance |
|---|---|
| Build final | npm.cmd run build: correcto, 1670 módulos, 10.85 segundos. |
| Pruebas frontend | npm.cmd test: 27 aprobadas, 0 fallidas, 0 omitidas; 10 sesión + 7 asignaciones + 5 contratos/payloads + 5 movimientos. |
| HTTP por proxy | 42 solicitudes verificadas con estado esperado. No son 42 endpoints diferentes y no sustituyen una regresión completa del backend. |
| Navegador ADMIN | Login, dashboard, asignaciones, crear/editar/trasladar/dar de baja equipo, historial y filtros; crear/editar categoría, crear subcategoría relacionada, crear/editar laboratorio y expandir árbol. |
| Navegador LECTOR | Un laboratorio asignado, sin botones de escritura ni acceso de menú a Asignaciones. Lista de equipos vacía tras traslado, pero conserva dos eventos visibles por origen. Catálogos en lectura. |
| Navegador GESTOR | Dos laboratorios asignados, alta disponible y equipos BAJA sin acciones. CRUD/traslado del gestor también verificados por HTTP. |
| Sesión y errores | Logout y recarga vuelven a Login; credencial incorrecta muestra mensaje. Backend temporal detenido: dashboard muestra errores/reintento y valores de equipos indisponibles, no ceros ficticios. |
| Presentación | Escritorio 1280 × 900 y móvil 390 × 844. En la vista móvil comprobada no hubo desbordamiento global; tabla desplaza dentro de su contenedor. Menú abre y cierra al navegar. |
| Entorno habitual al cerrar la auditoría inicial | Frontend Vite en http://localhost:5173 y backend Docker en http://localhost:8080. Los dos contenedores de base/backend estaban saludables; login ADMIN y dashboard comprobados desde el navegador. |

Las consultas HTTP adicionales cubrieron login, catálogos y organización con CRUD/relaciones, conflicto 409 al bajar un padre con hijas, equipo creado/editado/trasladado/dado de baja por GESTOR, filtros combinados, persistencia del historial y seguridad. Se comprobaron 401 sin JWT o con token inválido; 403 por rol y alcance; 400 al enviar un campo inmutable por PUT; 409 al editar BAJA. LECTOR recibió 403 al consultar el equipo fuera de su laboratorio actual y conservó acceso al historial por origen.

Las rutas administrativas tienen guardia de rol en el código. No se afirma que cada alias/ruta directa se haya ejercitado en navegador. Los tests de sesión y asignaciones cubren respuestas tardías y doble envío; no se hizo una prueba exhaustiva de concurrencia mediante la interfaz.

## Auditoría inicial: base temporal y preservación

Todas las escrituras de la auditoría inicial usaron **inventario_verificacion_jason_91d7ac79**, con backend Docker separado en 18080 y frontend en 5174. El entorno temporal reutilizó la imagen del backend de esa revisión; no modificó su código, Compose ni migraciones. La verificación Docker posterior utilizó otra base temporal, detallada en la actualización; tampoco escribió en el inventario habitual.

Al terminar se registraron 3 usuarios, 3 asignaciones, 4 categorías, 6 subcategorías, 2 sedes, 3 áreas, 4 laboratorios, 2 equipos y 2 movimientos en esa base. Son conteos físicos, incluidos registros con baja lógica.

- Flyway: nueve migraciones exitosas, V1–V9.
- Equipos no BAJA con subcategoría inactiva: 0.
- Equipos no BAJA con laboratorio inactivo: 0.
- Asignaciones activas a laboratorio inactivo: 0.
- Movimientos huérfanos en las relaciones comprobadas: 0.
- TRASLADO con origen igual a destino: 0.
- Conexiones antes de eliminar la base temporal: 0.
- Base temporal y contenedor eliminados; ausencia de la base confirmada.
- Archivo temporal de credenciales eliminado y servidor Vite 5174 detenido.
- Puertos temporales 5174/18080 sin servicios al terminar.

| Tabla habitual Docker | Antes | Después |
|---|---:|---:|
| usuario | 3 | 3 |
| usuario_laboratorio | 0 | 0 |
| categoria | 2 | 2 |
| subcategoria | 4 | 4 |
| sede | 1 | 1 |
| area | 2 | 2 |
| laboratorio | 2 | 2 |
| equipo | 0 | 0 |
| movimiento_equipo | 0 | 0 |

No se ejecutaron escrituras de prueba en inventario_laboratorios ni se cambiaron contraseñas. En el entorno habitual se verificaron salud, conteos y autenticación. La suite histórica de 233 pruebas Gradle **no se reejecutó** en esta revisión del frontend.

## Versiones comprobadas

Node 24.16.0; npm 11.13.0; Docker 29.8.0; React/React DOM 18.3.1; Vite 6.4.3; plugin React 4.7.0; Axios 1.20.0; React Router DOM 7.18.4; Lucide React 0.468.0. Se utilizaron las dependencias instaladas de Jason, sin migrar de stack.

## Evidencia y uso

- [Flujo completo Docker y publicación backend/frontend en Actions](../despliegue/verificacion-docker-actions-2026-10-03.md).
- [Resultados HTTP, SQL, conteos y build](evidencias/verificacion-2026-10-03.json).
- [Dashboard conectado al Docker habitual](evidencias/dashboard-docker-habitual.png).
- [Historial de verificación en escritorio](evidencias/movimientos-desktop.png).
- [Historial de verificación en móvil](evidencias/movimientos-movil.png).
- [Guía de ejecución y pruebas manuales](../../frontend/version-jason/frontend/README.md).

Las capturas de movimientos contienen fixtures eliminados al cerrar las pruebas. El dashboard habitual muestra su inventario real vacío. Ningún archivo de evidencia incluye tokens, contraseñas o hashes.

## Límites y siguientes decisiones

La integración local de las funcionalidades existentes quedó operativa. FE-08, FE-09 y FE-10 siguen siendo placeholders; no se dan por terminados. Completar la fidelidad visual requerirá acordar qué elementos del mockup caben en el backend actual. No se incorporaron funciones de mantenimiento, reportes, usuarios completos ni configuración para simular ese cierre.

Queda como mejora de interfaz revisar exhaustivamente navegación por teclado, foco/Escape y restauración de foco de modales, lectores de pantalla y otros tamaños de dispositivo. Los nombres accesibles básicos y el menú móvil se mejoraron, pero esta revisión no certifica accesibilidad integral.
