import { ESTADOS_MANTENIMIENTO, TIPOS_MANTENIMIENTO, errorFormulario, idPositivoMantenimiento, rangoFechas } from './mantenimientos.js';

export function filtrosReportes(form) {
  const params = rangoFechas(form);
  for (const campo of ['idSede', 'idArea', 'idLaboratorio']) if (String(form[campo] ?? '').trim()) params[campo] = idPositivoMantenimiento(form[campo], campo);
  for (const [campo, permitidos] of [
    ['estado', ['OPERATIVO', 'MANTENIMIENTO', 'INOPERATIVO', 'BAJA']],
    ['estadoMantenimiento', ESTADOS_MANTENIMIENTO], ['tipoMantenimiento', TIPOS_MANTENIMIENTO]
  ]) {
    if (form[campo]) {
      if (!permitidos.includes(form[campo])) throw errorFormulario('Selecciona un filtro de reporte válido.');
      params[campo] = form[campo];
    }
  }
  return params;
}

const totalValido = valor => Number.isSafeInteger(valor) && valor >= 0;
export function validarConteos(data) {
  if (!Array.isArray(data) || data.some(item => typeof item?.valor !== 'string' || !item.valor || !totalValido(item.total))) throw new Error('El reporte de conteos no es válido.');
  return data;
}

export function validarResumenReporte(data) {
  if (!data || ['totalEquipos', 'totalMovimientos', 'totalMantenimientos'].some(campo => !totalValido(data[campo]))) throw new Error('El resumen de reportes no es válido.');
  validarConteos(data.equiposPorEstado);
  validarConteos(data.mantenimientosPorEstado);
  return data;
}

export function validarEquiposPorLaboratorio(data) {
  if (!Array.isArray(data) || data.some(item => !Number.isInteger(item?.idLaboratorio) || item.idLaboratorio < 1 ||
      typeof item.codigo !== 'string' || typeof item.nombre !== 'string' || !totalValido(item.total))) throw new Error('El reporte de laboratorios no es válido.');
  return data;
}

export function validarReporteMovimientos(data) {
  if (!data || !totalValido(data.total)) throw new Error('El reporte de movimientos no es válido.');
  validarConteos(data.porTipo);
  return data;
}

export function validarReporteMantenimientos(data) {
  if (!data || !totalValido(data.total)) throw new Error('El reporte de mantenimientos no es válido.');
  validarConteos(data.porEstado);
  validarConteos(data.porTipo);
  return data;
}

// Un grupo ausente en una agregación válida equivale a cero registros de ese grupo.
export const conteoReportado = (conteos, valor) => validarConteos(conteos).find(item => item.valor === valor)?.total ?? 0;
