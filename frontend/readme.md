# Frontends — Inventario de Laboratorios

El frontend integrado y publicado es la versión **Jason**, ubicada en
[`version-jason/frontend/`](version-jason/frontend/README.md). La versión Marko se
conserva por separado como antecedente; sus resultados no certifican Jason.

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
- Docker local ejecuta las imágenes publicadas del commit `e1ce75a`, con sus tres
  contenedores saludables. La [última verificación](version-jason/frontend/evidencias/docker-actual-2026-10-03.json)
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
automáticamente los contenedores locales. Ese despliegue sigue pendiente.

## Documentación

- [Guía de ejecución Jason](version-jason/frontend/README.md).
- [Sprints Jason y alcance funcional](../docs/sprints_realizados-frontend-Jason/README.md).
- [Auditoría de integración y diferencias con mockups](../docs/sprints_realizados-frontend-Jason/auditoria-integracion.md).
- [Evidencia Docker, preservación de la base y Actions](../docs/despliegue/verificacion-docker-actions-2026-10-03.md).

Mantenimientos (FE-08) y Reportes (FE-09) están integrados. Configuración (FE-10),
pantalla de Auditoría y exportación de reportes permanecen fuera del alcance
funcional actual. Esta validación no certifica fidelidad visual 1:1 o
accesibilidad exhaustiva. No se exponen contraseñas, JWT ni secretos en esta guía.
