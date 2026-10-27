import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  ACCIONES_AUDITORIA, ENTIDADES_AUDITORIA, etiquetaAccionAuditoria, etiquetaEntidadAuditoria,
  fechaAuditoriaValida, filtrosAuditoria, mostrarFechaAuditoria, registroAuditoria, validarAuditoria
} from './auditoria.js';

const fuenteBackend = ruta => readFileSync(new URL(`../../../../../backend/inventario/src/main/${ruta}`, import.meta.url), 'utf8');
const camposJava = fuente => [...fuente.matchAll(/private\s+\w+\s+(\w+);/g)].map(match => match[1]).sort();
const evento = cambios => ({ id: 91, idUsuarioActor: 3, userNameActor: 'ana', accion: 'CAMBIAR_ROL',
  entidad: 'usuario', idEntidad: 12, cambios: 'rol=LECTOR -> GESTOR', fecha: '2026-10-26T15:30:00.123456Z', ...cambios });

test('auditoría construye exclusivamente los seis filtros del DTO backend', () => {
  const dto = fuenteBackend('java/com/utec/inventario/dto/request/FiltroAuditoriaRequest.java');
  const params = filtrosAuditoria({ entidad: ' usuario ', idEntidad: ' 12 ', idUsuario: '3', accion: 'CAMBIAR_ROL',
    fechaDesde: '2026-10-01', fechaHasta: '2026-10-31', userName: 'no-enviar', idLaboratorio: 7, formato: 'PDF' });
  assert.deepEqual(Object.keys(params).sort(), camposJava(dto));
  assert.deepEqual(params, { fechaDesde: '2026-10-01', fechaHasta: '2026-10-31', idEntidad: 12,
    idUsuario: 3, entidad: 'usuario', accion: 'CAMBIAR_ROL' });
});

test('Todos y los campos vacíos omiten parámetros; un filtro de usuario no exige otro de entidad', () => {
  assert.deepEqual(filtrosAuditoria(), {});
  assert.deepEqual(filtrosAuditoria({ entidad: '', accion: '', idEntidad: ' ', idUsuario: null, fechaDesde: '', fechaHasta: '' }), {});
  assert.deepEqual(filtrosAuditoria({ idUsuario: 2147483647 }), { idUsuario: 2147483647 });
});

test('los IDs rechazan ceros, negativos, fracciones, coerciones y valores fuera del INTEGER backend', () => {
  for (const campo of ['idEntidad', 'idUsuario']) {
    for (const valor of ['0', '-1', '1.5', '1e3', '0x10', '2147483648', 2147483648, true, [], [1], {}, Infinity]) {
      assert.throws(() => filtrosAuditoria({ [campo]: valor }), { code: 'VALIDACION_LOCAL' }, `${campo}: ${String(valor)}`);
    }
  }
});

test('los filtros validan el calendario y conservan días UTC sin convertirlos a Lima', () => {
  assert.deepEqual(filtrosAuditoria({ fechaDesde: '2024-02-29', fechaHasta: '2024-02-29' }),
    { fechaDesde: '2024-02-29', fechaHasta: '2024-02-29' });
  for (const filtros of [{ fechaDesde: '2026-02-29' }, { fechaHasta: '2026-04-31' },
    { fechaDesde: '26/10/2026' }, { fechaHasta: '2026-10-26T00:00:00Z' },
    { fechaDesde: '2026-10-27', fechaHasta: '2026-10-26' }]) {
    assert.throws(() => filtrosAuditoria(filtros), { code: 'VALIDACION_LOCAL' });
  }
});

test('entidades y acciones de la interfaz coinciden con V13 y el enum, sin inventar eventos de login', () => {
  const migracion = fuenteBackend('resources/db/migration/V13__crear_auditoria.sql');
  const entidadesSql = [...migracion.match(/ck_auditoria_entidad CHECK \(entidad IN \(([\s\S]*?)\)\)/)[1].matchAll(/'([^']+)'/g)].map(match => match[1]);
  const enumJava = fuenteBackend('java/com/utec/inventario/domain/AccionAuditoria.java');
  const accionesJava = enumJava.match(/enum AccionAuditoria\s*\{([\s\S]*?)\}/)[1].split(',').map(valor => valor.trim());
  assert.deepEqual([...ENTIDADES_AUDITORIA].sort(), entidadesSql.sort());
  assert.deepEqual([...ACCIONES_AUDITORIA].sort(), accionesJava.sort());
  assert.throws(() => filtrosAuditoria({ entidad: 'movimiento' }), { code: 'VALIDACION_LOCAL' });
  assert.throws(() => filtrosAuditoria({ accion: 'LOGIN' }), { code: 'VALIDACION_LOCAL' });
});

test('un resultado vacío válido se distingue de una respuesta incompleta o de otro formato', () => {
  assert.deepEqual(validarAuditoria([]), []);
  for (const data of [null, undefined, {}, { content: [] }, '<html>Error</html>', [null], [{}]]) {
    assert.throws(() => validarAuditoria(data), { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
  }
});

test('los ocho campos del DTO son necesarios y el actor puede ser nulo conservando su username histórico', () => {
  const dto = fuenteBackend('java/com/utec/inventario/dto/response/AuditoriaResponse.java');
  const item = evento({ idUsuarioActor: null });
  assert.deepEqual(Object.keys(item).sort(), camposJava(dto));
  const data = [item];
  assert.equal(validarAuditoria(data), data);
  assert.equal(data[0].userNameActor, 'ana');
  for (const campo of Object.keys(item)) {
    const incompleto = { ...item };
    delete incompleto[campo];
    assert.throws(() => validarAuditoria([incompleto]), { code: 'RESPUESTA_AUDITORIA_INVALIDA' }, campo);
  }
});

test('la respuesta no admite identificadores inválidos, duplicados, acciones desconocidas o resúmenes vacíos', () => {
  for (const cambios of [{ id: 0 }, { id: '91' }, { idUsuarioActor: -1 }, { idEntidad: 2147483648 },
    { userNameActor: ' ' }, { userNameActor: 'a'.repeat(51) }, { accion: 'LOGIN' }, { entidad: 'reporte' },
    { cambios: null }, { cambios: ' ' }, { cambios: 'a'.repeat(2001) }]) {
    assert.throws(() => validarAuditoria([evento(cambios)]), { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
  }
  assert.throws(() => validarAuditoria([evento(), evento()]), { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
});

test('el resumen original se conserva como texto, sin fabricar un diff de campos que no viene del servidor', () => {
  const resumen = 'Campos actualizados: nombre, apellido, email, cargo';
  const item = validarAuditoria([evento({ accion: 'EDITAR', cambios: resumen })])[0];
  assert.equal(item.cambios, resumen);
  assert.deepEqual(Object.keys(item).sort(), Object.keys(evento()).sort());
});

test('las fechas de eventos requieren un instante real con zona y admiten precisión PostgreSQL', () => {
  for (const fecha of ['2026-10-26T15:30:00Z', '2026-10-26T10:30:00-05:00', '2026-10-26T15:30:00.123456789Z']) {
    assert.equal(fechaAuditoriaValida(fecha), true, fecha);
  }
  for (const fecha of ['2026-02-30T15:30:00Z', '2026-10-26', '2026-10-26T15:30:00',
    '2026-10-26T24:00:00Z', '2026-10-26T15:30:00+19:00', null, 123]) {
    assert.equal(fechaAuditoriaValida(fecha), false, String(fecha));
    assert.throws(() => validarAuditoria([evento({ fecha })]), { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
  }
});

test('la presentación fija Lima y respeta el día anterior aunque el equipo use otra zona', () => {
  assert.match(mostrarFechaAuditoria('2026-10-26T04:30:00Z'), /25\/10\/2026.*23:30:00/);
  assert.match(mostrarFechaAuditoria('2026-10-26T15:30:00Z'), /26\/10\/2026.*10:30:00/);
  assert.equal(mostrarFechaAuditoria('inválida'), '—');
});

test('las asignaciones identifican el usuario afectado y todas las etiquetas son legibles', () => {
  assert.equal(registroAuditoria(evento()), 'Usuario #12');
  assert.equal(registroAuditoria(evento({ entidad: 'usuario_laboratorio' })), 'Asignaciones del usuario #12');
  for (const accion of ACCIONES_AUDITORIA) assert.notEqual(etiquetaAccionAuditoria(accion), accion);
  for (const entidad of ENTIDADES_AUDITORIA) assert.notEqual(etiquetaEntidadAuditoria(entidad), entidad);
});
