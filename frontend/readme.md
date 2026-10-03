# Frontends — Inventario de Laboratorios

El frontend integrado y publicado es la versión **Jason**, ubicada en
[`version-jason/frontend/`](version-jason/frontend/README.md). La versión Marko se
conserva por separado como antecedente; sus resultados no certifican Jason.

## Ejecución y estado comprobado

- Docker completo: frontend Nginx en `http://localhost:3000`, backend en 8080 y
  PostgreSQL en la red interna. El único Compose está en `backend/inventario/`.
- Desarrollo: Jason mediante Vite en `http://localhost:5173`, con proxy `/api` al
  backend. La guía de Jason describe comandos y configuración sin secretos.
- Auditoría inicial: 27 pruebas automatizadas, build y 42 comprobaciones HTTP por
  proxy, con entorno de escritura temporal eliminado.
- Verificación Docker posterior del 3 de octubre de 2026: flujo completo con tres
  roles, 33/33 comprobaciones HTTP adicionales y 24/24 aserciones de flujo y
  consistencia. No son pruebas adicionales de la suite JUnit del backend.
- GitHub Actions: backend y frontend Jason en verde en la
  [ejecución 37136137900](https://github.com/markopuch/inventario-laboratorios/actions/runs/37136137900),
  commit `9956939`. Jason aprobó 27/27 pruebas antes de publicar; ambas imágenes
  fueron confirmadas en GHCR con `latest` y `sha-9956939`.

Publicar imágenes en GHCR no despliega la aplicación en la nube ni actualiza
automáticamente los contenedores locales. Ese despliegue sigue pendiente.

## Documentación

- [Guía de ejecución Jason](version-jason/frontend/README.md).
- [Sprints Jason y alcance funcional](../docs/sprints_realizados-frontend-Jason/README.md).
- [Auditoría de integración y diferencias con mockups](../docs/sprints_realizados-frontend-Jason/auditoria-integracion.md).
- [Evidencia Docker, preservación de la base y Actions](../docs/despliegue/verificacion-docker-actions-2026-10-03.md).

Mantenimientos, Reportes y Configuración (FE-08–FE-10) siguen como Próximamente.
La publicación no implementa esos módulos ni certifica fidelidad visual 1:1 o
accesibilidad exhaustiva. No se exponen contraseñas, JWT ni secretos en esta guía.
