# Postman — API vigente V13

Actualización: **27 de octubre de 2026**. La colección cubre las **70
combinaciones método/ruta de los 19 Controllers** del backend. Contiene **75
solicitudes**: el login se presenta para tres roles y hay tres consultas
negativas adicionales sobre rutas ya contadas. Conserva el formato Postman
Collection **2.1** y los identificadores de los archivos anteriores.

| Archivo | Uso |
|---|---|
| [Colección](Inventario-Laboratorios.postman_collection.json) | Contratos, cuerpos, filtros, autenticación y checks de respuesta. |
| [Environment](Inventario-Laboratorios.postman_environment.json) | Plantilla de variables sin credenciales, tokens ni IDs reales. |
| [Validador estático](validar_coleccion.py) | Compara rutas y DTO Java; ejecuta el sandbox local de scripts. |
| [Sandbox de scripts](validar_scripts.cjs) | Simula Postman con Node VM; sin HTTP ni acceso a servicios. Lo invoca el validador. |

La colección histórica Sprint 7 cubría 42 operaciones. Esta versión añade los
28 contratos posteriores: cinco PATCH y cinco GET administrativos de catálogos,
siete operaciones de usuarios, cinco de mantenimiento, cinco de reportes y una
de auditoría. Asignaciones y equipos administrativos ya pertenecían al alcance
anterior; siguen incluidos. Salud Actuator y documentación OpenAPI son rutas
técnicas separadas de las setenta operaciones de negocio.

## Preparar la sesión manual

1. Importar la colección y el environment con **Import** en Postman y seleccionar
   `Inventario Laboratorios · Verificación manual V13`.
2. Configurar `baseUrl` con la raíz del backend o proxy elegido, sin barra final
   ni sufijo `/api`. El valor inicial es `http://localhost:8080`; ese puerto por
   sí solo no identifica una base de pruebas. Para escrituras, utilizar una
   instancia dedicada de verificación con Flyway V1–V13.
3. Completar localmente los pares `userNameAdmin/passwordAdmin`,
   `userNameGestor/passwordGestor` y `userNameLector/passwordLector` con cuentas
   de esa instancia. La plantilla los entrega vacíos, sin presuponer nombres
   o contraseñas de demostración.
4. Enviar individualmente los tres logins. Son públicos y usan **No Auth**.
   Solo una respuesta HTTP 200 con token, expiración, identidad, cuenta activa
   y rol válidos guarda `tokenAdmin/Gestor/Lector` y el ID del actor. Un test
   fallido no autoriza guardar un token por sí mismo.
5. Consultar perfil, alcance y listados. La colección hereda por defecto
   `Bearer {{tokenAdmin}}`; las consultas negativas de GESTOR/LECTOR y la
   consulta anónima contienen su autorización específica. Para comparar otras
   lecturas con otro rol, cambiar su Authorization a ese token después de sus
   logins; el backend conserva la autorización real sobre cada recurso.

Postman Vault es recomendable para custodiar contraseñas. Esta plantilla usa
variables secretas vacías para mantener compatibilidad con distintos clientes;
el tipo `secret` enmascara valores, pero **no convierte un environment exportado
en un archivo seguro**. Los tests conservan tokens en el environment local.
Antes de compartir o exportar, vaciar credenciales, tokens e IDs y verificar
el JSON. Los cuerpos de login/restablecimiento no se imprimen en scripts.

## Variables, cuerpos y filtros

- Todos los IDs iniciales están vacíos. Las altas 201 guardan únicamente su ID
  creado si el contrato público de la respuesta es válido. Las consultas GET no
  seleccionan destinatarios ni crean recursos automáticamente.
- `idUsuarioObjetivo` identifica el usuario que se consulta/edita/restablece.
  `idUsuarioAsignaciones` elige explícitamente el destinatario de pertenencias;
  ninguno se deduce automáticamente del actor autenticado.
- Los IDs opcionales de responsable vacíos se envían como `null`. Los IDs
  obligatorios deben ser enteros positivos de hasta 2147483647.
- Completar `sufijo` manualmente con 1–20 caracteres `a-z`, `0-9`, guion o guion
  bajo antes de usar los ejemplos de alta/edición de catálogos o equipo. Permite
  identificar recursos de verificación sin reutilizar nombres reservados.
- Los textos de variables se serializan como JSON para conservar comillas,
  saltos de línea y caracteres Unicode. Las contraseñas no se recortan.
- Altas/restablecimientos de contraseña requieren **al menos 15 caracteres
  Unicode y como máximo 72 bytes UTF-8**, sin claves compuestas solo por blancos.
  Login de cuentas existentes conserva su contrato; no se fuerza otro mínimo.
- Cada body identifica su DTO de entrada y contiene solamente sus campos.
  PUT general de usuario excluye username/rol/actividad/password; PUT Equipo
  excluye código interno/laboratorio/fechas; PUT Mantenimiento excluye idEquipo
  y los campos del ciclo. Actor y origen de traslado los decide el servidor.
- Los filtros opcionales aparecen **deshabilitados** en la pestaña Params.
  Completar su variable y habilitar solo los necesarios. Los listados no tienen
  `page/size`; las respuestas válidas pueden ser `[]` y los reportes pueden
  contener totales cero.
- Fechas de filtro usan `YYYY-MM-DD`, con `fechaHasta >= fechaDesde`.
  Reportes de Equipo/Movimiento y Auditoría interpretan timestamps por días UTC;
  mantenimiento utiliza fecha programada de calendario. Los ocho filtros de
  Reporte y los seis de Mantenimiento/Auditoría corresponden a sus DTO actuales.

## Escrituras y orden del recorrido

`habilitarEscrituras` empieza en **`false`**. El pre-request bloquea POST/PUT/
PATCH/DELETE de recursos hasta que se active manualmente `true`; los logins no
están bloqueados. Importar o enviar un GET no desencadena otras solicitudes.
Los scripts no usan `pm.sendRequest`, `fetch` ni llamadas auxiliares a la API.

La organización visual es una referencia por módulos, **no un Runner listo
para ejecutar de principio a fin**. Los DELETE están junto al CRUD y las
transiciones cambian precondiciones. En una base dedicada, seleccionar el
recorrido manual:

1. Login → perfil/alcance → lecturas y consultas negativas 401/403.
2. Categoría → Subcategoría; Sede → Área → dos Laboratorios de verificación.
   Anotar explícitamente origen y `idLaboratorioDestino`.
3. Crear un usuario de verificación con username/email/contraseña locales;
   probar su edición y rutas específicas. Conservar un ADMIN activo.
4. Elegir `idUsuarioAsignaciones` y enviar el conjunto completo deseado de
   laboratorios. PUT **reemplaza todo el conjunto**; `[]` lo vacía.
5. Crear/editar Equipo. Programar y editar un mantenimiento mientras está
   PROGRAMADO; iniciar y completar/cancelar antes de trasladar o dar de baja
   el equipo. Solo un mantenimiento EN_PROCESO por equipo.
6. Trasladar → consultar historial → Reportes/Auditoría. GESTOR necesita acceso
   a origen **y** destino para mover; la historia consulta origen **o** destino.
7. Dar de baja el equipo al final. La baja es lógica e irreversible desde la
   API; después vaciar asignaciones de verificación y desactivar hijos antes
   de padres cuando corresponda. No hay DELETE de Usuario/Mantenimiento ni
   escritura libre de Movimiento/Auditoría.

Los checks de éxito verifican status exacto, JSON/contrato público y ausencia
de campos sensibles; las altas comprueban Location y los 204 cuerpo vacío.
Los 401/403 negativos comprueban ApiError. Los códigos generales mencionados
en descripciones dependen de cada operación: no se prometen todos en todas
las rutas. Un 409 puede ser la respuesta correcta ante un duplicado o una
precondición inválida; revisar el recurso antes de repetir una escritura.

## Validación local sin API ni base de datos

Desde la raíz Git, con Python 3 y Node.js instalados:

```powershell
python -B .\docs\backend-final\postman\validar_coleccion.py
```

El validador solo lee JSON/fuentes y ejecuta mocks de Postman en un sandbox.
Compara las setenta rutas, cuerpos con los campos exactos de sus DTO, filtros,
variables declaradas, autorización y scripts; su entorno de ejecución no
expone funciones de red. No ejecuta Postman/Newman, Gradle, npm ni SQL.

**Resultado del 27 de octubre:** 19 Controllers, **70/70 operaciones**, 75
solicitudes, 30 cuerpos contrastados con DTO y siete listados con DTO de filtros.
Se compilaron **150 scripts** y se simularon las 75 solicitudes, con **33
escrituras bloqueadas por defecto**, **24 respuestas de login inválidas sin
guardar token/ID** y **ocho casos de contraseña Unicode**. Las contraseñas de
esas simulaciones son datos sintéticos generados en memoria.

Esta comprobación acredita estructura y comportamiento local de scripts.
La ejecución HTTP manual de esta colección en una instancia dedicada queda
separada de los resultados previos de JUnit, frontend, Docker y migración cloud.
No se probó contra la base habitual ni contra Render para generar estos archivos.

Fuentes: [Controllers](../../../backend/inventario/src/main/java/com/utec/inventario/controller/),
[DTO de entrada](../../../backend/inventario/src/main/java/com/utec/inventario/dto/request/),
[contratos HTTP](../endpoints.md), [permisos](../../matriz-permisos.md) y
[memoria backend](../../memory/backend-database.md).
