import test from 'node:test';
import assert from 'node:assert/strict';
import { celdaCsv, crearReporteCsv, descargarReporteCsv, filtrosCsvCoinciden, puedeExportarReporte } from './reportesCsv.js';

const generadoEn = '2026-10-27T01:02:03.000Z';
const resumen = { totalEquipos: 0, totalMovimientos: 0, totalMantenimientos: 0, equiposPorEstado: [], mantenimientosPorEstado: [] };
const laboratorios = [{ idLaboratorio: 7, codigo: 'LAB-7', nombre: 'Química', total: 3 }];

// Lector independiente para verificar delimitadores, comillas y celdas
// multilínea sin confundir saltos de línea de una celda con nuevas filas.
function leerCsv(contenido) {
  assert.equal(contenido[0], '\uFEFF');
  const filas = [];
  let fila = [], celda = '', entreComillas = false;
  for (let i = 1; i < contenido.length; i++) {
    const actual = contenido[i];
    if (actual === '"') {
      if (entreComillas && contenido[i + 1] === '"') { celda += '"'; i++; }
      else entreComillas = !entreComillas;
    } else if (!entreComillas && actual === ';') { fila.push(celda); celda = ''; }
    else if (!entreComillas && actual === '\r' && contenido[i + 1] === '\n') {
      fila.push(celda); filas.push(fila); fila = []; celda = ''; i++;
    } else celda += actual;
  }
  assert.equal(entreComillas, false);
  assert.equal(celda, '');
  assert.equal(fila.length, 0);
  assert.ok(filas.every(item => item.length === 17), 'Todas las filas deben conservar las 17 columnas.');
  return filas;
}

test('CSV de resumen conserva los tres totales del servidor, incluidos ceros reales', () => {
  const { contenido, nombreArchivo } = crearReporteCsv('resumen', resumen, { generadoEn });
  const [cabecera, ...filas] = leerCsv(contenido);
  assert.equal(nombreArchivo, 'reporte-resumen-general-2026-10-27.csv');
  assert.equal(cabecera.at(-1), 'Total');
  assert.deepEqual(filas.map(fila => [fila[12], fila[16]]), [['Total equipos', '0'], ['Total movimientos', '0'], ['Total mantenimientos', '0']]);
});

test('CSV conserva tildes, punto y coma, comillas y saltos de línea como una sola celda', () => {
  const datos = [{ ...laboratorios[0], codigo: 'Q;"7"', nombre: 'Química, física; "Norte"\nPiso 2\rPiso 3' }];
  const { contenido } = crearReporteCsv('equiposPorLaboratorio', datos, { generadoEn });
  const filas = leerCsv(contenido);
  assert.equal(filas.length, 2);
  assert.equal(filas[1][14], 'Q;"7"');
  assert.equal(filas[1][15], 'Química, física; "Norte"\r\nPiso 2\r\nPiso 3');
  assert.equal(filas[1][16], '3');
  assert.ok(contenido.endsWith('\r\n'));
});

test('texto que podría ejecutar fórmulas se neutraliza antes del escape CSV', () => {
  for (const texto of ['=1+1', '+SUM(A1:A2)', '-1+1', '@SUM(A1)', ' \t=1', '\u00a0+1', '\u200b=1', '\ttexto', ' \ttexto', '\rtexto', '\ntexto', '\t\r\n@A1']) {
    assert.ok(celdaCsv(texto).startsWith('"\''), `Se debe neutralizar ${JSON.stringify(texto)}`);
  }
  assert.equal(celdaCsv('Equipo - operativo'), '"Equipo - operativo"');
  assert.equal(celdaCsv(42), '"42"');
  const { contenido } = crearReporteCsv('equiposPorLaboratorio', [{ ...laboratorios[0], codigo: '=HYPERLINK("x")', nombre: '  @SUM(1)' }], { generadoEn });
  const [, fila] = leerCsv(contenido);
  assert.equal(fila[14], '\'=HYPERLINK("x")');
  assert.equal(fila[15], '\'  @SUM(1)');
});

test('se exportan filtros aplicados normalizados y contexto sin serializar sesión ni campos ajenos', () => {
  const filtrosAplicados = { idSede: '1', idArea: 2, idLaboratorio: '7', estado: 'OPERATIVO', fechaDesde: '2026-10-01', fechaHasta: '2026-10-26', estadoMantenimiento: 'PROGRAMADO', tipoMantenimiento: 'PREVENTIVO', token: 'dato-que-no-debe-salir', usuario: 'privado' };
  const { contenido } = crearReporteCsv('equiposPorLaboratorio', laboratorios.map(fila => ({ ...fila, token: 'respuesta-no-debe-salir' })), { generadoEn, filtrosAplicados, alcanceGlobal: true, token: 'tampoco-debe-salir' });
  const [, fila] = leerCsv(contenido);
  assert.deepEqual(fila.slice(0, 11), ['Equipos por laboratorio', generadoEn, 'Global', '1', '2', '7', 'OPERATIVO', '2026-10-01', '2026-10-26', 'PROGRAMADO', 'PREVENTIVO']);
  assert.ok(!contenido.includes('no-debe-salir'));
  assert.ok(!contenido.includes('privado'));
  const [, sinFiltros] = leerCsv(crearReporteCsv('equiposPorLaboratorio', laboratorios, { generadoEn }).contenido);
  assert.equal(sinFiltros[2], 'Laboratorios asignados');
  assert.deepEqual(sinFiltros.slice(3, 11), Array(8).fill('Todos'));
});

test('movimientos y mantenimientos distinguen el total de cada agrupación en un único archivo', () => {
  const movimiento = { total: 4, porTipo: [{ valor: 'TRASLADO', total: 4 }] };
  const mantenimiento = { total: 4, porEstado: [{ valor: 'PROGRAMADO', total: 4 }], porTipo: [{ valor: 'PREVENTIVO', total: 3 }, { valor: 'CORRECTIVO', total: 1 }] };
  const filasMovimiento = leerCsv(crearReporteCsv('movimientos', movimiento, { generadoEn }).contenido).slice(1);
  assert.deepEqual(filasMovimiento.map(fila => fila.slice(11, 13)), [['Total', 'Movimientos'], ['Por tipo de movimiento', 'TRASLADO']]);
  const filasMantenimiento = leerCsv(crearReporteCsv('mantenimientos', mantenimiento, { generadoEn }).contenido).slice(1);
  assert.deepEqual(filasMantenimiento.map(fila => [fila[11], fila[12], fila[16]]), [
    ['Total', 'Mantenimientos', '4'], ['Por estado de mantenimiento', 'PROGRAMADO', '4'],
    ['Por tipo de mantenimiento', 'PREVENTIVO', '3'], ['Por tipo de mantenimiento', 'CORRECTIVO', '1']
  ]);
  const filasEstado = leerCsv(crearReporteCsv('equiposPorEstado', [{ valor: 'BAJA', total: 2 }], { generadoEn }).contenido);
  assert.deepEqual(filasEstado[1].slice(11, 13), ['Por estado de equipo', 'BAJA']);
});

test('bloques sin filas no se exportan; resumen con cero sigue siendo un resultado válido', () => {
  const identidad = {};
  for (const [nombre, datos] of [['equiposPorEstado', []], ['equiposPorLaboratorio', []], ['movimientos', { total: 0, porTipo: [] }], ['mantenimientos', { total: 0, porEstado: [], porTipo: [] }]]) {
    assert.equal(puedeExportarReporte(nombre, { estado: 'listo', datos, identidad }, identidad, {}, {}), false);
    assert.throws(() => crearReporteCsv(nombre, datos), /No hay filas/);
  }
  assert.equal(puedeExportarReporte('resumen', { estado: 'listo', datos: resumen, identidad }, identidad, {}, {}), true);
});

test('se rechazan respuestas parciales o valores inválidos en lugar de exportar datos inventados', () => {
  const casos = [
    ['resumen', { totalEquipos: 1 }], ['resumen', { ...resumen, totalEquipos: Infinity }],
    ['equiposPorEstado', [{ valor: 'BAJA', total: -1 }]], ['equiposPorEstado', [{ valor: '', total: 1 }]],
    ['equiposPorLaboratorio', [{ idLaboratorio: 7, total: 3 }]], ['equiposPorLaboratorio', [{ ...laboratorios[0], total: '3' }]],
    ['movimientos', { total: 1 }], ['mantenimientos', { total: 1, porEstado: [] }], ['__proto__', []]
  ];
  const identidad = {};
  for (const [nombre, datos] of casos) {
    assert.throws(() => crearReporteCsv(nombre, datos));
    assert.equal(puedeExportarReporte(nombre, { estado: 'listo', datos, identidad }, identidad, {}, {}), false);
  }
  assert.throws(() => crearReporteCsv('resumen', resumen, { generadoEn: 'fecha inválida' }), /fecha/);
});

test('cambios pendientes, filtros inválidos, errores y consultas de otra sesión o alcance bloquean la descarga', () => {
  const identidad = { sesion: 'actual', alcance: 'laboratorio-7' };
  const consulta = { estado: 'listo', datos: laboratorios, identidad };
  const aplicados = { idLaboratorio: 7, fechaDesde: '2026-10-01' };
  assert.equal(puedeExportarReporte('equiposPorLaboratorio', consulta, identidad, aplicados, { idLaboratorio: '7', fechaDesde: '2026-10-01' }), true);
  assert.equal(puedeExportarReporte('equiposPorLaboratorio', consulta, identidad, aplicados, { idLaboratorio: '8', fechaDesde: '2026-10-01' }), false);
  assert.equal(puedeExportarReporte('equiposPorLaboratorio', consulta, identidad, aplicados, { fechaDesde: '2026-10-03', fechaHasta: '2026-10-01' }), false);
  for (const nuevaIdentidad of [{ ...identidad, sesion: 'nueva' }, { ...identidad, alcance: 'laboratorio-8' }, { ...identidad, reintento: 1 }]) {
    assert.equal(puedeExportarReporte('equiposPorLaboratorio', consulta, nuevaIdentidad, aplicados, aplicados), false);
  }
  for (const estado of ['cargando', 'error']) assert.equal(puedeExportarReporte('equiposPorLaboratorio', { ...consulta, estado }, identidad, aplicados, aplicados), false);
  assert.equal(puedeExportarReporte('equiposPorLaboratorio', consulta, null, aplicados, aplicados), false);
  assert.equal(filtrosCsvCoinciden({ idSede: 1 }, { idSede: '1', estado: '' }), true);
});

function navegadorSimulado(fallar = false) {
  const eventos = [], temporizadores = [], blobs = [];
  const enlace = { click() { eventos.push('click'); if (fallar) throw new Error('Descarga bloqueada'); }, remove() { eventos.push('remove'); } };
  return {
    eventos, temporizadores, blobs, enlace,
    entorno: {
      Blob,
      document: { body: { appendChild(valor) { assert.equal(valor, enlace); eventos.push('append'); } }, createElement(tipo) { assert.equal(tipo, 'a'); return enlace; } },
      URL: { createObjectURL(blob) { blobs.push(blob); return 'blob:reporte-prueba'; }, revokeObjectURL(url) { assert.equal(url, 'blob:reporte-prueba'); eventos.push('revoke'); } },
      setTimeout(callback, demora) { assert.equal(demora, 1000); temporizadores.push(callback); }
    }
  };
}

test('la descarga genera Blob UTF-8 con BOM, retira el enlace y libera su URL después de iniciarse', async () => {
  const navegador = navegadorSimulado();
  const archivo = crearReporteCsv('resumen', resumen, { generadoEn });
  descargarReporteCsv(archivo, navegador.entorno);
  assert.equal(navegador.enlace.download, archivo.nombreArchivo);
  assert.equal(navegador.enlace.href, 'blob:reporte-prueba');
  assert.equal(navegador.blobs[0].type, 'text/csv;charset=utf-8');
  assert.deepEqual([...new Uint8Array(await navegador.blobs[0].arrayBuffer()).slice(0, 3)], [0xef, 0xbb, 0xbf]);
  assert.deepEqual(navegador.eventos, ['append', 'click', 'remove']);
  navegador.temporizadores[0]();
  assert.deepEqual(navegador.eventos, ['append', 'click', 'remove', 'revoke']);
});

test('si el navegador rechaza la descarga se libera el recurso y el error llega a la interfaz', () => {
  const navegador = navegadorSimulado(true);
  assert.throws(() => descargarReporteCsv(crearReporteCsv('resumen', resumen), navegador.entorno), /Descarga bloqueada/);
  assert.deepEqual(navegador.eventos, ['append', 'click', 'remove']);
  navegador.temporizadores[0]();
  assert.equal(navegador.eventos.at(-1), 'revoke');
  assert.throws(() => descargarReporteCsv({}, {}), /navegador/);
});
