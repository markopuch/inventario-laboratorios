# FE-02 — Dashboard y pantalla inicial

Fecha de inicio: 1 de octubre de 2026.

**Estado actualizado: dashboard conectado y comprobado en los recorridos de integración y Docker. Contadores e historial usan datos reales; errores e indisponibilidad se distinguen de ceros. La fidelidad visual y accesibilidad exhaustivas continúan pendientes.**

## Evidencia vigente al 3 de octubre de 2026

La [auditoría de integración](auditoria-integracion.md) comprobó resúmenes,
historial y fallo/reintento. El contador de laboratorios consulta
`GET /api/auth/me/laboratorios` al entrar, también para ADMIN; no confunde la
lectura global del catálogo con el alcance del usuario.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
comprobó login y navegación con las imágenes del entorno habitual, que ahora
sirve Jason en 3000 y backend en 8080. Actions confirmó el build y publicación
de Jason con 27/27 pruebas del frontend. Esto no certifica cada combinación de
datos del dashboard ni agrega un mockup de Dashboard que no fue aportado.

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
