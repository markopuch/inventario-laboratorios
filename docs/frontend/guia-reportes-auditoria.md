# Uso de Reportes, Auditoría y Asignaciones

Actualización: **27 de octubre de 2026, America/Lima**. Describe el cierre
frontend autorizado para Jason. Las comprobaciones locales del 26 y 27 se
detallan al final; la evidencia del 3 de octubre conserva su alcance histórico.
La publicación posterior, la revisión efectiva y el recorrido autenticado de
Vercel/Render se registran en el
[informe cloud de paginación y login](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md):
59/59 comprobaciones API, Auditoría paginada y espera de login verificadas.
Las pruebas de descargas CSV descritas abajo mantienen su evidencia local;
esa comprobación cloud no se presenta como una nueva prueba de las cinco descargas.

## Reportes y descargas CSV

ADMIN consulta agregados globales. GESTOR y LECTOR consultan los agregados de sus
laboratorios autorizados. La API aplica los permisos; elegir un laboratorio
fuera del alcance se rechaza. Descargar no amplía el alcance ni concede escritura.

1. Abre **Reportes** y selecciona sede, área, laboratorio, estado de equipo,
   estado/tipo de mantenimiento y fechas según corresponda.
2. Pulsa **Aplicar filtros** y espera la respuesta. Un rango de fechas invertido
   debe corregirse antes de consultar.
3. Revisa los totales y pulsa **Exportar CSV** en el bloque que necesitas. Hay cinco:
   resumen, equipos por estado, equipos por laboratorio, movimientos y
   mantenimientos. Este último reúne total, estados y tipos.
4. Si cambias los filtros, vuelve a aplicarlos antes de descargar. Los botones
   quedan deshabilitados mientras haya filtros editados sin aplicar. Un cambio
   de sesión o alcance invalida los resultados anteriores.

Los archivos contienen **agregados**, no fichas individuales de equipos ni un
volcado de la base. Repiten por fila el nombre del reporte, la generación UTC,
el alcance y los ocho filtros aplicados: `idSede`, `idArea`, `idLaboratorio`,
`estado`, `fechaDesde`, `fechaHasta`, `estadoMantenimiento` y `tipoMantenimiento`.
Las columnas de datos son `Agrupación`, `Indicador`, `ID laboratorio`,
`Código laboratorio`, `Laboratorio` y `Total`.
El resumen y mantenimiento utilizan varias filas para sus distintos indicadores.

El formato es CSV con **punto y coma**, UTF-8 con BOM, saltos CRLF y celdas entre
comillas. Las comillas internas se escapan y los textos que podrían interpretarse
como fórmulas se neutralizan. Si una hoja de cálculo no separa las columnas al
abrir, usa su importador de texto/CSV con UTF-8 y delimitador `;`.

La descarga se genera en el navegador desde la respuesta validada del bloque;
no llama a una API nueva ni vuelve a solicitar información. Cada bloque debe
tener filas vigentes para habilitar su descarga; un error no se convierte
en un reporte con ceros. Los bloques sin filas deshabilitan la descarga;
el resumen puede exportar sus tres totales válidos aunque sean cero.
El backend sigue definiendo fechas y agregados:
timestamps por días UTC y mantenimiento por fecha programada de calendario.

## Auditoría para ADMIN

La entrada **Auditoría** abre `/auditoria`. Está disponible solo para ADMIN;
GESTOR y LECTOR no tienen entrada ni acceso autorizado por la API. La pantalla
consume `GET /api/admin/auditoria` y permite consultar, sin modificar eventos.

Desde el 27 de octubre la interfaz solicita páginas al servidor, con tamaños
20/50/100, navegación Anterior/Siguiente y total del conjunto filtrado. Cambiar
filtros o tamaño vuelve a la primera página. El contrato usa base cero y
`{content,number,size,totalElements,totalPages,first,last}`; sin `page/size`
se mantiene el array para consumidores anteriores. La consulta y el conteo
aplican los mismos filtros y el acceso exclusivo de ADMIN.

Los filtros son entidad, ID del registro, ID de usuario actor, acción, fecha
desde y fecha hasta. Los IDs deben ser enteros positivos. Un rango de fechas
invertido se rechaza; puedes omitir filtros para consultar el conjunto disponible.

La tabla presenta fecha en **America/Lima**, actor, acción, registro afectado y
resumen del cambio. Las fechas de los filtros representan **días UTC** en el
servidor: por ejemplo, el día UTC puede comenzar la tarde del día anterior en
Lima. La conversión visual no cambia los límites de la consulta.

Durante la consulta se muestra la carga; una lista vacía indica que no hay
eventos para los filtros aplicados. **Actualizar** vuelve a consultar y
**Reintentar auditoría** recupera una consulta fallida. **Limpiar** restablece
los filtros y consulta de nuevo. Los resúmenes largos permiten desplegar su
contenido completo sin modificarlo.

Los eventos incluyen altas, ediciones, cambios de actividad o rol,
restablecimientos de contraseña, asignaciones, traslados y transiciones de
mantenimiento. El resumen conserva campos seleccionados del evento; no reconstruye
cada versión histórica de una entidad. No existen acciones para editar o borrar
eventos desde esta pantalla ni un formulario de escritura libre de auditoría.

## Asignaciones para ADMIN

La entrada **Asignaciones** abre `/asignaciones`; también se puede administrar
el alcance desde **Usuarios → Laboratorios**. Consulta al usuario por ID antes
de seleccionar laboratorios. Guardar **reemplaza el conjunto completo**:
una selección vacía retira sus asignaciones. Ser responsable de un equipo no
concede acceso a su laboratorio.

ADMIN tiene alcance global independientemente de sus asignaciones explícitas.
GESTOR y LECTOR requieren asignación activa a laboratorio activo. Revisa la
[matriz de roles y el ejemplo visual](../matriz-permisos.md).

## Configuración y comprobación del cierre

Configuración se retira del menú y de las rutas. La antigua dirección
`/configuracion` sigue el fallback al inicio. No hay preferencias persistidas ni
seguridad configurable; su definición queda para una ampliación futura.

El [plan de cierre](../plan-cierre-web.md) reúne los criterios y el
[registro de verificación](../memory/verification-2026-10-26.md) conserva las
ejecuciones con su fecha y alcance. No se atribuye este cambio a la nube sin
comprobar el release publicado y los recorridos autenticados.

## Verificación local del 26 de octubre

- `npm test`: **90 pruebas aprobadas**, sin fallos, canceladas, omitidas ni
  pendientes; salida 0. Son pruebas frontend de lógica y contratos.
- `npm run build`: aprobado, **1679 módulos**. No acredita publicación remota.
- Playwright con Chrome headless en `127.0.0.1:5173`, **API simulada**:
  diez comprobaciones aprobadas y cero errores de página.
- Navegación: Configuración retirada; Asignaciones y Auditoría ADMIN;
  GESTOR/LECTOR sin menú ni acceso a las rutas administrativas.
- Auditoría: actores, fecha en Lima, resumen desplegable, seis filtros,
  rango inválido, 403/reintento y lista vacía.
- Reportes: cinco archivos CSV descargados con BOM; bloqueo ante filtros
  editados sin aplicar; contenido filtrado; estados carga/error/vacío y alcance
  de GESTOR/LECTOR.
- Móvil de 390 px: sin desbordamiento de la página y menú que cierra al navegar.

La API simulada permite comprobar el comportamiento del cliente, los archivos
descargados y los permisos de interfaz; **no prueba la integración actual con
backend o BD reales**. No se ejecutaron suites backend nuevas, ni se hicieron
cambios de datos o publicación remota. Las capturas y el JSON de la comprobación
son temporales y no se incorporan como evidencia versionada del repositorio.

## Comprobación posterior con API y BD reales

El 26 de octubre, tras aplicar la carga local, se aprobaron **8/8 comprobaciones
de navegador**, sin errores de página. Se verificaron ADMIN/GESTOR/LECTOR,
alcance por laboratorio, Auditoría exclusiva ADMIN con los cinco actores de la
carga y móvil de 390 px. Jorge se usó temporalmente como LECTOR y quedó restaurado
a GESTOR al finalizar.

Los CSV de Resumen, Equipos por estado y Equipos por laboratorio se descargaron
con datos reales y se contrastaron con la API, incluido el filtro L201. Los bloques
de Movimientos y Mantenimientos estaban vacíos y sus descargas permanecieron
deshabilitadas; el recorrido real no fabricó actividad para llenarlos.

El 27 de octubre, al actualizar contraseñas, la suite frontend pasó **93/93** y
el build **1679 módulos**. Estos resultados complementan la primera verificación
simulada. El [anexo del cierre](../despliegue/verificacion-cierre-2026-10-27.md)
separa pruebas, publicación GitHub/Actions y límites de Docker/nube.
