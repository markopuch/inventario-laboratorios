import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  cargarCatalogos, estadoLaboratorio, etiquetaEstadoLaboratorio, formularioCatalogo,
  guardarCatalogo, payloadEdicionCatalogo
} from './catalogos.js';

const laboratorio = {
  id: 5, nombre: 'Laboratorio A', codigo: 'L-A', ubicacion: 'Primer piso', activo: true,
  estadoOperativo: 'OPERATIVO', area: { id: 2, nombre: 'Área A' }, fechaCreacion: '2026-10-03T12:00:00Z'
};
const categoria = { id: 8, nombre: 'Medición', descripcion: null, activo: true };

const camposDto = nombre => [...readFileSync(new URL(
  `../../../../../backend/inventario/src/main/java/com/utec/inventario/dto/request/${nombre}.java`, import.meta.url
), 'utf8').matchAll(/private\s+\w+\s+(\w+);/g)].map(match => match[1]).sort();

test('ADMIN consulta los cinco listados administrativos y conserva los inactivos recibidos', async () => {
  const llamadas = [];
  const cliente = Object.fromEntries(['Categorias', 'Subcategorias', 'Sedes', 'Areas', 'Laboratorios'].map(nombre => [
    `listar${nombre}Admin`, async () => { llamadas.push(nombre); return { data: [{ id: 1, activo: true }, { id: 2, activo: false }] }; }
  ]));
  const respuesta = await cargarCatalogos(cliente, true, ['categorias', 'subcategorias', 'sedes', 'areas', 'laboratorios']);
  assert.deepEqual(llamadas, ['Categorias', 'Subcategorias', 'Sedes', 'Areas', 'Laboratorios']);
  assert.equal(respuesta.categorias[1].activo, false);
  assert.equal(respuesta.laboratorios.length, 2);
});

test('GESTOR y LECTOR consultan rutas comunes sin solicitar listados administrativos', async () => {
  const tipos = ['categorias', 'subcategorias', 'sedes', 'areas', 'laboratorios'];
  const llamadas = [];
  const cliente = Object.fromEntries(tipos.map(tipo => [tipo, async () => {
    llamadas.push(tipo); return { data: [{ id: 1, activo: true }] };
  }]));
  for (const rol of ['GESTOR', 'LECTOR']) {
    const data = await cargarCatalogos(cliente, rol === 'ADMIN', tipos);
    assert.equal(data.laboratorios[0].activo, true);
  }
  assert.deepEqual(llamadas, [...tipos, ...tipos]);
});

test('activo y estadoOperativo distinguen Activo, En mantenimiento e Inactivo', () => {
  assert.equal(etiquetaEstadoLaboratorio(laboratorio), 'Activo');
  const mantenimiento = { ...laboratorio, estadoOperativo: 'MANTENIMIENTO' };
  assert.equal(etiquetaEstadoLaboratorio(mantenimiento), 'En mantenimiento');
  assert.equal(etiquetaEstadoLaboratorio({ ...mantenimiento, activo: false }), 'Inactivo');
  assert.equal(estadoLaboratorio({ ...mantenimiento, activo: false }), 'INACTIVO');
  const form = formularioCatalogo('laboratorios', { ...mantenimiento, activo: false });
  assert.equal(form.estadoOperativo, 'MANTENIMIENTO');
  assert.equal(form.estadoLaboratorio, 'INACTIVO');
});

test('payloads de edición cumplen los DTO actuales y nunca incluyen activo, IDs de respuesta o fechas', () => {
  const casos = [
    ['categorias', 'Categoria', categoria],
    ['subcategorias', 'Subcategoria', { ...categoria, categoria: { id: 2 } }],
    ['sedes', 'Sede', { ...categoria, direccion: 'Av. A', distrito: 'Lima', departamento: 'Lima' }],
    ['areas', 'Area', { ...categoria, sede: { id: 1 } }],
    ['laboratorios', 'Laboratorio', laboratorio]
  ];
  for (const [tipo, nombre, item] of casos) {
    const payload = payloadEdicionCatalogo(tipo, { ...formularioCatalogo(tipo, item), fechaCreacion: 'extra', passwordHash: 'extra' });
    assert.deepEqual(Object.keys(payload).sort(), camposDto(`Update${nombre}Request`));
    assert.equal('activo' in payload, false);
    assert.equal('passwordHash' in payload, false);
  }
});

test('pasar laboratorio activo a mantenimiento usa PUT con estadoOperativo y no desactiva', async () => {
  const llamadas = [];
  const cliente = { actualizarLaboratorio: async (id, payload) => {
    llamadas.push({ id, payload }); return { data: { ...laboratorio, ...payload } };
  } };
  const resultado = await guardarCatalogo(cliente, 'laboratorios', {
    ...formularioCatalogo('laboratorios', laboratorio), estadoLaboratorio: 'MANTENIMIENTO'
  }, laboratorio);
  assert.equal(llamadas[0].payload.estadoOperativo, 'MANTENIMIENTO');
  assert.equal('activo' in llamadas[0].payload, false);
  assert.deepEqual(resultado.aplicadas, ['actualizar']);
  assert.equal(resultado.registro.activo, true);
});

test('laboratorio inactivo se reactiva por PATCH antes del PUT para editar y poner en mantenimiento', async () => {
  const llamadas = [];
  const inactivo = { ...laboratorio, activo: false };
  const cliente = {
    cambiarEstadoLaboratorio: async (id, activo) => { llamadas.push(['PATCH', id, activo]); return { data: { ...inactivo, activo } }; },
    actualizarLaboratorio: async (id, payload) => { llamadas.push(['PUT', id, payload]); return { data: { ...laboratorio, ...payload } }; }
  };
  const resultado = await guardarCatalogo(cliente, 'laboratorios', {
    ...formularioCatalogo('laboratorios', inactivo), nombre: 'Nombre actualizado', estadoLaboratorio: 'MANTENIMIENTO'
  }, inactivo);
  assert.deepEqual(llamadas[0], ['PATCH', 5, true]);
  assert.equal(llamadas[1][0], 'PUT');
  assert.equal(llamadas[1][2].estadoOperativo, 'MANTENIMIENTO');
  assert.deepEqual(resultado.aplicadas, ['cambiarEstado', 'actualizar']);
});

test('desactivar categoría o laboratorio sin editar datos solicita solo el PATCH de activo', async () => {
  const llamadas = [];
  const cliente = {
    cambiarEstadoCategoria: async (id, activo) => { llamadas.push(['Categoria', id, activo]); return { data: { ...categoria, activo } }; },
    cambiarEstadoLaboratorio: async (id, activo) => { llamadas.push(['Laboratorio', id, activo]); return { data: { ...laboratorio, activo } }; }
  };
  await guardarCatalogo(cliente, 'categorias', { ...formularioCatalogo('categorias', categoria), activo: false }, categoria);
  await guardarCatalogo(cliente, 'laboratorios', { ...formularioCatalogo('laboratorios', laboratorio), estadoLaboratorio: 'INACTIVO' }, laboratorio);
  assert.deepEqual(llamadas, [['Categoria', 8, false], ['Laboratorio', 5, false]]);
});

test('crear laboratorio seleccionado Inactivo hace POST con DTO real y después PATCH, sin éxito local ficticio', async () => {
  const llamadas = [];
  const cliente = {
    crearLaboratorio: async payload => { llamadas.push(['POST', payload]); return { data: laboratorio }; },
    cambiarEstadoLaboratorio: async (id, activo) => { llamadas.push(['PATCH', id, activo]); return { data: { ...laboratorio, activo } }; }
  };
  const resultado = await guardarCatalogo(cliente, 'laboratorios', {
    ...formularioCatalogo('laboratorios', laboratorio), estadoLaboratorio: 'INACTIVO'
  });
  assert.deepEqual(Object.keys(llamadas[0][1]).sort(), camposDto('CreateLaboratorioRequest'));
  assert.deepEqual(llamadas[1], ['PATCH', 5, false]);
  assert.equal(resultado.registro.activo, false);
});

test('409 del padre inactivo se conserva y detiene la edición posterior', async () => {
  const conflicto = Object.assign(new Error('Conflicto'), { response: { status: 409, data: { message: 'La sede seleccionada está inactiva.' } } });
  const area = { id: 4, nombre: 'Área A', descripcion: null, activo: false, sede: { id: 2 } };
  let put = 0;
  const cliente = {
    cambiarEstadoArea: async () => { throw conflicto; },
    actualizarArea: async () => { put += 1; }
  };
  await assert.rejects(guardarCatalogo(cliente, 'areas', { ...formularioCatalogo('areas', area), activo: true }, area), error => {
    assert.equal(error, conflicto);
    assert.deepEqual(error.operacionesAplicadas, []);
    assert.equal(error.response.data.message, 'La sede seleccionada está inactiva.');
    return true;
  });
  assert.equal(put, 0);
});

test('fallo tras reactivar se propaga como operación parcial y no entrega resultado de éxito', async () => {
  const llamadas = [];
  const inactiva = { ...categoria, activo: false };
  const conflicto = Object.assign(new Error('Duplicado'), { response: { status: 409 } });
  const cliente = {
    cambiarEstadoCategoria: async (id, activo) => { llamadas.push(['PATCH', id, activo]); return { data: { ...inactiva, activo } }; },
    actualizarCategoria: async () => { llamadas.push(['PUT']); throw conflicto; }
  };
  await assert.rejects(guardarCatalogo(cliente, 'categorias', {
    ...formularioCatalogo('categorias', inactiva), activo: true, nombre: 'Duplicado'
  }, inactiva), error => {
    assert.equal(error, conflicto);
    assert.deepEqual(error.operacionesAplicadas, ['cambiarEstado']);
    return true;
  });
  assert.equal(llamadas.length, 2);
  assert.equal(inactiva.activo, false, 'No se muta el objeto local recibido del servidor');
});

test('no se envía PUT de un registro que debe permanecer inactivo ni se acepta un estado inventado', async () => {
  const inactiva = { ...categoria, activo: false };
  await assert.rejects(guardarCatalogo({}, 'categorias', {
    ...formularioCatalogo('categorias', inactiva), nombre: 'Cambio prohibido'
  }, inactiva), { code: 'VALIDACION_LOCAL' });
  await assert.rejects(guardarCatalogo({}, 'laboratorios', {
    ...formularioCatalogo('laboratorios', laboratorio), estadoLaboratorio: 'BAJA'
  }, laboratorio), { code: 'VALIDACION_LOCAL' });
});
