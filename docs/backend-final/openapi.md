# Swagger/OpenAPI y Postman — contratos V13

Actualización: **27 de octubre de 2026**. La documentación se genera a partir de
los Controllers y DTO vigentes: **70 combinaciones de método y ruta**, sin contar
Actuator ni las rutas de documentación. No se añaden tablas ni migraciones.

## Abrir Swagger

Con el backend actualizado en ejecución:

- Interfaz: <http://localhost:8080/swagger-ui.html>.
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>.
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>.

Estas direcciones corresponden al backend. El frontend Vercel y el proxy de
desarrollo reenvían `/api`; no sirven las rutas `/swagger-ui` y `/v3/api-docs`.
En Render se utiliza el dominio del **backend**, una vez desplegado este cambio,
con los mismos sufijos. Una comprobación local no acredita ese despliegue.

La especificación agrupa autenticación, catálogos, organización, usuarios y
asignaciones, equipos, movimientos, mantenimientos, reportes y auditoría.
Incluye cuerpos, filtros, enums, respuestas, errores y reglas de rol/alcance.
El [catálogo de endpoints](endpoints.md) amplía las reglas y los ejemplos.

## Autenticarse y probar

1. Abrir **Autenticación → POST /api/auth/login → Try it out**.
2. Introducir una cuenta existente y su contraseña. La documentación no incluye
   cuentas ni claves reales. Pulsar **Execute** y copiar `accessToken`.
3. Abrir **Authorize**, pegar solamente el token y autorizar. Swagger añade
   `Authorization: Bearer …`; el login permanece público y no lo necesita.
4. Comprobar primero `GET /api/auth/me` y `GET /api/auth/me/laboratorios`.
5. Seleccionar una operación y revisar su rol, alcance, parámetros y cuerpo.

**Execute envía una solicitud real a la base del backend seleccionado.** Las
operaciones POST/PUT/PATCH/DELETE modifican datos. Para ensayar todo el recorrido,
usar una instancia de verificación propia, no la base de la demostración.
Swagger no conserva la autorización entre recargas ni envía el contrato a un
validador externo. Cerrar la pestaña o quitar la autorización al terminar.

ADMIN tiene alcance global. GESTOR y LECTOR requieren asignaciones activas a
laboratorios activos; ocultar un botón no sustituye la autorización del servidor.
Los listados son arreglos JSON completos: esta tarea no incorpora paginación.
La baja de Equipo es lógica; el traslado tiene su propia operación. Los reportes
son agregados; los CSV los genera el frontend y no son endpoints adicionales.

## Importar Postman

Importar la [colección](postman/Inventario-Laboratorios.postman_collection.json)
y el [environment](postman/Inventario-Laboratorios.postman_environment.json).
La [guía de Postman](postman/README.md) explica variables, selección de rol,
solicitudes, ejecución y controles de escritura. Los exports versionados no
contienen contraseñas, tokens ni IDs de la base habitual. No exportar valores
privados después de completar el environment local.

La colección documenta las mismas 70 operaciones de aplicación. Variantes de
login por rol y ejemplos de filtro no representan rutas nuevas. OpenAPI también
puede importarse en Postman por URL o archivo; la colección mantenida añade los
scripts y variables del flujo del proyecto.

## Configuración

Se usa `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`. La
[matriz oficial de compatibilidad](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)
asocia Spring Boot 4.1.x con Springdoc 3.1.x–3.2.x. No se cambia Spring Boot 4.1.1.

`API_DOCS_ENABLED` habilita la especificación y la interfaz; por defecto es `true`.
Para deshabilitarlas, configurar `API_DOCS_ENABLED=false` y reiniciar el backend.
La configuración se encuentra en `application.properties`, `OpenApiConfig` y
`SecurityConfig`. Solo las rutas GET concretas de documentación son públicas;
las rutas de negocio conservan JWT, rol, alcance y denegación por defecto.

Las contraseñas de altas y restablecimientos se documentan como campos de solo
escritura, mínimo 15 caracteres Unicode y máximo 72 bytes UTF-8. Ese límite en
bytes requiere validación del servidor; `maxLength` de OpenAPI por sí solo no lo
expresa. Login conserva la compatibilidad con contraseñas existentes.
No se implementan refresh token, revocación o recuperación de contraseña.

## Comprobación reproducible

`OpenApiIntegrationTests` arranca el backend con puerto aleatorio y contrasta
la especificación HTTP con los mappings MVC reales y con la colección Postman.
Comprueba seguridad JWT/login, DTO públicos, contraseñas de solo escritura,
201/204, errores, filtros, Swagger UI y denegación de rutas protegidas.
`OpenApiDisabledIntegrationTests` comprueba que se pueda deshabilitar la
documentación manteniendo la protección de la API.

Ambas clases requieren `DB_URL` de verificación; no se ejecutan sobre la base
habitual. La regresión completa se ejecuta mediante `verificar-backend.ps1`
con PostgreSQL temporal y credenciales aleatorias. El resultado y el alcance
efectivamente comprobados se registran en la memoria técnica tras la ejecución.

## Resultado del 27 de octubre

- **373/373 pruebas backend**, incluidas las ocho nuevas; cero fallos, errores
  u omitidas. JAR generado. **93/93 pruebas frontend** aprobadas.
- Postman: **70/70 operaciones**, 75 solicitudes, 30 cuerpos y siete filtros
  DTO; 150 scripts compilados y 75 simulaciones sin HTTP. Se comprobaron
  guardias de escritura, rechazo de login inválido y ocho casos Unicode.
- Chrome headless: Swagger mostró las **70 operaciones**, abrió Authorize,
  envió un JWT de verificación al consultar el perfil (**HTTP 200**) y perdió
  la autorización al recargar. **Cero errores de página**.
- Los tests usaron PostgreSQL temporal. La prueba visual utilizó el JAR nuevo
  montado de solo lectura en un backend Docker temporal con red/BD propias.
  Los contenedores y redes de verificación se eliminaron y sus puertos quedaron
  libres. Los tres servicios habituales siguieron saludables.

[Captura de Swagger](evidencias/swagger-ui-2026-10-27.png),
[resultado del navegador](evidencias/swagger-ui-2026-10-27.json) y
[evidencia de contratos y pruebas](evidencias/openapi-postman-2026-10-27.json).
Esta comprobación no ejecutó todas las escrituras de Postman contra una API
real, ni modificó Render, PostgreSQL Windows o la base Docker habitual.
Los cambios de código, colección y CI siguen locales, sin publicación remota
durante este bloque.
