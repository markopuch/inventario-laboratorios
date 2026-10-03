import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { equiposParaMantenimiento, fechaLocalValida, filtrosMantenimiento, mostrarFechaProgramada, payloadEstadoMantenimiento, payloadMantenimiento, puedeGestionarMantenimiento, transicionesMantenimiento } from './mantenimientos.js';
import { mensajeError } from '../servicios/api.js';

const camposDto = nombre => [...readFileSync(new URL(`../../../../../backend/inventario/src/main/java/com/utec/inventario/dto/request/${nombre}.java`, import.meta.url), 'utf8').matchAll(/private\s+\w+\s+(\w+);/g)].map(match => match[1]).sort();
const formulario = { id: 18, idEquipo: '7', tipo: 'PREVENTIVO', descripcion: '  Limpieza y revisión  ', fechaProgramada: '2026-10-03', idResponsable: '', observaciones: ' ', estado: 'COMPLETADO', fechaInicio: '2026-10-01T01:00:00Z', equipo: { id: 7 }, password: 'campo-ajeno' };

test('crear mantenimiento envía exactamente el DTO actual sin estado, fechas del servidor ni campos ajenos', () => {
  const payload = payloadMantenimiento(formulario);
  assert.deepEqual(Object.keys(payload).sort(), camposDto('CreateMantenimientoRequest'));
  assert.equal(payload.idEquipo, 7);
  assert.equal(payload.descripcion, 'Limpieza y revisión');
  assert.equal(payload.idResponsable, null);
  assert.equal(payload.observaciones, null);
});

test('editar excluye el equipo inmutable y conserva responsable numérico y fecha LocalDate', () => {
  const payload = payloadMantenimiento({ ...formulario, idResponsable: '12' }, true);
  assert.deepEqual(Object.keys(payload).sort(), camposDto('UpdateMantenimientoRequest'));
  assert.equal(payload.idResponsable, 12);
  assert.equal(payload.fechaProgramada, '2026-10-03');
  assert.equal('idEquipo' in payload, false);
});

test('datos inválidos no generan payload de mantenimiento', () => {
  for (const cambio of [{ descripcion: ' ' }, { tipo: 'DESCONOCIDO' }, { idEquipo: '0' }, { idResponsable: '-2' }, { fechaProgramada: '2026-02-30' }, { observaciones: 'x'.repeat(4001) }]) {
    assert.throws(() => payloadMantenimiento({ ...formulario, ...cambio }), error => error.code === 'VALIDACION_LOCAL');
  }
});

test('las transiciones corresponden al ciclo de vida y nunca reabren estados terminales', () => {
  assert.deepEqual(transicionesMantenimiento('PROGRAMADO'), ['EN_PROCESO', 'CANCELADO']);
  assert.deepEqual(transicionesMantenimiento('EN_PROCESO'), ['COMPLETADO', 'CANCELADO']);
  for (const final of ['COMPLETADO', 'CANCELADO']) assert.deepEqual(transicionesMantenimiento(final), []);
  assert.throws(() => payloadEstadoMantenimiento('PROGRAMADO', 'COMPLETADO', ''));
  assert.throws(() => payloadEstadoMantenimiento('COMPLETADO', 'EN_PROCESO', ''));
  const payload = payloadEstadoMantenimiento('EN_PROCESO', 'COMPLETADO', '  Finalizado  ');
  assert.deepEqual(Object.keys(payload).sort(), camposDto('CambiarEstadoMantenimientoRequest'));
  assert.deepEqual(payload, { estado: 'COMPLETADO', observaciones: 'Finalizado' });
  assert.equal(payloadEstadoMantenimiento('PROGRAMADO', 'CANCELADO', '').observaciones, '');
  assert.equal(payloadEstadoMantenimiento('PROGRAMADO', 'CANCELADO').observaciones, null);
});

test('filtros omiten parámetros ajenos y preservan los seis campos admitidos', () => {
  const params = filtrosMantenimiento({ idEquipo: '7', idLaboratorio: '2', estado: 'PROGRAMADO', tipo: 'OTRO', fechaDesde: '2026-10-01', fechaHasta: '2026-10-31', idUsuario: 9, pagina: 2 });
  assert.deepEqual(Object.keys(params).sort(), camposDto('FiltroMantenimientoRequest'));
  assert.equal(params.idEquipo, 7);
  assert.equal(params.idLaboratorio, 2);
  assert.deepEqual(filtrosMantenimiento({ estado: '', idEquipo: '', fechaDesde: '' }), {});
  assert.throws(() => filtrosMantenimiento({ fechaDesde: '2026-10-03', fechaHasta: '2026-10-02' }));
});

test('LocalDate no cambia de día por la zona horaria y valida días reales', () => {
  assert.equal(mostrarFechaProgramada('2026-10-03'), '03/10/2026');
  assert.equal(fechaLocalValida('2024-02-29'), true);
  assert.equal(fechaLocalValida('2026-02-29'), false);
  assert.equal(fechaLocalValida('2026-10-03T00:00:00Z'), false);
});

test('LECTOR no escribe; GESTOR solo selecciona equipos vigentes de laboratorios activos asignados', () => {
  const labs = [{ id: 1, activo: true }, { id: 2, activo: true }, { id: 3, activo: false }];
  const equipos = [{ id: 1, estado: 'OPERATIVO', laboratorio: { id: 1 } }, { id: 2, estado: 'BAJA', laboratorio: { id: 1 } }, { id: 3, estado: 'INOPERATIVO', laboratorio: { id: 2 } }, { id: 4, estado: 'OPERATIVO', laboratorio: { id: 3 } }];
  const gestor = { puedeGestionar: true, esAdmin: false, alcance: { laboratorios: [{ id: 1 }] } };
  assert.deepEqual(equiposParaMantenimiento(equipos, labs, gestor).map(equipo => equipo.id), [1]);
  assert.deepEqual(equiposParaMantenimiento(equipos, labs, { ...gestor, esAdmin: true }).map(equipo => equipo.id), [1, 3]);
  assert.equal(puedeGestionarMantenimiento({ equipo: equipos[0] }, { ...gestor, puedeGestionar: false }), false);
  assert.equal(puedeGestionarMantenimiento({ equipo: equipos[2] }, gestor), false);
});

test('conflictos de mantenimiento conservan el mensaje de negocio y 403 muestra falta de permiso', () => {
  assert.equal(mensajeError({ response: { status: 409, data: { message: 'Solo puede editarse un mantenimiento PROGRAMADO.' } } }), 'Solo puede editarse un mantenimiento PROGRAMADO.');
  assert.match(mensajeError({ response: { status: 403 } }), /permiso/i);
});
