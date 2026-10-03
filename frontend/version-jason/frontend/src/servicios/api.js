import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env?.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000
});

let tokenSesion = null;
let versionSesion = 0;
const alExpirar = new Set();

function establecerToken(token) {
  tokenSesion = token;
  versionSesion += 1;
}

export function limpiarSesionLegacy() {
  for (const nombre of ['localStorage', 'sessionStorage']) {
    try {
      const storage = globalThis[nombre];
      storage?.removeItem('inventario_token');
      storage?.removeItem('inventario_usuario');
    } catch { /* La sesión en memoria funciona aunque el almacenamiento esté bloqueado. */ }
  }
}

api.interceptors.request.use((config) => {
  const esLogin = config.sinAutenticacion || config.url === '/auth/login';
  config.versionSesion = versionSesion;
  config.conSesion = Boolean(tokenSesion) && !esLogin;
  if (config.conSesion) config.headers.set('Authorization', `Bearer ${tokenSesion}`);
  else config.headers.delete('Authorization');
  return config;
}, undefined, { synchronous: true });

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && error.config?.conSesion &&
        error.config.versionSesion === versionSesion) {
      establecerToken(null);
      alExpirar.forEach(notificar => notificar());
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: (data, config = {}) => api.post('/auth/login', data, { ...config, sinAutenticacion: true }),
  me: (config = {}) => api.get('/auth/me', config),
  laboratorios: (config = {}) => api.get('/auth/me/laboratorios', config)
};

export const esCancelacion = (error) => axios.isCancel(error) || error?.name === 'AbortError';

export function mensajeError(error, alternativa = 'No se pudo completar la operación.') {
  if (esCancelacion(error)) return 'La operación fue cancelada.';
  if (error?.code === 'SESION_INVALIDA') return 'El servidor devolvió una sesión inválida. Intenta iniciar sesión nuevamente.';
  const estado = error?.response?.status;
  if (estado === 401) return error.config?.url === '/auth/login'
    ? 'Usuario o contraseña incorrectos.' : 'La sesión expiró. Inicia sesión nuevamente.';
  if (estado >= 500) return 'El servidor no pudo completar la operación. Intenta nuevamente.';
  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') return 'El servidor está tardando en responder. Intenta nuevamente.';
  if (!estado && (error?.request || error?.code === 'ERR_NETWORK')) return 'No se pudo conectar con el servidor. Revisa la conexión e intenta nuevamente.';
  if (error?.code === 'VALIDACION_LOCAL') return error.message;
  if ([400, 403, 404, 409, 422].includes(estado)) {
    const textoSeguro = (valor) => typeof valor === 'string' && valor.length <= 300 && !/[<>\r\n]/.test(valor);
    const errores = error.response?.data?.errors;
    const campos = errores && typeof errores === 'object' && !Array.isArray(errores)
      ? Object.values(errores).filter(textoSeguro).slice(0, 3) : [];
    if (campos.length) return campos.join(' ');
    if (textoSeguro(error.response?.data?.message)) return error.response.data.message;
  }
  if (estado === 403) return 'No tienes permiso para realizar esta operación.';
  return alternativa;
}

const vacia = () => ({ token: null, usuario: null, alcance: null, cargando: false, mensajeSesion: '' });
const idValido = (id) => Number.isInteger(id) && id > 0;
const textoValido = (texto) => typeof texto === 'string' && texto.trim().length > 0;

function sesionInvalida() {
  const error = new Error('Respuesta de autenticación inválida.');
  error.code = 'SESION_INVALIDA';
  return error;
}

function validarLogin(data) {
  if (!textoValido(data?.accessToken) || typeof data.tokenType !== 'string' || data.tokenType.toLowerCase() !== 'bearer' ||
      !Number.isFinite(data.expiresIn) || data.expiresIn <= 0) throw sesionInvalida();
  return validarUsuario(data.usuario);
}

function validarUsuario(usuario) {
  if (!idValido(usuario?.id) ||
      !textoValido(usuario.userName) || usuario.activo !== true ||
      !['ADMIN', 'GESTOR', 'LECTOR'].includes(usuario.rol)) throw sesionInvalida();
  return usuario;
}

function validarAlcance(data, usuario) {
  if (!data || typeof data.alcanceGlobal !== 'boolean' || !Array.isArray(data.laboratorios) ||
      data.alcanceGlobal !== (usuario.rol === 'ADMIN') ||
      data.laboratorios.some(lab => !idValido(lab?.id) || !textoValido(lab.codigo) || !textoValido(lab.nombre)) ||
      new Set(data.laboratorios.map(lab => lab.id)).size !== data.laboratorios.length) throw sesionInvalida();
  return data;
}

// Coordina las respuestas asíncronas antes de publicar una sesión completa a React.
export function crearControlSesion(publicar, cliente = authApi) {
  let operacion = 0;
  let pendiente = null;
  let activo = false;
  let sesionActual = vacia();
  const publicarEstado = (estado) => {
    sesionActual = estado;
    publicar(estado);
  };
  const cancelar = () => {
    operacion += 1;
    pendiente?.abort();
    pendiente = null;
    establecerToken(null);
  };
  const logout = (mensajeSesion = '') => {
    cancelar();
    limpiarSesionLegacy();
    if (activo) publicarEstado({ ...vacia(), mensajeSesion });
  };
  const expirar = () => logout('La sesión expiró. Inicia sesión nuevamente.');
  return {
    activar() {
      activo = true;
      limpiarSesionLegacy();
      alExpirar.add(expirar);
    },
    desactivar() {
      activo = false;
      alExpirar.delete(expirar);
      cancelar();
    },
    logout,
    cancelarLogin() {
      if (pendiente) logout();
    },
    async refrescarSesion() {
      if (!activo || !sesionActual.token) throw new axios.CanceledError();
      pendiente?.abort();
      const numero = ++operacion;
      const solicitud = new AbortController();
      pendiente = solicitud;
      const token = sesionActual.token;
      const sigueVigente = () => activo && numero === operacion && !solicitud.signal.aborted;
      try {
        const { data } = await cliente.me({ signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const usuario = validarUsuario(data);
        const { data: laboratorios } = await cliente.laboratorios({ signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const alcance = validarAlcance(laboratorios, usuario);
        publicarEstado({ token, usuario, alcance, cargando: false, mensajeSesion: '' });
        return usuario;
      } catch (error) {
        if (sigueVigente()) logout('Vuelve a iniciar sesión para comprobar tus permisos actuales.');
        throw error;
      } finally {
        if (numero === operacion) pendiente = null;
      }
    },
    async login(userName, password) {
      if (!activo) throw new axios.CanceledError();
      cancelar();
      const numero = operacion;
      const solicitud = new AbortController();
      pendiente = solicitud;
      publicarEstado({ ...vacia(), cargando: true });
      const sigueVigente = () => activo && numero === operacion && !solicitud.signal.aborted;
      try {
        const { data } = await cliente.login({ userName: userName.trim(), password }, { signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const usuario = validarLogin(data);
        establecerToken(data.accessToken);
        const { data: laboratorios } = await cliente.laboratorios({ signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const alcance = validarAlcance(laboratorios, usuario);
        publicarEstado({ token: data.accessToken, usuario, alcance, cargando: false, mensajeSesion: '' });
        return data;
      } catch (error) {
        if (sigueVigente()) {
          establecerToken(null);
          publicarEstado(vacia());
        }
        throw error;
      } finally {
        if (numero === operacion) pendiente = null;
      }
    }
  };
}

export const catalogoApi = {
  categorias: () => api.get('/categorias'),
  crearCategoria: (data) => api.post('/categorias', data),
  actualizarCategoria: (id, data) => api.put(`/categorias/${id}`, data),
  eliminarCategoria: (id) => api.delete(`/categorias/${id}`),
  subcategorias: () => api.get('/subcategorias'),
  crearSubcategoria: (data) => api.post('/subcategorias', data),
  actualizarSubcategoria: (id, data) => api.put(`/subcategorias/${id}`, data),
  eliminarSubcategoria: (id) => api.delete(`/subcategorias/${id}`),
  sedes: () => api.get('/sedes'),
  crearSede: (data) => api.post('/sedes', data),
  actualizarSede: (id, data) => api.put(`/sedes/${id}`, data),
  eliminarSede: (id) => api.delete(`/sedes/${id}`),
  areas: () => api.get('/areas'),
  crearArea: (data) => api.post('/areas', data),
  actualizarArea: (id, data) => api.put(`/areas/${id}`, data),
  eliminarArea: (id) => api.delete(`/areas/${id}`),
  laboratorios: () => api.get('/laboratorios'),
  crearLaboratorio: (data) => api.post('/laboratorios', data),
  actualizarLaboratorio: (id, data) => api.put(`/laboratorios/${id}`, data),
  eliminarLaboratorio: (id) => api.delete(`/laboratorios/${id}`),
  listarCategoriasAdmin: (params = {}) => api.get('/admin/categorias', { params: parametrosPermitidos(params, ['activo']) }),
  listarSubcategoriasAdmin: (params = {}) => api.get('/admin/subcategorias', { params: parametrosPermitidos(params, ['activo']) }),
  listarSedesAdmin: (params = {}) => api.get('/admin/sedes', { params: parametrosPermitidos(params, ['activo']) }),
  listarAreasAdmin: (params = {}) => api.get('/admin/areas', { params: parametrosPermitidos(params, ['activo']) }),
  listarLaboratoriosAdmin: (params = {}) => api.get('/admin/laboratorios', { params: parametrosPermitidos(params, ['activo']) }),
  cambiarEstadoCategoria: (id, activo) => api.patch(`/categorias/${id}/estado`, { activo }),
  cambiarEstadoSubcategoria: (id, activo) => api.patch(`/subcategorias/${id}/estado`, { activo }),
  cambiarEstadoSede: (id, activo) => api.patch(`/sedes/${id}/estado`, { activo }),
  cambiarEstadoArea: (id, activo) => api.patch(`/areas/${id}/estado`, { activo }),
  cambiarEstadoLaboratorio: (id, activo) => api.patch(`/laboratorios/${id}/estado`, { activo })
};

export const equiposApi = {
  listar: (params = {}) => api.get('/equipos', { params }),
  detalle: (id) => api.get(`/equipos/${id}`),
  crear: (data) => api.post('/equipos', data),
  actualizar: (id, data) => api.put(`/equipos/${id}`, data),
  baja: (id) => api.delete(`/equipos/${id}`),
  listarAdmin: (params = {}) => api.get('/admin/equipos', { params })
};

export const movimientosApi = {
  listar: (params = {}) => api.get('/movimientos', { params }),
  equipo: (id) => api.get(`/equipos/${id}/movimientos`),
  trasladar: (id, data) => api.post(`/equipos/${id}/traslados`, data)
};

export const usuariosApi = {
  listar: () => api.get('/admin/usuarios'),
  detalle: (id) => api.get(`/admin/usuarios/${id}`),
  crear: (data) => api.post('/admin/usuarios', data),
  actualizar: (id, data) => api.put(`/admin/usuarios/${id}`, data),
  cambiarRol: (id, rol) => api.patch(`/admin/usuarios/${id}/rol`, { rol }),
  cambiarEstado: (id, activo) => api.patch(`/admin/usuarios/${id}/estado`, { activo }),
  cambiarPassword: (id, password) => api.put(`/admin/usuarios/${id}/password`, { password }),
  laboratorios: (id) => api.get(`/admin/usuarios/${id}/laboratorios`),
  actualizarLaboratorios: (id, data) => api.put(`/admin/usuarios/${id}/laboratorios`, data)
};

function parametrosPermitidos(params, campos) {
  return Object.fromEntries(campos.filter(campo => params[campo] !== undefined && params[campo] !== null && params[campo] !== '')
    .map(campo => [campo, params[campo]]));
}

const filtrosMantenimiento = ['idEquipo', 'idLaboratorio', 'estado', 'tipo', 'fechaDesde', 'fechaHasta'];
const filtrosReporte = ['idSede', 'idArea', 'idLaboratorio', 'estado', 'fechaDesde', 'fechaHasta', 'estadoMantenimiento', 'tipoMantenimiento'];

export const mantenimientosApi = {
  listar: (params = {}, config = {}) => api.get('/mantenimientos', { ...config, params: parametrosPermitidos(params, filtrosMantenimiento) }),
  detalle: (id) => api.get(`/mantenimientos/${id}`),
  crear: (data) => api.post('/mantenimientos', data),
  actualizar: (id, data) => api.put(`/mantenimientos/${id}`, data),
  cambiarEstado: (id, data) => api.patch(`/mantenimientos/${id}/estado`, data)
};

export const reportesApi = {
  resumen: (params = {}, config = {}) => api.get('/reportes/resumen', { ...config, params: parametrosPermitidos(params, filtrosReporte) }),
  equiposPorEstado: (params = {}, config = {}) => api.get('/reportes/equipos/por-estado', { ...config, params: parametrosPermitidos(params, filtrosReporte) }),
  equiposPorLaboratorio: (params = {}, config = {}) => api.get('/reportes/equipos/por-laboratorio', { ...config, params: parametrosPermitidos(params, filtrosReporte) }),
  movimientos: (params = {}, config = {}) => api.get('/reportes/movimientos', { ...config, params: parametrosPermitidos(params, filtrosReporte) }),
  mantenimientos: (params = {}, config = {}) => api.get('/reportes/mantenimientos', { ...config, params: parametrosPermitidos(params, filtrosReporte) })
};

export default api;
