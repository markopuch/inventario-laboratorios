# Frontend — Inventario de Laboratorios

El frontend integrado y publicado es la versión **Jason**, ubicada en
[`version-jason/frontend/`](version-jason/frontend/README.md).

**Estado publicado al 27 de octubre de 2026:** Equipos y Auditoría usan páginas
del servidor de 20/50/100 registros; Equipos busca con `q` antes de contar y
paginar. Login muestra la espera de `429/Retry-After` y deshabilita el envío.
El límite agrega cinco fallos por nombre normalizado en 900 s, independiente
de la conexión. La API conserva arrays cuando no se solicita `page/size`.
La revisión `ef31d65` aprobó CI con 410 pruebas backend, 107 frontend y 28 del
importador. Render/Vercel y el recorrido público aprobaron 59 comprobaciones
API y la verificación visual descrita en el
[informe cloud](../docs/despliegue/verificacion-paginacion-login-cloud-2026-10-27.md).
Los resultados locales e históricos que siguen conservan su fecha y alcance;
la auditoría del 26 no sustituye esa comprobación posterior.

Desde el **26 de octubre de 2026**, la variante de tutoría
`frontend/version-marko/` y sus registros `docs/sprints_realizados-frontend-Marko/`
se conservan únicamente en el equipo local, excluidos del seguimiento de Git y
ausentes en nuevos clones. No son necesarios para ejecutar, probar o desplegar
Jason. Sus resultados históricos no certifican esta aplicación; el material
anterior sigue disponible en el historial de Git.

## Ejecución y estado comprobado

- Docker completo: frontend Nginx en `http://localhost:3000`, backend en 8080 y
  PostgreSQL en la red interna. El único Compose está en `backend/inventario/`.
- Desarrollo: Jason mediante Vite en `http://localhost:5173`, con proxy `/api` al
  backend. La guía de Jason describe comandos y configuración sin secretos.
- Jason está adaptado al backend V13: usuarios administrativos, reactivación de
  catálogos, estado operativo de Laboratorio, Mantenimientos y Reportes reales.
- La [adaptación del 3 de octubre](version-jason/frontend/evidencias/adaptacion-backend-2026-10-03.json)
  aprobó **67/67 pruebas frontend** y build. Conserva las 27 pruebas iniciales y
  añade 40; no se volvió a ejecutar esa suite al actualizar Markdown.
- En la comprobación del **3 de octubre**, Docker local ejecutó las imágenes del
  commit `e1ce75a`, con sus tres contenedores saludables. Esa [verificación histórica](version-jason/frontend/evidencias/docker-actual-2026-10-03.json)
  aprobó **94/94 comprobaciones HTTP**, 30 aserciones funcionales y siete
  controles SQL sin inconsistencias. No son tests JUnit ni endpoints distintos.
- GitHub Actions: backend y frontend Jason en verde en la
  [ejecución 37158784428](https://github.com/markopuch/inventario-laboratorios/actions/runs/37158784428),
  commit `e1ce75a`. Jason aprobó 67 pruebas antes de publicar; las imágenes
  `sha-e1ce75a` se descargaron y ejecutaron localmente.

Flyway aplicó V10–V13 conservando los checksums V1–V9, el contenedor/volumen de
PostgreSQL y los registros anteriores: tres usuarios y cuatro asignaciones.
Las escrituras del recorrido se hicieron únicamente en una base de verificación,
eliminada después con sus contenedores, red y volumen. La baja se verificó por API
debido al bloqueo de su confirmación nativa en la automatización del navegador.
La interfaz mostró BAJA sin acciones y conservó el historial visible al LECTOR.

Los resultados iniciales de 27 pruebas/42 HTTP, Docker 33 HTTP/24 aserciones y
Actions `9956939` se conservan como evidencia histórica; no sustituyen estas cifras.

Publicar imágenes en GHCR no despliega la aplicación en la nube ni actualiza
automáticamente los contenedores locales. Posteriormente se documentó la
publicación de Jason en Vercel y del backend/base de datos en Render. La
[auditoría del 26 de octubre](../docs/memory/verification-2026-10-26.md)
registró Docker inaccesible y no acreditó el flujo autenticado en nube; los
resultados del 3 de octubre no certifican el estado operativo actual.

## Documentación

- [Guía de ejecución Jason](version-jason/frontend/README.md).
- [Reportes CSV y Auditoría ADMIN](../docs/frontend/guia-reportes-auditoria.md).
- [Roles, acciones y alcance por laboratorio](../docs/matriz-permisos.md).
- [Plan de cierre de la web y documentación](../docs/plan-cierre-web.md).
- [Sprints Jason y alcance funcional](../docs/sprints_realizados-frontend-Jason/README.md).
- [Auditoría de integración y diferencias con mockups](../docs/sprints_realizados-frontend-Jason/auditoria-integracion.md).
- [Evidencia Docker, preservación de la base y Actions](../docs/despliegue/verificacion-docker-actions-2026-10-03.md).

El cierre del **26 de octubre** incorpora CSV de agregados en Reportes y una
pantalla de Auditoría de solo lectura para ADMIN. Asignaciones tiene acceso
directo en el menú ADMIN. Configuración se retira del menú y de las rutas;
las preferencias persistidas siguen como ampliación futura. Verificación local
del cierre: **90/90 tests**, build aprobado y diez comprobaciones de navegador
con API simulada, sin errores de página. Posteriormente se aprobaron **8/8
comprobaciones con API/BD locales reales**, roles y auditoría de cinco actores;
tres CSV con datos contrastados con la API y los otros dos bloques vacíos con
descarga deshabilitada. El 27 de octubre, tras reforzar contraseñas, Jason aprobó
**93/93 tests** y build de **1679 módulos**. El
[anexo del cierre](../docs/despliegue/verificacion-cierre-2026-10-27.md) separa
estos resultados de JUnit, Actions y despliegue cloud. Las cifras anteriores
mantienen su fecha histórica.
Esta documentación no certifica fidelidad visual 1:1 o
accesibilidad exhaustiva. No se exponen contraseñas, JWT ni secretos en esta guía.
