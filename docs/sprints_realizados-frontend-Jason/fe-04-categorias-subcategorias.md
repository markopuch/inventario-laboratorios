# FE-04 — Categorías y subcategorías

Fecha de inicio: 1 de octubre de 2026.

**Estado actualizado: CRUD de categorías/subcategorías y conflicto de baja de padre con hijas comprobados en la auditoría de integración. La UI refleja `activo` real y respeta los DTO. Docker y publicación de Jason confirmados; se conservan las diferencias con el mockup.**

## Evidencia vigente al 3 de octubre de 2026

La [auditoría de integración](auditoria-integracion.md) registra creación/edición
de categoría y subcategoría relacionada por UI, y CRUD/conflictos por HTTP.
El selector libre de estado entregado originalmente era inoperante y se retiró;
las respuestas usan `activo`, y la baja usa DELETE según el contrato real.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
usó catálogos de fixtures para el flujo de equipos, confirmó el entorno completo
y la publicación de Jason tras 27/27 pruebas. No declara una repetición por UI
de cada baja de catálogo ni completa la vista maestro/detalle del mockup.

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
