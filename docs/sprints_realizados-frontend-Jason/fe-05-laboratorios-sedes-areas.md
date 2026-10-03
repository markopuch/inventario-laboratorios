# FE-05 — Laboratorios, sedes y áreas

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Jerarquía Sede → Área → Laboratorio integrada, con activos/inactivos administrativos y estado operativo real de Laboratorio.**

## Implementación y verificación actuales

ADMIN consulta `/admin/sedes`, `/admin/areas` y `/admin/laboratorios` y utiliza PATCH de actividad. Desde V10, Laboratorio tiene `estadoOperativo` OPERATIVO/MANTENIMIENTO mediante su contrato de edición; es independiente de `activo`. Se verificó MANTENIMIENTO → OPERATIVO manteniendo `activo=true`. No debe confundirse el estado de ubicación con el ciclo de Mantenimiento de Equipo. Padres/dependencias vigentes pueden impedir inactivaciones con 409. Continúan las diferencias visuales de ficha, búsqueda del árbol y filtros combinados.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las declaraciones siguientes de estados Activo/Inactivo/En mantenimiento reflejan
la inspección inicial y se conservan como antecedente del defecto corregido; no
deben usarse como especificación actual del contrato.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/pantallas/Laboratorios.jsx` | IMPLEMENTADO |
| Pestaña Laboratorios | IMPLEMENTADA |
| Pestaña Sedes | IMPLEMENTADA |
| Pestaña Áreas | IMPLEMENTADA |
| KPI Sedes | IMPLEMENTADO |
| KPI Áreas | IMPLEMENTADO |
| KPI Laboratorios | IMPLEMENTADO |
| Árbol Sede → Área → Laboratorio | IMPLEMENTADO |
| Búsqueda | IMPLEMENTADA |
| Crear/editar/eliminar | IMPLEMENTADOS |
| Estado de Sede | IMPLEMENTADO: Activo/Inactivo |
| Estado de Área | IMPLEMENTADO: Activo/Inactivo |
| Estado de Laboratorio | IMPLEMENTADO: Activo/En mantenimiento/Inactivo |
| Fecha «12 de octubre de 2025» | ELIMINADA de la interfaz |

## Alcance confirmado

- Consulta de sedes, áreas y laboratorios.
- Contadores separados.
- Jerarquía expandible de sede, área y laboratorio.
- Edición de Sede y Área con estado.
- Edición de Laboratorio con estado, incluyendo «En mantenimiento».
- Búsqueda.
- Acciones administrativas condicionadas por `esAdmin`.

La opción «En mantenimiento» se ofrece específicamente para Laboratorios en la
implementación actual; Sedes y Áreas tienen Activo/Inactivo.

## Referencia visual revisada

Se conserva la organización de la referencia de Laboratorios con una sección propia
para ubicaciones. Se retiró la fecha decorativa «12 de octubre de 2025» que no aportaba
información funcional.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/sedes` | sedes |
| `POST /api/sedes` | alta |
| `PUT /api/sedes/{id}` | edición |
| `DELETE /api/sedes/{id}` | baja |
| `GET /api/areas` | áreas |
| `POST /api/areas` | alta |
| `PUT /api/areas/{id}` | edición |
| `DELETE /api/areas/{id}` | baja |
| `GET /api/laboratorios` | laboratorios |
| `POST /api/laboratorios` | alta |
| `PUT /api/laboratorios/{id}` | edición |
| `DELETE /api/laboratorios/{id}` | baja |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Separar Sedes, Áreas y Laboratorios | Implementado |
| 2 | Contadores | Implementado |
| 3 | Árbol jerárquico | Implementado |
| 4 | CRUD de sedes | Implementado |
| 5 | CRUD de áreas | Implementado |
| 6 | CRUD de laboratorios | Implementado |
| 7 | Estados | Implementado en interfaz |
| 8 | Retiro de fecha decorativa | Implementado |
| 9 | Validación de contratos backend | Pendiente de evidencia completa |
| 10 | Revisión responsive/teclado | Pendiente de comprobación |

## Evidencia de implementación

La pantalla carga las tres colecciones en paralelo y mantiene el árbol mediante
estado local de nodos abiertos.

El formulario reutiliza la misma estructura para los tres tipos y adapta los
selectores de Sede/Área según corresponda.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Laboratorios.jsx`.
- `src/servicios/api.js`.
- Mockup de Laboratorios, Sedes y Áreas.
