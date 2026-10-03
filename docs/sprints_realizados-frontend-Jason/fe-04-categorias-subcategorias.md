# FE-04 — Categorías y subcategorías

Fecha de inicio: 1 de octubre de 2026.

**Estado: implementación frontend realizada. Categorías y subcategorías están separadas de Sedes, Áreas y Laboratorios y cuentan con CRUD visual/API; la persistencia de estado debe considerarse pendiente de validación según el contrato backend.**

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
