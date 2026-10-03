const texto = (value) => String(value ?? '').trim();
const opcional = (value) => texto(value) || null;
export function errorValidacion(message) {
  return Object.assign(new Error(message), { code: 'VALIDACION_LOCAL' });
}
const idPositivo = (value, campo) => {
  const id = Number(value);
  if (!Number.isInteger(id) || id <= 0) throw errorValidacion(`Selecciona ${campo}.`);
  return id;
};

export function payloadEquipo(form, editar = false) {
  if (!['OPERATIVO', 'MANTENIMIENTO', 'INOPERATIVO'].includes(form.estado)) throw errorValidacion('Selecciona un estado válido para el equipo.');
  const payload = {
    nombre: texto(form.nombre), serieUtec: opcional(form.serieUtec), numeroSerie: opcional(form.numeroSerie),
    marca: opcional(form.marca), modelo: opcional(form.modelo), estado: form.estado,
    anio: texto(form.anio) ? Number(form.anio) : null, ordenCompra: opcional(form.ordenCompra),
    ubicacionInterna: opcional(form.ubicacionInterna), comentario: opcional(form.comentario),
    requiereMantenimiento: Boolean(form.requiereMantenimiento),
    idSubcategoria: idPositivo(form.idSubcategoria, 'una subcategoría'),
    idResponsable: texto(form.idResponsable) ? idPositivo(form.idResponsable, 'un responsable válido') : null
  };
  if (!editar) {
    payload.codigoInterno = texto(form.codigoInterno);
    payload.idLaboratorio = idPositivo(form.idLaboratorio, 'un laboratorio');
  }
  return payload;
}

export function payloadCatalogo(tipo, form) {
  const payload = { nombre: texto(form.nombre), descripcion: opcional(form.descripcion) };
  if (tipo === 'subcategorias') payload.idCategoria = idPositivo(form.idCategoria, 'una categoría');
  return payload;
}

export function payloadOrganizacion(tipo, form) {
  const payload = { nombre: texto(form.nombre) };
  if (tipo === 'sedes') {
    payload.direccion = opcional(form.direccion); payload.distrito = opcional(form.distrito); payload.departamento = opcional(form.departamento);
  } else if (tipo === 'areas') {
    payload.descripcion = opcional(form.descripcion); payload.idSede = idPositivo(form.idSede, 'una sede');
  } else {
    payload.codigo = texto(form.codigo); payload.ubicacion = opcional(form.ubicacion); payload.idArea = idPositivo(form.idArea, 'un área');
  }
  return payload;
}

export function laboratoriosPermitidos(laboratorios, alcance, esAdmin) {
  const ids = new Set((alcance?.laboratorios || []).map((lab) => lab.id));
  return laboratorios.filter((lab) => lab?.activo === true && (esAdmin || ids.has(lab.id)));
}

export const lista = (value) => Array.isArray(value) ? value.filter(Boolean) : [];
