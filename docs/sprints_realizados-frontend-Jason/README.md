# Sprints realizados — Frontend Jason

Esta carpeta registra la entrega y la revisión del frontend Jason de Inventario
de Laboratorios, ubicado en `frontend/version-jason/frontend/`.

La [auditoría de integración del 3 de octubre de 2026](auditoria-integracion.md)
consolida los hallazgos posteriores, las correcciones verificadas, las diferencias
con los mockups y los límites de las pruebas. Los documentos FE-01 a FE-11 conservan
el registro de la entrega; sus afirmaciones originales deben leerse junto con
esa auditoría. No se trasladan resultados de la versión Marko a la versión Jason.

La [verificación posterior de Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md)
registra el flujo completo con Nginx, backend y PostgreSQL, y confirma la publicación
de ambas imágenes. Los estados actualizados de cada FE remiten a esa evidencia;
los apartados originales permanecen identificados como historia.

La [guía de ejecución de Jason](../../frontend/version-jason/frontend/README.md)
explica Docker, Vite en 5173, el proxy `/api`, la sesión en memoria y los checks.
La revisión final registra 27/27 pruebas automatizadas, build aprobado y 42 solicitudes
HTTP verificadas por proxy, además de recorridos de navegador en un entorno aislado.
La integración local es operativa en las funcionalidades implementadas; FE-08–FE-10,
la fidelidad visual 1:1 y la accesibilidad exhaustiva continúan pendientes.

## Estado comprobado al 3 de octubre de 2026

| Comprobación | Evidencia actual |
|---|---|
| Docker completo | Tres contenedores habituales saludables; frontend en 3000 y backend en 8080. |
| Flujo con las imágenes Docker | Login de tres roles, crear/consultar/editar/trasladar/bajar equipo, historial, filtros y sesión comprobados en entorno aislado. |
| HTTP adicional Docker | 33/33 comprobaciones aprobadas; no son endpoints distintos ni una nueva suite backend. |
| Aserciones Docker | 24/24 aserciones de flujo y consistencia aprobadas; no son pruebas JUnit. |
| PostgreSQL | V1–V9 exitosas, cinco consultas de inconsistencia en cero y conteos habituales preservados. Entorno temporal eliminado. |
| Actions backend y frontend | Ambos trabajos exitosos en [ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900), commit `9956939`. Jason ejecutó 27/27 pruebas antes de publicar. |
| GHCR | Ambas imágenes confirmadas con `latest` y `sha-9956939`; digests registrados en la evidencia consolidada. |
| Despliegue en la nube | Pendiente. GHCR acredita publicación de imágenes, no una URL pública operativa. |
| FE-08–FE-10 y diseño | Conservan sus pendientes funcionales, visuales y de accesibilidad. |

Las 27 pruebas y las 42 comprobaciones HTTP de la auditoría Vite se conservan como
su evidencia original. Las 33 HTTP y las 24 aserciones corresponden a una validación
posterior distinta; no se suman entre sí ni a las 233 pruebas históricas del backend.

La estructura y el nivel de detalle siguen el formato de `fe-01-login-sesion.md`:
cada documento separa estado comprobado, alcance, referencia visual, contratos,
recorrido guiado y evidencia. Se conserva la distinción entre implementación
del frontend y comportamiento que solo puede certificarse mediante pruebas con
el backend.

## Documentos

| Archivo | Bloque |
|---|---|
| [Auditoría de integración](auditoria-integracion.md) | Estado consolidado, hallazgos y evidencia de validación |
| [Verificación Docker y Actions](../despliegue/verificacion-docker-actions-2026-10-03.md) | Flujo completo, aislamiento de pruebas y publicación de ambas imágenes |
| [FE-01](fe-01-login-sesion.md) | Diseño común, login y sesión |
| [FE-02](fe-02-dashboard.md) | Dashboard |
| [FE-03](fe-03-equipos.md) | Equipos e inventario |
| [FE-04](fe-04-categorias-subcategorias.md) | Categorías y subcategorías |
| [FE-05](fe-05-laboratorios-sedes-areas.md) | Laboratorios, sedes y áreas |
| [FE-06](fe-06-usuarios-roles-asignacion.md) | Usuarios, roles y asignación |
| [FE-07](fe-07-movimientos.md) | Movimientos e historial |
| [FE-08](fe-08-mantenimientos.md) | Mantenimientos |
| [FE-09](fe-09-reportes.md) | Reportes, indicadores y exportación |
| [FE-10](fe-10-configuracion.md) | Configuración general y seguridad |
| [FE-11](fe-11-arquitectura-frontend.md) | Arquitectura, navegación y componentes comunes |

## Estado declarado en la entrega original

Este resumen conserva la clasificación inicial. La auditoría detectó defectos en
sesión, permisos, asignaciones, estados y movimientos; la existencia de llamadas
API no demuestra por sí sola que esos recorridos estén terminados.

### Implementado

- Login y sesión.
- Protección de la aplicación.
- Dashboard.
- Equipos.
- Categorías y subcategorías.
- Laboratorios, sedes y áreas.
- Historial de movimientos.
- Asignación de laboratorios a usuarios.
- Navegación lateral y componentes comunes.
- Cliente HTTP con Axios.

### Parcial o visual

- Usuarios: actualmente se implementan asignaciones, no CRUD completo.
- Mantenimientos: superficie visual preparada.
- Reportes: superficie visual preparada.
- Configuración: superficie visual preparada.

## Referencias visuales

Las diez referencias están en [docs/frontend/mockup](../frontend/mockup/).
La entrega comparte parte de su lenguaje visual, pero mantiene diferencias
estructurales: Equipos usa tarjetas, Categorías usa pestañas, Usuarios solo tiene
asignaciones y Mantenimientos/Reportes/Configuración son placeholders. La auditoría
detalla las diferencias por pantalla; no se certifica fidelidad visual completa.

Los valores CSS son adaptación visual; no se consideran valores oficiales
extraídos de los mockups.

## Criterio documental

`IMPLEMENTADO` significa que existe código correspondiente en el frontend.
`REVISADO ESTÁTICAMENTE` significa que se inspeccionó el código o estructura.
`COMPROBADO POR EJECUCIÓN` se reserva para resultados de ejecución aportados o
realizados y documentados de forma explícita.
`PENDIENTE` significa que no hay evidencia suficiente para cerrar ese punto.

Esta carpeta documenta frontend y no implica cambios en backend ni PostgreSQL.
