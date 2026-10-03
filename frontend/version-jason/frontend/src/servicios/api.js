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
  if (estado === 403) return 'No tienes permiso para realizar esta operación.';
  if (estado >= 500) return 'El servidor no pudo completar la operación. Intenta nuevamente.';
  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') return 'El servidor está tardando en responder. Intenta nuevamente.';
  if (!estado && (error?.request || error?.code === 'ERR_NETWORK')) return 'No se pudo conectar con el servidor. Revisa la conexión e intenta nuevamente.';
  if ([400, 404, 409, 422].includes(estado)) {
    const textoSeguro = (valor) => typeof valor === 'string' && valor.length <= 300 && !/[<>\r\n]/.test(valor);
    const errores = error.response?.data?.errors;
    const campos = errores && typeof errores === 'object' && !Array.isArray(errores)
      ? Object.values(errores).filter(textoSeguro).slice(0, 3) : [];
    if (campos.length) return campos.join(' ');
    if (textoSeguro(error.response?.data?.message)) return error.response.data.message;
  }
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
  const usuario = data?.usuario;
  if (!textoValido(data?.accessToken) || typeof data.tokenType !== 'string' || data.tokenType.toLowerCase() !== 'bearer' ||
      !Number.isFinite(data.expiresIn) || data.expiresIn <= 0 || !idValido(usuario?.id) ||
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
  const cancelar = () => {
    operacion += 1;
    pendiente?.abort();
    pendiente = null;
    establecerToken(null);
  };
  const logout = (mensajeSesion = '') => {
    cancelar();
    limpiarSesionLegacy();
    if (activo) publicar({ ...vacia(), mensajeSesion });
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
    async login(userName, password) {
      if (!activo) throw new axios.CanceledError();
      cancelar();
      const numero = operacion;
      const solicitud = new AbortController();
      pendiente = solicitud;
      publicar({ ...vacia(), cargando: true });
      const sigueVigente = () => activo && numero === operacion && !solicitud.signal.aborted;
      try {
        const { data } = await cliente.login({ userName: userName.trim(), password }, { signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const usuario = validarLogin(data);
        establecerToken(data.accessToken);
        const { data: laboratorios } = await cliente.laboratorios({ signal: solicitud.signal });
        if (!sigueVigente()) throw new axios.CanceledError();
        const alcance = validarAlcance(laboratorios, usuario);
        publicar({ token: data.accessToken, usuario, alcance, cargando: false, mensajeSesion: '' });
        return data;
      } catch (error) {
        if (sigueVigente()) {
          establecerToken(null);
          publicar(vacia());
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
  eliminarLaboratorio: (id) => api.delete(`/laboratorios/${id}`)
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
  laboratorios: (id) => api.get(`/admin/usuarios/${id}/laboratorios`),
  actualizarLaboratorios: (id, data) => api.put(`/admin/usuarios/${id}/laboratorios`, data)
};

export default api;
