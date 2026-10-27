# Publicación y verificación de Swagger en Render

**Fecha:** 27 de octubre de 2026, America/Lima. El usuario autorizó publicar
los cambios en GitHub, comprobar el despliegue automático y verificar Swagger
en el backend de Render.

## Versión y despliegue

- Repositorio: [markopuch/inventario-laboratorios](https://github.com/markopuch/inventario-laboratorios).
- Rama: `main`.
- Commit de aplicación: [d8f212321107afedcb3b703b3bf0751d9c641860](https://github.com/markopuch/inventario-laboratorios/commit/d8f212321107afedcb3b703b3bf0751d9c641860).
- Servicio Render: `inventario-laboratorios`, `srv-db60s7id0e5s73900jr0`.
- [Despliegue](https://dashboard.render.com/web/srv-db60s7id0e5s73900jr0/deploys/dep-dbge9f15efls73f5u2c0):
  `dep-dbge9f15efls73f5u2c0`, **Auto-Deploy**, **Deploy succeeded | Live**.
- Duración mostrada por Render: **4m56s**; inicio 12:51:56 Lima.
- Logs: Java 21.0.12.1, Spring Boot 4.1.1, perfil `dev`; arranque Spring en
  **175.701 segundos**. Tomcat inició en 8080 a las 12:56:43 Lima.
- Flyway validó **13 migraciones**, esquema versión **13**, sin migración nueva.

Render construyó con su Dockerfile existente. El disparador On Commit inicia su
despliegue de forma independiente de Actions; no se cambió esa configuración.
Publicar en GHCR y desplegar en Render son comprobaciones distintas.

## GitHub Actions y registro

[Run 40554578802](https://github.com/markopuch/inventario-laboratorios/actions/runs/40554578802),
SHA exacto d8f2123: **completed / success, cinco de cinco trabajos aprobados**.
Terminó a las **12:56:25 Lima**.

| Trabajo | Resultado acreditado por logs |
|---|---|
| Backend | 373 pruebas; cero fallos, errores u omitidas; compilación y JAR correctos. |
| Postman, dentro de backend | 19 Controllers, 70 operaciones, 75 solicitudes, 30 cuerpos y siete filtros DTO; 150 scripts y 75 simulaciones sin HTTP. |
| Frontend | 93/93 pruebas; build correcto de 1679 módulos. |
| Importador | 28 pruebas, OK. |
| Publicaciones backend/frontend | Ambas aprobadas en GHCR. |

Las etiquetas `sha-d8f2123` y `latest` se consultaron independientemente mediante
`docker buildx imagetools inspect`; coinciden por componente:

| Imagen | Digest del índice OCI |
|---|---|
| `ghcr.io/markopuch/inventario-laboratorios-backend` | `sha256:6aa03d099c8b7d7158c50e0038070bd0259cb137ad922e6b0ec0312e4a79b350` |
| `ghcr.io/markopuch/inventario-laboratorios-frontend-jason` | `sha256:2e8910b7b276d691f5d270a7d900af9d1133685e152220da901b8d431f6066b6` |

Ambos índices contienen `linux/amd64` y atestación. No se sustituyeron las
imágenes ni la base del stack Docker principal.

## Comprobaciones públicas y autenticadas

Backend: <https://inventario-laboratorios-tyni.onrender.com>.
Primera especificación V13 acreditada a las **12:57:29 Lima**; consultas HTTP
posteriores finalizadas a las **12:59:14 Lima**.

| Comprobación | Resultado |
|---|---|
| `/actuator/health` | HTTP 200, `UP`. |
| `/swagger-ui.html` y `/swagger-ui/index.html` | HTTP 200, interfaz cargada. |
| JavaScript de Swagger y `/v3/api-docs/swagger-config` | HTTP 200. |
| `/v3/api-docs` | HTTP 200, OpenAPI 3.1.0, 70 operaciones y esquema JWT Bearer. |
| `/v3/api-docs.yaml` | HTTP 200, `application/vnd.oai.openapi`, usando Accept compatible. |
| Login documentado | `security: []`; las operaciones protegidas conservan Bearer. |
| `/api/auth/me` y `/api/admin/auditoria`, anónimos | HTTP 401. |
| Jason ADMIN, Jorge GESTOR y Gabriela ADMIN | Tres logins HTTP 200; perfil y alcance correctos. |
| Auditoría con GESTOR | HTTP 403. |
| Equipos / usuarios / auditorías de ADMIN | 258 / 6 / 351. |
| Consulta autenticada de equipos mediante Vercel | HTTP 200, 258 equipos. |

Para YAML, un cliente que envía exclusivamente `Accept: application/json`
puede recibir 406 por negociación de contenido. La comprobación aprobada usa
`Accept: */*`; la URL JSON sigue siendo `/v3/api-docs`.

## Swagger en navegador

Se probó directamente la interfaz de Render:

1. Cargó las **70 operaciones** y los schemas.
2. `POST /api/auth/login` desde Try it out devolvió **200** con una cuenta ADMIN.
3. Se aplicó su JWT mediante **Authorize**.
4. `GET /api/auth/me` desde Swagger devolvió **200**, con el perfil esperado.
5. Se recargó la página: **69 operaciones protegidas sin autorización**, cero
   candados autorizados. El token no persistió; las 70 operaciones siguieron visibles.

Las credenciales y JWT solo se utilizaron para la comprobación. No se guardaron
en esta evidencia, capturas ni repositorio. La captura final se hizo después
de recargar y limpiar la autorización y los formularios de prueba.

## Evidencia y límites

- [Resultados HTTP y navegador](evidencias/swagger-render-2026-10-27.json).
- [Swagger sin sesión de prueba](evidencias/swagger-render-2026-10-27.png).
- [Render Live y commit](evidencias/render-swagger-live-2026-10-27.png).
- [Verificación local previa](../backend-final/openapi.md).

Se hicieron logins y lecturas, sin escrituras de negocio, cambios de contraseña,
migraciones nuevas ni copia adicional de datos. Se conservaron PostgreSQL Windows,
Docker principal y los conteos de Render. Esta verificación no ejecuta las
70 operaciones de escritura en producción ni sustituye un E2E integral.
El plan Free conserva su posible demora después de inactividad.
