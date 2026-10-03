# Sprints realizados — Frontend Jason

Esta carpeta registra la entrega y la revisión del frontend Jason de Inventario
de Laboratorios, ubicado en `frontend/version-jason/frontend/`.

La [auditoría de integración del 3 de octubre de 2026](auditoria-integracion.md)
consolida los hallazgos posteriores, las correcciones verificadas, las diferencias
con los mockups y los límites de las pruebas. Los documentos FE-01 a FE-11 conservan
el registro de la entrega; sus afirmaciones originales deben leerse junto con
esa auditoría. No se trasladan resultados de la versión Marko a la versión Jason.

La [guía de ejecución de Jason](../../frontend/version-jason/frontend/README.md)
explica Docker, Vite en 5173, el proxy `/api`, la sesión en memoria y los checks.
La revisión final registra 27/27 pruebas automatizadas, build aprobado y 42 solicitudes
HTTP verificadas por proxy, además de recorridos de navegador en un entorno aislado.
La integración local es operativa en las funcionalidades implementadas; FE-08–FE-10,
la fidelidad visual 1:1 y la accesibilidad exhaustiva continúan pendientes.

La estructura y el nivel de detalle siguen el formato de `fe-01-login-sesion.md`:
cada documento separa estado comprobado, alcance, referencia visual, contratos,
recorrido guiado y evidencia. Se conserva la distinción entre implementación
del frontend y comportamiento que solo puede certificarse mediante pruebas con
el backend.

## Documentos

| Archivo | Bloque |
|---|---|
| [Auditoría de integración](auditoria-integracion.md) | Estado consolidado, hallazgos y evidencia de validación |
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
