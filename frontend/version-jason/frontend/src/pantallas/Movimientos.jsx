import { useEffect, useState } from 'react';
import { ArrowRightLeft, Search, Clock3, List } from 'lucide-react';
import FilaMovimiento from '../componentes/FilaMovimiento';
import Boton from '../componentes/Boton';
import { movimientosApi, mensajeError } from '../servicios/api';
import { useAuth } from '../contextos/AuthContext';
import { esMovimientoDeHoy, filtrarMovimientos, validarListaMovimientos } from '../utilidades/movimientos';

export default function Movimientos() {
  const { alcance, token } = useAuth();
  const laboratorios = alcance?.laboratorios ?? [];
  const [laboratorio, setLaboratorio] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [intento, setIntento] = useState(0);
  const [consulta, setConsulta] = useState({ filtro: null, estado: 'cargando', datos: [], error: '' });

  useEffect(() => {
    let vigente = true;
    setConsulta({ filtro: laboratorio, estado: 'cargando', datos: [], error: '' });
    movimientosApi.listar(laboratorio ? { idLaboratorio: Number(laboratorio) } : {}).then(({ data }) => {
      const datos = validarListaMovimientos(data);
      if (vigente) setConsulta({ filtro: laboratorio, estado: 'listo', datos, error: '' });
    }).catch(error => {
      if (vigente) setConsulta({ filtro: laboratorio, estado: 'error', datos: [],
        error: mensajeError(error, 'No se pudo cargar el historial de movimientos.') });
    });
    return () => { vigente = false; };
  }, [laboratorio, intento, token]);

  const cargando = consulta.filtro !== laboratorio || consulta.estado === 'cargando';
  const fallo = !cargando && consulta.estado === 'error';
  const visibles = !cargando && !fallo ? filtrarMovimientos(consulta.datos, busqueda) : [];
  const disponible = !cargando && !fallo;
  const ahora = new Date();
  const hoy = visibles.filter(movimiento => esMovimientoDeHoy(movimiento, ahora)).length;
  const traslados = visibles.filter(movimiento => movimiento.tipoMovimiento === 'TRASLADO').length;

  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Trazabilidad</span><h1>Movimientos</h1><p>Consulta el historial y sus indicadores según los filtros actuales.</p></div><div className="cabecera-icono"><ArrowRightLeft/></div></div>
    <div className="contadores-grid tres" aria-busy={cargando}>
      <div className="contador-card"><div className="contador-icono azul"><Clock3 size={27}/></div><div><span>Movimientos hoy</span><strong>{disponible ? hoy : '—'}</strong></div></div>
      <div className="contador-card"><div className="contador-icono amarillo"><List size={27}/></div><div><span>Total de movimientos</span><strong>{disponible ? visibles.length : '—'}</strong></div></div>
      <div className="contador-card"><div className="contador-icono verde"><ArrowRightLeft size={27}/></div><div><span>Traslados</span><strong>{disponible ? traslados : '—'}</strong></div></div>
    </div>
    <div className="barra-herramientas simple">
      <div className="busqueda"><Search size={17}/><input value={busqueda} onChange={event => setBusqueda(event.target.value)} aria-label="Buscar movimientos" placeholder="Buscar equipo, actor, laboratorio o motivo..."/></div>
      <select value={laboratorio} onChange={event => setLaboratorio(event.target.value)} aria-label="Filtrar movimientos por laboratorio">
        <option value="">Todos los laboratorios de mi alcance</option>
        {laboratorios.map(lab => <option key={lab.id} value={lab.id}>{lab.codigo} · {lab.nombre}</option>)}
      </select>
    </div>
    <section className="panel" aria-busy={cargando}>
      {cargando ? <div className="vacio" role="status">Cargando movimientos...</div>
        : fallo ? <div className="vacio"><p className="mensaje-error" role="alert">{consulta.error}</p><Boton variante="secundario" onClick={() => setIntento(valor => valor + 1)}>Reintentar</Boton></div>
        : visibles.length ? <div className="tabla-wrap"><table>
          <thead><tr><th>Fecha</th><th>Tipo</th><th>Equipo</th><th>Origen</th><th aria-label="Dirección"></th><th>Destino</th><th>Actor</th><th>Motivo</th></tr></thead>
          <tbody>{visibles.map(movimiento => <FilaMovimiento key={movimiento.id} movimiento={movimiento}/>)}</tbody>
        </table></div>
        : <div className="vacio">{busqueda.trim() ? 'No se encontraron movimientos con esta búsqueda.' : 'No hay movimientos visibles para los laboratorios seleccionados.'}</div>}
    </section>
  </div>;
}
