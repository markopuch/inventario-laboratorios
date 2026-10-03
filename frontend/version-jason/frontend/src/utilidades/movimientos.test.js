import test from 'node:test';
import assert from 'node:assert/strict';
import { esMovimientoDeHoy, fechaValida, filtrarMovimientos, ordenarMovimientos, validarListaMovimientos } from './movimientos.js';

test('hoy se calcula por día local a partir de fechaMovimiento', () => {
  const ahora = new Date(2026, 9, 3, 22, 30);
  const madrugadaLocal = new Date(2026, 9, 3, 0, 1).toISOString();
  const nocheLocal = new Date(2026, 9, 3, 23, 59).toISOString();
  const ayerLocal = new Date(2026, 9, 2, 23, 59).toISOString();
  assert.equal(esMovimientoDeHoy({ fechaMovimiento: madrugadaLocal }, ahora), true);
  assert.equal(esMovimientoDeHoy({ fechaMovimiento: nocheLocal }, ahora), true);
  assert.equal(esMovimientoDeHoy({ fechaMovimiento: ayerLocal }, ahora), false);
  assert.equal(esMovimientoDeHoy({ fecha: madrugadaLocal }, ahora), false);
});

test('fechas nulas o inválidas no se convierten en 1970 ni cuentan como hoy', () => {
  for (const valor of [null, undefined, '', 'no-es-fecha', 0]) {
    assert.equal(fechaValida(valor), null);
    assert.equal(esMovimientoDeHoy({ fechaMovimiento: valor }), false);
  }
});

test('recientes se ordenan por instante real, conservando entrada y fechas inválidas al final', () => {
  const datos = [
    { id: 1, fechaMovimiento: '2026-10-03T10:00:00-05:00' },
    { id: 2, fechaMovimiento: null },
    { id: 3, fechaMovimiento: '2026-10-03T14:00:00Z' },
    { id: 4, fechaMovimiento: '2026-10-03T15:30:00Z' }
  ];
  assert.deepEqual(ordenarMovimientos(datos).map(item => item.id), [4, 1, 3, 2]);
  assert.deepEqual(datos.map(item => item.id), [1, 2, 3, 4]);
});

test('búsqueda consulta campos visibles y acepta mayúsculas, tildes y relaciones nulas', () => {
  const movimiento = { id: 1, equipo: { codigoInterno: 'EQ-1', nombre: 'Microscopio' },
    actor: { nombre: 'José', apellido: 'Pérez' }, tipoMovimiento: 'TRASLADO',
    laboratorioOrigen: null, laboratorioDestino: { codigo: 'LAB-Q', nombre: 'Química' }, motivo: 'Práctica' };
  for (const texto of ['JOSE PEREZ', 'quimica', 'practica', 'EQ-1', 'traslado']) {
    assert.deepEqual(filtrarMovimientos([movimiento], texto), [movimiento]);
  }
  assert.deepEqual(filtrarMovimientos([movimiento], 'laboratorioDestino'), []);
  assert.deepEqual(filtrarMovimientos([{ id: 2 }], 'inexistente'), []);
});

test('una respuesta inválida se rechaza y una lista vacía legítima se conserva', () => {
  for (const valor of [null, {}, [null], [{ id: 0 }], [{ id: '1' }]]) {
    assert.throws(() => validarListaMovimientos(valor));
  }
  assert.deepEqual(validarListaMovimientos([]), []);
});
