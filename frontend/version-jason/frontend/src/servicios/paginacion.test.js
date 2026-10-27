import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';
import api, { auditoriaApi, equiposApi } from './api.js';

function capturar(t) {
  const original = api.defaults.adapter;
  const llamadas = [];
  api.defaults.adapter = async config => {
    llamadas.push(config);
    return { config, data: [], status: 200, statusText: 'OK', headers: {} };
  };
  t.after(() => { api.defaults.adapter = original; });
  return llamadas;
}

test('dashboard, selectores e importadores pueden conservar listas legacy sin page ni size', async t => {
  const llamadas = capturar(t);
  await equiposApi.listar();
  await equiposApi.listarAdmin();
  await auditoriaApi.listar();
  assert.deepEqual(llamadas.map(config => config.url), ['/equipos', '/admin/equipos', '/admin/auditoria']);
  for (const config of llamadas) assert.deepEqual(config.params, {});
});

test('solo los métodos paginados activan el contrato de página y conservan filtros y cancelación', async t => {
  const llamadas = capturar(t);
  const solicitud = new AbortController();
  const filtros = { page: 2, size: 50, q: 'EQ-200', estado: 'BAJA', idLaboratorio: 4,
    idSubcategoria: 3, requiereMantenimiento: false, inventado: 'no-enviar' };
  for (const metodo of ['listarPagina', 'listarPaginaAdmin']) {
    await equiposApi[metodo](filtros, { signal: solicitud.signal });
    assert.equal(llamadas.at(-1).signal, solicitud.signal);
    assert.deepEqual(llamadas.at(-1).params, { page: 2, size: 50, q: 'EQ-200', estado: 'BAJA',
      idLaboratorio: 4, idSubcategoria: 3, requiereMantenimiento: false });
  }
  await auditoriaApi.listarPagina({ size: 100, entidad: 'equipo', idUsuario: 1, page: 0, q: 'ignorar' }, { signal: solicitud.signal });
  assert.deepEqual(llamadas.at(-1).params, { size: 100, page: 0, entidad: 'equipo', idUsuario: 1 });
  assert.equal(llamadas.at(-1).signal, solicitud.signal);
  assert.equal(filtros.inventado, 'no-enviar');
});

test('una consulta paginada nueva usa página cero y tamaño veinte por defecto', async t => {
  const llamadas = capturar(t);
  await equiposApi.listarPagina();
  await equiposApi.listarPaginaAdmin();
  await auditoriaApi.listarPagina();
  for (const config of llamadas) assert.deepEqual(config.params, { page: 0, size: 20 });
});

test('abortar una página pendiente propaga cancelación sin convertirla en lista vacía', async t => {
  const original = api.defaults.adapter;
  t.after(() => { api.defaults.adapter = original; });
  api.defaults.adapter = config => new Promise((resolve, reject) => {
    config.signal.addEventListener('abort', () => reject(new axios.CanceledError()), { once: true });
  });
  const solicitud = new AbortController();
  const resultado = equiposApi.listarPagina({ page: 1 }, { signal: solicitud.signal });
  solicitud.abort();
  await assert.rejects(resultado, axios.isCancel);
});
