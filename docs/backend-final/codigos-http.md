# Códigos HTTP del backend entregado

**Revisión vigente:** 3 de octubre de 2026, commit e1ce75a y Flyway V13. [Evidencia Docker/Actions](../despliegue/verificacion-docker-actions-2026-10-03.md).

Los códigos indican el resultado de la solicitud. Los contratos completos y
permisos están en [endpoints](endpoints.md). Las respuestas de negocio y seguridad
usan DTO; una excepción no devuelve Entity, SQL, contraseña, hash ni stack trace.

| Código | Significado | Ejemplo real |
|---|---|---|
| 200 | Consulta o acción completada | Login, GET Equipo, PUT Equipo, reemplazo de asignaciones, POST traslado |
| 201 | Recurso creado | POST Categoria/Subcategoria/Sede/Area/Laboratorio/Equipo/Usuario/Mantenimiento; devuelve Location |
| 204 | Acción completada sin cuerpo | DELETE lógico de catálogo/Equipo y PUT administrativo de contraseña |
| 400 | Solicitud inválida | JSON mal formado, motivo vacío, ID no entero, campos prohibidos en PUT Equipo o traslado |
| 401 | Falta autenticación válida | Sin Bearer, JWT inválido/expirado, credenciales de login incorrectas, usuario o rol inactivo |
| 403 | Operación o alcance no autorizado | LECTOR hace POST Equipo; GESTOR intenta traslado sin alcance en ambos extremos |
| 404 | Recurso no encontrado según el contrato | Equipo inexistente, catálogo inexistente/inactivo, referencia padre inexistente |
| 409 | Conflicto con el estado del negocio | Código duplicado, baja de padre con hijos activos, Equipo BAJA, destino inactivo o igual al origen |
| 500 | Fallo interno inesperado | Excepción no prevista; respuesta genérica sin detalles internos |

POST traslado devuelve 200 porque confirma una acción conjunta sobre Equipo e
historial. No debe confundirse con POST de alta CRUD, que devuelve 201.

## Forma de los errores

Ejemplo ilustrativo de un conflicto; la fecha la genera el servidor:

```json
{
  "timestamp": "2026-09-22T00:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "El laboratorio destino debe ser diferente al laboratorio actual.",
  "path": "/api/equipos/1/traslados"
}
```

En errores de validación de campos puede aparecer `errors`, un objeto
campo → mensaje. Cuando está vacío se omite. El ID del ejemplo no es un fixture
garantizado: usa los IDs reales obtenidos al crear/consultar tus datos.

## Cómo distinguir 401, 403, 404 y 409

1. **401:** el backend no puede aceptar la identidad de esa petición. Obtener
   otro JWT o corregir las credenciales puede resolverlo.
2. **403:** la identidad es válida, pero su rol o alcance no autoriza la acción.
   Repetir el login no concede permisos; las asignaciones se leen actualmente.
3. **404:** no existe el recurso solicitado según las reglas del endpoint. Los
   catálogos ocultan registros inactivos; Equipo conserva BAJA como histórico.
4. **409:** el recurso y la solicitud pueden existir, pero su estado impide la
   operación. Trasladar un Equipo BAJA o desactivar un padre con hijos activos
   no se soluciona cambiando el formato JSON.

La seguridad se aplica antes de ejecutar negocio: por ejemplo, un LECTOR que
envía un traslado inválido sigue recibiendo 403. No se promete que todas las
validaciones produzcan el mismo primer error si hay varios problemas simultáneos.

## Casos particulares conservados

- Categoría conserva 404 para un ID entero inexistente, incluso cero o negativo.
  Los otros módulos con `@Positive` devuelven 400 para esos valores. Un ID que
  no se puede convertir a entero devuelve 400.
- GESTOR/LECTOR sin asignaciones obtienen listas de Equipos/Movimientos vacías;
  un filtro explícito de laboratorio fuera de alcance produce 403.
- Un Equipo fuera del alcance actual puede tener historia parcialmente visible
  por el origen o destino de movimientos previos. Véanse las reglas de historial.
- No existe API de edición/borrado ni POST directo de movimientos. Las rutas
  no autorizadas explícitamente se deniegan por defecto; no se anuncian como CRUD.
- 405 (método no admitido) y 415 (tipo de contenido no admitido) se manejan si
  Spring MVC alcanza esos casos; seguridad puede rechazar antes la solicitud.

## Evidencia

`GlobalExceptionHandler`, `SecurityErrorHandler` y `ApiError` definen la forma
de respuesta. Las pruebas existentes verifican validación, autenticación,
autorización, conflictos, errores internos seguros y rollback. El cierre medido
está en [verificación final](verificacion-final.md). No se agrega un endpoint
que provoque errores 500 para demostrar el taller.


## Contratos de las extensiones vigentes

- PATCH de actividad de catálogo/Usuario, PATCH de rol y PATCH de estado de
  Mantenimiento responden 200 con su DTO actualizado.
- PUT de contraseña administrativa responde 204, sin devolver el valor nuevo.
- Desactivar/degradar al último ADMIN activo produce 409.
- Editar, dar de baja o trasladar Equipo con mantenimiento EN_PROCESO produce 409.
- Un ciclo de mantenimiento inválido o reabrir un estado final produce 409.
- Un rango invertido de reportes/filtros produce 400; GESTOR/LECTOR solicitando
  auditoría o usuarios administrativos produce 403.
- GET administrativo puede incluir catálogo inactivo o Usuario inactivo.
  Su inactividad no significa por sí sola 404 en ese contrato; GET normal
  de catálogo conserva el filtrado por activo.

Los Requests vigentes y el alcance están en [endpoints](endpoints.md).
No se deben interpretar los ejemplos históricos de Sprint 7 como pruebas
nuevas del despliegue actual.
