import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Plus, Pencil, Trash2, Building2, Layers3, FlaskConical, Search, ChevronDown, ChevronRight } from 'lucide-react';
import Boton from '../componentes/Boton';
import { catalogoApi, mensajeError } from '../servicios/api';
import { useAuth } from '../contextos/AuthContext';
import { lista, payloadOrganizacion } from '../utilidades/equipos';

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
  const [errorCarga, setErrorCarga] = useState('');
  const [cargando, setCargando] = useState(true), [ocupado, setOcupado] = useState(false);
  const bloqueo = useRef(false), solicitud = useRef(0);
  const cfg = config[tipo];
  const cargar = useCallback(async () => {
    const actual = ++solicitud.current; setCargando(true); setErrorCarga('');
    try {
      const [labs, areas, sedes] = await Promise.all([catalogoApi.laboratorios(), catalogoApi.areas(), catalogoApi.sedes()]);
      if (actual === solicitud.current) setData({ laboratorios: lista(labs.data), areas: lista(areas.data), sedes: lista(sedes.data) });
    } catch (err) { if (actual === solicitud.current) setErrorCarga(mensajeError(err, 'No se pudo cargar la organización.')); }
    finally { if (actual === solicitud.current) setCargando(false); }
  }, []);
  useEffect(() => { cargar(); return () => { solicitud.current += 1; }; }, [cargar]);
  const visibles = useMemo(() => data[tipo].filter((item) => `${item.nombre ?? ''} ${item.codigo ?? ''} ${item.descripcion ?? ''} ${item.ubicacion ?? ''} ${item.direccion ?? ''}`.toLowerCase().includes(busqueda.toLowerCase())), [data, tipo, busqueda]);
  const cerrar = () => { if (!bloqueo.current) { setModal(null); setError(''); } };
  function abrir(item) {
    if (!esAdmin || bloqueo.current || cargando || errorCarga) return;
    setForm(item ? { ...item, idSede: item.sede?.id ?? '', idArea: item.area?.id ?? '' } : {});
    setModal(item ? 'editar' : 'crear'); setError('');
  }
  async function ejecutar(accion) {
    if (!esAdmin || bloqueo.current) return;
    bloqueo.current = true; setOcupado(true); setError('');
    try { await accion(); setModal(null); await cargar(); }
    catch (err) { setError(err.code === 'VALIDACION_LOCAL' ? err.message : mensajeError(err, 'No se pudo completar la operación.')); }
    finally { bloqueo.current = false; setOcupado(false); }
  }
  function guardar(event) {
    event.preventDefault();
    ejecutar(() => modal === 'crear' ? catalogoApi[`crear${cfg.metodo}`](payloadOrganizacion(tipo, form)) : catalogoApi[`actualizar${cfg.metodo}`](form.id, payloadOrganizacion(tipo, form)));
  }
  function eliminar(item) {
    if (!esAdmin || bloqueo.current || !window.confirm(`¿Dar de baja ${item.nombre}?`)) return;
    ejecutar(() => catalogoApi[`eliminar${cfg.metodo}`](item.id));
  }
  const toggle = (id) => setAbiertas((prev) => ({ ...prev, [id]: !prev[id] }));
  const sinPadre = tipo === 'laboratorios' ? !data.areas.some((item) => item.activo) : tipo === 'areas' ? !data.sedes.some((item) => item.activo) : false;
  const toolbar = <div className="barra-tabla"><div className="busqueda"><Search size={16}/><input value={busqueda} onChange={(event) => setBusqueda(event.target.value)} aria-label={`Buscar ${cfg.titulo.toLowerCase()}`} placeholder="Buscar por código o nombre..."/></div></div>;
  const tabla = <>{toolbar}{cargando ? <div className="vacio" role="status">Cargando organización…</div> : errorCarga ? <div className="mensaje-error" role="alert"><span>{errorCarga}</span><Boton variante="secundario" disabled={ocupado} onClick={cargar}>Reintentar organización</Boton></div> : <><div className="tabla-wrap"><table><thead><tr>{tipo === 'laboratorios' && <th>Código</th>}<th>Nombre</th>{tipo === 'laboratorios' ? <><th>Área</th><th>Sede</th><th>Ubicación</th></> : <><th>{tipo === 'sedes' ? 'Dirección' : 'Descripción'}</th>{tipo === 'areas' && <th>Sede</th>}</>}<th>Estado</th>{esAdmin && <th>Acciones</th>}</tr></thead><tbody>{visibles.map((item) => <tr key={item.id}>
    {tipo === 'laboratorios' && <td>{item.codigo || '—'}</td>}<td><strong>{item.nombre}</strong></td>
    {tipo === 'laboratorios' ? <><td>{item.area?.nombre || '—'}</td><td>{data.areas.find((area) => area.id === item.area?.id)?.sede?.nombre || '—'}</td><td>{item.ubicacion || '—'}</td></> : <><td>{(tipo === 'sedes' ? item.direccion : item.descripcion) || '—'}</td>{tipo === 'areas' && <td>{item.sede?.nombre || '—'}</td>}</>}
    <td><span className={`estado estado-${item.activo ? 'activo' : 'inactivo'}`}>{item.activo ? 'Activo' : 'Inactivo'}</span></td>{esAdmin && <td>{item.activo && <div className="acciones-tabla"><button disabled={ocupado} title={`Editar ${item.nombre}`} onClick={() => abrir(item)}><Pencil size={15}/></button><button disabled={ocupado} className="peligro" title={`Dar de baja ${item.nombre}`} onClick={() => eliminar(item)}><Trash2 size={15}/></button></div>}</td>}
  </tr>)}</tbody></table></div>{!visibles.length && <div className="vacio">No se encontraron registros.</div>}</>}</>;
  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Ubicaciones</span><h1>Laboratorios</h1><p>Estructura de sedes, áreas y laboratorios.</p></div><div className="cabecera-acciones">{esAdmin && <Boton disabled={cargando || Boolean(errorCarga) || ocupado || sinPadre} onClick={() => abrir()}><Plus size={17}/> {tipo === 'laboratorios' ? 'Nuevo' : 'Nueva'} {cfg.singular}</Boton>}</div></div>
    <div className="contadores-grid tres"><div className="contador-card"><div className="contador-icono azul"><Building2 size={27}/></div><div><span>Sedes</span><strong>{cargando || errorCarga ? '—' : data.sedes.length}</strong></div></div><div className="contador-card"><div className="contador-icono verde"><Layers3 size={27}/></div><div><span>Áreas</span><strong>{cargando || errorCarga ? '—' : data.areas.length}</strong></div></div><div className="contador-card"><div className="contador-icono rojo"><FlaskConical size={27}/></div><div><span>Laboratorios</span><strong>{cargando || errorCarga ? '—' : data.laboratorios.length}</strong></div></div></div>
    <div className="tabs tabs-grandes">{Object.entries(config).map(([key, value]) => <button disabled={ocupado} className={tipo === key ? 'tab activo' : 'tab'} onClick={() => { setTipo(key); setBusqueda(''); setModal(null); setError(''); }} key={key}>{value.titulo}</button>)}</div>
    {error && !modal && <div className="mensaje-error" role="alert">{error}<Boton variante="secundario" disabled={ocupado} onClick={() => setError('')}>Cerrar</Boton></div>}
    {tipo === 'laboratorios' ? <div className="laboratorios-layout"><section className="panel estructura-panel"><div className="panel-titulo"><div><h2>Estructura organizacional</h2><small>Sede → Área → Laboratorio</small></div></div><div className="arbol">{cargando || errorCarga ? <p>{cargando ? 'Cargando estructura…' : 'La estructura no está disponible.'}</p> : data.sedes.map((sede) => <div className="arbol-sede" key={sede.id}><button className="arbol-linea" aria-expanded={Boolean(abiertas[`s-${sede.id}`])} onClick={() => toggle(`s-${sede.id}`)}>{abiertas[`s-${sede.id}`] ? <ChevronDown size={15}/> : <ChevronRight size={15}/>}<Building2 size={17}/><strong>{sede.nombre}</strong></button>{abiertas[`s-${sede.id}`] && data.areas.filter((area) => area.sede?.id === sede.id).map((area) => <div className="arbol-area" key={area.id}><button className="arbol-linea" aria-expanded={Boolean(abiertas[`a-${area.id}`])} onClick={() => toggle(`a-${area.id}`)}>{abiertas[`a-${area.id}`] ? <ChevronDown size={14}/> : <ChevronRight size={14}/>}<Layers3 size={16}/><span>{area.nombre}</span></button>{abiertas[`a-${area.id}`] && data.laboratorios.filter((lab) => lab.area?.id === area.id).map((lab) => <div className="arbol-lab" key={lab.id}><FlaskConical size={15}/>{lab.codigo || lab.nombre}</div>)}</div>)}</div>)}</div></section><section className="panel">{tabla}</section></div> : <section className="panel">{tabla}</section>}
    {modal && <div className="modal-fondo" onMouseDown={cerrar}><div className="modal modal-pequeno" role="dialog" aria-modal="true" aria-labelledby="titulo-organizacion" onMouseDown={(event) => event.stopPropagation()}><div className="modal-cabecera"><div><span className="eyebrow">{modal === 'crear' ? 'Nuevo' : 'Editar'}</span><h2 id="titulo-organizacion">{cfg.singular}</h2></div><button disabled={ocupado} className="icono-btn" aria-label="Cerrar" onClick={cerrar}>×</button></div>
      <form onSubmit={guardar}>{cfg.campos.map(([name, label, maxLength]) => {
        const opciones = name === 'idSede' ? data.sedes : name === 'idArea' ? data.areas : null;
        return <label key={name}>{label}{opciones ? <select required disabled={ocupado} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })}><option value="">Seleccionar</option>{opciones.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select> : name === 'descripcion' ? <textarea disabled={ocupado} maxLength={maxLength} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })}/> : <input disabled={ocupado} maxLength={maxLength} value={form[name] ?? ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })} required={['nombre', 'codigo'].includes(name)}/>}</label>;
      })}{error && <div className="mensaje-error" role="alert">{error}</div>}<div className="modal-acciones"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : modal === 'crear' ? 'Crear' : 'Guardar cambios'}</Boton></div></form>
    </div></div>}
  </div>;
}
