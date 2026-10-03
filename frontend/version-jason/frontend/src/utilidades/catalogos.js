import { lista, payloadCatalogo, payloadOrganizacion } from './equipos.js';

const metodos = {
  categorias: 'Categoria', subcategorias: 'Subcategoria', sedes: 'Sede',
  areas: 'Area', laboratorios: 'Laboratorio'
};
const listadosAdmin = {
  categorias: 'listarCategoriasAdmin', subcategorias: 'listarSubcategoriasAdmin',
  sedes: 'listarSedesAdmin', areas: 'listarAreasAdmin', laboratorios: 'listarLaboratoriosAdmin'
};

function errorValidacion(message) {
  return Object.assign(new Error(message), { code: 'VALIDACION_LOCAL' });
}

export function estadoLaboratorio(item) {
  if (item.activo !== true) return 'INACTIVO';
  return item.estadoOperativo === 'MANTENIMIENTO' ? 'MANTENIMIENTO' : 'OPERATIVO';
}

export function etiquetaEstadoLaboratorio(item) {
  return { OPERATIVO: 'Activo', MANTENIMIENTO: 'En mantenimiento', INACTIVO: 'Inactivo' }[estadoLaboratorio(item)];
}

export function formularioCatalogo(tipo, item = null) {
  const form = {
    id: item?.id, nombre: item?.nombre ?? '', descripcion: item?.descripcion ?? '',
    activo: item ? item.activo === true : true
  };
  if (tipo === 'subcategorias') form.idCategoria = item?.categoria?.id ?? '';
  if (tipo === 'sedes') Object.assign(form, {
    direccion: item?.direccion ?? '', distrito: item?.distrito ?? '', departamento: item?.departamento ?? ''
  });
  if (tipo === 'areas') form.idSede = item?.sede?.id ?? '';
  if (tipo === 'laboratorios') Object.assign(form, {
    codigo: item?.codigo ?? '', ubicacion: item?.ubicacion ?? '', idArea: item?.area?.id ?? '',
    estadoLaboratorio: item ? estadoLaboratorio(item) : 'OPERATIVO',
    estadoOperativo: item?.estadoOperativo ?? 'OPERATIVO'
  });
  return form;
}

export async function cargarCatalogos(cliente, esAdmin, tipos) {
  const respuestas = await Promise.all(tipos.map(tipo => cliente[esAdmin ? listadosAdmin[tipo] : tipo]()));
  return Object.fromEntries(tipos.map((tipo, index) => [tipo, lista(respuestas[index].data)]));
}

function activoSolicitado(tipo, form) {
  if (tipo === 'laboratorios') {
    if (!['OPERATIVO', 'MANTENIMIENTO', 'INACTIVO'].includes(form.estadoLaboratorio)) {
      throw errorValidacion('Selecciona un estado válido para el laboratorio.');
    }
    return form.estadoLaboratorio !== 'INACTIVO';
  }
  if (typeof form.activo !== 'boolean') throw errorValidacion('Selecciona Activo o Inactivo.');
  return form.activo;
}

export function payloadEdicionCatalogo(tipo, form) {
  if (['categorias', 'subcategorias'].includes(tipo)) return payloadCatalogo(tipo, form);
  if (tipo === 'laboratorios') {
    const estadoOperativo = form.estadoLaboratorio === 'INACTIVO'
      ? form.estadoOperativo : form.estadoLaboratorio;
    return payloadOrganizacion(tipo, { ...form, estadoOperativo });
  }
  return payloadOrganizacion(tipo, form);
}

// Los PUT actuales solo aceptan registros activos. PATCH y PUT son peticiones
// independientes: una operación parcial debe mostrarse y consultarse de nuevo.
export async function guardarCatalogo(cliente, tipo, form, original = null) {
  const metodo = metodos[tipo];
  if (!metodo) throw errorValidacion('El catálogo seleccionado no es válido.');
  const activo = activoSolicitado(tipo, form);
  const payload = payloadEdicionCatalogo(tipo, form);
  const aplicadas = [];
  let registro = original;
  const llamar = async (nombre, ...args) => {
    const respuesta = await cliente[`${nombre}${metodo}`](...args);
    registro = respuesta.data;
    aplicadas.push(nombre);
  };
  try {
    if (!original) {
      await llamar('crear', payload);
      if (!activo) await llamar('cambiarEstado', registro.id, false);
    } else {
      const anterior = payloadEdicionCatalogo(tipo, formularioCatalogo(tipo, original));
      const cambiaDatos = JSON.stringify(payload) !== JSON.stringify(anterior);
      if (!original.activo && !activo) {
        if (cambiaDatos) throw errorValidacion('Reactiva el registro antes de editar sus datos.');
        return { registro: original, aplicadas };
      }
      if (!original.activo) await llamar('cambiarEstado', original.id, true);
      if (cambiaDatos) await llamar('actualizar', original.id, payload);
      if (original.activo && !activo) await llamar('cambiarEstado', original.id, false);
    }
    return { registro, aplicadas };
  } catch (error) {
    error.operacionesAplicadas = [...aplicadas];
    throw error;
  }
}
