import { useCallback, useEffect, useRef, useState } from 'react';
import { Plus, Search, X, Boxes, CheckCircle, Wrench } from 'lucide-react';
import Boton from '../componentes/Boton';
import TarjetaEquipo from '../componentes/TarjetaEquipo';
import FiltroEquipo from '../componentes/FiltroEquipo';
import { catalogoApi, equiposApi, movimientosApi, mensajeError } from '../servicios/api';
import { useAuth } from '../contextos/AuthContext';
import { errorValidacion, laboratoriosPermitidos, lista, payloadEquipo } from '../utilidades/equipos';

const inicial = { codigoInterno: '', nombre: '', estado: 'OPERATIVO', requiereMantenimiento: false, idSubcategoria: '', idLaboratorio: '', serieUtec: '', numeroSerie: '', marca: '', modelo: '', anio: '', ordenCompra: '', ubicacionInterna: '', comentario: '', idResponsable: '' };
const filtrosIniciales = { estado: '', idLaboratorio: '', idSubcategoria: '', requiereMantenimiento: '' };
const campos = [['codigoInterno', 'Código interno', 50], ['nombre', 'Nombre', 150], ['marca', 'Marca', 100], ['modelo', 'Modelo', 100], ['serieUtec', 'Serie UTEC', 100], ['numeroSerie', 'Número de serie', 100], ['anio', 'Año'], ['ordenCompra', 'Orden de compra', 50], ['ubicacionInterna', 'Ubicación interna', 200]];

export default function Equipos() {
  const { puedeGestionar, esAdmin, alcance } = useAuth();
  const [equipos, setEquipos] = useState([]), [labs, setLabs] = useState([]), [subs, setSubs] = useState([]);
  const [filtros, setFiltros] = useState(filtrosIniciales), [aplicados, setAplicados] = useState(filtrosIniciales);
  const [modal, setModal] = useState(null), [form, setForm] = useState(inicial), [traslado, setTraslado] = useState({});
  const [error, setError] = useState(''), [busqueda, setBusqueda] = useState('');
  const [errorLista, setErrorLista] = useState(''), [errorCatalogos, setErrorCatalogos] = useState('');
  const [cargando, setCargando] = useState(true), [catalogosListos, setCatalogosListos] = useState(false), [ocupado, setOcupado] = useState(false);
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
  const bloqueo = useRef(false), solicitud = useRef(0), solicitudCatalogos = useRef(0);
  const cargar = useCallback(async () => {
    const actual = ++solicitud.current;
    setCargando(true); setErrorLista('');
    try {
      const response = await equiposApi.listar(Object.fromEntries(Object.entries(aplicados).filter(([, value]) => value !== '')));
      if (actual === solicitud.current) setEquipos(lista(response.data));
    } catch (err) {
      if (actual === solicitud.current) setErrorLista(mensajeError(err, 'No se pudieron cargar los equipos.'));
    } finally { if (actual === solicitud.current) setCargando(false); }
  }, [aplicados]);
  useEffect(() => { cargar(); return () => { solicitud.current += 1; }; }, [cargar]);
  const cargarCatalogos = useCallback(async () => {
    const actual = ++solicitudCatalogos.current;
    setCargandoCatalogos(true); setCatalogosListos(false); setErrorCatalogos('');
    try {
      const [l, s] = await Promise.all([catalogoApi.laboratorios(), catalogoApi.subcategorias()]);
      if (actual === solicitudCatalogos.current) { setLabs(lista(l.data)); setSubs(lista(s.data)); setCatalogosListos(true); }
    } catch (err) {
      if (actual === solicitudCatalogos.current) setErrorCatalogos(mensajeError(err, 'No se pudieron cargar los laboratorios y subcategorías.'));
    } finally { if (actual === solicitudCatalogos.current) setCargandoCatalogos(false); }
  }, []);
  useEffect(() => { cargarCatalogos(); return () => { solicitudCatalogos.current += 1; }; }, [cargarCatalogos]);
  const permitidos = laboratoriosPermitidos(labs, alcance, esAdmin);
  const subcategorias = subs.filter((sub) => sub.activo === true);
  const visibles = equipos.filter((equipo) => [equipo.nombre, equipo.codigoInterno, equipo.marca, equipo.modelo, equipo.laboratorio?.nombre].filter(Boolean).join(' ').toLowerCase().includes(busqueda.toLowerCase()));
  const destinos = permitidos.filter((lab) => lab.id !== form.laboratorio?.id);
  const puedeModificar = (equipo) => puedeGestionar && equipo?.estado !== 'BAJA' && (esAdmin || permitidos.some((lab) => lab.id === equipo?.laboratorio?.id));
  const cerrar = () => { if (!bloqueo.current) { setModal(null); setError(''); } };
  function abrir(equipo) {
    if (!puedeGestionar || bloqueo.current || (equipo && !puedeModificar(equipo))) return;
    setForm(equipo ? { ...inicial, ...equipo, idSubcategoria: equipo.subcategoria?.id ?? '', idLaboratorio: equipo.laboratorio?.id ?? '', idResponsable: equipo.responsable?.id ?? '', requiereMantenimiento: Boolean(equipo.requiereMantenimiento) } : { ...inicial });
    setModal(equipo ? 'editar' : 'crear'); setError('');
  }
  async function ejecutar(accion) {
    if (bloqueo.current) return;
    bloqueo.current = true; setOcupado(true); setError('');
    try { await accion(); setModal(null); await cargar(); }
    catch (err) { setError(err.code === 'VALIDACION_LOCAL' ? err.message : mensajeError(err, 'No se pudo completar la operación.')); }
    finally { bloqueo.current = false; setOcupado(false); }
  }
  function guardar(event) {
    event.preventDefault();
    if (!puedeGestionar || (modal === 'editar' && !puedeModificar(form))) return;
    ejecutar(async () => {
      const payload = payloadEquipo(form, modal === 'editar');
      if (modal === 'crear') {
        if (!permitidos.some((lab) => lab.id === payload.idLaboratorio)) throw errorValidacion('Selecciona un laboratorio de tu alcance.');
        await equiposApi.crear(payload);
      } else await equiposApi.actualizar(form.id, payload);
    });
  }
  function baja(equipo) {
    if (!puedeModificar(equipo) || bloqueo.current || !window.confirm(`¿Dar de baja el equipo ${equipo.codigoInterno}? Esta baja es definitiva.`)) return;
    ejecutar(() => equiposApi.baja(equipo.id));
  }
  function abrirTraslado(equipo) {
    if (!puedeModificar(equipo) || bloqueo.current) return;
    setForm(equipo); setTraslado({ idLaboratorioDestino: '', motivo: '', ubicacionInternaDestino: '' }); setError(''); setModal('trasladar');
  }
  function guardarTraslado(event) {
    event.preventDefault();
    if (!puedeModificar(form)) return;
    ejecutar(async () => {
      const idLaboratorioDestino = Number(traslado.idLaboratorioDestino);
      if (!destinos.some((lab) => lab.id === idLaboratorioDestino)) throw errorValidacion('Selecciona un laboratorio destino disponible.');
      await movimientosApi.trasladar(form.id, { idLaboratorioDestino, motivo: traslado.motivo.trim(), ubicacionInternaDestino: traslado.ubicacionInternaDestino.trim() || null });
    });
  }
  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Inventario</span><h1>Equipos</h1><p>Consulta y administra los equipos según el alcance de tu usuario.</p></div>{puedeGestionar && <Boton disabled={ocupado || !catalogosListos || !permitidos.length || !subcategorias.length} onClick={() => abrir()}><Plus size={17}/> Nuevo equipo</Boton>}</div>
    <div className="contadores-grid tres">
      <div className="contador-card"><div className="contador-icono azul"><Boxes/></div><div><span>Total equipos</span><strong>{cargando || errorLista ? '—' : equipos.length}</strong></div></div>
      <div className="contador-card"><div className="contador-icono verde"><CheckCircle/></div><div><span>Operativos</span><strong>{cargando || errorLista ? '—' : equipos.filter((equipo) => equipo.estado === 'OPERATIVO').length}</strong></div></div>
      <div className="contador-card"><div className="contador-icono rojo"><Wrench/></div><div><span>En mantenimiento</span><strong>{cargando || errorLista ? '—' : equipos.filter((equipo) => equipo.estado === 'MANTENIMIENTO').length}</strong></div></div>
    </div>
    <div className="barra-herramientas"><div className="busqueda"><Search size={17}/><input value={busqueda} onChange={(event) => setBusqueda(event.target.value)} placeholder="Buscar por código, nombre, marca..." aria-label="Buscar equipos"/></div><FiltroEquipo filtros={filtros} setFiltros={setFiltros} laboratorios={permitidos} subcategorias={subcategorias} disabled={ocupado || cargando} onApply={() => { setError(''); setAplicados({ ...filtros }); }}/></div>
    {cargandoCatalogos && <p role="status">Cargando laboratorios y subcategorías…</p>}
    {errorCatalogos && <div className="mensaje-error" role="alert"><span>No se pudieron cargar las opciones de laboratorio y subcategoría. {errorCatalogos}</span><Boton variante="secundario" disabled={ocupado || cargandoCatalogos} onClick={cargarCatalogos}>Reintentar opciones</Boton></div>}
    {error && !modal && <div className="mensaje-error" role="alert">{error}<button aria-label="Cerrar error" onClick={() => setError('')}><X size={15}/></button></div>}
    {cargando ? <div className="vacio" role="status">Cargando equipos…</div> : errorLista ? <div className="mensaje-error" role="alert"><span>{errorLista}</span><Boton variante="secundario" disabled={ocupado} onClick={cargar}>Reintentar equipos</Boton></div> : <div className="equipos-grid">{visibles.map((equipo) => <TarjetaEquipo key={equipo.id} equipo={equipo} puedeGestionar={puedeModificar(equipo)} disabled={ocupado || !catalogosListos} onEdit={abrir} onMove={abrirTraslado} onDelete={baja}/>)}{!visibles.length && <div className="vacio grande">No se encontraron equipos con los filtros actuales.</div>}</div>}
    {modal && <div className="modal-fondo" onMouseDown={cerrar}><div className="modal" role="dialog" aria-modal="true" aria-labelledby="titulo-equipo" onMouseDown={(event) => event.stopPropagation()}><div className="modal-cabecera"><div><span className="eyebrow">Inventario</span><h2 id="titulo-equipo">{modal === 'crear' ? 'Registrar equipo' : modal === 'editar' ? 'Editar equipo' : `Trasladar ${form.codigoInterno}`}</h2></div><button className="icono-btn" disabled={ocupado} aria-label="Cerrar" onClick={cerrar}><X/></button></div>
      {modal === 'trasladar' ? <form onSubmit={guardarTraslado}>
        <label>Laboratorio destino<select required disabled={ocupado} value={traslado.idLaboratorioDestino} onChange={(event) => setTraslado({ ...traslado, idLaboratorioDestino: event.target.value })}><option value="">Seleccionar</option>{destinos.map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} · {lab.nombre}</option>)}</select></label>
        {!destinos.length && <p>No hay otro laboratorio activo disponible en tu alcance.</p>}
        <label>Motivo<textarea required maxLength={500} disabled={ocupado} value={traslado.motivo} onChange={(event) => setTraslado({ ...traslado, motivo: event.target.value })}/></label>
        <label>Ubicación interna destino<input maxLength={200} disabled={ocupado} value={traslado.ubicacionInternaDestino} onChange={(event) => setTraslado({ ...traslado, ubicacionInternaDestino: event.target.value })}/></label>
        {error && <div className="mensaje-error" role="alert">{error}</div>}
        <div className="modal-acciones"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado || !destinos.length}>{ocupado ? 'Guardando…' : 'Registrar traslado'}</Boton></div>
      </form> : <form onSubmit={guardar} className="form-grid">
        {campos.map(([name, label, maxLength]) => <label key={name}>{label}<input type={name === 'anio' ? 'number' : 'text'} min={name === 'anio' ? 1900 : undefined} max={name === 'anio' ? 2100 : undefined} maxLength={maxLength} disabled={ocupado || (name === 'codigoInterno' && modal === 'editar')} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })} required={['codigoInterno', 'nombre'].includes(name)}/></label>)}
        <label>Estado<select disabled={ocupado} value={form.estado} onChange={(event) => setForm({ ...form, estado: event.target.value })}><option>OPERATIVO</option><option>MANTENIMIENTO</option><option>INOPERATIVO</option></select></label>
        <label>Subcategoría<select required disabled={ocupado} value={form.idSubcategoria} onChange={(event) => setForm({ ...form, idSubcategoria: event.target.value })}><option value="">Seleccionar</option>{subcategorias.map((sub) => <option key={sub.id} value={sub.id}>{sub.nombre}</option>)}</select></label>
        <label>Laboratorio{modal === 'editar' ? <input disabled value={[form.laboratorio?.codigo, form.laboratorio?.nombre].filter(Boolean).join(' · ')}/> : <select required disabled={ocupado} value={form.idLaboratorio} onChange={(event) => setForm({ ...form, idLaboratorio: event.target.value })}><option value="">Seleccionar</option>{permitidos.map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} · {lab.nombre}</option>)}</select>}</label>
        <label>ID de responsable (opcional)<input type="number" min="1" step="1" disabled={ocupado} value={form.idResponsable ?? ''} onChange={(event) => setForm({ ...form, idResponsable: event.target.value })}/></label>
        <label className="check-label"><input type="checkbox" disabled={ocupado} checked={form.requiereMantenimiento} onChange={(event) => setForm({ ...form, requiereMantenimiento: event.target.checked })}/> Requiere mantenimiento</label>
        <label className="campo-ancho">Comentario<textarea disabled={ocupado} value={form.comentario ?? ''} onChange={(event) => setForm({ ...form, comentario: event.target.value })}/></label>
        {error && <div className="mensaje-error campo-ancho" role="alert">{error}</div>}
        <div className="modal-acciones campo-ancho"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : 'Guardar equipo'}</Boton></div>
      </form>}
    </div></div>}
  </div>;
}
