# Plan de cierre de la web y su documentación

Actualización: **27 de octubre de 2026, America/Lima**.
Estado: **cierre implementado y recorrido comprobado con backend/BD locales
reales**. La publicación GitHub/Actions se registra por separado en el
[anexo del cierre](despliegue/verificacion-cierre-2026-10-27.md).

## Objetivo y alcance

Terminar la web integrada de Jason y documentar su funcionamiento en Markdown.
La prioridad actual es el producto y sus documentos; la PPT y el ensayo de la
exposición quedan para una etapa posterior. Tras revisar el plan, el usuario
autorizó implementar el cierre descrito abajo.

La revisión se apoya en la [memoria canónica](../memory.md), el
[anexo frontend](memory/frontend.md), los [contratos y pendientes](backend-final/backlog.md)
y el [registro de verificaciones](memory/verification-2026-10-26.md).
Los comentarios favorables sobre las funcionalidades proceden de lo relatado
por el usuario; las recomendaciones adicionales del informe no se convierten
automáticamente en requisitos del cierre.

## Orden de trabajo

| Paso | Trabajo | Resultado verificable |
|---|---|---|
| 1 | Revisar la web existente: navegación, formularios, permisos, filtros, mensajes y adaptación a pantalla pequeña. | Lista de fallos reproducibles, con módulo, rol, pasos, resultado esperado y observado. No marcar como fallo una ampliación no acordada. |
| 2 | Documentar ADMIN, GESTOR y LECTOR con matriz de acciones y gráfico Mermaid. | [Matriz de roles](matriz-permisos.md) con acciones, alcance y ejemplo de traslado entre laboratorios. |
| 3 | Retirar Configuración del menú y de las rutas. | No aparece la entrada; `/configuracion` sigue el fallback al inicio. Preferencias y seguridad configurable quedan como ampliación futura. |
| 4 | Mejorar el cierre de los módulos actuales. | Asignaciones accesible en menú ADMIN; Reportes con CSV de agregados; Auditoría de solo lectura para ADMIN. Sin contratos backend nuevos ni cambios de BD. |
| 5 | Consolidar la documentación de usuario y técnica. | Guía de uso por rol, flujos principales, requisitos de ejecución, estado de módulos, límites y referencias accesibles desde el README. Reutilizar los documentos existentes. |
| 6 | Comprobar el cierre funcional en el entorno elegido. | Build y pruebas pertinentes, recorrido de los tres roles y evidencia fechada. Registrar qué se comprobó realmente, sin reutilizar resultados antiguos como nuevos. |

Auditoría se incluye expresamente en el cierre autorizado, reutilizando su API
ADMIN existente. La [guía de uso](frontend/guia-reportes-auditoria.md) documenta
esta consulta, los CSV y Asignaciones. La lectura del código y esta documentación
no sustituyen la verificación final ni acreditan un despliegue remoto.

## Decisiones y estado de los principales asuntos

| Asunto | Estado documentado | Tratamiento en el cierre |
|---|---|---|
| Equipos, catálogos, organización, usuarios/asignaciones, movimientos, mantenimientos y reportes | Implementados e integrados en fuente. | Revisar su uso real y corregir fallos observados. |
| Permisos de los tres roles | Definidos en backend y utilizados por la interfaz. | Elaborar la explicación visual y comprobar coherencia en los recorridos. |
| Configuración | Se retira el placeholder del menú y la ruta. | Verificar que no se ofrezcan opciones sin persistencia y que la dirección antigua vuelva al inicio. |
| Asignaciones | Ruta y contratos ADMIN existentes; nuevo acceso propio en menú. | Comprobar visibilidad ADMIN y reemplazo de asignaciones sin cambiar contratos. |
| Reportes CSV | Cinco descargas de agregados desde respuestas validadas, con filtros aplicados y alcance. | Comprobar UTF-8/BOM, `;`, comillas, protección de fórmulas, metadatos y bloqueo ante filtros sin aplicar o datos inválidos. |
| Auditoría | API ADMIN existente; nueva pantalla de solo lectura con filtros. | Comprobar acceso ADMIN, denegación de otros roles, fechas UTC y visualización en Lima, estados vacío/error y filtros válidos. |
| Vercel y Render | Publicación documentada; flujo autenticado actual pendiente de acreditar. | Verificar el entorno que se utilizará para cerrar la web. |
| Docker local | Motor accesible y tres servicios saludables del stack principal anterior. Las imágenes nuevas 58ac8f6 aprobaron 22 comprobaciones aisladas, incluidas siete de UI. | El stack principal conserva imágenes/datos anteriores; actualizarlo solo si se autoriza. La base Docker no contiene la carga Excel nativa. |
| Variante y sprints Marko | Conservados localmente y excluidos del seguimiento Git. | Mantenerlos fuera del producto activo y preservar sus archivos locales. |

## Ampliaciones que siguen pendientes de priorización

PDF/XLSX, exportación de registros detallados, gráficos completos del mockup,
calendario, paginación, refresh tokens, recuperación de contraseña, permisos
dinámicos y Swagger/OpenAPI no se incorporan en bloque al cierre.
Se evaluarán según necesidad y requisitos acordados.
La demora inicial al ingresar permanece fuera del trabajo solicitado.

El usuario autorizó después reforzar contraseñas y preparar la verificación de
GitHub Actions/Docker antes de publicar en GitHub; ese trabajo sí forma parte
del cierre actual y conserva el esquema y las claves existentes.

## Criterios de cierre

- Los fallos incluidos en la lista acordada tienen corrección y comprobación.
- Configuración queda fuera de la navegación; Auditoría es ADMIN de solo lectura.
- Los CSV contienen agregados de los filtros aplicados y del alcance vigente,
  sin solicitudes o datos adicionales ni resultados de otra sesión.
- La matriz de permisos coincide con acciones y alcance reales.
- Los Markdown describen el estado vigente y fechan la evidencia histórica.
- Los enlaces funcionan en un clon nuevo y no dependen de las carpetas locales de Marko.
- Las verificaciones identifican su entorno y no publican credenciales o tokens.

## Verificación del cierre

El 26 de octubre se aprobaron **90/90 tests frontend** y un build de **1679
módulos**. Playwright con Chrome headless en `127.0.0.1:5173` y **API simulada**
aprobó diez comprobaciones, sin errores de página: menú/rutas por rol,
Asignaciones, Auditoría/filtros/fechas/errores, cinco descargas CSV y sus estados,
y móvil de 390 px. Ver [detalle y límites](frontend/guia-reportes-auditoria.md#verificación-local-del-26-de-octubre).

Esa primera comprobación utilizó API simulada. El recorrido posterior del
26 de octubre aprobó **8/8 comprobaciones con API/BD reales**, cero errores de
página, roles ADMIN/GESTOR/LECTOR, auditoría de los cinco actores y vista de 390 px.
Tres CSV con datos se descargaron y contrastaron con la API; los bloques de
Movimientos/Mantenimientos vacíos deshabilitaron sus descargas. El 27 de octubre,
tras reforzar contraseñas, se aprobaron **93/93 tests frontend** y build de
**1679 módulos**. La regresión backend aprobó **365/365** con JAR generado en
PostgreSQL temporal; importador **28/28**. El commit 58ac8f6 se publicó en main;
Actions aprobó **5/5 trabajos** y publicó ambas imágenes en GHCR, con cuatro
etiquetas consultadas y digests coincidentes. Ver
[anexo del cierre](despliegue/verificacion-cierre-2026-10-27.md).
Las pruebas históricas del 3 de octubre y las comprobaciones iniciales del
26 se conservan por separado en el [registro de verificación](memory/verification-2026-10-26.md).

## Carga de datos solicitada después del cierre de interfaz

El usuario autorizó importar su Excel en la base local y crear las cuentas demo
Jason ADMIN, Jorge GESTOR y Gabriela ADMIN, con participación de Marko ADMIN y
Aldo GESTOR en auditoría. La [guía de importación](importacion-inventario-demo.md)
registra selección, normalizaciones y reparto: **257 equipos importados** y 31
pendientes por estado vacío. El conflicto de serie de L201, fila 57, se resolvió
con serie vacía y referencia al valor original, conservando el equipo previo en
BAJA. La base suma 258 equipos; la carga acredita 337 altas de recursos y la
participación real de los cinco actores en Auditoría.
La herramienta usa la API existente y no incorpora una pantalla de importación.
