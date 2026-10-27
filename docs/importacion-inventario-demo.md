# Carga local del inventario de demostración

Actualización: **27 de octubre de 2026, America/Lima**. Estado: **carga local
aplicada y comprobada mediante la API real**. Se crearon las tres cuentas demo y
se importaron los 257 equipos con estado definido; 31 registros siguen pendientes.
La base conserva además un equipo previo en BAJA: **258 equipos totales**.
No acredita cambios en Render/Vercel ni en la base de Docker.

## Selección autorizada

El archivo de origen es `INVENTARIO Equipos grandes con código.xlsx`, proporcionado
por el usuario desde su carpeta Downloads. El archivo original se lee y se conserva.

| Hoja | Filas seleccionadas | Candidatos | Estado definido | Pendientes |
|---|---|---:|---:|---:|
| L201 | 2–128, ambas incluidas | 127 | 100 | 27 |
| L409 | Todas las filas con equipos, 2–61 | 60 | 56 | 4 |
| L413 | Todas las filas con equipos, 2–62 | 61 | 61 | 0 |
| L414 | Todas las filas con equipos, 2–41 | 40 | 40 | 0 |
| **Total** | | **288** | **257** | **31** |

La fila 1 es encabezado. L409 contiene otras 16 filas con solo «Operativo» en
la columna Estado; no representan equipos. Las hojas restantes quedan excluidas.
La fila 126 de L201 se incorpora como L201 por indicación expresa del usuario,
conservando «L209» como ambiente original en el comentario de procedencia.

## Usuarios, alcance y auditoría

Los nombres, apellidos y correos nuevos corresponden a cuentas de demostración.
Las contraseñas nuevas se generaron al aplicar y permanecen en un archivo privado
fuera de Git. Las contraseñas existentes de Marko y Aldo no se cambiaron. La
política posterior del 27 de octubre exige al menos 15 caracteres Unicode y
máximo 72 bytes UTF-8 para altas y restablecimientos; no obliga a renovar claves
anteriores ni modifica el login de esas cuentas.

| Cuenta | Rol | Laboratorio de la carga |
|---|---|---|
| marko | ADMIN existente | L414 |
| aldo | GESTOR existente | L413 |
| jason | ADMIN nuevo, Jason Demo, `jason@inventario.test` | L201 |
| jorge | GESTOR nuevo, Jorge Demo, `jorge@inventario.test` | L409 |
| gabriela | ADMIN nuevo, Gabriela Demo, `gabriela@inventario.test` | Sin asignación nueva |

El reparto se obtiene con una permutación seudoaleatoria cuya semilla procede
del SHA-256 del Excel. Es reproducible y no cambia al reanudar el mismo archivo.
Para las cuatro primeras cuentas, la carga establece exactamente el laboratorio
indicado; conserva una instantánea previa de sus asignaciones. No modifica
asignaciones de otros usuarios. Los ADMIN mantienen alcance global por su rol.

Las altas se ejecutan por la API autenticada, con las cuentas autorizadas. Las
altas de usuarios las realiza Marko; las de categorías/subcategorías pueden
realizarse con Gabriela; los equipos se distribuyen según el laboratorio de cada
actor. Si los catálogos ya existen, Gabriela puede crear uno de los nuevos equipos
como ADMIN. El ejecutor comprueba eventos `CREAR` relacionados con recursos de
esta carga para los cinco usuarios. Si ya estaba todo cargado y falta la
participación de alguno, informa la limitación sin inventar actividad adicional.

No inserta ni cambia directamente filas de auditoría, no fabrica fechas anteriores
y no crea traslados o mantenimientos para simular actividad. `idResponsable` es
custodia del equipo y no determina la autoría del evento; se mantiene vacío.

## Tratamiento de datos

- Código interno: prefijo del laboratorio y normalización de espacios alrededor
  de guiones. Si un código se repite dentro del laboratorio, se añade un sufijo
  determinista de fila `-F####` a cada repetición. Los dos faltantes de L201 quedan
  como `L201-IMP-F0125` y `L201-IMP-F0126`. El original se conserva en el comentario.
- Series: conservar como texto, incluidos ceros iniciales almacenados como texto
  y series compuestas. Quitar caracteres invisibles de formato. Guiones y valores
  ausentes se representan como `null`.
- Hay tres series de fabricante repetidas en seis filas. En esas filas el campo
  normalizado queda vacío y el valor original se conserva en el comentario,
  pendiente de verificar. No se inventan nuevos números de serie.
- Estados claros: Operativo → `OPERATIVO`; Inoperativo, No Operativo, Averiado,
  Dañado y Malogrado → `INOPERATIVO`; Mantenimiento → `MANTENIMIENTO`.
- Los estados Completo, Incompleto, En préstamo, Sin batería, guion o vacío quedan
  como `null` en los **31 registros pendientes del plan**. No se envían a la API:
  el DTO y el esquema vigentes exigen un estado. Dejarlos vacíos no modifica esa
  regla ni permite crear equipos incompletos. No se presupone que sean operativos.
- Año ausente o guion → `null`. El resto se valida dentro de 1900–2100.
- Categorías y subcategorías conservan su denominación; no se asume que una sigla
  y un nombre largo sean equivalentes. Se reutilizan registros activos coincidentes.
- La bandera `requiereMantenimiento` se activa únicamente para el estado explícito
  MANTENIMIENTO de esta carga. No se inventan órdenes ni historial de mantenimiento.

Cada comentario conserva el hash del Excel, la hoja y la fila. La preparación
verifica campos obligatorios y límites de longitud. Los conflictos contra registros
ya existentes se revisan durante la aplicación; no se sobrescribe inventario previo.

### Resolución del conflicto de L201, fila 57

El brazo robótico OpenManipulator X tenía el número de serie de fabricante `1`,
ya reservado por un equipo previo en BAJA. Se preservó ese registro sin editarlo
y se creó el equipo importado con número de serie vacío (`null`), conservando
el valor original en el comentario de procedencia. La resolución consta en
`field_resolutions` del manifiesto privado; no se inventó otra serie.
El equipo importado quedó con ID 258 y el registro anterior conserva su ID 1.
La excepción dejó de ser un pendiente de carga; los **31 pendientes restantes**
corresponden exclusivamente a los estados vacíos acordados.

## Ejecutar desde PowerShell

Desde la raíz Git, para preparar o revisar sin acceder a la API ni a PostgreSQL:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\importar-inventario-demo.ps1
```

El script utiliza el Python del runtime local de Codex si está disponible, o
Python del equipo con `openpyxl`. No instala dependencias ni cambia el Excel.
Se puede indicar otra ruta con `-Archivo "C:\ruta\inventario.xlsx"`.

Para aplicar, iniciar primero el backend **contra PostgreSQL local en
`127.0.0.1:5432/inventario_laboratorios`** y mantenerlo abierto en otra terminal:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

Después, en otra terminal y desde la misma raíz:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\importar-inventario-demo.ps1 -Aplicar
```

La carga solicita la credencial PostgreSQL con entrada oculta, realiza un respaldo
custom con `pg_dump` 18 y comprueba su listado mediante `pg_restore --list`. Guarda
un marcador con hash del respaldo. Si el backend no está disponible o el respaldo
falla, no continúa. Luego solicita localmente las contraseñas de Marko y Aldo de
la aplicación. No escribir contraseñas en argumentos CLI ni en el chat.

Destino HTTP fijo: `http://127.0.0.1:8080/api`, sin proxy externo ni redirecciones.
El respaldo y la API deben corresponder a la misma base local. No usar este comando
para Docker ni Render; son instancias distintas.

## Archivos privados y reanudación

Los datos de trabajo se guardan fuera del repositorio, en
`%LOCALAPPDATA%\inventario-laboratorios\importacion-demo\<12 caracteres del hash>\`.
El wrapper restringe los permisos de esa carpeta al usuario Windows que lo ejecuta.

- `plan.json`: selección, normalizaciones, reparto, equipos preparados y pendientes.
- Respaldo `.backup` y marcador de verificación: contienen información privada;
  no se incorporan a Git.
- `credenciales-demo.json`: claves generadas de las nuevas cuentas. No contiene
  claves guardadas de Marko/Aldo. No copiarlo a documentación o repositorio.
- Manifiesto y verificaciones de ejecución: permiten identificar recursos creados,
  conflictos y operaciones ya completadas sin guardar JWT ni contraseñas.

Al reanudar, el wrapper localiza el respaldo original acreditado por el hash del
manifiesto y vuelve a comprobar su listado; no sustituye ese respaldo por uno
tomado después de una carga parcial. Si falta, se detiene.

Las altas HTTP individuales no forman una única transacción global. Si falla una
fila, detenerse y revisar el manifiesto; reanudar contrasta procedencia y datos
antes de repetir. Un timeout de escritura requiere reconciliar el resultado y no
justifica volver a enviar un POST sin comprobación. No borrar la base para repetir
la carga ni restaurar un respaldo automáticamente sobre la instancia habitual.

## Comprobaciones realizadas

La preparación se ejecutó con Windows PowerShell y leyó el Excel real: **288
candidatos, 257 listos y 31 pendientes**, con el reparto indicado. El SHA del
archivo original permanece intacto. La ejecución conjunta de `unittest` aprobó
**25/25 pruebas**: 12 de preparación con Excel sintéticos y 13 del ejecutor con
HTTP simulado, sin PostgreSQL real. Incluyen cinco actores, permisos, conflictos,
asignación de un laboratorio, cuentas preexistentes, protección de secretos y
reanudación tras perder una respuesta ya confirmada en el servidor simulado.
La sintaxis PowerShell se validó y el modo de preparación funcionó con Windows
PowerShell. Esas pruebas no acreditan una importación ni un respaldo reales.

La regresión del cierre del **27 de octubre** amplió la suite y aprobó
**28/28 pruebas**: 12 de preparación y 16 de aplicación, con Excel sintético y
HTTP simulado. El total de 25 anterior corresponde a la primera preparación.

La preparación anterior no había aplicado escrituras. Posteriormente se ejecutó
la carga autenticada contra PostgreSQL nativo y la API local: el manifiesto
acredita **337 altas de recursos relacionadas con la importación**, incluidos
los 257 equipos, y eventos `CREAR` de Marko, Aldo, Jason, Jorge y Gabriela.
Las cuentas y el reparto de laboratorios se verificaron. No se insertaron
eventos de auditoría directamente ni se fabricaron fechas anteriores.

El recorrido real del 26 de octubre aprobó **8/8 comprobaciones de navegador**,
cero errores de página: roles ADMIN/GESTOR/LECTOR, alcance, Auditoría, Reportes y
móvil de 390 px. Se descargaron tres CSV con datos y se contrastaron con la API;
Movimientos y Mantenimientos estaban vacíos y sus descargas quedaron deshabilitadas.
Ver [cierre local y límites de publicación](despliegue/verificacion-cierre-2026-10-27.md).

Fuentes de implementación: [preparador](../scripts/preparar_inventario_demo.py),
[ejecutor](../scripts/aplicar_inventario_demo.py),
[comando PowerShell](../importar-inventario-demo.ps1),
[contratos y base de datos](memory/backend-database.md).
