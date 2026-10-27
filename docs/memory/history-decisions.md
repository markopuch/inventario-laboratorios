# Memoria de conversaciones, decisiones y evolución

**Bloque posterior autorizado y publicado el 27:** el usuario pidió continuar con
paginación y límite de login, y después indicó continuar hasta terminar.
Equipos/Auditoría paginan sin romper arrays anteriores; búsqueda y COUNT respetan
alcance/filtros. Login limita en memoria con 429/Retry-After y espera visible.
406 backend,107 frontend, build y UI real temporales aprobados. No se cambian
claves, migraciones ni datos habituales durante la implementación. El usuario
autorizó después publicar backend y frontend y verificar Render/Vercel. Se
publicó primero el backend compatible `34d8033`: Actions 40564481861 aprobó 5/5
(406 backend, 93 frontend anterior y 28 importador); Render quedó Live en 4m16s
con salud UP, trece migraciones al día y lectura ADMIN paginada de 20/258 equipos.
Después se publicó el frontend `7412831`; Vercel reporta success para ese SHA,
ficha `nBQ59JBg7CcodNPtqu2y7NGYCpTs`. Su CI 40565271628 terminó correctamente
(406 backend, 107 frontend/build 1681 y 28 importador). La UI pública comprobó
páginas/tamaños 20/50/100 y búsqueda con 258 equipos, además de Auditoría
20/100 sobre 351 eventos. [Publicación cloud](../despliegue/verificacion-paginacion-login-cloud-2026-10-27.md).
La primera prueba de límite cloud obtuvo seis 401 con una cuenta inexistente.
Se aprobó corregir el contador a cinco fallos agregados por username normalizado
en 900s, sin depender de un peer estable; permanece la cuota 60/60s por peer.
No se confía en headers IP ni se añaden dependencias. La corrección se publicó
en ef31d65 tras 410/410 pruebas backend locales, JAR y BUILD SUCCESSFUL en
2m17s; Actions 40567596491 aprobó 5/5 con 410 backend, 107 frontend/build 1681
y 28 importador, ambas imágenes publicadas. Vercel reporta success para ef31d65,
ficha `E4o5pumzx8Z9hPRVXSsvtQKYysTu`; Render desplegó ef31d65 en
`dep-dbgfh18473hc739j058g`, Live en 11m46s y salud UP. El diagnóstico SQL de
solo lectura con TLS confirmó 258 equipos, trece migraciones y ningún
bloqueador en ese momento, sin demostrar la causa exacta del arranque
prolongado; no hubo reinicios manuales ni cambios de configuración. La
respuesta 429 cloud se comprobó después. El guard posterior observó cuatro
GET públicos sin caché con el contrato previo pese al Live del panel, mientras
GitHub ef31d65 contiene el contrato agregado. Tras contrastar repo/main,
root/Dockerfile/contexto, se inició Clear build cache & deploy del mismo SHA,
`dep-dbgfo1mi0phs73dt8020`. No se demostró la causa de la discrepancia ni se
dio por aprobado el límite; el primer intento UI ficticio terminó en 401 y se
detuvo. La reconstrucción limpia acreditó checkout ef31d659, COPY de src y
bootJar BUILD SUCCESSFUL en 26 s. El nuevo despliegue quedó Live en 4m52s,
startup 171.8 s y OpenAPI pública corregida. Cinco POST con conexiones
independientes a Render dieron 401 para un username ficticio; el sexto desde
la UI Vercel mostró espera 868→839 s y botón deshabilitado. La discrepancia
quedó resuelta tras reconstruir sin causa exacta demostrada. La comprobación
API cloud final aprobó **59/59** el 27, de 14:37:40 a 14:38:42 Lima: cinco 401 y
sexto 429 desde conexiones independientes, Retry-After 893 s/no-store/ApiError;
login ADMIN posterior 200. Páginas, búsqueda, Auditoría, Jorge con 56 equipos y
proxy correctos; final 258 equipos, seis usuarios y 351 auditorías. El JSON
inicial fallido queda separado de la evidencia final. El PostgreSQL
privado quedó detenido, con limpieza bloqueada por revisión automática y residuo
fuera de Git. Un ataque contra un nombre puede
causar una espera temporal a su dueño; no hay bloqueo en BD y los contadores
son por proceso, reiniciables y sin coordinación entre réplicas.
La revisión automática bloqueó limpiar un fixture temporal privado de una BD
de prueba ya eliminada dentro de build/ ignorado. [Detalle](../despliegue/verificacion-paginacion-login-2026-10-27.md).

**Nuevo bloque autorizado el 27 de octubre:** el usuario pidió actualizar
Postman y añadir Swagger/OpenAPI. Se incorporó Springdoc 3.2.0, documentación
generada de las 70 operaciones y colección V13 sin credenciales. Regresión
backend 373/373 y frontend 93/93; scripts Postman simulados y Swagger probado
en Chrome contra backend/BD temporales, incluidos Authorize y GET autenticado.
No se cambiaron migraciones ni bases habituales durante esa implementación.
Por solicitud posterior, se publicó **d8f2123** en main y se comprobó Actions
40554578802 (5/5) y Auto-Deploy Live de Render. Swagger y JWT funcionan en el
dominio publicado; conteos 258 equipos/seis cuentas/351 auditorías conservados.
La [verificación cloud](../despliegue/verificacion-swagger-render-2026-10-27.md)
separa esa evidencia de las pruebas locales.
Detalle y evidencia: [documentación API](../backend-final/openapi.md).

Corte: **27 de octubre de 2026, America/Lima**. Este archivo conserva contexto
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

**Actualización posterior, 27 de octubre:** el usuario autorizó expresamente
copiar PostgreSQL Windows a `miproyecto-db` después de respaldar ambas bases y
confirmó prescindibles los datos remotos anteriores. Ambos respaldos se
restauraron en un PostgreSQL temporal antes del cambio; Render reprodujo las
trece tablas, con 258 equipos, seis cuentas, 351 auditorías y cuatro asignaciones.
Se preservaron credenciales, roles y alcance. Backend reanudado, salud UP,
tres logins correctos y proxy Vercel autenticado comprobados. Es una copia
puntual y no una sincronización. Fuente:
[informe de migración](../despliegue/verificacion-migracion-render-2026-10-27.md).
La tabla siguiente conserva los hitos anteriores con su fecha.

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
| Cierre web autorizado, 26 de octubre | El usuario descartó Configuración y autorizó quitar su ruta/menú, mejorar acceso Asignaciones, exportar cinco CSV agregados y consultar Auditoría solo ADMIN. Implementación local: 90 pruebas frontend y build de 1679 módulos correctos; 10 comprobaciones UI con API simulada, sin nuevas verificaciones de backend o nube. PPT aplazada. |
| Carga y comprobación real, 26–27 de octubre | 257 equipos Excel importados y 31 estados vacíos pendientes; 258 equipos totales al conservar uno previo en BAJA. Cinco actores acreditados en auditoría real; 337 altas de recursos de la carga. Recorrido real del 26: 8/8 comprobaciones con API/BD, tres CSV con datos y vista móvil. |
| Cierre de seguridad y GitHub, 27 de octubre | Usuario autorizó resolver el conflicto, verificar roles/reportes, reforzar contraseñas y publicar hasta GitHub considerando Actions/Docker. Política de 15 caracteres Unicode y máximo 72 bytes UTF-8 para altas/restablecimientos/demo ausentes; login previo intacto. Backend 365/365, frontend 93/93, importador 28/28 y JAR/build correctos localmente y repetidos en CI. Commit 58ac8f6 publicado; run 40454293249 aprobó cinco trabajos y publicó las dos imágenes, con cuatro etiquetas/digests comprobados en GHCR. Sin despliegue manual Render/Vercel. |
| Mantenimiento demo y Docker posterior, 27 de octubre | El usuario encendió Docker y pidió restablecer cuentas demo locales. Nueve operaciones comprobadas: seis nativas y tres Docker, con logins correctos, roles/asignaciones/inventario conservados y eventos reales. La política general 15/72 no cambió; helper temporal eliminado y código restaurado. Las imágenes exactas 58ac8f6 aprobaron 22 comprobaciones Docker aisladas, incluidas siete de UI; el stack principal anterior y su volumen quedaron intactos y saludables. Sin copiar datos ni intervenir cuentas cloud. |

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
- **Swagger, decisión posterior del 27 de octubre:** inicialmente se aplazó para continuar con Vercel. El usuario autorizó después actualizar Postman y añadir Swagger/OpenAPI; la implementación actual usa Springdoc 3.2.0 y conserva los permisos de negocio. Ver [guía y verificación](../backend-final/openapi.md). Esto no acredita un nuevo despliegue remoto.
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

**Decisión posterior del 26 de octubre:** el usuario autorizó una carga local del
Excel (L201 hasta fila 128, L409, L413 y L414), usuarios demo Jason ADMIN, Jorge
GESTOR y Gabriela ADMIN, y un laboratorio por Marko/Aldo/Jason/Jorge. Confirmó
Marko ADMIN y Aldo GESTOR, ambiente L209 de la fila 126 tratado como L201, códigos
normalizados y estados ambiguos vacíos. Las auditorías deben proceder de altas
reales con cada cuenta, sin modificar el historial. La carga local se aplicó:
257 equipos, 31 pendientes y participación de los cinco actores. El conflicto
de serie de L201, fila 57, se resolvió conservando referencia y registro previo.
Ver [guía](../importacion-inventario-demo.md) y
[cierre del 27 de octubre](../despliegue/verificacion-cierre-2026-10-27.md).

1. Leer [memory.md](../../memory.md), el anexo del componente y `git status`.
2. Identificar entorno: nativo, Compose o nube, y no transferir credenciales entre ellos por suposición.
3. El acceso ADMIN, permisos y flujos cloud quedaron acreditados el 27, con comprobación posterior 59/59. Al cambiar un despliegue, volver a comprobar con una cuenta autorizada, sin publicar credenciales ni usar la BD habitual como fixture.
4. Configuración está fuera del alcance y retirada. Auditoría ADMIN y CSV se incorporaron en el cierre del 26 de octubre; mantener separada esa evidencia de los sprints antiguos y del despliegue efectivo.
5. Actualizar fecha y evidencia de la memoria cuando cambie el estado.
