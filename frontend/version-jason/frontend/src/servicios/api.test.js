import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';
import api, { authApi, crearControlSesion, esCancelacion, limpiarSesionLegacy, mensajeError } from './api.js';

const respuestaLogin = (id = 1, rol = 'GESTOR') => ({
  accessToken: `token-ficticio-${id}`, tokenType: 'Bearer', expiresIn: 3600,
  usuario: { id, userName: `prueba-${id}`, nombre: 'Prueba', apellido: 'Local', rol, activo: true }
});
const alcance = { alcanceGlobal: false, laboratorios: [{ id: 1, codigo: 'LAB-TEST', nombre: 'Laboratorio de prueba' }] };
const diferida = () => {
  let resolve, reject;
  const promise = new Promise((si, no) => { resolve = si; reject = no; });
  return { promise, resolve, reject };
};
const respuesta = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const falloHttp = (config, status) => new axios.AxiosError('Fallo simulado', 'ERR_BAD_RESPONSE', config, {}, {
  config, status, data: { message: 'Detalle interno no publicable' }, headers: {}
});
function controlar(t, cliente) {
  const estados = [];
  const control = crearControlSesion(estado => estados.push(estado), cliente);
  control.activar();
  t.after(() => control.desactivar());
  return { control, estados, ultimo: () => estados.at(-1) };
}
function adaptar(t, funcion) {
  const original = api.defaults.adapter;
  api.defaults.adapter = funcion;
  t.after(() => { api.defaults.adapter = original; });
}

test('publica token, perfil y alcance juntos y conserva los espacios de contraseña', async t => {
  const labs = diferida();
  let credenciales;
  const { control, estados, ultimo } = controlar(t, {
    login: async datos => { credenciales = datos; return { data: respuestaLogin() }; },
    laboratorios: () => labs.promise
  });
  const login = control.login(' prueba-1 ', ' clave con espacios ');
  await Promise.resolve();
  assert.equal(ultimo().token, null);
  assert.equal(ultimo().cargando, true);
  assert.equal(credenciales.userName, 'prueba-1');
  assert.equal(credenciales.password, ' clave con espacios ');
  labs.resolve({ data: alcance });
  await login;
  assert.equal(ultimo().usuario.id, 1);
  assert.equal(ultimo().alcance, alcance);
  assert.ok(estados.every(estado => !estado.token || (estado.usuario && estado.alcance && !estado.cargando)));
});

test('rechaza perfiles inválidos y alcance incoherente con el rol', async t => {
  for (const data of [null, {}, { ...respuestaLogin(), usuario: { ...respuestaLogin().usuario, activo: false } },
    { ...respuestaLogin(), usuario: { ...respuestaLogin().usuario, rol: 'OTRO' } }]) {
    const { control, ultimo } = controlar(t, { login: async () => ({ data }), laboratorios: async () => ({ data: alcance }) });
    await assert.rejects(control.login('prueba', 'clave'), { code: 'SESION_INVALIDA' });
    assert.equal(ultimo().token, null);
    control.desactivar();
  }
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin(1, 'ADMIN') }), laboratorios: async () => ({ data: alcance })
  });
  await assert.rejects(control.login('prueba', 'clave'), { code: 'SESION_INVALIDA' });
  assert.equal(ultimo().usuario, null);
});

test('un fallo de alcance limpia el token provisional y deja la sesión cerrada', async t => {
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin() }),
    laboratorios: async () => { throw { response: { status: 500 } }; }
  });
  await assert.rejects(control.login('prueba', 'clave'));
  assert.equal(ultimo().token, null);
  adaptar(t, async config => {
    assert.equal(config.headers.get('Authorization'), undefined);
    return respuesta(config, []);
  });
  await api.get('/equipos');
});

test('logout durante login impide que una respuesta tardía restaure la sesión', async t => {
  const solicitud = diferida();
  let alcanceConsultado = false;
  const { control, ultimo } = controlar(t, {
    login: () => solicitud.promise,
    laboratorios: async () => { alcanceConsultado = true; return { data: alcance }; }
  });
  const login = control.login('prueba', 'clave');
  const rechazado = assert.rejects(login, esCancelacion);
  control.logout();
  solicitud.resolve({ data: respuestaLogin() });
  await rechazado;
  assert.equal(ultimo().token, null);
  assert.equal(alcanceConsultado, false);
});

test('cancelar mientras llega el alcance no publica una sesión', async t => {
  const labs = diferida();
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin() }), laboratorios: () => labs.promise
  });
  const login = control.login('prueba', 'clave');
  const rechazado = assert.rejects(login, esCancelacion);
  await Promise.resolve();
  control.cancelarLogin();
  labs.resolve({ data: alcance });
  await rechazado;
  assert.equal(ultimo().token, null);
});

test('un login anterior no borra ni reemplaza un login posterior', async t => {
  const anterior = diferida();
  let numero = 0;
  const { control, ultimo } = controlar(t, {
    login: () => ++numero === 1 ? anterior.promise : Promise.resolve({ data: respuestaLogin(2) }),
    laboratorios: async () => ({ data: alcance })
  });
  const viejo = control.login('primero', 'clave');
  const rechazado = assert.rejects(viejo);
  await control.login('segundo', 'clave');
  anterior.reject(new Error('Respuesta tardía'));
  await rechazado;
  assert.equal(ultimo().usuario.id, 2);
});

test('401 antiguo no cierra sesión nueva; 403 la conserva; 401 vigente la cierra', async t => {
  let numero = 0;
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin(++numero) }), laboratorios: async () => ({ data: alcance })
  });
  await control.login('primero', 'clave');
  const antiguo = diferida();
  let configAntigua;
  adaptar(t, config => {
    if (config.url === '/antigua') { configAntigua = config; return antiguo.promise; }
    return Promise.reject(falloHttp(config, config.url === '/prohibida' ? 403 : 401));
  });
  const solicitudAntigua = api.get('/antigua');
  const rechazoAntiguo = assert.rejects(solicitudAntigua);
  await control.login('segundo', 'clave');
  antiguo.reject(falloHttp(configAntigua, 401));
  await rechazoAntiguo;
  assert.equal(ultimo().usuario.id, 2);
  await assert.rejects(api.get('/prohibida'));
  assert.equal(ultimo().usuario.id, 2);
  await assert.rejects(api.get('/expirada'));
  assert.equal(ultimo().token, null);
  assert.match(ultimo().mensajeSesion, /expiró/);
});

test('login nunca envía Authorization y su 401 no cierra una sesión existente', async t => {
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin() }), laboratorios: async () => ({ data: alcance })
  });
  await control.login('prueba', 'clave');
  adaptar(t, async config => {
    assert.equal(config.headers.get('Authorization'), undefined);
    throw falloHttp(config, 401);
  });
  await assert.rejects(authApi.login({ userName: 'prueba', password: 'ficticia' }));
  assert.equal(ultimo().usuario.id, 1);
});

test('borra almacenamiento legacy sin escribir credenciales', t => {
  const eliminadas = [];
  for (const nombre of ['localStorage', 'sessionStorage']) {
    const descriptor = Object.getOwnPropertyDescriptor(globalThis, nombre);
    Object.defineProperty(globalThis, nombre, { configurable: true, value: {
      removeItem: clave => eliminadas.push(`${nombre}:${clave}`),
      setItem: () => assert.fail('No debe persistir la sesión')
    } });
    t.after(() => descriptor ? Object.defineProperty(globalThis, nombre, descriptor) : delete globalThis[nombre]);
  }
  limpiarSesionLegacy();
  assert.equal(eliminadas.length, 4);
});

test('mensajes de red y 500 no revelan detalles internos', () => {
  assert.equal(api.defaults.baseURL, '/api');
  assert.equal(mensajeError({ response: { status: 500, data: { message: 'detalle privado' } } }),
    'El servidor no pudo completar la operación. Intenta nuevamente.');
  assert.match(mensajeError({ code: 'ERR_NETWORK' }), /conectar/);
  assert.match(mensajeError({ response: { status: 403 } }), /permiso/);
});

test('refrescar tras un autocambio de rol actualiza perfil y alcance sin persistir ni reemplazar JWT', async t => {
  let rol = 'ADMIN';
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin(1, rol) }),
    me: async () => ({ data: respuestaLogin(1, rol).usuario }),
    laboratorios: async () => ({ data: rol === 'ADMIN' ? { alcanceGlobal: true, laboratorios: [] } : alcance })
  });
  await control.login('prueba', 'clave');
  const token = ultimo().token;
  rol = 'LECTOR';
  await control.refrescarSesion();
  assert.equal(ultimo().usuario.rol, 'LECTOR');
  assert.equal(ultimo().alcance.alcanceGlobal, false);
  assert.equal(ultimo().token, token);
});

test('una consulta de perfil tardía no restaura la sesión después de logout', async t => {
  const perfil = diferida();
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin() }),
    me: () => perfil.promise, laboratorios: async () => ({ data: alcance })
  });
  await control.login('prueba', 'clave');
  const refresco = control.refrescarSesion();
  const rechazado = assert.rejects(refresco, esCancelacion);
  control.logout();
  perfil.resolve({ data: respuestaLogin().usuario });
  await rechazado;
  assert.equal(ultimo().token, null);
});

test('un perfil inactivo al refrescar borra permisos obsoletos', async t => {
  const { control, ultimo } = controlar(t, {
    login: async () => ({ data: respuestaLogin() }),
    me: async () => ({ data: { ...respuestaLogin().usuario, activo: false } }),
    laboratorios: async () => ({ data: alcance })
  });
  await control.login('prueba', 'clave');
  await assert.rejects(control.refrescarSesion(), { code: 'SESION_INVALIDA' });
  assert.equal(ultimo().usuario, null);
  assert.equal(ultimo().token, null);
});

test('400, 403, 404 y 409 muestran mensajes seguros de negocio; 500 los oculta', () => {
  for (const status of [400, 403, 404, 409]) {
    assert.equal(mensajeError({ response: { status, data: { message: 'No se puede desactivar al último ADMIN activo.' } } }),
      'No se puede desactivar al último ADMIN activo.');
  }
  assert.equal(mensajeError({ response: { status: 409, data: { message: '<script>detalle</script>' } } }, 'Conflicto.'), 'Conflicto.');
});
