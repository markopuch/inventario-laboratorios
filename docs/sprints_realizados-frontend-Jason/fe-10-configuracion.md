# FE-10 — Configuración general y seguridad

Fecha de inicio: 1 de octubre de 2026.
Decisión de alcance: **26 de octubre de 2026**. Evidencia Docker histórica:
3 de octubre, despliegue `e1ce75a`.

**Estado vigente: Configuración se retira del menú y de las rutas. Preferencias
persistentes y seguridad configurable continúan como ampliaciones futuras.**

## Implementación y verificación actuales

La decisión del 26 de octubre retira el placeholder de `App.jsx` y la entrada de
`BarraLateral.jsx`. `/configuracion` ya no declara una ruta propia y sigue el
fallback general al inicio; sin sesión, se exige login. No hay opciones de
preferencias o seguridad persistidas ni un contrato backend nuevo.

Auditoría tiene ahora un módulo independiente ADMIN de solo lectura; no pertenece
a Configuración. Asignaciones también tiene acceso propio ADMIN. Autenticación,
roles, alcance y administración de cuentas siguen en sus módulos funcionales.
Consulta la [guía de uso](../frontend/guia-reportes-auditoria.md).

**Verificación del 26 de octubre:** menú y fallback de la ruta retirada
comprobados en navegador local con API simulada, dentro de diez comprobaciones
sin errores de página. El cierre aprobó 90/90 tests frontend y build. No se
publicó este cambio ni se probó contra BD/backend reales. Las métricas siguientes
son históricas del 3 de octubre, anteriores a retirar Configuración.

La [verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) y la [evidencia Docker actual](../../frontend/version-jason/frontend/evidencias/docker-actual-2026-10-03.json) registran el commit `e1ce75a`, ambas imágenes publicadas y tres contenedores habituales saludables. El smoke aislado aprobó **94/94 comprobaciones HTTP**, **30 aserciones funcionales** y **siete controles SQL sin inconsistencias**, con Flyway V1–V13. Son resultados del flujo completo, no pruebas exclusivas de este sprint ni una nueva ejecución JUnit.

La [evidencia de adaptación](../../frontend/version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json) acredita la ejecución anterior de **67/67 pruebas frontend** y el build aprobado con 1677 módulos. En la actualización Docker no se repitieron las suites npm/Gradle. Las cuatro asignaciones y las filas previas del inventario habitual se conservaron; la base de escritura temporal y sus recursos se eliminaron. No se publican credenciales.

## Registro de entrega original (histórico)

Las secciones siguientes conservan la inspección inicial del 3 de octubre de 2026, anterior a la adaptación V10–V13. Sus pendientes no sustituyen el estado vigente indicado arriba.

## Avance comprobado en archivos

| Elemento | Estado comprobado en archivos |
|---|---|
| Ruta `/configuracion` | IMPLEMENTADA |
| Acceso condicionado a ADMIN | IMPLEMENTADO en `BarraLateral.jsx` |
| `ModuloVisual.jsx` | IMPLEMENTADO como superficie visual |
| Preferencias generales | PENDIENTES |
| Configuración de seguridad | PENDIENTE |
| Persistencia de configuración | PENDIENTE |
| Endpoints de configuración | NO IMPLEMENTADOS en el cliente actual |

## Alcance confirmado

- Entrada de Configuración para administradores.
- Encabezado del módulo.
- Superficie visual preparada.
- Control de visibilidad del enlace mediante rol ADMIN.

No se debe documentar como implementado un panel real de preferencias o seguridad.

## Referencia visual revisada

La referencia contempla configuración general y seguridad. La versión entregada
mantiene la estructura sin simular opciones que todavía no existen.

## Recorrido guiado

| Bloque | Resultado | Estado |
|---|---|---|
| 1 | Ruta | Implementado |
| 2 | Visibilidad para ADMIN | Implementado |
| 3 | Superficie visual | Implementado |
| 4 | Preferencias | Pendiente |
| 5 | Seguridad configurable | Pendiente |
| 6 | Persistencia | Pendiente |
| 7 | Pruebas | Pendiente |

## Evidencia de implementación

`BarraLateral.jsx` marca Usuarios y Configuración como entradas administrativas.
`ModuloVisual.jsx` representa la superficie visual de Configuración.

## Fuentes y continuidad

- [Estado general de sprints](README.md).
- `src/componentes/BarraLateral.jsx`.
- `src/pantallas/ModuloVisual.jsx`.
- `src/App.jsx`.
- Mockup de Configuración general y seguridad.
