# FE-02 — Dashboard y pantalla inicial

Fecha de inicio: 1 de octubre de 2026.
Actualización de evidencia: 3 de octubre de 2026, posterior al despliegue `e1ce75a`.

**Estado vigente: Dashboard conectado a datos actuales de equipos, alcance, movimientos y reportes de mantenimiento.**

## Implementación y verificación actuales

`Home.jsx` consulta equipos, movimientos, `/auth/me/laboratorios` y `/reportes/resumen`. Distingue carga, error y ausencia de registros; el fallo de una consulta no fabrica un cero. Total, programados y mantenimientos en proceso provienen del resumen real del servidor. El contador de laboratorios consulta el alcance al entrar. No existe mockup independiente de Dashboard ni certificación exhaustiva de accesibilidad.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las tablas y contratos siguientes corresponden a la inspección inicial. La
auditoría y el estado vigente precisan las correcciones de carga/error y alcance.

El usuario implementó la pantalla inicial del inventario y sus componentes de resumen.
Este documento registra lo realizado sin presentar datos ficticios como evidencia de
backend.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| `src/pantallas/Home.jsx` | IMPLEMENTADO: carga equipos, laboratorios y movimientos |
| Tarjetas de resumen | IMPLEMENTADAS: equipos, laboratorios, mantenimiento y bajas |
| Movimientos recientes | IMPLEMENTADOS con tabla y acceso al historial |
| Accesos rápidos | IMPLEMENTADOS hacia Equipos, Categorías y Laboratorios |
| Estado de carga | IMPLEMENTADO mediante `—` mientras se consultan datos |
| Datos ficticios | NO IMPLEMENTADOS; los valores se derivan de respuestas API |
| Navegación | IMPLEMENTADA mediante React Router |

## Alcance confirmado

- Conteo de equipos registrados.
- Conteo de laboratorios.
- Conteo de equipos que requieren mantenimiento o tienen estado de mantenimiento.
- Conteo de equipos dados de baja.
- Visualización de movimientos recientes.
- Enlace al historial completo.
- Accesos rápidos a módulos principales.

## Referencia visual revisada

Se conserva el lenguaje visual del mockup: encabezado de página, tarjetas,
paneles blancos, indicadores y acciones de acceso rápido.

Los valores CSS son una adaptación del mockup y no una medición oficial de la imagen.

## Contratos utilizados

| Operación | Uso |
|---|---|
| `GET /api/equipos` | total y estados de equipos |
| `GET /api/laboratorios` | cantidad de laboratorios |
| `GET /api/movimientos` | movimientos recientes |

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Estructura de pantalla inicial | Implementado |
| 2 | Tarjetas de resumen | Implementado |
| 3 | Movimientos recientes | Implementado |
| 4 | Accesos rápidos | Implementado |
| 5 | Manejo de carga/error | Implementado de forma básica |
| 6 | Verificación con backend | Pendiente de evidencia completa |
| 7 | Revisión responsive | Pendiente de comprobación manual |
| 8 | Documentación | En actualización |

## Evidencia de implementación

`Home.jsx` utiliza `Promise.all` para cargar equipos, laboratorios y movimientos.
No incorpora un conjunto de datos estático para simular resultados.

La pantalla inicial corresponde a `/`.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/pantallas/Home.jsx`.
- `src/componentes/TarjetaBienvenida.jsx`.
- `src/componentes/ResumenSeleccion.jsx`.
- `src/componentes/FilaMovimiento.jsx`.
- Mockup de Dashboard/Equipos del proyecto.
