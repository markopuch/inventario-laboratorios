# Estado de los sprints del frontend

Registro histórico: 1 de octubre de 2026. Revisión de estado: 3 de octubre de 2026.

**Nota de estado — 3 de octubre de 2026:** este registro corresponde únicamente
a la versión Marko, conservada en `frontend/version-marko/inventario-frontend`.
La tutoría permanece en pausa; la versión de trabajo y el despliegue Docker
actual son de [Jason](../sprints_realizados-frontend-Jason/README.md).
La [validación Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
no certifica las funciones pendientes de la versión Marko. Los resultados del
1 de octubre y las rutas originales que aparecen abajo son históricos.

Esta carpeta registra el avance real del frontend. Su nombre no implica que los
sprints aquí mencionados estén terminados. FE-01 es independiente del Sprint 1
histórico del backend.

La implementación será guiada: el usuario crea el código, guarda los archivos y
ejecuta los comandos. La autorización inicial para escribir documentación se
limita al registro de estado solicitado en esta carpeta.

| Sprint | Alcance | Estado registrado el 1 de octubre |
|---|---|---|
| FE-00 | Preparación, contratos y base técnica | Base y cliente HTTP implementados; GET protegido mediante proxy comprobado con 401. Repetir lint/build sobre los siguientes cambios |
| [FE-01](fe-01-login-sesion.md) | Diseño común, login y sesión | En curso: próximo bloque composición visual de Login; autenticación y panel pendientes |
| FE-02 | Listado, filtros y detalle de equipos | Planificado; no iniciado |
| FE-03 | Alta, edición y baja de equipos | Planificado; no iniciado |
| FE-04 | Categorías y subcategorías | Planificado; no iniciado |
| FE-05 | Sedes, áreas y laboratorios | Planificado; no iniciado |
| FE-06 | Traslado e historial | Planificado; no iniciado |
| FE-07 | Asignaciones de laboratorios a usuarios existentes | Planificado; no iniciado |
| FE-08 | Consolidación y cierre del frontend | Planificado; no iniciado |

## Cómo se registra la evidencia

- **IMPLEMENTADO:** el código o documento existe; no implica funcionamiento comprobado.
- **COMPROBADO POR EJECUCIÓN:** hay una ejecución observada o un resultado aportado por el usuario; se indica qué demuestra.
- **REVISADO ESTÁTICAMENTE:** se leyeron archivos o contratos sin ejecutar la funcionalidad.
- **PENDIENTE DE VERIFICAR:** falta evidencia de ejecución o revisión del resultado.

Al iniciar este registro, `frontend/` contenía únicamente `readme.md`, vacío.
Después, el usuario creó la aplicación en `frontend/inventario-frontend/`
mediante Vite e instaló sus dependencias. La carpeta actual de esa versión es
`frontend/version-marko/inventario-frontend`; su traslado no implica que sus
sprints pendientes estén terminados.

En la revisión posterior, App, estilos, HTML, puerto 5173 con `strictPort`,
proxy `/api` hacia 8080 y archivos de entorno ya están preparados. Existe
`dist/`. El usuario aportó después un build correcto: 16 módulos transformados
y finalización en 184 ms. También aportó la cabecera de ejecución de ESLint,
sin diagnósticos en el fragmento; no se adjuntó código de salida ni retorno
al indicador. No se observó la pantalla en el navegador durante esta revisión.

ESLint y su configuración ya existen y el script está corregido a `eslint .`.
Oxlint sigue instalado, pero ya no es el comando del script `lint`.
`src/servicios/api.js` fue creado por el usuario y revisado estáticamente.
El usuario aportó la ejecución desde el navegador de `solicitarApi('/auth/me')`:
GET a `http://localhost:5173/api/auth/me`, estado 401 y mensaje
«Se requiere autenticación válida para acceder a este recurso.».
Quedan comprobados ese recorrido por el proxy y la conservación del error
por el cliente. Las otras ramas del cliente HTTP y el login siguen pendientes
de comprobar. El próximo bloque guiado prepara la composición visual de Login.
Las consultas de versión dieron Node `v24.16.0` y npm `11.13.0`. El agente no
ejecutó instalación, lint, build, servidores ni pruebas funcionales.

## Fuentes

- [Plan de sprints frontend v1](../frontend/plan-sprints-frontend-inventario-v1.md).
- [Estructura y adaptación del proyecto](../frontend/estructura-frontend-y-adaptacion-inventario.md).
- [Mockups](../frontend/mockup/).
- [Contratos del backend](../backend-final/endpoints.md).
- [Estado y recorrido de FE-01](fe-01-login-sesion.md).

Techstore v2 se utiliza como referencia pedagógica de React, componentes,
props, estado, formularios y CSS. Su login simulado no implementa la
autenticación JWT requerida por el inventario.
