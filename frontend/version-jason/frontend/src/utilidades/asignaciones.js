const idValido = value => Number.isSafeInteger(value) && value > 0;

function idUsuario(value) {
  const id = Number(value);
  if (!idValido(id)) throw new Error('Ingresa un ID de usuario entero y mayor que cero.');
  return id;
}

export function prepararGuardadoAsignaciones({ idActual, idCargado, usuario, ids }) {
  const actual = idUsuario(idActual);
  if (!idValido(idCargado) || actual !== idCargado || usuario?.id !== idCargado) {
    throw new Error('Vuelve a consultar el usuario antes de guardar sus asignaciones.');
  }
  if (!Array.isArray(ids) || ids.some(id => !idValido(id))) {
    throw new Error('La selección de laboratorios no es válida. Vuelve a consultar el usuario.');
  }
  return { idUsuario: idCargado, payload: { idsLaboratorio: [...new Set(ids)] } };
}

export function validarRespuestaAsignaciones(data, idEsperado) {
  if (data?.usuario?.id !== idEsperado || !Array.isArray(data.laboratorios) ||
      data.laboratorios.some(lab => !idValido(lab?.id))) {
    throw new Error('La respuesta de asignaciones no corresponde al usuario consultado.');
  }
}

// Cada solicitud conserva una revisión independiente del render de React.
export function crearControlAsignaciones() {
  let idActual = '';
  let revision = 0;
  let pendiente = null;
  return {
    cambiarId(value) {
      idActual = value;
      revision += 1;
      pendiente = null;
    },
    invalidar() {
      revision += 1;
      pendiente = null;
    },
    iniciar() {
      if (pendiente) return null;
      pendiente = { id: idUsuario(idActual), revision };
      return pendiente;
    },
    vigente(solicitud) {
      return Boolean(solicitud && pendiente === solicitud && solicitud.revision === revision);
    },
    finalizar(solicitud) {
      if (pendiente === solicitud) pendiente = null;
    }
  };
}
