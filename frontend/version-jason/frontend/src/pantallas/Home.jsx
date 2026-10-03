import { useEffect, useState } from 'react';
import { Activity, Boxes, MapPinned, Wrench } from 'lucide-react';
import TarjetaBienvenida from '../componentes/TarjetaBienvenida';
import ResumenSeleccion from '../componentes/ResumenSeleccion';
import { authApi, equiposApi, movimientosApi, mensajeError } from '../servicios/api';
import { Link } from 'react-router-dom';
import FilaMovimiento from '../componentes/FilaMovimiento';
import Boton from '../componentes/Boton';
import { useAuth } from '../contextos/AuthContext';
import { validarListaMovimientos } from '../utilidades/movimientos';

export default function Home() {
  const { esAdmin, puedeGestionar, token } = useAuth();
  const [inventario, setInventario] = useState({ estado: 'cargando', datos: null, error: '' });
  const [laboratorios, setLaboratorios] = useState({ estado: 'cargando', cantidad: null, error: '' });
  const [historial, setHistorial] = useState({ estado: 'cargando', datos: [], error: '' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setInventario({ estado: 'cargando', datos: null, error: '' });
    setLaboratorios({ estado: 'cargando', cantidad: null, error: '' });
    setHistorial({ estado: 'cargando', datos: [], error: '' });
    authApi.laboratorios().then(({ data }) => {
      if (typeof data?.alcanceGlobal !== 'boolean' || !Array.isArray(data.laboratorios) ||
          data.laboratorios.some(laboratorio => !Number.isInteger(laboratorio?.id) || laboratorio.id <= 0)) {
        throw new Error('La respuesta del alcance de laboratorios no es válida.');
      }
      if (vigente) setLaboratorios({ estado: 'listo', cantidad: data.laboratorios.length, error: '' });
    }).catch(error => {
      if (vigente) setLaboratorios({ estado: 'error', cantidad: null,
        error: mensajeError(error, 'No se pudo cargar el alcance actual de laboratorios.') });
    });
    equiposApi.listar().then(({ data }) => {
      if (!Array.isArray(data) || data.some(equipo => !equipo || typeof equipo !== 'object')) {
        throw new Error('La respuesta de equipos no es válida.');
      }
      if (vigente) setInventario({ estado: 'listo', datos: {
        equipos: data.length,
        mantenimiento: data.filter(equipo => equipo.estado !== 'BAJA' &&
          (equipo.requiereMantenimiento === true || equipo.estado === 'MANTENIMIENTO')).length,
        bajas: data.filter(equipo => equipo.estado === 'BAJA').length
      }, error: '' });
    }).catch(error => {
      if (vigente) setInventario({ estado: 'error', datos: null,
        error: mensajeError(error, 'No se pudo cargar el resumen de equipos.') });
    });
    movimientosApi.listar().then(({ data }) => {
      const movimientos = validarListaMovimientos(data);
      if (vigente) setHistorial({ estado: 'listo', datos: movimientos.slice(0, 5), error: '' });
    }).catch(error => {
      if (vigente) setHistorial({ estado: 'error', datos: [],
        error: mensajeError(error, 'No se pudieron cargar los movimientos recientes.') });
    });
    return () => { vigente = false; };
  }, [intento, token]);

  const detalleInventario = inventario.estado === 'cargando' ? 'Cargando...' : inventario.estado === 'error' ? 'No disponible' : null;
  const detalleLaboratorios = laboratorios.estado === 'cargando' ? 'Cargando...' : laboratorios.estado === 'error' ? 'No disponible' : null;
  const reintentar = () => setIntento(valor => valor + 1);
  return <div className="pagina">
    <TarjetaBienvenida/>
    {inventario.estado === 'error' && <div className="mensaje-error" role="alert">
      <span>{inventario.error}</span><Boton variante="secundario" onClick={reintentar}>Reintentar</Boton>
    </div>}
    {laboratorios.estado === 'error' && <div className="mensaje-error" role="alert">
      <span>{laboratorios.error}</span><Boton variante="secundario" onClick={reintentar}>Reintentar</Boton>
    </div>}
    <div className="resumen-grid" aria-busy={inventario.estado === 'cargando' || laboratorios.estado === 'cargando'}>
      <ResumenSeleccion titulo="Equipos registrados" valor={inventario.datos?.equipos ?? '—'} detalle={detalleInventario || 'Inventario visible'}/>
      <ResumenSeleccion titulo="Laboratorios" valor={laboratorios.cantidad ?? '—'} detalle={detalleLaboratorios || (esAdmin ? 'Alcance global activo' : 'Dentro de tu alcance')}/>
      <ResumenSeleccion titulo="Mantenimiento" valor={inventario.datos?.mantenimiento ?? '—'} detalle={detalleInventario || 'Requieren atención'}/>
      <ResumenSeleccion titulo="Equipos de baja" valor={inventario.datos?.bajas ?? '—'} detalle={detalleInventario || 'Históricos visibles'}/>
    </div>
    <section className="panel" aria-busy={historial.estado === 'cargando'}>
      <div className="panel-titulo"><div><span className="eyebrow">Trazabilidad</span><h2>Movimientos recientes</h2></div><Link to="/movimientos" className="enlace">Ver historial →</Link></div>
      {historial.estado === 'cargando' ? <div className="vacio" role="status">Cargando movimientos recientes...</div>
        : historial.estado === 'error' ? <div className="vacio"><p className="mensaje-error" role="alert">{historial.error}</p><Boton variante="secundario" onClick={reintentar}>Reintentar</Boton></div>
        : historial.datos.length ? <div className="tabla-wrap"><table>
          <thead><tr><th>Fecha</th><th>Tipo</th><th>Equipo</th><th>Origen</th><th aria-label="Dirección"></th><th>Destino</th><th>Actor</th><th>Motivo</th></tr></thead>
          <tbody>{historial.datos.map(movimiento => <FilaMovimiento key={movimiento.id} movimiento={movimiento}/>)}</tbody>
        </table></div>
        : <div className="vacio"><Activity size={30}/><p>No hay movimientos visibles todavía.</p></div>}
    </section>
    <div className="accesos">
      <Link to="/equipos"><Boxes/><span><strong>{puedeGestionar ? 'Gestionar equipos' : 'Consultar equipos'}</strong><small>{puedeGestionar ? 'Crear, editar, dar de baja y trasladar' : 'Consulta el inventario dentro de tu alcance'}</small></span></Link>
      <Link to="/categorias"><Wrench/><span><strong>{esAdmin ? 'Administrar categorías' : 'Consultar categorías'}</strong><small>Categorías y subcategorías de equipos</small></span></Link>
      <Link to="/laboratorios"><MapPinned/><span><strong>{esAdmin ? 'Administrar laboratorios' : 'Consultar laboratorios'}</strong><small>Sedes, áreas y laboratorios</small></span></Link>
    </div>
  </div>;
}
