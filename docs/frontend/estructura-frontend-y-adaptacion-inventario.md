# Estructura del frontend y adaptación a Inventario de Laboratorios

Fecha de revisión: **1 de octubre de 2026**.

Este documento explica la organización de **Tech Store v2**, desarrollada como referencia del **Taller 3 — Parte 2**, y cómo aplicar sus conceptos al frontend de Inventario de Laboratorios.

**Estado del documento:** guía de arquitectura y propuesta inicial de implementación. La estructura de Tech Store descrita fue revisada en su código. Las carpetas y configuraciones propuestas para el inventario pertenecen a ese diseño histórico; la nota siguiente remite a la implementación activa y su evidencia. Esta guía no crea la aplicación ni modifica el backend.

**Estado del proyecto actualizado — 3 de octubre de 2026:** la propuesta de esta
guía se conserva como referencia del curso. La versión activa está en
`frontend/version-jason/frontend`, con React Router, Axios, sesión en memoria y
los módulos compatibles con la API. Su flujo completo en Docker y la publicación
de backend/frontend en GitHub Actions están
[verificados con evidencia](../despliegue/verificacion-docker-actions-2026-10-03.md).
La versión Marko permanece separada. Las diferencias entre la propuesta inicial,
Jason y los mockups se registran en la
[auditoría de integración](../sprints_realizados-frontend-Jason/auditoria-integracion.md);
no se afirma fidelidad visual 1:1 ni implementación de Mantenimientos/Reportes/Configuración.

## 1. Contexto y alcance

Tech Store es una aplicación de práctica construida con React y Vite. Reutiliza la apariencia del proyecto original en HTML/CSS, llamado v0, y organiza las vistas como componentes de React. La carpeta v2 identifica la versión del proyecto; el nombre académico acordado es Taller 3 — Parte 2, aunque el material de referencia y el README de Tech Store conservan el título Taller 2.

La referencia revisada está en `fullstackweb/frontend-judith/actividad-techstore/v2/tech-store`.

El proyecto demuestra:

- Separación de pantallas y componentes reutilizables.
- Navegación mediante estado y renderizado condicional.
- Formularios controlados, validación y comunicación mediante props.
- Consulta de una API con `fetch`.
- Estado compartido entre catálogo, carrito y encabezado.
- Estilos generales y estilos asociados a cada pantalla.

El acceso y el boletín son simulaciones. Los productos proceden de una API pública; las cantidades seleccionadas existen únicamente en memoria. No hay autenticación real, persistencia propia ni procesamiento de compras.

Inventario de Laboratorios tiene una situación diferente: ya dispone de una API con autenticación, permisos, organización de laboratorios, clasificación, equipos y movimientos. Su frontend debe consumir esos contratos. Los **10 mockups de [esta carpeta](mockup/)** fueron revisados visualmente y son la referencia para la apariencia, composición y navegación del inventario.

Las referencias del frontend cumplen tres funciones complementarias:

| Referencia | Qué define |
|---|---|
| Tech Store, Taller 3 — Parte 2 | Patrón técnico: pantallas, componentes, estado, props, formularios y consumo de API |
| Mockups de Inventario de Laboratorios | Diseño del login, panel interno, menú, tablas, formularios, indicadores y jerarquía visual |
| Contratos del backend de inventario | Datos disponibles, nombres de campos, validaciones, permisos y operaciones que pueden guardarse |

La implementación debe conservar la identidad visual del inventario y reutilizar la organización de React aprendida en Tech Store. Las diferencias concretas entre diseño y API se documentan en la sección 12.2.1; una función dibujada aún puede requerir una ampliación del backend.

## 2. Tecnologías y configuraciones de Tech Store

### 2.1. Herramientas utilizadas

| Herramienta | Responsabilidad | Versión resuelta en el lock revisado |
|---|---|---|
| React | Componentes, estado e interfaz | 19.3.0 |
| React DOM | Montar la aplicación en el navegador | 19.3.0 |
| Vite | Desarrollo local y compilación | 8.3.0 |
| Plugin React de Vite | Integrar React con Vite | 6.1.1 |
| ESLint | Analizar problemas del código | 10.10.0 |

Son las versiones del proyecto revisado, no una instrucción de actualizar dependencias. `package.json` declara rangos; por ejemplo, React usa `^19.2.8`, mientras que `package-lock.json` registra la versión concreta resuelta, 19.3.0.

El código utiliza **JavaScript y JSX**. JSX permite describir la interfaz dentro de JavaScript. Los paquetes `@types/react` y `@types/react-dom` aportan definiciones de tipos, pero su presencia no convierte el proyecto en TypeScript.

No se utilizan React Router, Redux, Axios ni un framework de CSS. La navegación se implementa con React, las solicitudes con `fetch` y el diseño con CSS normal.

En el entorno revisado se dispone de Node.js 24.16.0 y npm 11.13.0. El proyecto no fija una versión de Node mediante `engines` en su `package.json`; al reproducirlo se deben respetar los requisitos de sus dependencias.

### 2.2. package.json y package-lock.json

`package.json` describe el proyecto, los comandos y sus dependencias:

- `private: true`: evita publicarlo accidentalmente como paquete npm.
- `type: "module"`: establece el uso de módulos con `import` y `export`.
- `dependencies`: incluye React y React DOM.
- `devDependencies`: incluye Vite, ESLint, plugins y ayudas de desarrollo.

Los comandos definidos son:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "lint": "eslint .",
    "preview": "vite preview"
  }
}
```

| Comando | Uso |
|---|---|
| `npm install` | Instalar dependencias durante el desarrollo |
| `npm ci` | Reinstalar según un lock compatible, útil para reproducir el entorno |
| `npm run dev` | Iniciar la aplicación en desarrollo |
| `npm run build` | Generar los archivos compilados en `dist/` |
| `npm run preview` | Revisar localmente la compilación generada |
| `npm run lint` | Revisar reglas de código con ESLint |

Los comandos se ejecutan desde la carpeta que contiene `package.json`. En PowerShell se puede usar `npm.cmd`, por ejemplo `npm.cmd run dev`, si la política del equipo bloquea `npm.ps1`.

`package-lock.json` debe conservarse junto con `package.json`. `node_modules/` puede reinstalarse; no es el código fuente de la aplicación. `dist/` se genera mediante build y no debe editarse a mano.

No hay un script `test` configurado. Compilar, ejecutar ESLint y probar el comportamiento de la aplicación son comprobaciones diferentes. `preview` tampoco publica la aplicación en Internet.

### 2.3. vite.config.js

La configuración actual es:

```js
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
})
```

Activa la integración de React. No define un puerto propio, alias de importación, proxy al backend ni configuración de despliegue personalizada. Los componentes se importan mediante rutas relativas.

Tech Store tampoco tiene una dirección de API configurable mediante un archivo de entorno: la URL de DummyJSON está escrita en `Catalogo.jsx`.

### 2.4. eslint.config.js

La configuración:

- Revisa archivos `.js` y `.jsx`.
- Usa las reglas recomendadas de JavaScript.
- Incluye reglas para Hooks de React y React Refresh.
- Reconoce variables del navegador, como `window` y `document`.
- Habilita el análisis de JSX.
- Ignora `dist/`.

ESLint ayuda a detectar errores de programación y usos incorrectos de ciertas APIs. No comprueba por sí solo que una solicitud llegue al backend o que una pantalla se vea correctamente.

### 2.5. .gitignore

Excluye dependencias, compilaciones, logs y configuraciones locales, entre ellas `node_modules/`, `dist/`, `*.local` y `.idea/`. El código, las configuraciones compartidas y el lock sí forman parte de los archivos que se conservan al compartir el proyecto.

Para una futura configuración de entorno, un `.env.example` puede documentar variables públicas. Las credenciales de PostgreSQL y el secreto JWT pertenecen al backend.

## 3. Estructura actual de Tech Store

```text
tech-store/
├── package.json
├── package-lock.json
├── vite.config.js
├── eslint.config.js
├── index.html
├── .gitignore
├── README.md
├── public/
├── iconos/
├── imagenes/
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── index.css
│   ├── App.css
│   ├── assets/
│   ├── pantallas/
│   │   ├── Home.jsx
│   │   ├── Home.css
│   │   ├── Login.jsx
│   │   ├── CrearCuenta.jsx
│   │   ├── Autenticacion.css
│   │   ├── Catalogo.jsx
│   │   ├── Catalogo.css
│   │   ├── Carrito.jsx
│   │   └── Carrito.css
│   └── componentes/
│       ├── Encabezado.jsx
│       ├── PiePagina.jsx
│       ├── TarjetaProducto.jsx
│       ├── FiltroPrecio.jsx
│       ├── ResumenSeleccion.jsx
│       ├── ControlCantidad.jsx
│       ├── FilaCarrito.jsx
│       ├── CampoContrasena.jsx
│       ├── Boletin.jsx
│       ├── Saludo.jsx
│       ├── TarjetaBienvenida.jsx
│       └── Boton.jsx
├── node_modules/                 # Dependencias instaladas
└── dist/                         # Resultado del build
```

Los componentes `Saludo`, `TarjetaBienvenida` y `Boton` se conservan de ejercicios anteriores y no participan en el recorrido principal actual. `App.css` y algunos archivos de `src/assets/` son restos de la plantilla; `App.css` no está importado en el flujo actual. Su presencia no obliga a copiarlos al inventario.

## 4. Cómo arranca la aplicación

```text
index.html → src/main.jsx → App.jsx → pantalla activa y componentes
```

`index.html` configura el idioma español, UTF-8, el título y el viewport para dispositivos móviles. Contiene el elemento donde se monta React y la entrada JavaScript:

```html
<div id="root"></div>
<script type="module" src="/src/main.jsx"></script>
```

`main.jsx` importa los estilos generales y monta `App`:

```jsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

`StrictMode` añade comprobaciones durante el desarrollo. `main.jsx` no contiene la lógica de negocio: su responsabilidad es iniciar la aplicación.

Las distintas pantallas comparten el mismo documento HTML. Se muestran o se retiran mediante React; no es necesario crear un HTML independiente para cada vista.

## 5. App.jsx: navegación y estado compartido

App mantiene tres estados principales:

```jsx
const [pantallaActual, setPantallaActual] = useState("home");
const [usuarioActivo, setUsuarioActivo] = useState("");
const [productos, setProductos] = useState([]);
```

| Estado | Responsabilidad |
|---|---|
| `pantallaActual` | Identificar qué pantalla está visible |
| `usuarioActivo` | Conservar el nombre del acceso simulado |
| `productos` | Conservar los productos de la API y las cantidades seleccionadas |

Sus funciones principales son:

| Función | Comportamiento |
|---|---|
| `cambiarPantalla` | Actualiza la pantalla y desplaza la página al inicio |
| `entrarAlCatalogo` | Guarda el nombre del usuario y muestra el catálogo |
| `cerrarSesion` | Limpia el usuario, vacía las cantidades y abre Login |
| `recibirProductos` | Guarda la consulta y conserva cantidades ajustadas al stock recibido |
| `actualizarCantidad` | Cambia una cantidad respetando cero, stock y máximo de 99 |
| `vaciarCarrito` | Coloca las cantidades en cero sin eliminar el catálogo descargado |

App monta una sola vez el encabezado y el pie. Entre ellos muestra la pantalla que corresponde:

```jsx
{pantallaActual === "carrito" && (
  <Carrito
    productos={productos}
    actualizarCantidad={actualizarCantidad}
    volverAlCatalogo={() => cambiarPantalla("catalogo")}
  />
)}
```

Esta navegación no modifica la URL ni añade entradas de navegación por pantalla al historial del navegador. Al recargar se vuelve a Home y se reinicia el estado.

## 6. Pantallas y componentes: responsabilidades

### 6.1. Pantallas

Una pantalla organiza una vista completa, coordina sus componentes y decide qué mostrar en cada estado.

| Pantalla | Responsabilidad |
|---|---|
| `Home` | Presentación de la tienda y acceso al catálogo |
| `Login` | Usuario, contraseña, validación y acceso al registro |
| `CrearCuenta` | Usuario, correo, contraseña y regreso al login |
| `Catalogo` | Consulta de productos, filtros, tarjetas y resumen |
| `Carrito` | Selección, cantidades, eliminación y total |

Login y CrearCuenta llaman a una función recibida desde App cuando sus datos son válidos. No consultan usuarios reales. Home puede abrir el catálogo directamente: el flujo actual tampoco protege esa pantalla mediante autenticación.

### 6.2. Componentes reutilizables

| Componente | Responsabilidad |
|---|---|
| `Encabezado` | Navegación, usuario y contador de unidades |
| `PiePagina` | Contenido común del pie e inclusión del boletín |
| `TarjetaProducto` | Imagen, nombre, precio y stock de un producto |
| `FiltroPrecio` | Captura y validación de precio mínimo/máximo |
| `ResumenSeleccion` | Unidades, productos seleccionados y total |
| `ControlCantidad` | Controles de aumento y disminución reutilizados en catálogo y carrito |
| `FilaCarrito` | Presentación y eliminación de un producto seleccionado |
| `CampoContrasena` | Campo controlado con mostrar/ocultar contraseña |
| `Boletin` | Validación de un correo y confirmación simulada |

Una pantalla usa componentes para construir su contenido. Un componente como `ControlCantidad` no necesita conocer toda la aplicación: recibe datos y ejecuta las funciones que le entregan.

## 7. Estado, props y eventos

`useState` conserva un valor entre renderizados del componente. La función que devuelve permite actualizarlo y solicitar una nueva representación de la interfaz.

Las props son parámetros de un componente. Pueden transportar texto, números, objetos, arreglos o funciones. En Tech Store, App entrega al catálogo esta función:

```jsx
onVerCarrito={() => cambiarPantalla("carrito")}
```

Catálogo la ejecuta al pulsar el botón:

```jsx
<button type="button" onClick={onVerCarrito}>
  Ver carrito
</button>
```

El patrón general es:

```text
App entrega datos y funciones por props
                  ↓
Un componente muestra los datos
                  ↓
El usuario realiza una acción
                  ↓
El componente ejecuta la función recibida
                  ↓
App actualiza el estado y React actualiza la interfaz
```

Los productos viven en App porque los necesitan Catálogo, Carrito y Encabezado. Los campos de Login viven en Login. La visibilidad de la contraseña vive en `CampoContrasena`.

Este patrón de mantener datos compartidos en el padre común se describe en la [guía de React sobre compartir estado](https://react.dev/learn/sharing-state-between-components).

**Conservación de datos:** cambiar de pantalla mantiene los productos porque App sigue montado. El filtro del catálogo se reinicia al salir y volver, porque pertenece a esa pantalla. Recargar el navegador reinicia también App. No se utiliza `localStorage`, `sessionStorage` ni una base de datos propia.

La regla para el inventario será conservar cada estado cerca de donde se necesita y subirlo a un padre común cuando deba coordinar varias vistas. No todos los campos ni todos los datos deben colocarse en App.

## 8. Formularios controlados y validación

Un campo controlado refleja un estado y lo actualiza al escribir:

```jsx
const [usuario, setUsuario] = useState("");

<input
  value={usuario}
  onChange={(evento) => setUsuario(evento.target.value)}
/>
```

El envío utiliza `onSubmit` y `preventDefault()` para evitar la recarga del documento. Después se validan los campos; los mensajes de error se guardan en estado y se muestran junto a los inputs.

Login valida que usuario y contraseña no estén vacíos. CrearCuenta añade la validación del formato de correo. El boletín valida el correo y muestra una confirmación de prueba. Los formularios enfocan el primer campo inválido para facilitar su corrección.

`CampoContrasena` recibe el valor y la función de actualización desde la pantalla; solo mantiene internamente si el contenido se muestra como texto o como contraseña. Su botón usa `type="button"` para no enviar el formulario.

En inventario, el mismo patrón sirve para código, nombre, laboratorio, subcategoría y demás campos del equipo. La validación del frontend facilita corregir errores, mientras que el backend sigue siendo responsable de validar y guardar la operación.

## 9. API y lógica del catálogo

La consulta está dentro de `Catalogo.jsx` y se ejecuta al pulsar Buscar productos. La URL actual es:

```text
https://dummyjson.com/products/category/laptops?limit=12&select=title,price,stock,thumbnail
```

Se utiliza `fetch` con promesas. No hay una carga automática mediante `useEffect`.

El flujo consiste en activar `cargando`, limpiar el error anterior, hacer la solicitud, comprobar `respuesta.ok`, leer el JSON, validar `datos.products`, entregarlo a App y finalizar el estado de carga. Si falla, se muestra un mensaje y se permite reintentar.

La pantalla distingue carga, fallo de consulta, catálogo aún no consultado, lista vacía y ausencia de coincidencias con el filtro.

Cada producto utiliza `id`, `title`, `price`, `stock` y `thumbnail`. App añade `cantidad`, que es la selección local del usuario. Cambiarla no actualiza el stock en DummyJSON.

| Operación | Aplicación |
|---|---|
| `map` | Dibujar listas y construir nuevas cantidades sin modificar directamente el arreglo anterior |
| `filter` | Obtener productos visibles y productos con cantidad mayor que cero |
| `reduce` | Sumar unidades y total monetario |

Los totales se calculan en centavos. Filtrar solo cambia lo visible, por lo que un producto oculto por precio puede continuar seleccionado. El máximo de 99 es una regla del ejercicio; no es una regla que deba trasladarse al inventario.

## 10. CSS, recursos y accesibilidad

| Archivo o carpeta | Uso |
|---|---|
| `src/index.css` | Tipografía, estructura común, contenedores, encabezado y pie |
| `Home.css` | Presentación del inicio |
| `Autenticacion.css` | Diseño compartido entre Login y CrearCuenta |
| `Catalogo.css` | Filtro, tarjetas, distribución y resumen |
| `Carrito.css` | Filas y distribución del carrito |
| `iconos/` | SVG importados por los componentes |
| `imagenes/` | Recursos del diseño heredados de v0 |
| `public/` | Recursos estáticos del proyecto |

Se utilizan media queries para adaptar la distribución a distintos anchos. Hay etiquetas de formulario, texto alternativo, estados de foco y atributos ARIA para describir controles y mensajes.

Los CSS normales tienen alcance global: importar un archivo desde una pantalla no limita automáticamente sus selectores a esa pantalla. Para el inventario conviene conservar nombres específicos de clase y ubicar las reglas realmente compartidas en estilos comunes.

## 11. Referencias para continuar

La propuesta final se basa en los contratos existentes del inventario. Si cambian, deben revisarse las pantallas y este documento.

- [README del proyecto](../../README.md).
- [Arquitectura del backend](../backend-final/arquitectura-backend.md).
- [Endpoints y cuerpos de solicitud](../backend-final/endpoints.md).
- [Códigos HTTP](../backend-final/codigos-http.md).
- [Matriz de permisos](../matriz-permisos.md).
- [Autenticación JWT](../autenticacion-jwt.md).
- [Reglas de negocio](../reglas-negocio.md).
- [Funciones pendientes fuera del backend cerrado](../backend-final/backlog.md).
- [Mockups del frontend](mockup/).
- [Variables de entorno de Vite](https://vite.dev/guide/env-and-mode).
- [Proxy de desarrollo de Vite](https://vite.dev/config/server-options#server-proxy).

## 12. Cómo debería estructurarse Inventario de Laboratorios

Esta sección es la propuesta de adaptación. Mantiene la separación técnica de pantallas y componentes aprendida en el taller, toma el diseño de los mockups del inventario y añade una capa de servicios para comunicarse con la API real. En la revisión actual, `frontend/` contiene un `readme.md`; aún no tiene una aplicación React con `package.json`.

### 12.1. Organización general del repositorio

```text
inventario-laboratorios/
├── backend/
│   └── inventario/                # API Spring Boot existente
├── frontend/                     # Aplicación React propuesta
├── docs/
│   ├── backend-final/             # Contratos y documentación existentes
│   ├── frontend/
│   │   ├── estructura-frontend-y-adaptacion-inventario.md
│   │   └── mockup/                # Referencia visual existente
│   └── ...                       # ERD, sprints, reglas y permisos
└── README.md
```

El código ejecutable del cliente debe quedar en `frontend/`; `docs/frontend/` debe contener explicaciones y referencias visuales. El frontend consulta la API por HTTP. El acceso a PostgreSQL, las transacciones y las reglas de negocio continúan en el backend existente.

### 12.2. Organización propuesta del frontend

```text
frontend/
├── package.json
├── package-lock.json
├── vite.config.js
├── eslint.config.js
├── index.html
├── .gitignore
├── .env.example
├── readme.md
├── public/
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── index.css
    ├── assets/
    │   ├── iconos/
    │   └── imagenes/
    ├── layouts/
    │   └── PanelInventario.jsx
    ├── pantallas/
    │   ├── Login.jsx
    │   ├── Inicio.jsx             # Dashboard por definir
    │   ├── Equipos.jsx
    │   ├── NuevoEquipo.jsx
    │   ├── EditarEquipo.jsx
    │   ├── DetalleEquipo.jsx
    │   ├── Categorias.jsx
    │   ├── Laboratorios.jsx
    │   ├── Movimientos.jsx
    │   └── AsignacionesLaboratorio.jsx
    ├── componentes/
    │   ├── Encabezado.jsx
    │   ├── MenuLateral.jsx
    │   ├── TituloPagina.jsx
    │   ├── IndicadorResumen.jsx
    │   ├── EtiquetaEstado.jsx
    │   ├── CampoContrasena.jsx
    │   ├── FilaEquipo.jsx
    │   ├── FormularioEquipo.jsx
    │   ├── FiltroEquipos.jsx
    │   ├── SelectorLaboratorio.jsx
    │   ├── ArbolOrganizacion.jsx
    │   ├── FormularioTraslado.jsx
    │   ├── TablaMovimientos.jsx
    │   ├── ModalFormulario.jsx
    │   ├── MensajeEstado.jsx
    │   └── ConfirmacionAccion.jsx
    ├── servicios/
    │   ├── api.js
    │   ├── authService.js
    │   ├── equiposService.js
    │   ├── catalogosService.js
    │   ├── organizacionService.js
    │   ├── movimientosService.js
    │   └── asignacionesService.js
    └── utilidades/
        ├── validaciones.js
        └── formatos.js
```

Los CSS específicos pueden acompañar a sus pantallas, como en Tech Store. `index.css` conservaría los estilos comunes. Las utilidades se extraen cuando existe una necesidad compartida; no hace falta llenar todas las carpetas antes de implementar la primera pantalla.

| Parte | Responsabilidad propuesta |
|---|---|
| `main.jsx` | Montar React e importar los estilos generales |
| `App.jsx` | Coordinar sesión, navegación inicial y estructura común |
| `layouts/PanelInventario.jsx` | Reunir menú lateral, barra superior y área de contenido de las pantallas internas |
| `pantallas/` | Coordinar listados, formularios, consultas y resultados de cada vista |
| `componentes/` | Representar controles y bloques reutilizables |
| `servicios/api.js` | Centralizar URL base, cabeceras y tratamiento HTTP común |
| Servicios por tema | Definir las solicitudes de autenticación, equipos, catálogos y movimientos |
| `utilidades/` | Formatear datos y compartir validaciones de interfaz |
| `assets/` | Conservar imágenes e iconos propios del frontend |

La navegación inicial puede mantenerse con `useState` y props. Si se requieren URLs de detalle, enlaces compartibles o historial del navegador, se debe incorporar navegación por rutas como una evolución explícita. No es necesario añadir un gestor global de estado para comenzar.

#### 12.2.1. Cómo se traducen los mockups a la estructura

El login tiene una composición independiente: formulario a la izquierda, ilustración a la derecha y un fondo claro con elementos decorativos. Las pantallas internas comparten menú lateral, barra superior y área principal. `PanelInventario` permite reutilizar ese contenedor sin repetirlo en cada pantalla. El encabezado y el pie comerciales de Tech Store se sustituyen por los bloques correspondientes al diseño del inventario.

| Mockup revisado | Pantallas y componentes propuestos | Cobertura del backend actual |
|---|---|---|
| [01. Inicio de sesión](mockup/01_inicio_sesion_inventario_laboratorios.png) | `Login`, `CampoContrasena` y bloque de presentación | Login con usuario y contraseña; recordar sesión, recuperación y acceso de prueba requieren decisiones adicionales |
| [02. Listado general de equipos](mockup/02_equipos_listado_general.png) | `Equipos`, `IndicadorResumen`, `FiltroEquipos`, `FilaEquipo`, `EtiquetaEstado` | Listado, filtros del contrato, detalle, edición y baja; revisar diferencias de columnas e indicadores |
| [03. Nuevo equipo](mockup/03_equipos_nuevo_registro.png) | `NuevoEquipo`, `FormularioEquipo`, `SelectorLaboratorio`; el formulario se comparte con `EditarEquipo` | Alta mediante JSON; adjuntos y algunas fechas del diseño todavía no tienen soporte |
| [04. Categorías y subcategorías](mockup/04_categorias_y_subcategorias.png) | `Categorias`, lista de categorías, panel de subcategorías y `ModalFormulario` | Consulta y operaciones de catálogos según rol; los listados existentes devuelven activos |
| [05. Laboratorios, sedes y áreas](mockup/05_laboratorios_sedes_y_areas.png) | `Laboratorios`, `ArbolOrganizacion`, tabla y panel de detalle | Jerarquía y operaciones existentes; capacidad y responsable del laboratorio no figuran en el contrato actual |
| [06. Usuarios y asignaciones](mockup/06_usuarios_roles_y_asignacion_laboratorios.png) | Primera etapa: `AsignacionesLaboratorio`; futuro: una pantalla completa `Usuarios` | Asignación de laboratorios a un usuario identificado, con rol ADMIN; sin listado ni CRUD general de usuarios |
| [07. Historial de movimientos](mockup/07_movimientos_historial_y_actividad.png) | `Movimientos`, `TablaMovimientos`, filtros y actividad reciente basada en los eventos devueltos | Consulta del historial y registro de traslados; sin flujo de movimientos pendientes/en proceso |
| [08. Mantenimientos](mockup/08_mantenimientos_programacion_y_calendario.png) | Futuro: `Mantenimientos`, formulario y calendario | El equipo tiene estado y bandera de mantenimiento; no hay API de programación, órdenes o calendario |
| [09. Reportes](mockup/09_reportes_indicadores_y_exportacion.png) | Futuro: `Reportes`, gráficos, filtros y exportación | Pueden calcularse resúmenes sobre datos autorizados disponibles; no existen endpoints específicos de reportes, historial de exportaciones ni estadísticas de mantenimiento |
| [10. Configuración](mockup/10_configuracion_general_y_seguridad.png) | Futuro: `Configuracion`, secciones de preferencias y formularios | No hay API para guardar estos ajustes, activar doble factor, gestionar notificaciones o cambiar permisos dinámicamente |

El menú también muestra **Dashboard**, pero no hay un mockup independiente de esa vista entre los diez archivos. `Inicio.jsx` queda como propuesta por definir; el listado de Equipos puede ser la primera pantalla después del login mientras se concreta ese diseño. Del mismo modo, `DetalleEquipo` y `EditarEquipo` se derivan de las acciones del listado y deben mantener la composición de los diseños existentes.

Las pantallas futuras de Usuarios, Mantenimientos, Reportes y Configuración no se incluyen como módulos funcionales en el árbol de la primera etapa. Si se muestran sus accesos durante una presentación del diseño, deben identificarse como pendientes; no se deben presentar como datos reales las cifras de ejemplo del mockup.

**Criterios visuales comunes:** fondo claro con matices celestes, paneles blancos, bordes suaves, esquinas redondeadas, títulos oscuros, selección del menú en celeste y acciones destacadas en rojo. Se deben compartir los estilos de tarjetas, botones, filtros, tablas y etiquetas de estado. Los colores exactos, espaciados y tipografías se definen al implementar y se cotejan con las imágenes; no se deducen valores CSS exactos de una inspección visual.

El diseño de escritorio también debe adaptarse a anchos pequeños: menú colapsable, formularios en una columna y tablas con un tratamiento legible. No hay mockups móviles en la carpeta revisada, por lo que esa adaptación deberá verificarse durante la implementación.

#### 12.2.2. Diferencias entre el diseño y los contratos que deben resolverse

Estas diferencias afectan textos, controles o datos. Deben ajustarse conservando la intención visual de los mockups:

1. **Identificación en Login.** La imagen dice «Correo institucional», pero la API autentica por `userName`. La primera versión debe pedir «Usuario» y enviar ese campo. Autenticar por correo requeriría cambiar el contrato. «Recordarme», «Olvidaste tu contraseña» y «Acceso de prueba» no deben aparentar funciones existentes sin una implementación definida; un acceso de prueba tampoco debe omitir la autenticación de la API.
2. **Estados y campos de equipo.** El formulario dibuja «Activo»; el contrato utiliza `OPERATIVO`, `MANTENIMIENTO`, `INOPERATIVO` y `BAJA`, y este último se establece mediante DELETE. Se utilizarán etiquetas claras coherentes con esos valores. Subcategoría es obligatoria para la API aunque el diseño no la marque, y responsable es opcional aunque aparezca con asterisco.
3. **Adjuntos y fechas.** Foto/documento, fecha de compra y último mantenimiento no pertenecen al request actual. `anio` y `ordenCompra` no equivalen a una fecha de compra. «Observaciones» puede presentarse como etiqueta del campo `comentario`, cuyo DTO actual no tiene el límite de 500 caracteres dibujado. Se incorporará la casilla `requiereMantenimiento`, obligatoria para la API.
4. **Categoría y responsable.** La categoría puede servir para elegir una subcategoría, pero se guarda `idSubcategoria`. El resumen de subcategoría dentro de un equipo solo trae ID y nombre; para mostrar la categoría se necesita relacionarlo con el catálogo de subcategorías, o mostrar explícitamente Subcategoría. No existe un listado general de usuarios para llenar el selector de responsables: no debe inventarse ese origen de datos. Una primera versión puede omitir esa asignación opcional hasta acordar cómo seleccionar un usuario válido.
5. **Búsqueda, paginación e indicadores.** Los endpoints de equipos no reciben búsqueda libre ni paginación. Si se implementan esos controles localmente, operan sobre el conjunto descargado y autorizado. El buscador general del encabezado no tiene una API de búsqueda global. Los contadores deben explicar su alcance; variaciones frente al mes anterior no pueden deducirse de una lista del estado actual. Exportar los datos descargados sería una función adicional del cliente, no una llamada a un endpoint de exportación ya existente.
6. **Catálogos y laboratorios.** Los listados de catálogos exponen activos; no permiten reproducir una tabla de activos e inactivos como si ambos estuvieran disponibles. Laboratorio contiene código, nombre, ubicación, área y actividad; no tiene capacidad, responsable ni un estado «En mantenimiento». Un contador de equipos por laboratorio debe provenir de consultas autorizadas y describir su alcance.
7. **Movimientos.** La respuesta contiene el actor del evento, que no debe confundirse con el responsable del equipo. No contiene estados «Pendiente», «En proceso» o «Completado». Las nuevas operaciones disponibles son traslados confirmados en una transacción; leer tipos históricos no implica poder crear todos los tipos dibujados. El filtro de laboratorio sí existe; filtros por texto, fecha o tipo serían locales sobre los datos recibidos salvo una ampliación de la API.
8. **Notificaciones y configuración.** La campana, los porcentajes, los calendarios, las preferencias de seguridad y el historial de reportes del diseño no demuestran que existan datos o servicios para alimentarlos. Cada control que se active debe tener una operación real o identificarse claramente como parte de una demostración visual.

Al validar una pantalla se deben comprobar dos cosas por separado: que conserva la composición del mockup y que sus campos, permisos y acciones corresponden al backend. Los ajustes acordados se documentan junto a la pantalla para no perder el vínculo con el diseño original.

### 12.3. Adaptación funcional desde Tech Store

| Referencia del taller | Aplicación al inventario |
|---|---|
| Home | Inicio con accesos a módulos y datos disponibles |
| Catálogo de productos | Listado y detalle de equipos |
| Tarjeta de producto | Fila o tarjeta con código, nombre, estado y laboratorio |
| Filtro de precio | Filtros de estado, laboratorio, subcategoría y mantenimiento |
| Formulario controlado | Alta, edición y traslado de equipos |
| Login simulado | Autenticación real con JWT |
| Cantidades y carrito | Solo reutilizar el patrón de interacción cuando exista una necesidad de selección |

Un equipo representa un activo individual. No se debe convertir automáticamente su registro en un producto con cantidad y precio. Los botones de incremento del carrito no equivalen a registrar un traslado ni a modificar la disponibilidad de un equipo.

Los servicios frontend son funciones cliente que llaman a la API. No sustituyen a las clases Service del backend, donde se aplican reglas, alcance y transacciones.

### 12.4. Conexión de desarrollo con el backend

La base local documentada del backend es `http://localhost:8080`. Una opción para el nuevo frontend es utilizar una ruta relativa y un proxy de Vite.

Contenido propuesto para `.env.example`, que se copia a `.env.local` al preparar el entorno:

```dotenv
VITE_API_URL=/api
```

Lectura desde `src/servicios/api.js`:

```js
export const API_URL = import.meta.env.VITE_API_URL;
```

Configuración propuesta para `vite.config.js`:

```js
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

Así, el navegador consulta `/api/equipos` en el servidor del frontend y Vite reenvía esa solicitud al backend manteniendo `/api/equipos`. `strictPort` evita cambiar silenciosamente de puerto si el configurado está ocupado. Esta configuración es propuesta: Tech Store no la tiene.

El proxy de `server` se utiliza en desarrollo. Al desplegar habrá que definir el destino real de la API y la configuración del servidor que sirva la aplicación. Si el navegador accede directamente a un backend de otro origen, debe configurarse CORS en el backend; habilitar CORS en Vite no concede permisos sobre la API.

Las variables `VITE_` quedan disponibles en el cliente y se incorporan a la compilación. Son adecuadas para la dirección pública de la API, no para el secreto JWT o las credenciales de PostgreSQL. Los cambios en los archivos de entorno requieren reiniciar el servidor de desarrollo. Estas reglas se describen en la [documentación de Vite](https://vite.dev/guide/env-and-mode).

### 12.5. Autenticación, sesión y permisos

El login debe enviar a `POST /api/auth/login` los nombres de campo reales del contrato:

```json
{
  "userName": "usuario_de_prueba",
  "password": "contrasena_de_prueba"
}
```

La respuesta incluye `accessToken`, `tokenType`, `expiresIn` y `usuario`. Las consultas protegidas envían `Authorization: Bearer <token>`. La contraseña no debe recortarse ni conservarse como estado compartido después del acceso.

Como primera implementación, la sesión puede conservarse en memoria y solicitar de nuevo el acceso al recargar. Si se desea persistencia de sesión, se debe definir expresamente su estrategia; el backend actual no ofrece refresh token ni un endpoint de logout. Cerrar sesión en el cliente limpia su estado, pero no revoca por sí mismo un JWT ya emitido.

El frontend adapta sus acciones al rol y al alcance de laboratorios:

- **ADMIN:** administra catálogos y asignaciones, además del acceso global previsto por la API.
- **GESTOR:** opera equipos y traslados dentro de los laboratorios autorizados.
- **LECTOR:** consulta la información permitida por su alcance.

La interfaz puede ocultar acciones que no correspondan, pero la autorización efectiva siempre la aplica el backend. Los datos compartidos de sesión no deben mezclarse con todos los campos de los formularios.

No debe copiarse CrearCuenta como registro funcional: actualmente no hay endpoint de alta de usuarios, recuperación de contraseña ni cambio de rol. La pantalla de asignaciones puede operar sobre un usuario identificado mediante las rutas existentes, pero no debe asumir un listado general de usuarios que el contrato no ofrece.

### 12.6. Servicios HTTP y contratos de datos

`api.js` debe concentrar el tratamiento repetido de solicitudes y respuestas: URL base, token cuando corresponda, JSON para cuerpos de solicitud y errores HTTP. Los servicios específicos construyen las rutas y datos de cada operación; las pantallas deciden cómo presentar el resultado.

Hay diferencias con la consulta del taller:

| Aspecto | Contrato del inventario |
|---|---|
| Listados | Arreglos JSON directos; no leer `datos.products` ni asumir paginación |
| Consulta sin resultados | `200` con `[]`, que debe mostrarse como lista vacía |
| Creación | `201` con respuesta JSON |
| Baja lógica | `204` sin cuerpo; no intentar ejecutar `response.json()` |
| Actualización | PUT reemplaza campos editables; un opcional omitido o nulo se limpia |
| Validación | `400`, con `errors` por campo cuando corresponde |
| Autenticación | `401`, que requiere tratar credenciales o sesión inválida |
| Permisos | `403`, que debe diferenciarse de un fallo de conexión |
| Conflicto de negocio | `409`, mostrando el mensaje que explica la operación rechazada |

Los errores de red también deben producir un mensaje y una opción de reintento. Las respuestas no exitosas no deben interpretarse como arreglos vacíos ni como operaciones guardadas.

Después de una creación, edición o traslado, se actualiza la vista con la respuesta confirmada del backend o se vuelve a consultar el listado. Un cambio local mediante `setState` no guarda nada en PostgreSQL.

### 12.7. Formularios y movimientos de equipos

El alta debe respetar `CreateEquipoRequest`. Entre sus datos obligatorios se encuentran `codigoInterno`, `nombre`, `idLaboratorio`, `idSubcategoria`, `estado` y `requiereMantenimiento`. Los selectores envían IDs numéricos; la casilla de mantenimiento envía un booleano. Los campos opcionales, sus límites y las referencias admitidas deben ajustarse a los [contratos documentados](../backend-final/endpoints.md).

La respuesta de equipo contiene objetos resumidos como `laboratorio` y `subcategoria`. Para editar, se transforman a los IDs que pide el request. No se debe enviar todo el objeto de respuesta mediante una copia indiscriminada.

El formulario de edición no envía `id`, `codigoInterno`, `idLaboratorio` ni fechas. El código es inmutable y el laboratorio se cambia mediante una operación de traslado. Dar de baja utiliza DELETE; no se permite elegir BAJA como estado de una creación o edición.

El traslado utiliza `POST /api/equipos/{idEquipo}/traslados` y un cuerpo como este:

```json
{
  "idLaboratorioDestino": 2,
  "motivo": "Reubicación para una práctica",
  "ubicacionInternaDestino": "Mesa 2"
}
```

El ID es ilustrativo y debe reemplazarse por un laboratorio válido y autorizado. El servidor determina equipo, origen real, actor, tipo y fecha. No se envían esos campos adicionales. El backend registra el traslado y modifica el equipo en una misma transacción; el frontend no debe intentar simular esa operación actualizando dos pantallas por separado.

### 12.8. Estados visuales y comprobaciones necesarias

Cada pantalla con una consulta debe distinguir carga, éxito con datos, éxito vacío y error. Cada formulario debe mostrar errores por campo, indicar cuándo se está enviando y evitar envíos duplicados mientras espera. Una acción como dar de baja debe explicar qué registro afecta y permitir confirmar o cancelar.

La revisión funcional del frontend debe incluir:

1. Login válido e inválido, cierre de sesión y respuesta ante vencimiento del token.
2. Listados con datos, vacíos, con filtros y con fallo de conexión.
3. Acciones disponibles según ADMIN, GESTOR y LECTOR.
4. Alta y edición con errores de validación y conflictos del backend.
5. Baja lógica y tratamiento correcto de respuestas 204.
6. Traslado autorizado, traslado rechazado y actualización del historial.
7. Conservación de los datos guardados después de recargar, al consultarlos de nuevo al backend.
8. Navegación con teclado y revisión visual en escritorio y móvil.

Se mantienen `npm run lint` y `npm run build` como comprobaciones técnicas. Si se añade un conjunto de pruebas automatizadas al nuevo frontend, se configura su herramienta y su script de manera explícita; no se presupone que ya existen.

### 12.9. Orden de implementación y límites de la primera versión

1. Preparar React y Vite dentro de `frontend/`, conservar su README y configurar la conexión local.
2. Construir el login y el contenedor `PanelInventario` siguiendo los mockups: encabezado, menú lateral y navegación inicial.
3. Implementar autenticación real, usuario activo y tratamiento de sesión.
4. Consultar y mostrar equipos con sus filtros y estados visuales.
5. Incorporar detalle, alta, edición y baja conforme a los permisos.
6. Añadir catálogos de clasificación y organización utilizando las operaciones existentes.
7. Incorporar traslados e historial de movimientos.
8. Añadir asignaciones de laboratorios con el alcance que permite la API actual.
9. Comparar cada pantalla con su mockup y verificar recorridos por rol, formularios, persistencia, compilación y presentación adaptable.

El alcance inicial debe corresponder a los endpoints existentes. Los mockups de administración completa de usuarios, programación de mantenimiento, reportes o configuración no constituyen por sí solos funciones ya soportadas. La bandera `requiereMantenimiento` y el estado MANTENIMIENTO sí existen; una agenda de mantenimiento es una ampliación diferente. Los detalles de esas ampliaciones se definen antes de convertirlos en operaciones de la interfaz.

Con esta organización, App coordina la aplicación, las pantallas coordinan sus vistas, los componentes resuelven partes reutilizables y los servicios concentran la comunicación HTTP. El backend existente conserva la autenticación, los permisos, las reglas de negocio y la persistencia.
