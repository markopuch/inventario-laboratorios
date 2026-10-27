import { filtrosReportes, validarConteos, validarEquiposPorLaboratorio, validarReporteMantenimientos, validarReporteMovimientos, validarResumenReporte } from './reportes.js';

const nombres = {
  resumen: ['Resumen general', 'resumen-general'],
  equiposPorEstado: ['Equipos por estado', 'equipos-por-estado'],
  equiposPorLaboratorio: ['Equipos por laboratorio', 'equipos-por-laboratorio'],
  movimientos: ['Movimientos por tipo', 'movimientos-por-tipo'],
  mantenimientos: ['Mantenimientos', 'mantenimientos']
};
const camposFiltro = ['idSede', 'idArea', 'idLaboratorio', 'estado', 'fechaDesde', 'fechaHasta', 'estadoMantenimiento', 'tipoMantenimiento'];
const encabezados = [
  'Reporte', 'Generado (UTC)', 'Alcance', 'Filtro sede ID', 'Filtro área ID', 'Filtro laboratorio ID',
  'Filtro estado equipo', 'Filtro fecha desde', 'Filtro fecha hasta', 'Filtro estado mantenimiento', 'Filtro tipo mantenimiento',
  'Agrupación', 'Indicador', 'ID laboratorio', 'Código laboratorio', 'Laboratorio', 'Total'
];

// Excel en español suele usar punto y coma. Todas las filas, incluidos los
// metadatos repetidos, tienen las mismas columnas; no se añade una línea sep=.
// Las comillas preservan delimitadores y saltos de línea. El apóstrofo evita
// interpretar texto como fórmula incluso tras espacios o controles iniciales.
export function celdaCsv(valor) {
  let texto = String(valor ?? '').replace(/\r\n|\r|\n/g, '\r\n');
  if (/^[\s\p{Cc}\p{Cf}]*[=+\-@\t\r\n]/u.test(texto)) texto = `'${texto}`;
  return `"${texto.replace(/"/g, '""')}"`;
}

function filasReporte(nombre, datos) {
  if (!Object.hasOwn(nombres, nombre)) throw new Error('El reporte seleccionado no es válido.');
  const conteos = (filas, grupo) => filas.map(fila => [grupo, fila.valor, '', '', '', fila.total]);
  switch (nombre) {
    case 'resumen':
      validarResumenReporte(datos);
      return [
        ['Resumen', 'Total equipos', '', '', '', datos.totalEquipos],
        ['Resumen', 'Total movimientos', '', '', '', datos.totalMovimientos],
        ['Resumen', 'Total mantenimientos', '', '', '', datos.totalMantenimientos]
      ];
    case 'equiposPorEstado':
      return conteos(validarConteos(datos), 'Por estado de equipo');
    case 'equiposPorLaboratorio':
      return validarEquiposPorLaboratorio(datos).map(fila => ['Por laboratorio', 'Equipos', fila.idLaboratorio, fila.codigo, fila.nombre, fila.total]);
    case 'movimientos':
      validarReporteMovimientos(datos);
      return datos.porTipo.length ? [
        ['Total', 'Movimientos', '', '', '', datos.total],
        ...conteos(datos.porTipo, 'Por tipo de movimiento')
      ] : [];
    case 'mantenimientos':
      validarReporteMantenimientos(datos);
      return datos.porEstado.length || datos.porTipo.length ? [
        ['Total', 'Mantenimientos', '', '', '', datos.total],
        ...conteos(datos.porEstado, 'Por estado de mantenimiento'),
        ...conteos(datos.porTipo, 'Por tipo de mantenimiento')
      ] : [];
    default:
      return [];
  }
}

export function filtrosCsvCoinciden(aplicados, editados) {
  try { return JSON.stringify(filtrosReportes(aplicados)) === JSON.stringify(filtrosReportes(editados)); }
  catch { return false; }
}

// La identidad pertenece a una consulta de una sesión, alcance y filtros
// concretos. Compararla también durante el render cierra la ventana anterior
// al cleanup del efecto cuando cambian esos valores.
export function puedeExportarReporte(nombre, consulta, identidadActual, aplicados, editados) {
  if (!identidadActual || consulta?.identidad !== identidadActual || consulta.estado !== 'listo' || !filtrosCsvCoinciden(aplicados, editados)) return false;
  try { return filasReporte(nombre, consulta.datos).length > 0; }
  catch { return false; }
}

export function crearReporteCsv(nombre, datos, { filtrosAplicados = {}, alcanceGlobal = false, generadoEn = new Date() } = {}) {
  const filas = filasReporte(nombre, datos);
  if (!filas.length) throw new Error('No hay filas para exportar con los filtros aplicados.');
  const fecha = new Date(generadoEn);
  if (!Number.isFinite(fecha.getTime())) throw new Error('La fecha de generación no es válida.');
  const filtros = filtrosReportes(filtrosAplicados);
  const instante = fecha.toISOString();
  // Solo entran filtros del contrato y un alcance descriptivo; no se serializa
  // ningún objeto de sesión, usuario, token o respuesta completa de la API.
  const contexto = [nombres[nombre][0], instante, alcanceGlobal ? 'Global' : 'Laboratorios asignados', ...camposFiltro.map(campo => filtros[campo] ?? 'Todos')];
  const contenido = '\uFEFF' + [encabezados, ...filas.map(fila => [...contexto, ...fila])]
    .map(fila => fila.map(celdaCsv).join(';')).join('\r\n') + '\r\n';
  return { nombreArchivo: `reporte-${nombres[nombre][1]}-${instante.slice(0, 10)}.csv`, contenido };
}

export function descargarReporteCsv(archivo, entorno = globalThis) {
  const { document: documento, URL: urls, Blob: Archivo, setTimeout: programar } = entorno;
  if (!documento?.body || typeof urls?.createObjectURL !== 'function' || typeof urls?.revokeObjectURL !== 'function' || typeof Archivo !== 'function' || typeof programar !== 'function') {
    throw new Error('Este navegador no permite descargar el CSV.');
  }
  let enlace;
  let url;
  try {
    const contenido = new Archivo([archivo.contenido], { type: 'text/csv;charset=utf-8' });
    url = urls.createObjectURL(contenido);
    enlace = documento.createElement('a');
    enlace.href = url;
    enlace.download = archivo.nombreArchivo;
    enlace.hidden = true;
    documento.body.appendChild(enlace);
    enlace.click();
  } finally {
    try { enlace?.remove(); }
    finally {
      // Se deja al navegador iniciar la descarga antes de liberar el Blob URL.
      if (url) programar(() => urls.revokeObjectURL(url), 1000);
    }
  }
}
