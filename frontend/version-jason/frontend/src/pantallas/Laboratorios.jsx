import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Plus, Pencil, Trash2, Building2, Layers3, FlaskConical, Search, ChevronDown, ChevronRight, RotateCcw } from 'lucide-react';
import Boton from '../componentes/Boton';
import { catalogoApi, mensajeError } from '../servicios/api';
import { useAuth } from '../contextos/AuthContext';
import { cargarCatalogos, formularioCatalogo, guardarCatalogo, estadoLaboratorio, etiquetaEstadoLaboratorio } from '../utilidades/catalogos';
import './Catalogo.css';

const config = {
  laboratorios: { titulo: 'Laboratorios', singular: 'laboratorio', metodo: 'Laboratorio', campos: [['codigo', 'Código', 30], ['nombre', 'Nombre', 100], ['ubicacion', 'Ubicación', 200], ['idArea', 'Área']] },
  sedes: { titulo: 'Sedes', singular: 'sede', metodo: 'Sede', campos: [['nombre', 'Nombre', 100], ['direccion', 'Dirección', 200], ['distrito', 'Distrito', 100], ['departamento', 'Departamento', 100]] },
  areas: { titulo: 'Áreas', singular: 'área', metodo: 'Area', campos: [['nombre', 'Nombre', 100], ['descripcion', 'Descripción', 255], ['idSede', 'Sede']] }
};

export default function Laboratorios() {
  const { esAdmin } = useAuth();
  const [data, setData] = useState({ sedes: [], areas: [], laboratorios: [] });
  const [tipo, setTipo] = useState('laboratorios'), [modal, setModal] = useState(null), [form, setForm] = useState({});
  const [error, setError] = useState(''), [busqueda, setBusqueda] = useState(''), [abiertas, setAbiertas] = useState({});
  const [original, setOriginal] = useState(null), [estado, setEstado] = useState('TODOS'), [mensaje, setMensaje] = useState('');
  const [errorCarga, setErrorCarga] = useState('');
  const [cargando, setCargando] = useState(true), [ocupado, setOcupado] = useState(false);
  const bloqueo = useRef(false), solicitud = useRef(0);
  const cfg = config[tipo];
  const cargar = useCallback(async () => {
    const actual = ++solicitud.current; setCargando(true); setErrorCarga('');
    try {
      const nuevos = await cargarCatalogos(catalogoApi, esAdmin, ['laboratorios', 'areas', 'sedes']);
      if (actual === solicitud.current) { setData(nuevos); return nuevos; }
    } catch (err) { if (actual === solicitud.current) setErrorCarga(mensajeError(err, 'No se pudo cargar la organización.')); }
    finally { if (actual === solicitud.current) setCargando(false); }
    return null;
  }, [esAdmin]);
  useEffect(() => { cargar(); return () => { solicitud.current += 1; }; }, [cargar]);
  const visibles = useMemo(() => data[tipo].filter((item) =>
    (estado === 'TODOS' || (estado === 'MANTENIMIENTO' ? estadoLaboratorio(item) === 'MANTENIMIENTO' : item.activo === (estado === 'ACTIVOS'))) &&
    `${item.nombre ?? ''} ${item.codigo ?? ''} ${item.descripcion ?? ''} ${item.ubicacion ?? ''} ${item.direccion ?? ''}`.toLowerCase().includes(busqueda.toLowerCase())
  ), [data, tipo, busqueda, estado]);
  const cerrar = () => { if (!bloqueo.current) { setModal(null); setOriginal(null); setError(''); } };
  function abrir(item) {
    if (!esAdmin || bloqueo.current || cargando || errorCarga) return;
    setForm(formularioCatalogo(tipo, item)); setOriginal(item ?? null);
    setModal(item ? 'editar' : 'crear'); setError(''); setMensaje('');
  }
  async function ejecutar(accion) {
    if (!esAdmin || bloqueo.current) return;
    bloqueo.current = true; setOcupado(true); setError(''); setMensaje('');
    let completada = false;
    try { await accion(); completada = true; setModal(null); setOriginal(null); }
    catch (err) {
      const parcial = Boolean(err.operacionesAplicadas?.length);
      const detalle = err.code === 'VALIDACION_LOCAL' ? err.message : mensajeError(err, 'No se pudo completar la operación.');
      setError(parcial ? `Se aplicó parte de los cambios, pero no se completó la operación. ${detalle} Revisa el estado recargado antes de continuar.` : detalle);
      if (parcial) { setModal(null); setOriginal(null); }
    } finally {
      const nuevos = await cargar();
      if (completada && nuevos) setMensaje('Información actualizada desde el servidor.');
      bloqueo.current = false; setOcupado(false);
    }
  }
  function guardar(event) {
    event.preventDefault();
    ejecutar(() => guardarCatalogo(catalogoApi, tipo, form, original));
  }
  function cambiarEstado(item) {
    if (!esAdmin || bloqueo.current || !window.confirm(`¿${item.activo ? 'Desactivar' : 'Reactivar'} ${item.nombre}?`)) return;
    ejecutar(() => catalogoApi[`cambiarEstado${cfg.metodo}`](item.id, !item.activo));
  }
  const toggle = (id) => setAbiertas((prev) => ({ ...prev, [id]: !prev[id] }));
  const sinPadre = tipo === 'laboratorios' ? !data.areas.some((item) => item.activo) : tipo === 'areas' ? !data.sedes.some((item) => item.activo) : false;
  const datosBloqueados = ocupado || (modal === 'editar' && original?.activo === false && (tipo === 'laboratorios' ? form.estadoLaboratorio === 'INACTIVO' : form.activo === false));
  const toolbar = <div className="barra-tabla catalogo-filtros"><div className="busqueda"><Search size={16}/><input value={busqueda} onChange={(event) => setBusqueda(event.target.value)} aria-label={`Buscar ${cfg.titulo.toLowerCase()}`} placeholder="Buscar por código o nombre..."/></div>{esAdmin && <label className="catalogo-filtro-estado">Estado<select disabled={ocupado} value={estado} onChange={event => setEstado(event.target.value)}><option value="TODOS">Todos</option><option value="ACTIVOS">Activos</option>{tipo === 'laboratorios' && <option value="MANTENIMIENTO">En mantenimiento</option>}<option value="INACTIVOS">Inactivos</option></select></label>}</div>;
  const tabla = <>{toolbar}{cargando ? <div className="vacio" role="status">Cargando organización…</div> : errorCarga ? <div className="mensaje-error" role="alert"><span>{errorCarga}</span><Boton variante="secundario" disabled={ocupado} onClick={cargar}>Reintentar organización</Boton></div> : <><div className="tabla-wrap"><table><thead><tr>{tipo === 'laboratorios' && <th>Código</th>}<th>Nombre</th>{tipo === 'laboratorios' ? <><th>Área</th><th>Sede</th><th>Ubicación</th></> : <><th>{tipo === 'sedes' ? 'Dirección' : 'Descripción'}</th>{tipo === 'areas' && <th>Sede</th>}</>}<th>Estado</th>{esAdmin && <th>Acciones</th>}</tr></thead><tbody>{visibles.map((item) => <tr key={item.id}>
    {tipo === 'laboratorios' && <td>{item.codigo || '—'}</td>}<td><strong>{item.nombre}</strong></td>
    {tipo === 'laboratorios' ? <><td>{item.area?.nombre || '—'}</td><td>{data.areas.find((area) => area.id === item.area?.id)?.sede?.nombre || '—'}</td><td>{item.ubicacion || '—'}</td></> : <><td>{(tipo === 'sedes' ? item.direccion : item.descripcion) || '—'}</td>{tipo === 'areas' && <td>{item.sede?.nombre || '—'}</td>}</>}
    <td><span className={`estado estado-${tipo === 'laboratorios' ? estadoLaboratorio(item).toLowerCase() : item.activo ? 'activo' : 'inactivo'}`}>{tipo === 'laboratorios' ? etiquetaEstadoLaboratorio(item) : item.activo ? 'Activo' : 'Inactivo'}</span></td>{esAdmin && <td><div className="acciones-tabla"><button disabled={ocupado} title={`Editar ${item.nombre}`} aria-label={`Editar ${item.nombre}`} onClick={() => abrir(item)}><Pencil size={15}/></button><button disabled={ocupado} className={item.activo ? 'peligro' : ''} title={`${item.activo ? 'Desactivar' : 'Reactivar'} ${item.nombre}`} aria-label={`${item.activo ? 'Desactivar' : 'Reactivar'} ${item.nombre}`} onClick={() => cambiarEstado(item)}>{item.activo ? <Trash2 size={15}/> : <RotateCcw size={15}/>}</button></div></td>}
  </tr>)}</tbody></table></div>{!visibles.length && <div className="vacio">No se encontraron registros.</div>}</>}</>;
  return <div className="pagina pagina-organizacion">
    <div className="pagina-cabecera"><div><span className="eyebrow">Ubicaciones</span><h1>Laboratorios</h1><p>Estructura de sedes, áreas y laboratorios.</p></div><div className="cabecera-acciones">{esAdmin && <Boton disabled={cargando || Boolean(errorCarga) || ocupado || sinPadre} onClick={() => abrir()}><Plus size={17}/> {tipo === 'laboratorios' ? 'Nuevo' : 'Nueva'} {cfg.singular}</Boton>}</div></div>
    <div className="contadores-grid tres"><div className="contador-card"><div className="contador-icono azul"><Building2 size={27}/></div><div><span>Sedes</span><strong>{cargando || errorCarga ? '—' : data.sedes.length}</strong></div></div><div className="contador-card"><div className="contador-icono verde"><Layers3 size={27}/></div><div><span>Áreas</span><strong>{cargando || errorCarga ? '—' : data.areas.length}</strong></div></div><div className="contador-card"><div className="contador-icono rojo"><FlaskConical size={27}/></div><div><span>Laboratorios</span><strong>{cargando || errorCarga ? '—' : data.laboratorios.length}</strong></div></div></div>
    <div className="tabs tabs-grandes">{Object.entries(config).map(([key, value]) => <button disabled={ocupado} className={tipo === key ? 'tab activo' : 'tab'} onClick={() => { setTipo(key); setBusqueda(''); setEstado('TODOS'); setModal(null); setError(''); setMensaje(''); }} key={key}>{value.titulo}</button>)}</div>
    {error && !modal && <div className="mensaje-error" role="alert">{error}<Boton variante="secundario" disabled={ocupado} onClick={() => setError('')}>Cerrar</Boton></div>}
    {mensaje && <div className="catalogo-feedback" role="status">{mensaje}</div>}
    {tipo === 'laboratorios' ? <div className="laboratorios-layout"><section className="panel estructura-panel"><div className="panel-titulo"><div><h2>Estructura organizacional</h2><small>Sede → Área → Laboratorio</small></div></div><div className="arbol">{cargando || errorCarga ? <p>{cargando ? 'Cargando estructura…' : 'La estructura no está disponible.'}</p> : data.sedes.map((sede) => <div className="arbol-sede" key={sede.id}><button className="arbol-linea" aria-expanded={Boolean(abiertas[`s-${sede.id}`])} onClick={() => toggle(`s-${sede.id}`)}>{abiertas[`s-${sede.id}`] ? <ChevronDown size={15}/> : <ChevronRight size={15}/>}<Building2 size={17}/><strong>{sede.nombre}</strong></button>{abiertas[`s-${sede.id}`] && data.areas.filter((area) => area.sede?.id === sede.id).map((area) => <div className="arbol-area" key={area.id}><button className="arbol-linea" aria-expanded={Boolean(abiertas[`a-${area.id}`])} onClick={() => toggle(`a-${area.id}`)}>{abiertas[`a-${area.id}`] ? <ChevronDown size={14}/> : <ChevronRight size={14}/>}<Layers3 size={16}/><span>{area.nombre}</span></button>{abiertas[`a-${area.id}`] && data.laboratorios.filter((lab) => lab.area?.id === area.id).map((lab) => <div className="arbol-lab" key={lab.id}><FlaskConical size={15}/>{lab.codigo || lab.nombre}</div>)}</div>)}</div>)}</div></section><section className="panel">{tabla}</section></div> : <section className="panel">{tabla}</section>}
    {modal && <div className="modal-fondo" onMouseDown={cerrar}><div className="modal modal-pequeno" role="dialog" aria-modal="true" aria-labelledby="titulo-organizacion" onMouseDown={(event) => event.stopPropagation()}><div className="modal-cabecera"><div><span className="eyebrow">{modal === 'crear' ? 'Nuevo' : 'Editar'}</span><h2 id="titulo-organizacion">{cfg.singular}</h2></div><button disabled={ocupado} className="icono-btn" aria-label="Cerrar" onClick={cerrar}>×</button></div>
      <form onSubmit={guardar}>{cfg.campos.map(([name, label, maxLength]) => {
        const opciones = name === 'idSede' ? data.sedes : name === 'idArea' ? data.areas : null;
        return <label key={name}>{label}{opciones ? <select required disabled={datosBloqueados} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })}><option value="">Seleccionar</option>{opciones.filter((item) => item.activo || item.id === Number(form[name])).map((item) => <option key={item.id} value={item.id} disabled={!item.activo}>{item.nombre}{item.activo ? '' : ' (inactiva)'}</option>)}</select> : name === 'descripcion' ? <textarea disabled={datosBloqueados} maxLength={maxLength} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })}/> : <input disabled={datosBloqueados} maxLength={maxLength} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })} required={['nombre', 'codigo'].includes(name)}/>}</label>;
      })}
      {tipo === 'laboratorios' ? <label>Estado<select disabled={ocupado} value={form.estadoLaboratorio} onChange={event => setForm({ ...form, estadoLaboratorio: event.target.value })}><option value="OPERATIVO">Activo</option><option value="MANTENIMIENTO">En mantenimiento</option><option value="INACTIVO">Inactivo</option></select></label> : modal === 'editar' && <label>Estado<select disabled={ocupado} value={String(form.activo)} onChange={event => setForm({ ...form, activo: event.target.value === 'true' })}><option value="true">Activo</option><option value="false">Inactivo</option></select></label>}
      {original?.activo === false && <p className="catalogo-nota">Los datos se editan después de reactivar el registro. Su padre, si lo tiene, también debe estar activo.</p>}
      {tipo === 'laboratorios' && <p className="catalogo-nota">En mantenimiento conserva el laboratorio activo y cambia su estado operativo. Inactivo desactiva el laboratorio según las dependencias que valide el servidor.</p>}
      {error && <div className="mensaje-error" role="alert">{error}</div>}<div className="modal-acciones"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : modal === 'crear' ? 'Crear' : 'Guardar cambios'}</Boton></div></form>
    </div></div>}
  </div>;
}
