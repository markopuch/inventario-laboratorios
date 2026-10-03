import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import api, { catalogoApi, usuariosApi, mantenimientosApi, reportesApi } from './api.js';

const controller = nombre => readFileSync(new URL(`../../../../../backend/inventario/src/main/java/com/utec/inventario/controller/${nombre}.java`, import.meta.url), 'utf8');
const contrato = (nombre, base, metodo, ruta = '') => {
  const fuente = controller(nombre);
  assert.ok(fuente.includes(`@RequestMapping("${base}")`));
  const mapping = `@${metodo[0].toUpperCase()}${metodo.slice(1).toLowerCase()}Mapping`;
  assert.ok(fuente.includes(ruta ? `${mapping}("${ruta}")` : mapping), `${nombre}: falta ${mapping} ${ruta}`);
};
function capturar(t) {
  const original = api.defaults.adapter;
  const llamadas = [];
  api.defaults.adapter = async config => {
    llamadas.push({ metodo: config.method, ruta: config.url, params: config.params, payload: config.data ? JSON.parse(config.data) : null });
    return { config, data: {}, status: 200, statusText: 'OK', headers: {} };
  };
  t.after(() => { api.defaults.adapter = original; });
  return llamadas;
}

test('ADMIN consulta inactivos y cambia estado mediante los contratos existentes de los cinco catálogos', async t => {
  const llamadas = capturar(t);
  for (const [tipo, singular] of [['categorias', 'Categoria'], ['subcategorias', 'Subcategoria'], ['sedes', 'Sede'], ['areas', 'Area'], ['laboratorios', 'Laboratorio']]) {
    contrato('CatalogoAdminController', '/api/admin', 'GET', `/${tipo}`);
    contrato(`${singular}Controller`, `/api/${tipo}`, 'PATCH', '/{id}/estado');
    await catalogoApi[`listar${singular === 'Area' ? 'Areas' : singular === 'Sede' ? 'Sedes' : singular === 'Categoria' ? 'Categorias' : singular === 'Subcategoria' ? 'Subcategorias' : 'Laboratorios'}Admin`]({ activo: false, estado: 'inventado' });
    assert.deepEqual(llamadas.at(-1).params, { activo: false });
    assert.equal(llamadas.at(-1).ruta, `/admin/${tipo}`);
    await catalogoApi[`cambiarEstado${singular}`](7, true);
    assert.deepEqual(llamadas.at(-1), { metodo: 'patch', ruta: `/${tipo}/7/estado`, params: undefined, payload: { activo: true } });
  }
});

test('usuarios separa datos públicos, rol, estado y password sin usar PUT completo para cambios de seguridad', async t => {
  const llamadas = capturar(t);
  for (const [metodo, ruta] of [['GET', ''], ['GET', '/{id}'], ['POST', ''], ['PUT', '/{id}'], ['PATCH', '/{id}/rol'], ['PATCH', '/{id}/estado'], ['PUT', '/{id}/password']]) contrato('AdminUsuarioController', '/api/admin/usuarios', metodo, ruta);
  await usuariosApi.listar();
  await usuariosApi.detalle(9);
  await usuariosApi.cambiarRol(9, 'LECTOR');
  assert.deepEqual(llamadas.at(-1).payload, { rol: 'LECTOR' });
  await usuariosApi.cambiarEstado(9, false);
  assert.deepEqual(llamadas.at(-1).payload, { activo: false });
  await usuariosApi.cambiarPassword(9, 'password-ficticia-de-test');
  assert.deepEqual(llamadas.at(-1), { metodo: 'put', ruta: '/admin/usuarios/9/password', params: undefined, payload: { password: 'password-ficticia-de-test' } });
  await usuariosApi.actualizarLaboratorios(9, { idsLaboratorio: [2] });
  assert.equal(llamadas.at(-1).ruta, '/admin/usuarios/9/laboratorios');
  assert.deepEqual(llamadas.at(-1).payload, { idsLaboratorio: [2] });
});

test('mantenimiento envía solo filtros soportados y conserva la transición como PATCH específico', async t => {
  const llamadas = capturar(t);
  contrato('MantenimientoController', '/api/mantenimientos', 'PATCH', '/{id}/estado');
  await mantenimientosApi.listar({ idEquipo: 2, idLaboratorio: 3, estado: 'PROGRAMADO', tipo: 'PREVENTIVO', fechaDesde: '2026-10-01', fechaHasta: '2026-10-03', idUsuarioActor: 999, activo: false });
  assert.deepEqual(llamadas.at(-1).params, { idEquipo: 2, idLaboratorio: 3, estado: 'PROGRAMADO', tipo: 'PREVENTIVO', fechaDesde: '2026-10-01', fechaHasta: '2026-10-03' });
  await mantenimientosApi.cambiarEstado(4, { estado: 'EN_PROCESO', observaciones: null });
  assert.deepEqual(llamadas.at(-1), { metodo: 'patch', ruta: '/mantenimientos/4/estado', params: undefined, payload: { estado: 'EN_PROCESO', observaciones: null } });
});

test('los cinco reportes utilizan sus rutas reales y omiten filtros ajenos al contrato', async t => {
  const llamadas = capturar(t);
  for (const [nombre, ruta] of [['resumen', '/resumen'], ['equiposPorEstado', '/equipos/por-estado'], ['equiposPorLaboratorio', '/equipos/por-laboratorio'], ['movimientos', '/movimientos'], ['mantenimientos', '/mantenimientos']]) {
    contrato('ReporteController', '/api/reportes', 'GET', ruta);
    await reportesApi[nombre]({ idLaboratorio: 2, estadoMantenimiento: 'EN_PROCESO', tipoMantenimiento: 'CORRECTIVO', fechaDesde: '', inventado: true });
    assert.deepEqual(llamadas.at(-1).params, { idLaboratorio: 2, estadoMantenimiento: 'EN_PROCESO', tipoMantenimiento: 'CORRECTIVO' });
    assert.equal(llamadas.at(-1).ruta, `/reportes${ruta}`);
  }
});
