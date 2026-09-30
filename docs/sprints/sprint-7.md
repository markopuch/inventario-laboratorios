# 1. Resumen Sprint 7

**BACKEND CERRADO PARA ENTREGA.** Se auditó y consolidó la entrega existente,
sin añadir funcionalidades de inventario. Se corrigió documentación, se retiraron
once marcadores de carpetas ya pobladas y se prepararon documentación final,
Postman y un script de verificación protegido.

Resultado: **233 pruebas aprobadas**, cero fallos, errores u omitidas;
compilación/JAR correctos, arranque real y **28 solicitudes HTTP de smoke**
correctas. La instancia se detuvo, la base temporal se eliminó y la habitual
conservó sus datos. Evidencia: [verificación final](../backend-final/verificacion-final.md).

# 2. Estado inicial auditado

Referencia inicial Git `e41b53d`, árbol limpio y 218 archivos versionados.
Sprint 6 cerrado con 233 pruebas en 33 suites. Flyway V1–V9 aplicado con
success=true y checksums coincidentes con los archivos SQL. Diez Entities,
diez tablas, 76 columnas, trece relaciones FK y 42 operaciones HTTP.

Se inspeccionaron backend/inventario, docs, README y el script existente antes
de editar. El [inventario inicial](../backend-final/inventario-auditoria.md)
registra configuración, capas, reglas, endpoints y datos habituales de partida.

# 3. Hallazgos

**Bugs reales:** no se demostró un defecto funcional que exigiera cambiar Java,
pruebas o esquema. La diferencia histórica de IDs no positivos en Categoría
(404 al no existir) se documentó; no se cambió únicamente por uniformidad.

**Inconsistencias documentales:** siete enlaces rotos en guías antiguas; JWT
todavía describía asignaciones/Equipo como pendientes; matriz mezclaba propuestas
administrativas con permisos actuales; RN-01/04/26/27 necesitaban precisar su
alcance; README conservaba referencias antiguas y una nota del ERD decía Equipo
«sin traslado». Once .gitkeep eran redundantes en paquetes con clases.

**Mejoras opcionales:** uniformar IDs inválidos de Categoría, paginación futura
y los módulos del backlog. Dos consultas sin consumidores actuales de
UsuarioLaboratorioRepository se conservaron: no son un fallo ni requieren
eliminar API interna para este cierre.

# 4. Correcciones realizadas

Se repararon los enlaces, se actualizaron JWT/matriz/reglas y la frase obsoleta
del ERD, y se reemplazó el README acumulativo por un documento de entrega con
15 secciones. RN-01–45 conservan su numeración y tienen clasificación/evidencia.

Se crearon los documentos finales, collection/environment y verificar-backend.ps1.
La limpieza retiró únicamente once .gitkeep sin referencias concretas, comprobando
primero que sus carpetas contienen Java. No se alteró el comportamiento del backend.

# 5. Arquitectura final

```text
Cliente HTTP / JSON
        ↓
Spring Security / JWT
        ↓
Controller → Request DTO + Validation → Mapper → Domain
                                                  ↓
                                               Service
                                                  ↓
                                               Repository
                                                  ↓
                                               Entity JPA
                                                  ↓
                                               PostgreSQL

Respuesta: Entity → Domain → Mapper → Response DTO → HTTP
```

Service concentra negocio, alcance y transacciones; Repository concentra
persistencia. Los Mappers no consultan la base y las respuestas no exponen
Entities ni hashes. Se mantienen @Autowired, MapStruct, Lombok y Domain/Entity.
Ejemplos: [arquitectura final](../backend-final/arquitectura-backend.md).

# 6. Modelo final

Sede, Area, Laboratorio, Categoria, Subcategoria, Rol, Usuario,
UsuarioLaboratorio, Equipo y MovimientoEquipo están implementados.

Las trece relaciones son: Sede–Area, Area–Laboratorio,
Categoria–Subcategoria, Rol–Usuario, Usuario–UsuarioLaboratorio,
Laboratorio–UsuarioLaboratorio, Subcategoria–Equipo, Laboratorio–Equipo,
Usuario–Equipo (responsable), Equipo–Movimiento, Usuario–Movimiento (actor),
Laboratorio–Movimiento (origen) y Laboratorio–Movimiento (destino).

UsuarioLaboratorio tiene PK compuesta. Responsable de Equipo y origen histórico
de Movimiento son opcionales; los demás vínculos indicados son obligatorios.
ERD v2 y sus SVG se conservan, sin rediseño ni v3 artificial.

# 7. Seguridad final

ADMIN tiene acceso global según cada contrato. GESTOR administra Equipos y
traslados dentro de su alcance; trasladar exige origen y destino autorizados.
LECTOR consulta Equipos/historia dentro de su alcance. Los catálogos se consultan
globalmente por los tres roles; su escritura corresponde a ADMIN.

Rol define qué operación se permite; UsuarioLaboratorio define dónde.
Custodia no concede permisos. JWT verifica firma/vigencia/emisor y recarga el
usuario/rol actuales. Actor y origen de traslado se obtienen en el servidor.
Se distinguen 401, 403, 404 y 409; los 500 no exponen SQL ni trazas al cliente.

# 8. Endpoints finales

**42 operaciones**, contrastadas entre Controllers, documentación y Postman:

| Grupo | Cantidad |
|---|---:|
| AUTH | 3 |
| CATEGORIA | 6 |
| SUBCATEGORIA | 5 |
| SEDE | 6 |
| AREA | 6 |
| LABORATORIO | 5 |
| USUARIO_LABORATORIO | 2 |
| EQUIPO | 6 |
| MOVIMIENTO | 3 |

Las jerarquías se agrupan con el padre; alcance propio pertenece a AUTH.
Filtros y variantes de rol no aumentan la cantidad. El
[catálogo final](../backend-final/endpoints.md) detalla cada contrato y errores.

# 9. Flyway

Versión final **V9**. No hubo nueva migración ni se modificó una aplicada.
Los nueve checksums coinciden con la base habitual y la temporal; success=true
en todas las versiones. El JAR volvió a validar las nueve migraciones.
Hibernate conserva ddl-auto=validate. El inventario incluye los checksums y la
[auditoría Entity/Flyway](../backend-final/auditoria-entity-flyway.md) coteja columnas y restricciones.

# 10. Tests

| Medición | Resultado |
|---|---:|
| Anteriores | 233 |
| Nuevos | 0 |
| Total | 233 |
| Aprobados | 233 |
| Fallidos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Suites | 33 |

Se reejecutaron todas las tareas con --rerun-tasks y se analizaron los XML;
las pruebas anteriores conservan sus archivos. Base utilizada:
`inventario_verificacion_s7_cierre_20260921_a73f`, eliminada al concluir.

# 11. Compilación

compileJava y bootJar correctos. La ejecución combinada de verificación terminó
BUILD SUCCESSFUL en 1 min 34 s, seis tareas ejecutadas. Se generó
`backend/inventario/build/libs/inventario-0.0.1-SNAPSHOT.jar`, de 61 012 323 bytes.
El script comprueba que existan XML JUnit y no haya fallos, errores u omitidas.

# 12. Arranque

El JAR arrancó realmente con Java 21 en 8,32 segundos, escuchando únicamente en
127.0.0.1, puerto automático 56448. Flyway validó V1–V9 y JPA inicializó su
EntityManagerFactory con validate. Se detuvo el proceso de prueba y se verificó
que su PID no existía y el puerto ya no escuchaba. No se dejó una instancia oculta.

# 13. Smoke test

28 solicitudes HTTP con códigos y contenido esperados: login de los tres roles,
alta de catálogos y jerarquía propia, asignaciones, consulta de alcance, creación,
consulta, edición y filtros de Equipo, traslado con GESTOR, historia visible por
origen para LECTOR, validaciones 400/401/403/409, baja y preservación del historial.

Se usaron los usuarios demo existentes únicamente como fixtures temporales.
El Equipo quedó en BAJA con un Movimiento TRASLADO; SQL confirmó destino, origen
y actor. No se implementó creación administrativa de usuarios para la demostración.

# 14. PostgreSQL

Las cinco consultas de consistencia dieron cero incidencias: Equipos no BAJA
bajo Subcategoría/Laboratorio inactivos; asignaciones activas hacia Laboratorio
inactivo; movimientos huérfanos; TRASLADO con origen igual a destino.

Se verificaron diez PK, trece FK, nueve restricciones UNIQUE, cuatro CHECK y
los índices funcionales únicos. No se modificaron filas para ocultar problemas.
Los fixtures propios se limpiaron, se comprobaron cero conexiones y se eliminó
solo la base temporal sin FORCE; pg_database confirmó su ausencia.

# 15. Base habitual

inventario_laboratorios recibió únicamente consultas read-only. Conteos y
huellas públicas son idénticos antes y después, sin leer ni mostrar password_hash.

| Tabla | Antes | Después |
|---|---:|---:|
| usuario | 3 | 3 |
| usuario_laboratorio | 0 | 0 |
| categoria | 2 | 2 |
| subcategoria | 4 | 4 |
| sede | 1 | 1 |
| area | 2 | 2 |
| laboratorio | 2 | 2 |
| equipo | 0 | 0 |
| movimiento_equipo | 0 | 0 |

# 16. Documentación creada

Diez Markdown en docs/backend-final: inventario de auditoría; cotejo Entity/Flyway;
arquitectura; endpoints; ocho flujos; códigos HTTP; backlog; checklist de 26
requisitos; resumen ejecutivo; verificación final. Este reporte registra las
24 secciones del cierre. Las rutas y propósitos se enumeran en la sección 21.

# 17. Postman

Collection v2.1 importable y environment creados. Siete carpetas, 65 solicitudes
para las mismas 42 rutas, con variantes de filtros/roles. Esquema oficial,
correspondencia de rutas, variables/cuerpos, sintaxis de 129 scripts y simulación
de captura/escape de tres login comprobados. Contraseñas, tokens e IDs vacíos.

Se entregan ejemplos de login, CRUD, alcance, filtros, traslado e historial.
El [orden guiado](../backend-final/endpoints.md#uso-guiado-de-postman) evita ejecutar
bajas antes de sus dependientes. No se afirma haber usado la aplicación gráfica
Postman ni ejecutado su Runner completo: el smoke real fue HTTP sobre el JAR.

# 18. README final

Quince secciones: descripción, stack, arquitectura, modelo, roles, alcance,
funcionalidades, endpoints, PostgreSQL/Flyway, configuración, ejecución, tests,
Postman, documentación y backlog. Conserva anclas usadas por guías antiguas.
Explica iniciar-backend.ps1 y verificar-backend.ps1, variables externas, Run Java,
puerto ocupado y las diferencias entre cierre actual y evidencia histórica.

# 19. Backlog

Fuera del backend entregado: CRUD administrativo de usuarios/cambio de roles,
mantenimiento como módulo, auditoría general, frontend, Docker, permisos
dinámicos, refresh token y eventual paginación. No son bugs ni bloqueos de esta
entrega. El [backlog](../backend-final/backlog.md) distingue ampliaciones y defectos.

# 20. Checklist Taller

Los **26 requisitos** están verificados con archivo/evidencia y demostración
cuando aplica: [checklist de entrega](../backend-final/checklist-entrega.md).
Las evidencias estructurales se distinguen de tests, arranque, SQL y smoke.
No se agregaron endpoints artificiales para cubrir requisitos del taller.

# 21. Archivos creados

**14 archivos nuevos**:

| Ruta | Propósito |
|---|---|
| verificar-backend.ps1 | compileJava/test/bootJar protegidos para base temporal |
| docs/backend-final/inventario-auditoria.md | Acta inicial de solo lectura |
| docs/backend-final/auditoria-entity-flyway.md | Cotejo de 10 Entities y esquema real |
| docs/backend-final/arquitectura-backend.md | Capas, seguridad y transacciones |
| docs/backend-final/endpoints.md | Las 42 operaciones reales |
| docs/backend-final/flujos-principales.md | Ocho flujos del backend |
| docs/backend-final/codigos-http.md | Éxitos, errores y ejemplos |
| docs/backend-final/backlog.md | Funciones fuera del alcance entregado |
| docs/backend-final/checklist-entrega.md | 26 requisitos con evidencia |
| docs/backend-final/resumen-backend.md | Resumen para exposición |
| docs/backend-final/verificacion-final.md | Pruebas, arranque, smoke, SQL y preservación |
| docs/backend-final/postman/Inventario-Laboratorios.postman_collection.json | Requests importables |
| docs/backend-final/postman/Inventario-Laboratorios.postman_environment.json | Variables sin secretos |
| docs/sprints/sprint-7.md | Este reporte de cierre |

# 22. Archivos modificados

**8 archivos modificados**:

| Ruta | Motivo |
|---|---|
| README.md | Documento principal de entrega con 15 secciones |
| docs/reglas-negocio.md | Clasificar RN-01–45 y precisar contradicciones |
| docs/matriz-permisos.md | Separar permisos implementados y backlog |
| docs/autenticacion-jwt.md | Actualizar alcance y estado vigente |
| docs/Erd_actual/erd-v2-cambios.md | Corregir una frase que omitía traslado |
| docs/sprints/sprint-3-categorias.md | Reparar cuatro enlaces |
| docs/sprints/sprint-4a-subcategorias.md | Reparar dos enlaces |
| docs/sprints/sprint-4b-organizacion.md | Reparar un enlace |

No cambió ningún Java, test, SQL de migración, build.gradle.kts, configuración
de Spring, script de arranque ni SVG. Los hechos de los sprints anteriores se
conservan; únicamente se repararon sus enlaces.

# 23. Archivos eliminados

Once `.gitkeep` de un byte, todos dentro de
`backend/inventario/src/main/java/com/utec/inventario/`:

| Ruta relativa | Justificación: clases Java presentes |
|---|---:|
| config/.gitkeep | 3 |
| controller/.gitkeep | 14 |
| domain/.gitkeep | 14 |
| dto/request/.gitkeep | 15 |
| dto/response/.gitkeep | 22 |
| entity/.gitkeep | 11 |
| exception/.gitkeep | 6 |
| mapper/.gitkeep | 9 |
| repository/.gitkeep | 10 |
| security/.gitkeep | 4 |
| service/.gitkeep | 10 |

Se comprobó ausencia de referencias a las rutas concretas y se resolvieron
dentro del workspace antes de eliminarlas. No se borraron carpetas, clases,
DTOs, migraciones ni documentos históricos. Evidencia en auditoria-entity-flyway.

# 24. Estado final

**BACKEND CERRADO PARA ENTREGA.** No hay bloqueos funcionales demostrados en el
alcance acordado. La entrega compila, pasa sus 233 pruebas, arranca y demuestra
autenticación, roles, alcance, CRUD, filtros, baja, traslado e historial.
No se inicia Sprint 8 ni se implementa ninguna función del backlog.

Checklist Sprint 7:

- [x] V1–V9 verificadas
- [x] Ninguna migración aplicada modificada
- [x] 10 Entities auditadas
- [x] Arquitectura auditada
- [x] Seguridad auditada
- [x] Endpoints auditados
- [x] Reglas auditadas
- [x] Matriz final
- [x] ERD final
- [x] README final
- [x] Arquitectura final documentada
- [x] Endpoints documentados
- [x] Flujos documentados
- [x] Códigos HTTP documentados
- [x] Postman final
- [x] Backlog documentado
- [x] Checklist Taller
- [x] Resumen ejecutivo
- [x] compileJava correcto
- [x] Suite completa correcta
- [x] bootJar correcto
- [x] Arranque correcto
- [x] Flyway correcto
- [x] Smoke test correcto
- [x] SQL de consistencia correcto
- [x] Base habitual preservada
- [x] Cero secretos reales en archivos versionados o nuevos de esta entrega
- [x] No se agregó funcionalidad fuera de alcance
