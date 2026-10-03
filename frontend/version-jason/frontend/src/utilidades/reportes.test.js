import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { conteoReportado, filtrosReportes, validarConteos, validarEquiposPorLaboratorio, validarReporteMantenimientos, validarReporteMovimientos, validarResumenReporte } from './reportes.js';

test('reportes envía únicamente los ocho filtros del DTO actual', () => {
  const dto = readFileSync(new URL('../../../../../backend/inventario/src/main/java/com/utec/inventario/dto/request/FiltroReporteRequest.java', import.meta.url), 'utf8');
  const campos = [...dto.matchAll(/private\s+\w+\s+(\w+);/g)].map(match => match[1]).sort();
  const resultado = filtrosReportes({ idSede: '1', idArea: '2', idLaboratorio: '3', estado: 'BAJA', estadoMantenimiento: 'CANCELADO', tipoMantenimiento: 'CALIBRACION', fechaDesde: '2026-10-01', fechaHasta: '2026-10-31', idEquipo: 99, formato: 'PDF' });
  assert.deepEqual(Object.keys(resultado).sort(), campos);
  assert.deepEqual([resultado.idSede, resultado.idArea, resultado.idLaboratorio], [1, 2, 3]);
});

test('filtros de reportes admiten Todos y rechazan rango invertido o estados incompatibles', () => {
  assert.deepEqual(filtrosReportes({ idSede: '', estado: '', tipoMantenimiento: '' }), {});
  assert.throws(() => filtrosReportes({ fechaDesde: '2026-10-03', fechaHasta: '2026-10-02' }));
  assert.throws(() => filtrosReportes({ estado: 'PROGRAMADO' }));
  assert.throws(() => filtrosReportes({ estadoMantenimiento: 'OPERATIVO' }));
  assert.throws(() => filtrosReportes({ idLaboratorio: '0' }));
});

test('el resumen muestra los agregados del servidor sin recalcular ni reemplazar campos ausentes por cero', () => {
  const data = { totalEquipos: 4, totalMovimientos: 7, totalMantenimientos: 2, equiposPorEstado: [{ valor: 'OPERATIVO', total: 4 }], mantenimientosPorEstado: [{ valor: 'PROGRAMADO', total: 2 }] };
  assert.equal(validarResumenReporte(data), data);
  assert.equal(conteoReportado(data.mantenimientosPorEstado, 'PROGRAMADO'), 2);
  assert.equal(conteoReportado(data.mantenimientosPorEstado, 'EN_PROCESO'), 0);
  assert.throws(() => validarResumenReporte({ ...data, totalEquipos: undefined }));
  assert.throws(() => conteoReportado(undefined, 'PROGRAMADO'));
});

test('los cinco formatos de reporte aceptan resultados vacíos reales y datos válidos', () => {
  assert.deepEqual(validarConteos([]), []);
  assert.deepEqual(validarEquiposPorLaboratorio([{ idLaboratorio: 1, codigo: 'L-1', nombre: 'Laboratorio', total: 3 }]), [{ idLaboratorio: 1, codigo: 'L-1', nombre: 'Laboratorio', total: 3 }]);
  assert.deepEqual(validarReporteMovimientos({ total: 0, porTipo: [] }), { total: 0, porTipo: [] });
  assert.deepEqual(validarReporteMantenimientos({ total: 0, porEstado: [], porTipo: [] }), { total: 0, porEstado: [], porTipo: [] });
  assert.doesNotThrow(() => validarResumenReporte({ totalEquipos: 0, totalMovimientos: 0, totalMantenimientos: 0, equiposPorEstado: [], mantenimientosPorEstado: [] }));
});

test('no se presentan como cifras válidas respuestas parciales, negativas o numéricas en texto', () => {
  assert.throws(() => validarConteos([{ valor: 'BAJA', total: -1 }]));
  assert.throws(() => validarConteos([{ valor: 'BAJA', total: '2' }]));
  assert.throws(() => validarEquiposPorLaboratorio([{ idLaboratorio: 1, total: 3 }]));
  assert.throws(() => validarReporteMovimientos({ total: 3 }));
  assert.throws(() => validarReporteMantenimientos({ total: 0, porEstado: [] }));
});
