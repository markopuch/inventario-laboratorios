import test from 'node:test';
import assert from 'node:assert/strict';
import { crearControlAsignaciones, prepararGuardadoAsignaciones, validarRespuestaAsignaciones } from './asignaciones.js';

test('consultar A y escribir B nunca prepara una escritura de las asignaciones de A sobre B', () => {
  assert.throws(() => prepararGuardadoAsignaciones({ idActual: '2', idCargado: 1, usuario: { id: 1 }, ids: [8] }), /consultar/);
  assert.throws(() => prepararGuardadoAsignaciones({ idActual: '1', idCargado: 1, usuario: { id: 2 }, ids: [8] }), /consultar/);
});

test('guardar una selección vacía conserva el contrato y el usuario consultado', () => {
  assert.deepEqual(prepararGuardadoAsignaciones({ idActual: '7', idCargado: 7, usuario: { id: 7 }, ids: [] }), {
    idUsuario: 7, payload: { idsLaboratorio: [] }
  });
});

test('no se escriben IDs inválidos ni una consulta no completada', () => {
  for (const idActual of ['', '0', '-1', '1.5', 'no-es-un-id']) {
    assert.throws(() => prepararGuardadoAsignaciones({ idActual, idCargado: 1, usuario: { id: 1 }, ids: [] }));
  }
  assert.throws(() => prepararGuardadoAsignaciones({ idActual: '1', idCargado: null, usuario: null, ids: [] }));
  assert.throws(() => prepararGuardadoAsignaciones({ idActual: '1', idCargado: 1, usuario: { id: 1 }, ids: [null] }));
});

test('una respuesta de otro usuario no reemplaza el resultado consultado', () => {
  assert.throws(() => validarRespuestaAsignaciones({ usuario: { id: 2 }, laboratorios: [] }, 1));
  assert.throws(() => validarRespuestaAsignaciones({ usuario: { id: 1 } }, 1));
  assert.doesNotThrow(() => validarRespuestaAsignaciones({ usuario: { id: 1 }, laboratorios: [] }, 1));
});

test('el segundo envío queda bloqueado hasta completar la operación vigente', () => {
  const control = crearControlAsignaciones();
  control.cambiarId('1');
  const primera = control.iniciar();
  assert.equal(control.iniciar(), null);
  assert.equal(control.vigente(primera), true);
  control.finalizar(primera);
  assert.ok(control.iniciar());
});

test('editar A a B y volver a A invalida también la primera respuesta de A', () => {
  const control = crearControlAsignaciones();
  control.cambiarId('1');
  const antigua = control.iniciar();
  control.cambiarId('2');
  control.cambiarId('1');
  const actual = control.iniciar();
  assert.equal(control.vigente(antigua), false);
  control.finalizar(antigua);
  assert.equal(control.vigente(actual), true);
  assert.equal(control.iniciar(), null);
});

test('al salir de la pantalla se descarta una respuesta que aún está pendiente', () => {
  const control = crearControlAsignaciones();
  control.cambiarId('1');
  const pendiente = control.iniciar();
  control.invalidar();
  assert.equal(control.vigente(pendiente), false);
});
