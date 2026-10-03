# FE-08 — Mantenimientos

Fecha de inicio: 1 de octubre de 2026.

**Estado: estructura visual preparada; módulo funcional pendiente. La pantalla existe y está integrada en navegación, pero la versión entregada no implementa programación, calendario ni operaciones de mantenimiento contra endpoints específicos.**

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| Ruta `/mantenimientos` | IMPLEMENTADA |
| `src/pantallas/ModuloVisual.jsx` | IMPLEMENTADO como componente visual reutilizable |
| Título y subtítulo | IMPLEMENTADOS |
| Iconografía del módulo | IMPLEMENTADA |
| CRUD/programación | PENDIENTE |
| Calendario | PENDIENTE |
| Historial de mantenimiento | PENDIENTE |
| Endpoints específicos | NO IMPLEMENTADOS en el cliente actual |

## Alcance confirmado

- Entrada de navegación.
- Presentación visual del módulo.
- Mensaje que identifica la preparación de la estructura.

No se debe documentar como implementado el calendario, programación o seguimiento
real del mantenimiento.

## Referencia visual revisada

La referencia de mantenimiento contempla programación y calendario. La implementación
actual deja preparada la superficie visual, sin inventar datos.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Ruta y navegación | Implementado |
| 2 | Encabezado | Implementado |
| 3 | Placeholder visual | Implementado |
| 4 | Programación | Pendiente |
| 5 | Calendario | Pendiente |
| 6 | Persistencia/API | Pendiente |
| 7 | Pruebas | Pendiente |

## Evidencia de implementación

`ModuloVisual.jsx` selecciona el icono de Mantenimientos y muestra que el módulo
puede conectarse cuando existan endpoints disponibles.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/ModuloVisual.jsx`.
- `src/App.jsx`.
- Mockup de Mantenimientos.
