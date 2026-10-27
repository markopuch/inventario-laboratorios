# Verificación local: paginación y límite de login

**27 de octubre de 2026, America/Lima.** Implementación local posterior a
Swagger d8f2123; no se hizo commit/push ni despliegue de este bloque.
Contratos y variables: [guía](../backend-final/paginacion-login.md).
[Resumen verificable](evidencias/paginacion-login-2026-10-27.json).

## Cambios comprobados

- Tres GET existentes: Equipos, alias ADMIN y Auditoría. page o size activa un
  objeto de siete campos; sin ambos se conserva el array anterior.
- Páginas base cero, tamaño predeterminado 20, máximo100, offset limitado a
  INTEGER; parámetros inválidos400 y páginas fuera de rango vacías.
- COUNT y contenido usan los mismos filtros/alcance. q busca texto literal
  IgnoreCase antes de limitar; orden estable por ID de equipo y fecha/id de evento.
- Interfaz con tamaños20/50/100, navegación, totales, búsqueda de servidor,
  reinicio de página y descarte/cancelación de solicitudes obsoletas.
- Login: cinco fallos por usuario/origen en900s y60 solicitudes/origen/60s.
  Reservas concurrentes previas a BCrypt, contadores acotados, expiración y
  reinicio de fallos al éxito. No se escriben bloqueos en cuentas.
- 429 con ApiError/Retry-After, cuota antes de parsear JSON/JWT y espera en
  interfaz, sin reintentos automáticos.

## Pruebas y límites

| Comprobación | Resultado |
|---|---|
| Backend mediante verificar-backend.ps1 | **406/406**, cero fallos, errores u omitidas; JAR generado. |
| Frontend Node | **107/107**, cero fallos. |
| Build Vite | Correcto, **1681 módulos**. |
| Postman/DTO | 19 Controllers, 70 operaciones, 75 solicitudes, 30 cuerpos y siete filtros DTO. |
| Scripts Postman sin HTTP | 150 scripts, 75 simulaciones; 23 páginas válidas y 33 inválidas rechazadas, tres429 válidos y21 contratos429 inválidos rechazados. |

La primera compilación de tests detectó que `delete(any())` era ambiguo al
añadir JpaSpecificationExecutor. Se tipó el matcher como EquipoEntity y se
repitió la regresión completa; no se cambió una regla de negocio para aprobarla.

Las pruebas usan PostgreSQL18 temporal con locale builtin C.UTF-8, credenciales
aleatorias y nombres inventario_verificacion_*. El test de expiración de login
inyecta un reloj monotónico controlado; no espera15min ni cambia cuentas de BD.
Se comprobaron límites concurrentes, capacidad/expiración, ausencia de enumeración,
normalización del username, headers de IP falsificados y rutas ajenas al login.
Las 17 pruebas nuevas de paginación cubren ADMIN/GESTOR/LECTOR/sin alcance,
COUNT filtrado, búsquedas fuera de primera página, comodines literales,
orden/timestamps UTC, página vacía y parámetros inválidos.

## Recorrido real en Chrome

Se ejecutó el JAR nuevo de solo lectura en backend/BD/red Docker temporales y
Vite en127.0.0.1:5175, con53 equipos creados por API y sus auditorías reales.
No se usó el inventario habitual como fixture.

1. Equipos: 1–20/53 →21–40/53; cambio a50 reinicia en1–50/53; siguiente51–53/53.
2. Búsqueda de un equipo situado fuera de la primera página: devuelve1/1 y
   reinicia página; limpiar recupera53 resultados.
3. Auditoría: páginas de20 y tamaño100 muestran los53 eventos esperados.
4. Vista390px sin desbordamiento horizontal; controles disponibles.
5. Cinco logins incorrectos devolvieron401; el sexto429, Retry-After positivo,
   contador visible y submit deshabilitado. No hubo reintentos automáticos.

**Cero errores de página.** Solo para esa prueba visual se configuró una ventana
de fallos de60s; el valor normal del código sigue en900s. La expiración/límites
se acreditan además por pruebas de reloj e integración independientes.

[Resultado del navegador](../frontend/evidencias/paginacion-login-2026-10-27.json),
[Equipos](../frontend/evidencias/paginacion-equipos-2026-10-27.png),
[Auditoría](../frontend/evidencias/paginacion-auditoria-2026-10-27.png),
[móvil](../frontend/evidencias/paginacion-movil-2026-10-27.png) y
[login429](../frontend/evidencias/login-limite-2026-10-27.png).

## Limpieza y estado de entrega

PostgreSQL de JUnit y los contenedores/redes de UI se eliminaron; Vite se detuvo.
Los tres contenedores habituales permanecieron saludables, sin alterar sus
imágenes, volúmenes o datos. No se modificó PostgreSQL Windows ni Render.

Un primer helper de preparación visual generó el archivo privado
`backend/inventario/build/paginacion-ui.private.json` para una instancia temporal
que ya fue eliminada. La revisión automática rechazó su limpieza, también al
intentar únicamente borrar ese archivo, con motivo `blocked by policy` sin más
detalle. No se intentó eliminarlo por otro mecanismo. El archivo está ignorado
por Git, no contiene claves de las bases habituales y no se publica su contenido.
Ese es el único residuo de acceso de este bloque; los servicios de prueba ya no
existen. La comprobación visual final pasó credenciales solo por entorno/memoria.

La API publicada en Render sigue siendo d8f2123; este bloque está preparado
localmente. Al publicar, coordinar backend y frontend: clientes antiguos siguen
aceptando arrays, pero el cliente nuevo necesita un backend con page/size.
Los contadores de login son por proceso; detrás de un proxy pueden compartir
origen. No se acredita coordinación entre réplicas ni defensa de borde.
