# FE-04 — Categorías y subcategorías

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Catálogos administrativos con consulta de activos/inactivos, cambio de estado real y reactivación integrados.**

## Implementación y verificación actuales

ADMIN utiliza `/admin/categorias` y `/admin/subcategorias`, con filtro `activo`, y PATCH `/{id}/estado` para cambiar actividad. Los otros roles leen el catálogo activo por rutas normales. La adaptación probó formularios de desactivación/reactivación mediante UI; el smoke Docker actual volvió a comprobar creación, inactivación, consulta y reactivación por HTTP. Una categoría con hijas activas no se desactiva: devuelve 409. `activo` es persistido por API; no es un badge fijo. La vista sigue en pestañas, sin afirmar maestro/detalle idéntico al mockup.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las menciones siguientes al estado visual y persistencia pendiente describen la
entrega inicial; el criterio vigente es el contrato y las correcciones auditadas.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/pantallas/Catalogo.jsx` | IMPLEMENTADO |
| Pestaña Categorías | IMPLEMENTADA |
| Pestaña Subcategorías | IMPLEMENTADA |
| KPI Total categorías | IMPLEMENTADO |
| KPI Total subcategorías | IMPLEMENTADO |
| Búsqueda | IMPLEMENTADA |
| Crear | IMPLEMENTADO |
| Editar | IMPLEMENTADO |
| Dar de baja | IMPLEMENTADO |
| Estado en formulario | PRESENTE visualmente |
| Separación de ubicaciones | IMPLEMENTADA; sedes/áreas/laboratorios están en FE-05 |

## Alcance confirmado

- Gestión visual de categorías.
- Gestión visual de subcategorías.
- Asociación de una subcategoría a una categoría.
- Búsqueda independiente por pestaña.
- Acciones de crear, editar y eliminar.
- Indicadores de cantidad.

El estado aparece en la interfaz, pero la implementación entregada debe verificarse
contra el contrato backend antes de afirmar persistencia de estados ACTIVA/INACTIVA.

## Referencia visual revisada

Se respeta la referencia de «Categorías y subcategorías» como módulo independiente.
No se mezclan en esta pantalla las tarjetas de Sedes, Áreas y Laboratorios.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/categorias` | listado |
| `POST /api/categorias` | alta |
| `PUT /api/categorias/{id}` | edición |
| `DELETE /api/categorias/{id}` | baja |
| `GET /api/subcategorias` | listado |
| `POST /api/subcategorias` | alta |
| `PUT /api/subcategorias/{id}` | edición |
| `DELETE /api/subcategorias/{id}` | baja |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Separación de categorías/subcategorías | Implementado |
| 2 | KPIs | Implementado |
| 3 | Búsqueda | Implementado |
| 4 | Alta/edición | Implementado |
| 5 | Baja | Implementado |
| 6 | Relación subcategoría → categoría | Implementado |
| 7 | Persistencia de estado | Pendiente de validar |
| 8 | Pruebas funcionales | Pendiente |

## Evidencia de implementación

La pantalla usa `catalogoApi` y muestra las dos entidades mediante pestañas.
El formulario de subcategorías carga las categorías disponibles.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Catalogo.jsx`.
- `src/pantallas/Catalogo.css`.
- `src/servicios/api.js`.
- Mockup de Categorías y subcategorías.
