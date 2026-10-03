import { useCallback, useEffect, useRef, useState } from 'react';
import { Plus, Wrench, X } from 'lucide-react';
import Boton from '../componentes/Boton';
import { useAuth } from '../contextos/AuthContext';
import { catalogoApi, equiposApi, mantenimientosApi, mensajeError } from '../servicios/api';
import {
  ESTADOS_MANTENIMIENTO, TIPOS_MANTENIMIENTO, equiposParaMantenimiento, etiquetaMantenimiento,
  filtrosMantenimiento, mostrarFechaInstante, mostrarFechaProgramada, payloadEstadoMantenimiento,
  payloadMantenimiento, puedeGestionarMantenimiento, transicionesMantenimiento, validarMantenimientos
} from '../utilidades/mantenimientos.js';

const filtrosIniciales = { idEquipo: '', idLaboratorio: '', estado: '', tipo: '', fechaDesde: '', fechaHasta: '' };
const formularioInicial = { idEquipo: '', tipo: 'PREVENTIVO', descripcion: '', fechaProgramada: '', idResponsable: '', observaciones: '' };
const accionEstado = { EN_PROCESO: 'Iniciar', COMPLETADO: 'Completar', CANCELADO: 'Cancelar mantenimiento' };

export default function Mantenimientos() {
  const permisos = useAuth();
  const { token, esAdmin, alcance, puedeGestionar } = permisos;
  const [items, setItems] = useState([]);
  const [equipos, setEquipos] = useState([]);
  const [laboratorios, setLaboratorios] = useState([]);
  const [filtros, setFiltros] = useState(filtrosIniciales);
  const [aplicados, setAplicados] = useState({});
  const [cargando, setCargando] = useState(true);
  const [opcionesListas, setOpcionesListas] = useState(false);
  const [errorLista, setErrorLista] = useState('');
  const [errorOpciones, setErrorOpciones] = useState('');
  const [errorFiltro, setErrorFiltro] = useState('');
  const [error, setError] = useState('');
  const [exito, setExito] = useState('');
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(formularioInicial);
  const [guardando, setGuardando] = useState(false);
  const solicitud = useRef(0), solicitudOpciones = useRef(0), bloqueo = useRef(false), montado = useRef(true), dialogo = useRef(null);

  const cargar = useCallback(async () => {
    const actual = ++solicitud.current;
    setCargando(true); setErrorLista(''); setItems([]);
    try {
      const { data } = await mantenimientosApi.listar(aplicados);
      const lista = validarMantenimientos(data);
      if (actual === solicitud.current) setItems(lista);
    } catch (err) {
      if (actual === solicitud.current) setErrorLista(mensajeError(err, 'No se pudieron cargar los mantenimientos.'));
    } finally {
      if (actual === solicitud.current) setCargando(false);
    }
  }, [aplicados, token]);

  const cargarOpciones = useCallback(async () => {
    const actual = ++solicitudOpciones.current;
    setOpcionesListas(false); setErrorOpciones('');
    try {
      const [e, l] = await Promise.all([
        esAdmin ? equiposApi.listarAdmin() : equiposApi.listar(),
        esAdmin ? catalogoApi.listarLaboratoriosAdmin() : catalogoApi.laboratorios()
      ]);
      if (!Array.isArray(e.data) || !Array.isArray(l.data)) throw new Error('Las opciones no son válidas.');
      if (actual === solicitudOpciones.current) {
        setEquipos(e.data); setLaboratorios(l.data); setOpcionesListas(true);
      }
    } catch (err) {
      if (actual === solicitudOpciones.current) setErrorOpciones(mensajeError(err, 'No se pudieron cargar equipos y laboratorios.'));
    }
  }, [token, esAdmin]);

  useEffect(() => { cargar(); return () => { solicitud.current += 1; }; }, [cargar]);
  useEffect(() => { cargarOpciones(); return () => { solicitudOpciones.current += 1; }; }, [cargarOpciones]);
  useEffect(() => { montado.current = true; return () => { montado.current = false; }; }, []);
  useEffect(() => {
    if (!modal) return;
    const anterior = document.activeElement;
    dialogo.current?.querySelector('input:not(:disabled),select:not(:disabled),textarea,button')?.focus();
    const teclado = event => {
      if (event.key === 'Escape' && !bloqueo.current) setModal(null);
      if (event.key !== 'Tab') return;
      const controles = [...(dialogo.current?.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled)') || [])];
      const primero = controles[0], ultimo = controles.at(-1);
      if (event.shiftKey && document.activeElement === primero) { event.preventDefault(); ultimo?.focus(); }
      else if (!event.shiftKey && document.activeElement === ultimo) { event.preventDefault(); primero?.focus(); }
    };
    document.addEventListener('keydown', teclado);
    return () => { document.removeEventListener('keydown', teclado); anterior?.focus?.(); };
  }, [modal]);

  const labsVisibles = laboratorios.filter(lab => esAdmin || alcance?.laboratorios?.some(permitido => permitido.id === lab.id));
  const elegibles = equiposParaMantenimiento(equipos, laboratorios, permisos);
  const cerrar = () => { if (!bloqueo.current) { setModal(null); setError(''); } };
  const gestionar = item => puedeGestionarMantenimiento(item, permisos);
  const abrir = (item = null) => {
    if (!puedeGestionar || bloqueo.current || (item && (!gestionar(item) || item.estado !== 'PROGRAMADO' || item.equipo.estado === 'BAJA'))) return;
    setError(''); setExito('');
    setForm(item ? { idEquipo: item.equipo.id, tipo: item.tipo, descripcion: item.descripcion, fechaProgramada: item.fechaProgramada, idResponsable: item.responsable?.id ?? '', observaciones: item.observaciones ?? '' } : { ...formularioInicial });
    setModal({ modo: item ? 'editar' : 'crear', item });
  };
  const abrirEstado = (item, destino) => {
    if (bloqueo.current || !gestionar(item) || !transicionesMantenimiento(item.estado).includes(destino)) return;
    setError(''); setExito(''); setForm({ observaciones: item.observaciones ?? '' });
    setModal({ modo: 'estado', item, destino });
  };

  async function guardar(event) {
    event.preventDefault();
    if (bloqueo.current || !puedeGestionar || !modal) return;
    if (modal.item && !gestionar(modal.item)) return;
    let payload;
    try {
      payload = modal.modo === 'estado'
        ? payloadEstadoMantenimiento(modal.item.estado, modal.destino, form.observaciones)
        : payloadMantenimiento(form, modal.modo === 'editar');
      if (modal.modo === 'crear' && !elegibles.some(equipo => equipo.id === payload.idEquipo)) throw new Error('Selecciona un equipo vigente dentro de tu alcance.');
    } catch (err) { setError(err.message); return; }
    bloqueo.current = true; setGuardando(true); setError(''); setExito('');
    try {
      if (modal.modo === 'crear') await mantenimientosApi.crear(payload);
      else if (modal.modo === 'editar') await mantenimientosApi.actualizar(modal.item.id, payload);
      else await mantenimientosApi.cambiarEstado(modal.item.id, payload);
      if (!montado.current) return;
      setModal(null); setExito('Mantenimiento guardado. Los datos se vuelven a consultar al servidor.');
      await Promise.all([cargar(), cargarOpciones()]);
    } catch (err) {
      if (montado.current) setError(mensajeError(err, 'No se pudo guardar el mantenimiento.'));
    } finally {
      bloqueo.current = false;
      if (montado.current) setGuardando(false);
    }
  }

  function aplicar(event) {
    event.preventDefault();
    try { setAplicados(filtrosMantenimiento(filtros)); setErrorFiltro(''); }
    catch (err) { setErrorFiltro(err.message); }
  }
  const actualizar = event => setFiltros(actual => ({ ...actual, [event.target.name]: event.target.value }));
  const editarCampo = event => setForm(actual => ({ ...actual, [event.target.name]: event.target.value }));
  const disponible = !cargando && !errorLista;

  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Gestión</span><h1>Mantenimientos</h1><p>Programación y seguimiento de los equipos dentro de tu alcance.</p></div>
      {puedeGestionar && <Boton disabled={guardando || !opcionesListas || !elegibles.length} onClick={() => abrir()}><Plus size={17}/> Nuevo mantenimiento</Boton>}
    </div>
    <div className="contadores-grid tres" aria-busy={cargando}>{[['PROGRAMADO', 'Programados'], ['EN_PROCESO', 'En proceso'], ['COMPLETADO', 'Completados']].map(([estado, titulo]) => <div className="contador-card" key={estado}><div className="contador-icono azul"><Wrench/></div><div><span>{titulo}</span><strong>{disponible ? items.filter(item => item.estado === estado).length : '—'}</strong></div></div>)}</div>
    <form className="barra-herramientas filtros-modulo" onSubmit={aplicar}>
      <label>Equipo<select name="idEquipo" value={filtros.idEquipo} onChange={actualizar} disabled={guardando}><option value="">Todos los equipos</option>{equipos.map(equipo => <option key={equipo.id} value={equipo.id}>{equipo.codigoInterno} · {equipo.nombre}</option>)}</select></label>
      <label>Laboratorio<select name="idLaboratorio" value={filtros.idLaboratorio} onChange={actualizar} disabled={guardando}><option value="">Todos los laboratorios de mi alcance</option>{labsVisibles.map(lab => <option key={lab.id} value={lab.id}>{lab.codigo} · {lab.nombre}</option>)}</select></label>
      <label>Estado<select name="estado" value={filtros.estado} onChange={actualizar} disabled={guardando}><option value="">Todos</option>{ESTADOS_MANTENIMIENTO.map(valor => <option key={valor} value={valor}>{etiquetaMantenimiento(valor)}</option>)}</select></label>
      <label>Tipo<select name="tipo" value={filtros.tipo} onChange={actualizar} disabled={guardando}><option value="">Todos</option>{TIPOS_MANTENIMIENTO.map(valor => <option key={valor} value={valor}>{etiquetaMantenimiento(valor)}</option>)}</select></label>
      <label>Programado desde<input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={actualizar} disabled={guardando}/></label>
      <label>Programado hasta<input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={actualizar} disabled={guardando}/></label>
      <Boton tipo="submit" disabled={guardando}>Aplicar filtros</Boton>
      <Boton variante="secundario" disabled={guardando} onClick={() => { setFiltros({ ...filtrosIniciales }); setAplicados({}); setErrorFiltro(''); }}>Limpiar</Boton>
    </form>
    {errorFiltro && <div className="mensaje-error" role="alert">{errorFiltro}</div>}
    {errorOpciones && <div className="mensaje-error" role="alert"><span>{errorOpciones}</span><Boton variante="secundario" disabled={guardando} onClick={cargarOpciones}>Reintentar opciones</Boton></div>}
    {puedeGestionar && opcionesListas && !elegibles.length && <p>No hay equipos vigentes disponibles para programar un mantenimiento.</p>}
    {exito && <div className="mensaje-exito" role="status">{exito}</div>}
    <section className="panel" aria-busy={cargando}>
      <div className="panel-titulo"><h2>Lista de mantenimientos</h2><small>Los indicadores corresponden a los filtros aplicados.</small></div>
      {cargando ? <div className="vacio" role="status">Cargando mantenimientos...</div> : errorLista ? <div className="mensaje-error" role="alert"><span>{errorLista}</span><Boton variante="secundario" disabled={guardando} onClick={cargar}>Reintentar</Boton></div> : !items.length ? <div className="vacio">No hay mantenimientos para los filtros seleccionados.</div> : <div className="tabla-wrap"><table><thead><tr><th>Equipo</th><th>Laboratorio</th><th>Tipo / descripción</th><th>Programado</th><th>Inicio</th><th>Fin</th><th>Responsable</th><th>Estado</th>{puedeGestionar && <th>Acciones</th>}</tr></thead><tbody>{items.map(item => <tr key={item.id}>
        <td><strong>{item.equipo.codigoInterno}</strong><small>{item.equipo.nombre}</small><small>Equipo: {item.equipo.estado}</small></td>
        <td>{item.equipo.laboratorio?.codigo || '—'}<small>{item.equipo.laboratorio?.nombre}</small></td>
        <td><strong>{etiquetaMantenimiento(item.tipo)}</strong><small>{item.descripcion}</small>{item.observaciones && <small>Observaciones: {item.observaciones}</small>}</td>
        <td>{mostrarFechaProgramada(item.fechaProgramada)}</td><td>{mostrarFechaInstante(item.fechaInicio)}</td><td>{mostrarFechaInstante(item.fechaFin)}</td>
        <td>{item.responsable ? [item.responsable.nombre, item.responsable.apellido].filter(Boolean).join(' ') || item.responsable.userName : 'Sin asignar'}</td>
        <td><span className={`estado estado-${item.estado === 'COMPLETADO' ? 'activo' : item.estado === 'CANCELADO' ? 'inactivo' : 'mantenimiento'}`}>{etiquetaMantenimiento(item.estado)}</span></td>
        {puedeGestionar && <td>{gestionar(item) && <div className="acciones-tabla acciones-mantenimiento">
          {item.estado === 'PROGRAMADO' && item.equipo.estado !== 'BAJA' && <button disabled={guardando} onClick={() => abrir(item)}>Editar</button>}
          {transicionesMantenimiento(item.estado).filter(destino => destino !== 'EN_PROCESO' || item.equipo.estado !== 'BAJA').map(destino => <button className={destino === 'CANCELADO' ? 'peligro' : ''} key={destino} disabled={guardando} onClick={() => abrirEstado(item, destino)}>{accionEstado[destino]}</button>)}
        </div>}</td>}
      </tr>)}</tbody></table></div>}
    </section>
    {modal && <div className="modal-fondo" onMouseDown={event => { if (event.target === event.currentTarget) cerrar(); }}><div className="modal" role="dialog" aria-modal="true" aria-labelledby="titulo-mantenimiento" ref={dialogo}>
      <div className="modal-cabecera"><div><span className="eyebrow">Mantenimientos</span><h2 id="titulo-mantenimiento">{modal.modo === 'estado' ? accionEstado[modal.destino] : modal.modo === 'crear' ? 'Nuevo mantenimiento' : 'Editar mantenimiento'}</h2></div><button className="icono-btn" aria-label="Cerrar formulario" disabled={guardando} onClick={cerrar}><X/></button></div>
      <form className="form-grid" onSubmit={guardar} aria-busy={guardando}>
        {modal.modo === 'estado' ? <p className="campo-ancho">{modal.item.equipo.codigoInterno}: {etiquetaMantenimiento(modal.item.estado)} → {etiquetaMantenimiento(modal.destino)}. El servidor actualizará el estado del equipo cuando corresponda.</p> : <>
          {modal.modo === 'crear' ? <label className="campo-ancho">Equipo<select name="idEquipo" required value={form.idEquipo} onChange={editarCampo} disabled={guardando}><option value="">Seleccionar equipo</option>{elegibles.map(equipo => <option key={equipo.id} value={equipo.id}>{equipo.codigoInterno} · {equipo.nombre} · {equipo.laboratorio?.codigo}</option>)}</select></label> : <p className="campo-ancho">Equipo: {modal.item.equipo.codigoInterno} · {modal.item.equipo.nombre}</p>}
          <label>Tipo<select name="tipo" required value={form.tipo} onChange={editarCampo} disabled={guardando}>{TIPOS_MANTENIMIENTO.map(tipo => <option key={tipo} value={tipo}>{etiquetaMantenimiento(tipo)}</option>)}</select></label>
          <label>Fecha programada<input name="fechaProgramada" type="date" required value={form.fechaProgramada} onChange={editarCampo} disabled={guardando}/></label>
          <label className="campo-ancho">Descripción<textarea name="descripcion" required maxLength={2000} value={form.descripcion} onChange={editarCampo} disabled={guardando}/></label>
          <label>ID de responsable (opcional)<input name="idResponsable" type="number" min="1" step="1" value={form.idResponsable} onChange={editarCampo} disabled={guardando}/></label>
        </>}
        <label className="campo-ancho">Observaciones<textarea name="observaciones" maxLength={4000} value={form.observaciones} onChange={editarCampo} disabled={guardando}/></label>
        {error && <div className="mensaje-error campo-ancho" role="alert">{error}</div>}
        <div className="modal-acciones campo-ancho"><Boton variante="secundario" disabled={guardando} onClick={cerrar}>Volver</Boton><Boton tipo="submit" disabled={guardando}>{guardando ? 'Guardando...' : modal.modo === 'estado' ? accionEstado[modal.destino] : 'Guardar mantenimiento'}</Boton></div>
      </form>
    </div></div>}
  </div>;
}
