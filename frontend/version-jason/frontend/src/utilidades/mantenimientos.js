export const ESTADOS_MANTENIMIENTO = ['PROGRAMADO', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO'];
export const TIPOS_MANTENIMIENTO = ['PREVENTIVO', 'CORRECTIVO', 'CALIBRACION', 'OTRO'];
export const etiquetaMantenimiento = valor => ({ PROGRAMADO: 'Programado', EN_PROCESO: 'En proceso', COMPLETADO: 'Completado', CANCELADO: 'Cancelado', PREVENTIVO: 'Preventivo', CORRECTIVO: 'Correctivo', CALIBRACION: 'Calibración', OTRO: 'Otro' }[valor] || valor || '—');
export const errorFormulario = mensaje => Object.assign(new Error(mensaje), { code: 'VALIDACION_LOCAL' });
const texto = valor => String(valor ?? '').trim();

export function idPositivoMantenimiento(valor, campo) {
  const id = Number(valor);
  if (!Number.isSafeInteger(id) || id < 1 || id > 2147483647) throw errorFormulario(`${campo} debe ser un ID entero mayor que cero.`);
  return id;
}

export function fechaLocalValida(valor) {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valor)) return false;
  const fecha = new Date(`${valor}T00:00:00Z`);
  return Number.isFinite(fecha.getTime()) && fecha.toISOString().slice(0, 10) === valor;
}

export function rangoFechas(filtros) {
  const resultado = {};
  for (const campo of ['fechaDesde', 'fechaHasta']) {
    if (filtros[campo]) {
      if (!fechaLocalValida(filtros[campo])) throw errorFormulario('Selecciona fechas válidas.');
      resultado[campo] = filtros[campo];
    }
  }
  if (resultado.fechaDesde && resultado.fechaHasta && resultado.fechaDesde > resultado.fechaHasta) throw errorFormulario('La fecha hasta debe ser igual o posterior a la fecha desde.');
  return resultado;
}

function observaciones(valor) {
  const resultado = texto(valor);
  if (resultado.length > 4000) throw errorFormulario('Las observaciones no pueden superar 4000 caracteres.');
  return resultado || null;
}

export function payloadMantenimiento(form, editar = false) {
  if (!TIPOS_MANTENIMIENTO.includes(form.tipo)) throw errorFormulario('Selecciona un tipo de mantenimiento válido.');
  const descripcion = texto(form.descripcion);
  if (!descripcion || descripcion.length > 2000) throw errorFormulario('La descripción es obligatoria y admite hasta 2000 caracteres.');
  if (!fechaLocalValida(form.fechaProgramada)) throw errorFormulario('Selecciona una fecha programada válida.');
  const payload = {
    tipo: form.tipo, descripcion, fechaProgramada: form.fechaProgramada,
    idResponsable: texto(form.idResponsable) ? idPositivoMantenimiento(form.idResponsable, 'El responsable') : null,
    observaciones: observaciones(form.observaciones)
  };
  if (!editar) payload.idEquipo = idPositivoMantenimiento(form.idEquipo, 'El equipo');
  return payload;
}

export function transicionesMantenimiento(estado) {
  if (estado === 'PROGRAMADO') return ['EN_PROCESO', 'CANCELADO'];
  if (estado === 'EN_PROCESO') return ['COMPLETADO', 'CANCELADO'];
  return [];
}

export function payloadEstadoMantenimiento(origen, destino, notas) {
  if (!transicionesMantenimiento(origen).includes(destino)) throw errorFormulario('La transición de mantenimiento no está permitida.');
  const contenido = observaciones(notas);
  // PATCH distingue null (conservar) de texto vacío (borrar observaciones).
  return { estado: destino, observaciones: notas == null ? null : contenido ?? '' };
}

export function filtrosMantenimiento(form) {
  const params = rangoFechas(form);
  for (const campo of ['idEquipo', 'idLaboratorio']) if (texto(form[campo])) params[campo] = idPositivoMantenimiento(form[campo], campo === 'idEquipo' ? 'El equipo' : 'El laboratorio');
  for (const [campo, permitidos] of [['estado', ESTADOS_MANTENIMIENTO], ['tipo', TIPOS_MANTENIMIENTO]]) {
    if (form[campo]) {
      if (!permitidos.includes(form[campo])) throw errorFormulario('El filtro de mantenimiento no es válido.');
      params[campo] = form[campo];
    }
  }
  return params;
}

export function puedeGestionarMantenimiento(item, permisos) {
  if (!permisos.puedeGestionar) return false;
  return permisos.esAdmin || (permisos.alcance?.laboratorios || []).some(lab => lab.id === item?.equipo?.laboratorio?.id);
}

export function equiposParaMantenimiento(equipos, laboratorios, permisos) {
  const activos = new Set(laboratorios.filter(lab => lab.activo === true).map(lab => lab.id));
  return equipos.filter(equipo => equipo.estado !== 'BAJA' && activos.has(equipo.laboratorio?.id) &&
    puedeGestionarMantenimiento({ equipo }, permisos));
}

export function validarMantenimientos(data) {
  if (!Array.isArray(data) || data.some(item => !Number.isInteger(item?.id) || item.id < 1 ||
      !Number.isInteger(item.equipo?.id) || !ESTADOS_MANTENIMIENTO.includes(item.estado) ||
      !TIPOS_MANTENIMIENTO.includes(item.tipo) || !fechaLocalValida(item.fechaProgramada))) throw new Error('La respuesta de mantenimientos no es válida.');
  return data;
}

export function mostrarFechaProgramada(fecha) {
  return fechaLocalValida(fecha) ? fecha.split('-').reverse().join('/') : '—';
}

export function mostrarFechaInstante(fecha) {
  if (!fecha) return '—';
  const valor = new Date(fecha);
  return Number.isFinite(valor.getTime()) ? valor.toLocaleString('es-PE') : '—';
}
