import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { payloadEquipo, payloadCatalogo, payloadOrganizacion, laboratoriosPermitidos } from './equipos.js';

const camposDto = (nombre) => {
  const archivo = new URL(`../../../../../backend/inventario/src/main/java/com/utec/inventario/dto/request/${nombre}.java`, import.meta.url);
  return [...readFileSync(archivo, 'utf8').matchAll(/private\s+\w+\s+(\w+);/g)].map((match) => match[1]).sort();
};
const equipoLeido = {
  id: 83, codigoInterno: 'EQ-83', nombre: 'Osciloscopio', estado: 'OPERATIVO',
  requiereMantenimiento: false, idSubcategoria: '4', idLaboratorio: '2', idResponsable: '7',
  laboratorio: { id: 2 }, subcategoria: { id: 4 }, responsable: { id: 7 },
  fechaCreacion: '2026-10-01T12:00:00Z', fechaActualizacion: '2026-10-01T12:00:00Z'
};

test('editar un equipo leído de la API cumple el DTO sin enviar campos protegidos ni metadatos', () => {
  const payload = payloadEquipo(equipoLeido, true);
  assert.deepEqual(Object.keys(payload).sort(), camposDto('UpdateEquipoRequest'));
  assert.equal(payload.idResponsable, 7);
  assert.equal(payload.idSubcategoria, 4);
  assert.equal(payload.anio, null);
  assert.equal(payload.numeroSerie, null);
});

test('crear conserva código y laboratorio y normaliza los opcionales vacíos', () => {
  const payload = payloadEquipo({ ...equipoLeido, idResponsable: '', numeroSerie: '  ', anio: '2024' });
  assert.deepEqual(Object.keys(payload).sort(), camposDto('CreateEquipoRequest'));
  assert.equal(payload.codigoInterno, 'EQ-83');
  assert.equal(payload.idLaboratorio, 2);
  assert.equal(payload.idResponsable, null);
  assert.equal(payload.anio, 2024);
  assert.equal(payload.numeroSerie, null);
});

test('BAJA y relaciones inválidas no se convierten en solicitudes de escritura', () => {
  assert.throws(() => payloadEquipo({ ...equipoLeido, estado: 'BAJA' }, true));
  assert.throws(() => payloadEquipo({ ...equipoLeido, idSubcategoria: '' }));
  assert.throws(() => payloadEquipo({ ...equipoLeido, idLaboratorio: '-1' }));
});

test('catálogos y organización envían sólo los campos de sus DTO reales', () => {
  const form = { id: 1, nombre: 'Ejemplo', estado: 'INACTIVO', activo: false, idCategoria: '2', idArea: '3', idSede: '4', codigo: 'L-5', area: {}, sede: {}, categoria: {}, fechaCreacion: '2026-10-01' };
  for (const [tipo, nombre] of [['categorias', 'Categoria'], ['subcategorias', 'Subcategoria']]) {
    for (const operacion of ['Create', 'Update']) assert.deepEqual(Object.keys(payloadCatalogo(tipo, form)).sort(), camposDto(`${operacion}${nombre}Request`));
  }
  for (const [tipo, nombre] of [['sedes', 'Sede'], ['areas', 'Area'], ['laboratorios', 'Laboratorio']]) {
    for (const operacion of ['Create', 'Update']) assert.deepEqual(Object.keys(payloadOrganizacion(tipo, form)).sort(), camposDto(`${operacion}${nombre}Request`));
  }
});

test('destinos de gestión requieren laboratorio activo y asignación vigente; ADMIN abarca activos', () => {
  const labs = [{ id: 1, activo: true }, { id: 2, activo: false }, { id: 3, activo: true }];
  const alcance = { laboratorios: [{ id: 1 }, { id: 2 }] };
  assert.deepEqual(laboratoriosPermitidos(labs, alcance, false).map((lab) => lab.id), [1]);
  assert.deepEqual(laboratoriosPermitidos(labs, null, false), []);
  assert.deepEqual(laboratoriosPermitidos(labs, alcance, true).map((lab) => lab.id), [1, 3]);
});
