import { errorFormulario, fechaLocalValida, rangoFechas } from './mantenimientos.js';

export const ACCIONES_AUDITORIA = Object.freeze([
  'CREAR', 'EDITAR', 'ACTIVAR', 'DESACTIVAR', 'CAMBIAR_ROL', 'RESTABLECER_CONTRASENA',
  'ASIGNAR_LABORATORIOS', 'TRASLADAR', 'INICIAR_MANTENIMIENTO',
  'COMPLETAR_MANTENIMIENTO', 'CANCELAR_MANTENIMIENTO'
]);

export const ENTIDADES_AUDITORIA = Object.freeze([
  'categoria', 'subcategoria', 'sede', 'area', 'laboratorio', 'usuario',
  'usuario_laboratorio', 'equipo', 'mantenimiento'
]);

const acciones = {
  CREAR: 'Crear', EDITAR: 'Editar', ACTIVAR: 'Activar', DESACTIVAR: 'Desactivar',
  CAMBIAR_ROL: 'Cambiar rol', RESTABLECER_CONTRASENA: 'Restablecer contraseña',
  ASIGNAR_LABORATORIOS: 'Asignar laboratorios', TRASLADAR: 'Trasladar equipo',
  INICIAR_MANTENIMIENTO: 'Iniciar mantenimiento', COMPLETAR_MANTENIMIENTO: 'Completar mantenimiento',
  CANCELAR_MANTENIMIENTO: 'Cancelar mantenimiento'
};
const entidades = {
  categoria: 'Categoría', subcategoria: 'Subcategoría', sede: 'Sede', area: 'Área',
  laboratorio: 'Laboratorio', usuario: 'Usuario', usuario_laboratorio: 'Asignaciones de laboratorios',
  equipo: 'Equipo', mantenimiento: 'Mantenimiento'
};

export const etiquetaAccionAuditoria = valor => acciones[valor] || valor || '—';
export const etiquetaEntidadAuditoria = valor => entidades[valor] || valor || '—';
export const registroAuditoria = evento => evento.entidad === 'usuario_laboratorio'
  ? `Asignaciones del usuario #${evento.idEntidad}`
  : `${etiquetaEntidadAuditoria(evento.entidad)} #${evento.idEntidad}`;

const idValido = valor => Number.isSafeInteger(valor) && valor > 0 && valor <= 2147483647;
const vacio = valor => valor == null || (typeof valor === 'string' && !valor.trim());

function idFiltro(valor, nombre) {
  const contenido = typeof valor === 'string' ? valor.trim() : typeof valor === 'number' ? String(valor) : '';
  const id = Number(contenido);
  if (!/^\d+$/.test(contenido) || !idValido(id)) {
    throw errorFormulario(`${nombre} debe ser un ID entero mayor que cero y no superar 2147483647.`);
  }
  return id;
}

export function filtrosAuditoria(form = {}) {
  const params = rangoFechas(form);
  for (const [campo, nombre] of [['idEntidad', 'El registro'], ['idUsuario', 'El usuario actor']]) {
    if (!vacio(form[campo])) params[campo] = idFiltro(form[campo], nombre);
  }
  for (const [campo, permitidos, mensaje] of [
    ['entidad', ENTIDADES_AUDITORIA, 'Selecciona un tipo de registro válido.'],
    ['accion', ACCIONES_AUDITORIA, 'Selecciona una acción de auditoría válida.']
  ]) {
    if (!vacio(form[campo])) {
      const valor = typeof form[campo] === 'string' ? form[campo].trim() : form[campo];
      if (!permitidos.includes(valor)) throw errorFormulario(mensaje);
      params[campo] = valor;
    }
  }
  return params;
}

const patronInstante = /^(\d{4}-\d{2}-\d{2})T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{1,9})?(?:Z|[+-](?:(?:0\d|1[0-7]):[0-5]\d|18:00))$/;

export function fechaAuditoriaValida(valor) {
  if (typeof valor !== 'string') return false;
  const partes = valor.match(patronInstante);
  return Boolean(partes && fechaLocalValida(partes[1]) && Number.isFinite(Date.parse(valor)));
}

export function validarAuditoria(data) {
  const textoValido = (valor, maximo) => typeof valor === 'string' && valor.trim().length > 0 && valor.length <= maximo;
  if (!Array.isArray(data) || data.some(item => !item || Array.isArray(item) ||
      !idValido(item.id) || !(item.idUsuarioActor === null || idValido(item.idUsuarioActor)) ||
      !textoValido(item.userNameActor, 50) || !ACCIONES_AUDITORIA.includes(item.accion) ||
      !ENTIDADES_AUDITORIA.includes(item.entidad) || !idValido(item.idEntidad) ||
      !textoValido(item.cambios, 2000) || !fechaAuditoriaValida(item.fecha)) ||
      new Set(data.map(item => item.id)).size !== data.length) {
    throw Object.assign(new Error('El servidor devolvió una respuesta de auditoría inválida. Intenta actualizar los eventos.'),
      { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
  }
  return data;
}

const formatoLima = new Intl.DateTimeFormat('es-PE', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
});

export function mostrarFechaAuditoria(valor) {
  return fechaAuditoriaValida(valor) ? formatoLima.format(new Date(valor)) : '—';
}
