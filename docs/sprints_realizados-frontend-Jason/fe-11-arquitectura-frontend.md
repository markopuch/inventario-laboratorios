# FE-11 — Arquitectura, navegación y componentes comunes

Fecha de inicio: 1 de octubre de 2026.
Actualización funcional: **26 de octubre de 2026**. Evidencia Docker histórica:
3 de octubre, despliegue `e1ce75a`.

**Estado vigente: React/Vite/Axios con navegación ADMIN para Usuarios,
Asignaciones y Auditoría; Configuración retirada del menú y de las rutas.**

## Implementación y verificación actuales

Rutas públicas/protegidas, `AuthContext`, servicios HTTP y utilidades mantienen sus responsabilidades. `/usuarios`, `/asignaciones` y `/auditoria` tienen guardia ADMIN; permisos visuales no sustituyen autorización del backend. Cliente usa `/api`, Bearer en memoria y protección frente a respuestas de sesión obsoletas; no recupera JWT desde localStorage.

La ruta retirada `/configuracion` sigue el fallback al inicio. Auditoría consulta
la API ADMIN existente con filtros y tabla de solo lectura; no crea endpoints.
Los CSV de Reportes se generan en el navegador desde agregados autorizados y
conservan filtros aplicados y alcance. Ver [guía de uso](../frontend/guia-reportes-auditoria.md)
y [matriz de roles](../matriz-permisos.md).

**Verificación del 26 de octubre:** 90/90 tests frontend, build aprobado con
1679 módulos y diez comprobaciones de navegador local con API simulada, sin
errores de página. Incluyen los tres roles, rutas/menú, Auditoría y cinco CSV;
en móvil de 390 px la página no desborda y el menú cierra al navegar. Ver
[detalle y límites](../frontend/guia-reportes-auditoria.md#verificación-local-del-26-de-octubre).
Sin nueva prueba contra backend/BD reales ni publicación remota. Las métricas,
la publicación y Docker descritos a continuación corresponden al 3 de octubre.

Actions [37158784428](https://github.com/markopuch/inventario-laboratorios/actions/runs/37158784428) publicó ambos trabajos del commit `e1ce75a`. Docker sirve Nginx en 3000, `/api` hacia backend:8080 y fallback SPA; Vite conserva 5173 para desarrollo. PostgreSQL habitual conserva contenedor y volumen con V1–V13. Consola de la última sesión: cero errores y advertencias; no certifica todos los recorridos. Fidelidad visual al píxel y accesibilidad exhaustivas no están certificadas.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes describen la estructura inicial; la mención de Bearer
desde `localStorage` ya no corresponde a la implementación vigente.

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
