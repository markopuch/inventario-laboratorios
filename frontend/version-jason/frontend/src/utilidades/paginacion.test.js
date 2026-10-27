import test from 'node:test';
import assert from 'node:assert/strict';
import { paginaDisponible, rangoPagina, validarPagina } from './paginacion.js';
import { validarEquipos } from './equipos.js';
import { validarAuditoria } from './auditoria.js';

const equipo = id => ({ id, codigoInterno: `EQ-${id}`, nombre: `Equipo ${id}`, estado: 'OPERATIVO',
  requiereMantenimiento: false, laboratorio: { id: 2 }, subcategoria: { id: 3 } });
const pagina = (number = 0, size = 20, totalElements = 45, crear = equipo) => ({
  content: Array.from({ length: Math.max(0, Math.min(size, totalElements - number * size)) }, (_, i) => crear(number * size + i + 1)),
  number, size, totalElements, totalPages: Math.ceil(totalElements / size), first: number === 0,
  last: totalElements === 0 || number >= Math.ceil(totalElements / size) - 1
});

test('páginas de equipos conservan total global y rangos de primera, intermedia y última página', () => {
  for (const [numero, desde, hasta] of [[0, 1, 20], [1, 21, 40], [2, 41, 45]]) {
    const datos = validarPagina(pagina(numero), validarEquipos);
    assert.equal(datos.totalElements, 45);
    assert.equal(datos.totalPages, 3);
    assert.deepEqual(rangoPagina(datos), { desde, hasta });
    assert.equal(paginaDisponible(datos), numero);
  }
});

test('cero resultados es una página válida y no se confunde con una respuesta inválida', () => {
  const datos = validarPagina(pagina(0, 20, 0), validarEquipos);
  assert.equal(datos.first, true);
  assert.equal(datos.last, true);
  assert.deepEqual(rangoPagina(datos), { desde: 0, hasta: 0 });
  assert.equal(paginaDisponible(datos), 0);
  assert.deepEqual(rangoPagina(null), { desde: 0, hasta: 0 });
});

test('una página fuera de rango permite volver a la última página sin perder los totales', () => {
  const datos = validarPagina(pagina(9), validarEquipos);
  assert.deepEqual(datos.content, []);
  assert.equal(datos.totalElements, 45);
  assert.equal(paginaDisponible(datos), 2);
  assert.equal(paginaDisponible(validarPagina(pagina(9, 20, 0))), 0);
});

test('admite tamaños hasta cien con el mismo contrato de metadatos', () => {
  for (const size of [20, 50, 100]) {
    const datos = validarPagina(pagina(0, size, 258), validarEquipos);
    assert.equal(datos.content.length, size);
    assert.deepEqual(rangoPagina(datos), { desde: 1, hasta: size });
  }
});

test('rechaza arrays legacy en el consumidor paginado y metadatos incoherentes', () => {
  const incorrectos = [null, [], {}, { ...pagina(), number: -1 }, { ...pagina(), size: 0 },
    { ...pagina(), size: 101 }, { ...pagina(), totalElements: '45' }, { ...pagina(), totalPages: 2 },
    { ...pagina(), first: false }, { ...pagina(), last: true }, { ...pagina(), content: [] },
    { ...pagina(), number: 0.5 }, { ...pagina(), totalElements: Number.MAX_SAFE_INTEGER + 1 }];
  for (const datos of incorrectos) {
    assert.throws(() => validarPagina(datos, validarEquipos), { code: 'RESPUESTA_PAGINA_INVALIDA' });
  }
});

test('un resultado malformado o duplicado no se presenta como una página vacía de equipos', () => {
  for (const content of [[{ ...equipo(1), estado: 'INVENTADO' }], [{ ...equipo(1), laboratorio: null }],
    [{ ...equipo(1), requiereMantenimiento: null }], [equipo(1), equipo(1)]]) {
    assert.throws(() => validarPagina({ ...pagina(0, 20, content.length), content }, validarEquipos),
      { code: 'RESPUESTA_EQUIPOS_INVALIDA' });
  }
  assert.equal(validarPagina(pagina(0, 20, 1, id => ({ ...equipo(id), estado: 'BAJA' })), validarEquipos).content[0].estado, 'BAJA');
});

test('la auditoría paginada conserva la validación completa de eventos', () => {
  const evento = id => ({ id, idUsuarioActor: 1, userNameActor: 'actor-test', accion: 'CREAR',
    entidad: 'equipo', idEntidad: 2, cambios: 'Creación de equipo de prueba', fecha: '2026-10-27T18:00:00Z' });
  const datos = validarPagina(pagina(0, 20, 1, evento), validarAuditoria);
  assert.equal(datos.content[0].userNameActor, 'actor-test');
  assert.throws(() => validarPagina(pagina(0, 20, 1, id => ({ ...evento(id), fecha: 'fecha-inválida' })), validarAuditoria),
    { code: 'RESPUESTA_AUDITORIA_INVALIDA' });
});
