import { useEffect, useState } from 'react';
import { BarChart3 } from 'lucide-react';
import Boton from '../componentes/Boton';
import ResumenSeleccion from '../componentes/ResumenSeleccion';
import { useAuth } from '../contextos/AuthContext';
import { catalogoApi, mensajeError, reportesApi } from '../servicios/api';
import { ESTADOS_MANTENIMIENTO, TIPOS_MANTENIMIENTO, etiquetaMantenimiento } from '../utilidades/mantenimientos.js';
import { filtrosReportes, validarConteos, validarEquiposPorLaboratorio, validarReporteMantenimientos, validarReporteMovimientos, validarResumenReporte } from '../utilidades/reportes.js';

const inicial = { idSede: '', idArea: '', idLaboratorio: '', estado: '', estadoMantenimiento: '', tipoMantenimiento: '', fechaDesde: '', fechaHasta: '' };
const cargando = () => ({ estado: 'cargando', datos: null, error: '' });
const inicialReportes = () => Object.fromEntries(['resumen', 'equiposPorEstado', 'equiposPorLaboratorio', 'movimientos', 'mantenimientos'].map(nombre => [nombre, cargando()]));
const etiquetas = { OPERATIVO: 'Operativo', MANTENIMIENTO: 'En mantenimiento', INOPERATIVO: 'Inoperativo', BAJA: 'Baja', TRASLADO: 'Traslado' };
const etiqueta = valor => etiquetas[valor] || etiquetaMantenimiento(valor);

function Conteos({ filas, encabezado }) {
  return filas.length ? <div className="tabla-wrap"><table className="tabla-reporte"><thead><tr><th>{encabezado}</th><th>Total</th></tr></thead><tbody>{filas.map(fila => <tr key={fila.valor}><td>{etiqueta(fila.valor)}</td><td>{fila.total}</td></tr>)}</tbody></table></div> : <div className="vacio">No hay registros para los filtros aplicados.</div>;
}

function PanelReporte({ titulo, consulta, reintentar, children }) {
  return <section className="panel" aria-busy={consulta.estado === 'cargando'}>
    <div className="panel-titulo"><h2>{titulo}</h2></div>
    {consulta.estado === 'cargando' ? <div className="vacio" role="status">Cargando reporte...</div> : consulta.estado === 'error' ? <div className="vacio"><p className="mensaje-error" role="alert">{consulta.error}</p><Boton variante="secundario" onClick={reintentar}>Reintentar reportes</Boton></div> : children(consulta.datos)}
  </section>;
}

export default function Reportes() {
  const { token, esAdmin, alcance } = useAuth();
  const [filtros, setFiltros] = useState(inicial);
  const [aplicados, setAplicados] = useState({});
  const [reportes, setReportes] = useState(inicialReportes);
  const [catalogos, setCatalogos] = useState({ sedes: [], areas: [], laboratorios: [] });
  const [errorFiltro, setErrorFiltro] = useState('');
  const [errorCatalogos, setErrorCatalogos] = useState('');
  const [intento, setIntento] = useState(0);
  const reintentar = () => setIntento(valor => valor + 1);

  useEffect(() => {
    let vigente = true;
    const solicitud = new AbortController();
    setReportes(inicialReportes());
    const validadores = { resumen: validarResumenReporte, equiposPorEstado: validarConteos, equiposPorLaboratorio: validarEquiposPorLaboratorio, movimientos: validarReporteMovimientos, mantenimientos: validarReporteMantenimientos };
    for (const [nombre, validar] of Object.entries(validadores)) {
      reportesApi[nombre](aplicados, { signal: solicitud.signal }).then(({ data }) => {
        const datos = validar(data);
        if (vigente) setReportes(actual => ({ ...actual, [nombre]: { estado: 'listo', datos, error: '' } }));
      }).catch(err => {
        if (vigente) setReportes(actual => ({ ...actual, [nombre]: { estado: 'error', datos: null, error: mensajeError(err, 'No se pudo cargar este reporte.') } }));
      });
    }
    return () => { vigente = false; solicitud.abort(); };
  }, [aplicados, intento, token]);

  useEffect(() => {
    let vigente = true;
    setErrorCatalogos('');
    Promise.all(esAdmin
      ? [catalogoApi.listarSedesAdmin(), catalogoApi.listarAreasAdmin(), catalogoApi.listarLaboratoriosAdmin()]
      : [catalogoApi.sedes(), catalogoApi.areas(), catalogoApi.laboratorios()]).then(([s, a, l]) => {
      if (![s.data, a.data, l.data].every(Array.isArray)) throw new Error('Catálogos inválidos.');
      if (vigente) setCatalogos({ sedes: s.data, areas: a.data, laboratorios: l.data });
    }).catch(err => { if (vigente) setErrorCatalogos(mensajeError(err, 'No se pudieron cargar las ubicaciones para los filtros.')); });
    return () => { vigente = false; };
  }, [intento, token, esAdmin]);

  const areas = catalogos.areas.filter(area => !filtros.idSede || area.sede?.id === Number(filtros.idSede));
  const labs = catalogos.laboratorios.filter(lab => (esAdmin || alcance?.laboratorios?.some(permitido => permitido.id === lab.id)) &&
    (!filtros.idArea || lab.area?.id === Number(filtros.idArea)) &&
    (!filtros.idSede || catalogos.areas.some(area => area.id === lab.area?.id && area.sede?.id === Number(filtros.idSede))));
  function cambiar(event) {
    const { name, value } = event.target;
    setFiltros(actual => ({ ...actual, [name]: value, ...(name === 'idSede' ? { idArea: '', idLaboratorio: '' } : name === 'idArea' ? { idLaboratorio: '' } : {}) }));
  }
  function aplicar(event) {
    event.preventDefault();
    try { setAplicados(filtrosReportes(filtros)); setErrorFiltro(''); }
    catch (err) { setErrorFiltro(err.message); }
  }
  const resumen = reportes.resumen;
  const detalle = resumen.estado === 'cargando' ? 'Cargando...' : resumen.estado === 'error' ? 'No disponible' : 'Según filtros y alcance';

  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Indicadores</span><h1>Reportes</h1><p>Agregados del inventario calculados por el servidor según tu alcance.</p></div><BarChart3 size={34}/></div>
    <form className="barra-herramientas filtros-modulo" onSubmit={aplicar}>
      <label>Sede<select name="idSede" value={filtros.idSede} onChange={cambiar}><option value="">Todas las sedes</option>{catalogos.sedes.map(sede => <option key={sede.id} value={sede.id}>{sede.nombre}</option>)}</select></label>
      <label>Área<select name="idArea" value={filtros.idArea} onChange={cambiar}><option value="">Todas las áreas</option>{areas.map(area => <option key={area.id} value={area.id}>{area.nombre}</option>)}</select></label>
      <label>Laboratorio<select name="idLaboratorio" value={filtros.idLaboratorio} onChange={cambiar}><option value="">Todos los laboratorios de mi alcance</option>{labs.map(lab => <option key={lab.id} value={lab.id}>{lab.codigo} · {lab.nombre}</option>)}</select></label>
      <label>Estado de equipo<select name="estado" value={filtros.estado} onChange={cambiar}><option value="">Todos</option>{['OPERATIVO', 'MANTENIMIENTO', 'INOPERATIVO', 'BAJA'].map(valor => <option key={valor} value={valor}>{etiqueta(valor)}</option>)}</select></label>
      <label>Estado de mantenimiento<select name="estadoMantenimiento" value={filtros.estadoMantenimiento} onChange={cambiar}><option value="">Todos</option>{ESTADOS_MANTENIMIENTO.map(valor => <option key={valor} value={valor}>{etiqueta(valor)}</option>)}</select></label>
      <label>Tipo de mantenimiento<select name="tipoMantenimiento" value={filtros.tipoMantenimiento} onChange={cambiar}><option value="">Todos</option>{TIPOS_MANTENIMIENTO.map(valor => <option key={valor} value={valor}>{etiqueta(valor)}</option>)}</select></label>
      <label>Fecha desde<input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiar}/></label>
      <label>Fecha hasta<input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiar}/></label>
      <Boton tipo="submit">Aplicar filtros</Boton><Boton variante="secundario" onClick={() => { setFiltros({ ...inicial }); setAplicados({}); setErrorFiltro(''); }}>Limpiar</Boton>
    </form>
    <p className="nota-reporte">El rango filtra la fecha de registro de equipos, la fecha del movimiento (días UTC) y la fecha programada de mantenimiento. El estado y tipo de mantenimiento solo afectan sus indicadores.</p>
    {errorFiltro && <div className="mensaje-error" role="alert">{errorFiltro}</div>}
    {errorCatalogos && <div className="mensaje-error" role="alert"><span>{errorCatalogos}</span><Boton variante="secundario" onClick={reintentar}>Reintentar ubicaciones</Boton></div>}
    {resumen.estado === 'error' && <div className="mensaje-error" role="alert"><span>{resumen.error}</span><Boton variante="secundario" onClick={reintentar}>Reintentar resumen</Boton></div>}
    <div className="contadores-grid tres" aria-busy={resumen.estado === 'cargando'}>
      <ResumenSeleccion titulo="Total equipos" valor={resumen.datos?.totalEquipos ?? '—'} detalle={detalle}/>
      <ResumenSeleccion titulo="Total movimientos" valor={resumen.datos?.totalMovimientos ?? '—'} detalle={detalle}/>
      <ResumenSeleccion titulo="Total mantenimientos" valor={resumen.datos?.totalMantenimientos ?? '—'} detalle={detalle}/>
    </div>
    <div className="reportes-grid">
      <PanelReporte titulo="Equipos por estado" consulta={reportes.equiposPorEstado} reintentar={reintentar}>{datos => <Conteos filas={datos} encabezado="Estado de equipo"/>}</PanelReporte>
      <PanelReporte titulo="Equipos por laboratorio" consulta={reportes.equiposPorLaboratorio} reintentar={reintentar}>{datos => datos.length ? <div className="tabla-wrap"><table className="tabla-reporte"><thead><tr><th>Laboratorio</th><th>Total equipos</th></tr></thead><tbody>{datos.map(fila => <tr key={fila.idLaboratorio}><td><strong>{fila.codigo}</strong><small>{fila.nombre}</small></td><td>{fila.total}</td></tr>)}</tbody></table></div> : <div className="vacio">No hay equipos para los filtros aplicados.</div>}</PanelReporte>
      <PanelReporte titulo="Movimientos por tipo" consulta={reportes.movimientos} reintentar={reintentar}>{datos => <><p className="total-reporte">Total de movimientos: <strong>{datos.total}</strong></p><Conteos filas={datos.porTipo} encabezado="Tipo de movimiento"/></>}</PanelReporte>
      <PanelReporte titulo="Mantenimientos" consulta={reportes.mantenimientos} reintentar={reintentar}>{datos => <><p className="total-reporte">Total de mantenimientos: <strong>{datos.total}</strong></p><Conteos filas={datos.porEstado} encabezado="Estado de mantenimiento"/><Conteos filas={datos.porTipo} encabezado="Tipo de mantenimiento"/></>}</PanelReporte>
    </div>
  </div>;
}
