# FE-11 — Arquitectura, navegación y componentes comunes

Fecha de inicio: 1 de octubre de 2026.

**Estado: implementación realizada. La aplicación cuenta con rutas, contexto de autenticación, cliente HTTP, componentes comunes, navegación lateral y estructura compartida; la verificación integral de accesibilidad, responsive y contratos continúa pendiente.**

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/App.jsx` | IMPLEMENTADO |
| React Router | IMPLEMENTADO |
| `AuthContext` | IMPLEMENTADO |
| `Protegida` | IMPLEMENTADO |
| `BarraLateral` | IMPLEMENTADA |
| `Encabezado` | IMPLEMENTADO |
| `PiePagina` | IMPLEMENTADO |
| `Boton` | IMPLEMENTADO |
| Componentes de Equipos/Movimientos | IMPLEMENTADOS |
| Cliente Axios | IMPLEMENTADO |
| Alias `/catalogo` y `/asignaciones` | IMPLEMENTADOS |
| Navegación administrativa | IMPLEMENTADA para Usuarios y Configuración |

## Navegación confirmada

```text
Dashboard
Equipos
Categorías
Laboratorios
Usuarios
Movimientos
Mantenimientos
Reportes
Configuración
```

Usuarios y Configuración se filtran para ADMIN en la barra lateral.

## Estructura implementada

```text
src/
├─ componentes/
├─ contextos/
├─ pantallas/
├─ servicios/
├─ iconos/
├─ imagenes/
├─ App.jsx
├─ App.css
├─ index.css
└─ main.jsx
```

## Cliente HTTP

`src/servicios/api.js` centraliza:

- autenticación;
- categorías;
- subcategorías;
- sedes;
- áreas;
- laboratorios;
- equipos;
- movimientos;
- asignaciones de laboratorios de usuarios.

Axios usa `VITE_API_URL` y agrega Bearer desde `localStorage`.

## Roles y permisos de interfaz

`AuthContext` expone:

- `esAdmin`;
- `puedeGestionar`;
- `usuario`;
- `token`;
- `alcance`;
- `cargando`;
- `login`;
- `logout`.

`puedeGestionar` se habilita para ADMIN y GESTOR.
La autorización definitiva corresponde al backend.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Estructura de carpetas | Implementado |
| 2 | Routing | Implementado |
| 3 | Contexto de autenticación | Implementado |
| 4 | Cliente HTTP | Implementado |
| 5 | Layout compartido | Implementado |
| 6 | Navegación por rol | Implementado |
| 7 | Componentes reutilizables | Implementado |
| 8 | Revisión integral responsive | Pendiente |
| 9 | Accesibilidad/teclado | Pendiente de comprobación |
| 10 | Build y pruebas finales | Pendiente de evidencia de cierre |

## Evidencia de implementación

La estructura de `App.jsx` separa `/login` del resto mediante `Protegida`.
`BarraLateral.jsx` concentra el menú principal y filtra las opciones administrativas.
Los módulos comparten `Boton`, paneles, tablas y componentes de presentación.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/App.jsx`.
- `src/contextos/AuthContext.jsx`.
- `src/servicios/api.js`.
- `src/componentes/BarraLateral.jsx`.
- `src/componentes/Encabezado.jsx`.
- `src/componentes/PiePagina.jsx`.
