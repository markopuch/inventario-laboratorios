# FE-05 — Laboratorios, sedes y áreas

Fecha de inicio: 1 de octubre de 2026.

**Estado actualizado: organización, CRUD HTTP y creación/edición de laboratorio por UI comprobados en la auditoría. La jerarquía Sede → Área → Laboratorio está disponible. La UI respeta `activo` real; no ofrece Mantenimiento como estado de laboratorio. Docker y publicación confirmados.**

## Evidencia vigente al 3 de octubre de 2026

La [auditoría de integración](auditoria-integracion.md) comprobó relación con
sede/área, árbol expandible y CRUD de organización. Las respuestas contienen
`activo`; los DTO de alta/edición no permiten un estado libre. La opción visual
En mantenimiento entregada originalmente no tenía contrato y se retiró.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
comprobó el traslado entre laboratorios de fixtures 201 y 206, alcance y filtros
con frontend Nginx. Flyway conservó V1–V9 y los conteos habituales no cambiaron.
Esto no agrega búsqueda del árbol, ficha detallada ni filtros combinados del mockup.

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
