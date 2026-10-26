# Memoria de conversaciones, decisiones y evolución

Corte: **26 de octubre de 2026, America/Lima**. Este archivo conserva contexto
útil para continuar el proyecto. Es una síntesis técnica contrastada con fuentes;
no es una transcripción literal ni un respaldo de las bases de datos.

## Cobertura de conversaciones

Se consultaron los seis chats técnicos principales con `read_thread`, paginando
hasta que no quedaron turnos anteriores. Son **224 registros de turnos**; varios
chats son bifurcaciones y repiten historia, por lo que no son 224 tareas distintas.
También se leyeron cinco conversaciones auxiliares completas, 14 registros.
Los títulos siguientes son los devueltos por la aplicación al consultar cada chat.
Las fechas se expresan en Lima y corresponden a los turnos recuperados, no a la
última apertura del chat.

| Chat | ID para volver a consultarlo | Turnos | Intervalo recuperado |
|---|---|---:|---|
| Etapa final | `01a12d6c-bb32-7fa2-9c1c-8e880f23e3b5` | 1 | 2026-10-11 |
| Resumen grabaciones - docker 24,25,26y28 | `01a0ef1b-8535-70a2-bc91-08e0de0f560d` | 42 | 2026-09-03 a 2026-10-11 |
| backend sprint 1 hasta el 7 | `01a07f35-88e9-74e3-9a21-efc9b5484dca` | 27 | 2026-09-07 a 2026-10-11 |
| frontend sprints | `01a0f8df-644a-70a2-b61e-f1b533b32418` | 61 | 2026-09-07 a 2026-10-03 |
| docker - validacion db y back | `01a0fa11-afd1-7970-9650-d32298f5e446` | 64 | 2026-09-03 a 2026-10-03 |
| docker | `01a0fab6-6b37-7d30-94ca-99259a8b9bc2` | 29 | 2026-09-03 a 2026-10-03 |
| proyecto v0 | `01a06922-3867-77f0-b70e-d56853400273` | 6 | 2026-09-03 a 2026-10-01 |
| Localiza la memoria de chats | `01a0f4c2-2557-72e1-847a-17af8e9ffa23` | 1 | 2026-09-30 |
| Revisar output del proyecto | `01a082c0-a146-7de0-8266-c816a83ec60f` | 3 | 2026-09-08 |
| Comparar estructuras de proyectos | `01a05eb7-3e4a-7383-97ca-992be3c28f03` | 3 | 2026-09-01 a 2026-09-16 |
| Confirma análisis de categorías | `01a069ad-1ba0-7682-a747-2079fb94db56` | 1 | 2026-09-03 |

Los cinco auxiliares incluyen conversaciones con cwd de otros proyectos del curso;
solo se incorporan antecedentes que explican Inventario. Se localizaron mediante
los índices de chats disponibles y el índice local de sesiones. No se afirma
cobertura universal de todos los chats de la cuenta, de sesiones borradas ni de
fuentes no disponibles. No se copiaron logs crudos, archivos adjuntos privados
ni secretos de conversaciones antiguas. La petición actual de crear esta memoria
añade documentación; no confirma nuevos cambios de producto o un nuevo despliegue.

## Evolución y decisiones que siguen siendo relevantes

| Etapa | Resultado y significado actual |
|---|---|
| Diseño y curso, septiembre | Se eligió completar una vertical de Categoría antes de Equipo, para aprender JPA, DTO, MapStruct, validación y seguridad sin depender de todas las relaciones. Era un orden de implementación; Equipo sí existe ahora. |
| Sprints 1–3 | Base Spring Boot/Gradle/Java, Flyway/PostgreSQL, Categoría y luego JWT/BCrypt/usuarios. Se conservaron las carpetas del proyecto y el estilo de las referencias del profesor. |
| Sprint 4A | Subcategoría, unicidad por categoría y protección de padres. |
| Sprint 4B–4D | Organización Sede → Área → Laboratorio. |
| Sprint 4E | UsuarioLaboratorio N:M, reemplazo atómico de asignaciones y alcance efectivo por rol. Las tablas ya existían; no se necesitó una migración nueva para cada API. |
| Sprint 5 | Equipo, filtros, custodia opcional, baja lógica, alcance y concurrencia. |
| Sprint 6 | Traslado transaccional e historial MovimientoEquipo. |
| Sprint 7, septiembre | Cierre del alcance original: V1–V9, 10 entidades, 42 operaciones HTTP y 233 pruebas. Es un corte histórico. |
| Sprint 8 y Docker | Monorepo conservado; imágenes independientes para backend y frontend y motor PostgreSQL en su propio contenedor. Un Compose coordina los tres. Se eliminó el Compose duplicado de Jason. |
| Marko, 1 de octubre | Base React/Vite, proxy y cliente HTTP como tutoría; FE-01 no terminó. Se pausó esta variante al integrar Jason. |
| Jason, 3 de octubre | Se conservaron su diseño y estructura; se corrigió la integración con el backend, permisos, payloads y asignaciones. Primera suite 27 tests; luego 67 al adaptar contratos ampliados. |
| Ampliación posterior a Sprint 7, 3 de octubre | Usuarios administrativos, reactivación de catálogos, estado operativo de laboratorio, mantenimientos, reportes y auditoría. Esquema V1–V13; 12 tablas/98 columnas/16 FK; 70 operaciones HTTP; 319 JUnit documentadas. |
| Docker integrado, 3 de octubre | Imágenes del commit `e1ce75a`, preservación del volumen/datos y smoke con 94 HTTP, 30 aserciones funcionales y 7 controles SQL en base temporal eliminada. No mezclar estas cantidades con JUnit o tests frontend. |
| Nube, 10–11 de octubre | El usuario creó una nueva BD Render, sin importar la local. Configuró backend Render y confirmó publicación de Jason en Vercel. Se añadió `vercel.json` en `b164c44`. |
| ERD, 11 de octubre | ERD v3 lógico y físico documentan V13. Commit local HEAD `0344b33` conserva ese cambio. |
| Etapa final, 11 de octubre | Revisión de solo lectura: frontend público 200, backend `UP` y proxy Vercel `/api/auth/me` 401. No se acreditó login autenticado en nube. |
| Memoria, 26 de octubre | Revisión nueva de código/entorno/historial. Jason 67/67 y lint Marko correctos; Docker daemon inaccesible; PostgreSQL Windows activo sin consulta de datos; Vercel HTML 200. Resultados remotos parciales en el anexo de deployment. |
| Conservación local de Marko, 26 de octubre | El usuario autorizó retirar del seguimiento Git `frontend/version-marko/` y `docs/sprints_realizados-frontend-Marko/`, ignorándolos y conservando íntegra su copia local. Los clones nuevos del árbol posterior a este cambio no incluyen esa variante ni sus sprints. El historial anterior permanece; Jason continúa como frontend integrado independiente. |

Fuentes: [índice backend](../sprints_realizados-backend/README.md),
[índice Jason](../sprints_realizados-frontend-Jason/README.md),
[índice Marko histórico, commit `0344b33`](https://github.com/markopuch/inventario-laboratorios/blob/0344b3301c93f5b50894fbf454a21c7b129af37b/docs/sprints_realizados-frontend-Marko/README.md),
[modelo V13](../Erd_actual/modelo-vigente-v13.md),
[evidencia Docker/Actions del 3 de octubre](../despliegue/verificacion-docker-actions-2026-10-03.md).

## Decisiones de arquitectura y operación

- **Un monorepo:** no hay decisión vigente de dividir backend y frontend en repositorios separados.
- **Tres servicios:** «todo junto» quedó resuelto como un único Compose con tres contenedores, no un proceso único que contenga la BD y ambas aplicaciones.
- **Jason es la versión integrada:** el usuario lo confirmó expresamente para Vercel. Marko tiene su propio proceso educativo local y sus dependencias no se deben mezclar. Compose y Actions seleccionan Jason; no necesitan la variante Marko ni sus sprints para su flujo de frontend.
- **Marko solo local desde el 26 de octubre:** sus carpetas de frontend y sprints quedan ignoradas y fuera del seguimiento para futuras versiones, manteniéndose en este equipo. Un clon nuevo del árbol posterior al cambio no las incluye; las fuentes anteriores siguen accesibles en el historial. Esta decisión conserva los antecedentes y versiones documentados en el [anexo frontend](frontend.md), y no autoriza volver a añadir esas carpetas por una revisión rutinaria de documentación.
- **Permisos:** rol define operaciones; UsuarioLaboratorio define alcance. Responsable de Equipo es custodia, no autorización.
- **Datos separados de imágenes:** publicar en GHCR no copia usuarios/equipos ni actualiza un volumen. Otra instalación obtiene los datos de migraciones/inicializadores o de un respaldo deliberadamente restaurado.
- **BDs independientes:** PostgreSQL nativo Windows, PostgreSQL de Compose y PostgreSQL de Render no son la misma base. Cambiar la conexión no traslada datos.
- **Sin Swagger por decisión temporal:** se detectó que no estaba instalado ni permitido y el usuario dejó pendiente añadirlo para continuar con Vercel. Un 401 en esa ruta no prueba que exista la documentación Swagger.
- **Puertos:** 8081 se sugirió para evitar conflicto con un backend local en 8080; el Compose vigente usa 8080. No tratar 8081 como contrato actual.
- **Memoria portable:** este conjunto vive en archivos del proyecto. No se escribió la base interna de memoria de Codex ni se exportaron los rollouts privados.

La revisión documental de esta reorganización, el **26 de octubre de 2026**, comprobó la presencia local del `package.json` de Marko y del índice de sus sprints, y releyó la selección de Jason en `backend/inventario/docker-compose.yml` y `.github/workflows/imagen.yml`. No repitió pruebas de aplicación ni verificó un despliegue nuevo. Las comprobaciones de auditoría anteriores conservan su fecha y alcance; la retirada del seguimiento no las convierte en pruebas nuevas.

## Acceso y creación de cuentas: lo que quedó pendiente

En el chat de despliegue el usuario confirmó que la base Render era nueva y que
solo la había creado siguiendo la guía. Se explicó activar temporalmente `dev`
con `DEMO_USER_PASSWORD` para crear cuentas que faltaran. **No hay respuesta
posterior que confirme haberlo ejecutado ni un login autenticado satisfactorio
en la nube.** No deducir que las cuentas locales existen allí.

El inicializador `DemoUsuariosConfig` usa cuentas fixture `marko`/ADMIN,
`aldo`/GESTOR y `romel`/LECTOR. Su contraseña procede de una variable privada;
no actualiza cuentas existentes. Cambiar esa variable no restablece contraseñas.
Se excluyen de esta memoria contraseñas históricas y ejemplos antiguos. El acceso
actual depende de la BD concreta y de su hash BCrypt; consultar el procedimiento
vigente antes de intentar recuperarlo.

## Cómo interpretar preferencias antiguas

El usuario valora explicaciones sencillas, pasos pequeños y correspondencia con
el curso. En varios turnos pidió escribir él mismo el código o no ejecutar nada;
en otros autorizó implementación, pruebas, commits y publicación. Esas órdenes
tenían alcance de tarea, y no deben convertirse en una prohibición permanente
contra la petición actual. Mantener la arquitectura y el diseño de Jason ha sido
una preferencia repetida; un cambio de producto posterior puede revisarla.

Las referencias académicas `Carlos_backend`, Techstore, sesiones de clase y el
proyecto anterior `Inventario` sirven para entender el origen. No son dependencias
de ejecución del producto actual ni fuentes superiores a sus contratos actuales.
La propuesta inicial de dos roles ADMIN/USUARIO quedó superada por los tres roles
actuales. Una explicación antigua de «análisis por categorías» tampoco modifica
por sí sola el esquema o los reportes que existen hoy.

## Reanudación sugerida

1. Leer [memory.md](../../memory.md), el anexo del componente y `git status`.
2. Identificar entorno: nativo, Compose o nube, y no transferir credenciales entre ellos por suposición.
3. Para cierre cloud, confirmar el primer ADMIN y ejecutar login/permisos/flujos con una cuenta autorizada, sin publicar credenciales ni usar la BD habitual como fixture.
4. Revisar pendientes de producto con el usuario; Configuración, auditoría visual y exportación son ampliaciones, no funciones completadas por los sprints antiguos.
5. Actualizar fecha y evidencia de la memoria cuando cambie el estado.
