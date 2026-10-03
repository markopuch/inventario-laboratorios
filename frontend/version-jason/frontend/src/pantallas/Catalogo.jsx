import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Plus, Trash2, Pencil, Search, Layers, Grid2x2 } from 'lucide-react';
import Boton from '../componentes/Boton';
import { catalogoApi, mensajeError } from '../servicios/api';
import { useAuth } from '../contextos/AuthContext';
import { lista, payloadCatalogo } from '../utilidades/equipos';
import './Catalogo.css';

const config = {
  categorias: { titulo: 'Categorías', singular: 'categoría', metodo: 'Categoria' },
  subcategorias: { titulo: 'Subcategorías', singular: 'subcategoría', metodo: 'Subcategoria' }
};

export default function Catalogo() {
  const { esAdmin } = useAuth();
  const [tipo, setTipo] = useState('categorias'), [data, setData] = useState({ categorias: [], subcategorias: [] });
  const [error, setError] = useState(''), [modal, setModal] = useState(null), [form, setForm] = useState({}), [busqueda, setBusqueda] = useState('');
  const [errorCarga, setErrorCarga] = useState('');
  const [cargando, setCargando] = useState(true), [ocupado, setOcupado] = useState(false);
  const bloqueo = useRef(false), solicitud = useRef(0);
  const cfg = config[tipo];
  const cargar = useCallback(async () => {
    const actual = ++solicitud.current; setCargando(true); setErrorCarga('');
    try {
      const [categorias, subcategorias] = await Promise.all([catalogoApi.categorias(), catalogoApi.subcategorias()]);
      if (actual === solicitud.current) setData({ categorias: lista(categorias.data), subcategorias: lista(subcategorias.data) });
    } catch (err) { if (actual === solicitud.current) setErrorCarga(mensajeError(err, 'No se pudieron cargar las categorías.')); }
    finally { if (actual === solicitud.current) setCargando(false); }
  }, []);
  useEffect(() => { cargar(); return () => { solicitud.current += 1; }; }, [cargar]);
  const visibles = useMemo(() => data[tipo].filter((item) => `${item.nombre ?? ''} ${item.descripcion ?? ''} ${item.categoria?.nombre ?? ''}`.toLowerCase().includes(busqueda.toLowerCase())), [data, tipo, busqueda]);
  const cerrar = () => { if (!bloqueo.current) { setModal(null); setError(''); } };
  function abrir(item) {
    if (!esAdmin || bloqueo.current || cargando || errorCarga) return;
    setForm(item ? { id: item.id, nombre: item.nombre ?? '', descripcion: item.descripcion ?? '', idCategoria: item.categoria?.id ?? '' } : { nombre: '', descripcion: '', idCategoria: '' });
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
    ejecutar(() => modal === 'crear' ? catalogoApi[`crear${cfg.metodo}`](payloadCatalogo(tipo, form)) : catalogoApi[`actualizar${cfg.metodo}`](form.id, payloadCatalogo(tipo, form)));
  }
  function eliminar(item) {
    if (!esAdmin || bloqueo.current || !window.confirm(`¿Dar de baja ${item.nombre}?`)) return;
    ejecutar(() => catalogoApi[`eliminar${cfg.metodo}`](item.id));
  }
  const sinCategoria = tipo === 'subcategorias' && !data.categorias.some((item) => item.activo);
  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Configuración</span><h1>Categorías y subcategorías</h1><p>Organiza la clasificación de los equipos.</p></div>{esAdmin && <Boton disabled={cargando || Boolean(errorCarga) || ocupado || sinCategoria} onClick={() => abrir()}><Plus size={17}/> Nueva {cfg.singular}</Boton>}</div>
    <div className="contadores-grid dos"><div className="contador-card"><div className="contador-icono azul"><Grid2x2 size={27}/></div><div><span>Total categorías</span><strong>{cargando || errorCarga ? '—' : data.categorias.length}</strong></div></div><div className="contador-card"><div className="contador-icono verde"><Layers size={27}/></div><div><span>Total subcategorías</span><strong>{cargando || errorCarga ? '—' : data.subcategorias.length}</strong></div></div></div>
    <div className="tabs tabs-grandes">{Object.entries(config).map(([key, value]) => <button disabled={ocupado} className={tipo === key ? 'tab activo' : 'tab'} onClick={() => { setTipo(key); setModal(null); setBusqueda(''); setError(''); }} key={key}>{value.titulo}</button>)}</div>
    {error && !modal && <div className="mensaje-error" role="alert">{error}<Boton variante="secundario" disabled={ocupado} onClick={() => setError('')}>Cerrar</Boton></div>}
    <section className="panel"><div className="panel-titulo"><div><h2>{cfg.titulo}</h2><small>{tipo === 'categorias' ? 'Clasificaciones principales de los equipos' : 'Clasificaciones dependientes de cada categoría'}</small></div>{esAdmin && <Boton variante="secundario" disabled={cargando || Boolean(errorCarga) || ocupado || sinCategoria} onClick={() => abrir()}><Plus size={16}/> Nueva {cfg.singular}</Boton>}</div>
      <div className="barra-tabla"><div className="busqueda"><Search size={16}/><input value={busqueda} onChange={(event) => setBusqueda(event.target.value)} aria-label={`Buscar ${cfg.titulo.toLowerCase()}`} placeholder={`Buscar ${cfg.titulo.toLowerCase()}...`}/></div></div>
      {cargando ? <div className="vacio" role="status">Cargando catálogos…</div> : errorCarga ? <div className="mensaje-error" role="alert"><span>{errorCarga}</span><Boton variante="secundario" disabled={ocupado} onClick={cargar}>Reintentar catálogos</Boton></div> : <><div className="tabla-wrap"><table><thead><tr><th>Nombre</th><th>Descripción</th>{tipo === 'subcategorias' && <th>Categoría</th>}<th>Estado</th>{esAdmin && <th>Acciones</th>}</tr></thead><tbody>{visibles.map((item) => <tr key={item.id}><td><strong>{item.nombre}</strong></td><td>{item.descripcion || '—'}</td>{tipo === 'subcategorias' && <td>{item.categoria?.nombre || '—'}</td>}<td><span className={`estado estado-${item.activo ? 'activo' : 'inactivo'}`}>{item.activo ? 'Activo' : 'Inactivo'}</span></td>{esAdmin && <td>{item.activo && <div className="acciones-tabla"><button disabled={ocupado} onClick={() => abrir(item)} title={`Editar ${item.nombre}`}><Pencil size={15}/></button><button disabled={ocupado} className="peligro" onClick={() => eliminar(item)} title={`Dar de baja ${item.nombre}`}><Trash2 size={15}/></button></div>}</td>}</tr>)}</tbody></table></div>{!visibles.length && <div className="vacio">No se encontraron registros.</div>}</>}
    </section>
    {modal && <div className="modal-fondo" onMouseDown={cerrar}><div className="modal modal-pequeno" role="dialog" aria-modal="true" aria-labelledby="titulo-catalogo" onMouseDown={(event) => event.stopPropagation()}><div className="modal-cabecera"><div><span className="eyebrow">{modal === 'crear' ? 'Nuevo' : 'Editar'}</span><h2 id="titulo-catalogo">{tipo === 'categorias' ? 'Categoría' : 'Subcategoría'}</h2></div><button disabled={ocupado} className="icono-btn" aria-label="Cerrar" onClick={cerrar}>×</button></div>
      <form onSubmit={guardar}><label>Nombre<input required maxLength={100} disabled={ocupado} value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })}/></label><label>Descripción<textarea className="campo-textarea" maxLength={255} disabled={ocupado} value={form.descripcion} onChange={(event) => setForm({ ...form, descripcion: event.target.value })}/></label>
        {tipo === 'subcategorias' && <label>Categoría<select required disabled={ocupado} value={form.idCategoria} onChange={(event) => setForm({ ...form, idCategoria: event.target.value })}><option value="">Seleccionar</option>{data.categorias.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>}
        {error && <div className="mensaje-error" role="alert">{error}</div>}
        <div className="modal-acciones"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : modal === 'crear' ? 'Crear' : 'Guardar cambios'}</Boton></div>
      </form></div></div>}
  </div>;
}
