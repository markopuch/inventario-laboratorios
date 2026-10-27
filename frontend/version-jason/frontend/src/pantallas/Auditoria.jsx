import { useEffect, useMemo, useState } from 'react';
import { History, RefreshCw } from 'lucide-react';
import Boton from '../componentes/Boton';
import { useAuth } from '../contextos/AuthContext';
import { auditoriaApi, esCancelacion, mensajeError } from '../servicios/api';
import {
  ACCIONES_AUDITORIA, ENTIDADES_AUDITORIA, etiquetaAccionAuditoria, etiquetaEntidadAuditoria,
  filtrosAuditoria, mostrarFechaAuditoria, registroAuditoria, validarAuditoria
} from '../utilidades/auditoria.js';

const filtrosIniciales = () => ({ entidad: '', idEntidad: '', idUsuario: '', accion: '', fechaDesde: '', fechaHasta: '' });

function ResumenEvento({ evento }) {
  if (evento.cambios.length <= 180) return <span className="auditoria-resumen">{evento.cambios}</span>;
  return <div className="auditoria-resumen">
    <span>{evento.cambios.slice(0, 180)}…</span>
    <details className="auditoria-detalle">
      <summary aria-label={`Ver resumen completo del evento ${evento.id}`}>Ver resumen completo</summary>
      <p>{evento.cambios}</p>
    </details>
  </div>;
}

export default function Auditoria() {
  const { esAdmin, token } = useAuth();
  const [filtros, setFiltros] = useState(filtrosIniciales);
  const [aplicados, setAplicados] = useState({});
  const [errorFiltros, setErrorFiltros] = useState('');
  const [intento, setIntento] = useState(0);
  const [consulta, setConsulta] = useState({ clave: null, estado: 'cargando', datos: [], error: '' });
  // Identifica también el resultado visible: al cambiar sesión/filtros no se muestran datos de la consulta anterior.
  const clave = useMemo(() => ({}), [esAdmin, token, aplicados, intento]);

  useEffect(() => {
    if (!esAdmin || !token) return;
    let vigente = true;
    const solicitud = new AbortController();
    setConsulta({ clave, estado: 'cargando', datos: [], error: '' });
    async function cargar() {
      try {
        const { data } = await auditoriaApi.listar(aplicados, { signal: solicitud.signal });
        const datos = validarAuditoria(data);
        if (vigente) setConsulta({ clave, estado: 'listo', datos, error: '' });
      } catch (error) {
        if (!vigente || esCancelacion(error)) return;
        setConsulta({ clave, estado: 'error', datos: [], error: error.code === 'RESPUESTA_AUDITORIA_INVALIDA'
          ? error.message : mensajeError(error, 'No se pudieron cargar los eventos de auditoría.') });
      }
    }
    cargar();
    return () => { vigente = false; solicitud.abort(); };
  }, [esAdmin, token, aplicados, clave]);

  const actualizar = () => setIntento(valor => valor + 1);
  function cambiar(event) {
    const { name, value } = event.target;
    setFiltros(actual => ({ ...actual, [name]: value }));
  }
  function aplicar(event) {
    event.preventDefault();
    try { setAplicados(filtrosAuditoria(filtros)); setErrorFiltros(''); }
    catch (error) { setErrorFiltros(error.message); }
  }
  function limpiar() {
    setFiltros(filtrosIniciales());
    setAplicados({});
    setErrorFiltros('');
  }

  if (!esAdmin || !token) return <div className="pagina"><div className="mensaje-error" role="alert">Solo ADMIN puede consultar la auditoría del sistema.</div></div>;

  const cargando = consulta.clave !== clave || consulta.estado === 'cargando';
  const fallo = !cargando && consulta.estado === 'error';
  const datos = !cargando && !fallo ? consulta.datos : [];

  return <div className="pagina">
    <div className="pagina-cabecera">
      <div><span className="eyebrow">Trazabilidad</span><h1>Auditoría</h1><p>Consulta quién realizó cada acción registrada y sobre qué registro.</p></div>
      <div className="cabecera-acciones"><History size={30}/><Boton variante="secundario" disabled={cargando} onClick={actualizar}><RefreshCw size={16}/> Actualizar</Boton></div>
    </div>
    <form className="barra-herramientas filtros-modulo auditoria-filtros" onSubmit={aplicar} aria-describedby="ayuda-filtros-auditoria">
      <label>Tipo de registro<select name="entidad" value={filtros.entidad} onChange={cambiar}><option value="">Todos los tipos</option>{ENTIDADES_AUDITORIA.map(entidad => <option key={entidad} value={entidad}>{etiquetaEntidadAuditoria(entidad)}</option>)}</select></label>
      <label>ID del registro<input name="idEntidad" inputMode="numeric" pattern="[0-9]+" maxLength={10} value={filtros.idEntidad} onChange={cambiar} placeholder="Ej. 12"/></label>
      <label>ID del usuario actor<input name="idUsuario" inputMode="numeric" pattern="[0-9]+" maxLength={10} value={filtros.idUsuario} onChange={cambiar} placeholder="Ej. 3"/></label>
      <label>Acción<select name="accion" value={filtros.accion} onChange={cambiar}><option value="">Todas las acciones</option>{ACCIONES_AUDITORIA.map(accion => <option key={accion} value={accion}>{etiquetaAccionAuditoria(accion)}</option>)}</select></label>
      <label>Desde (día UTC)<input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiar}/></label>
      <label>Hasta (día UTC)<input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiar}/></label>
      <Boton tipo="submit">Aplicar filtros</Boton><Boton variante="secundario" onClick={limpiar}>Limpiar</Boton>
    </form>
    <p id="ayuda-filtros-auditoria" className="nota-reporte">Los filtros de fecha consultan días UTC completos. La tabla muestra fecha y hora de Lima (UTC−05:00), empezando por los eventos más recientes.</p>
    <p className="nota-reporte">Los resúmenes describen cambios seleccionados de cada operación; no reconstruyen todas las versiones de un registro. Esta consulta es de solo lectura.</p>
    {errorFiltros && <div className="mensaje-error" role="alert">{errorFiltros}</div>}
    <section className="panel" aria-busy={cargando}>
      <div className="panel-titulo"><h2>Eventos registrados</h2><span className="auditoria-total" role="status">{cargando ? 'Consultando…' : fallo ? 'No disponibles' : `${datos.length} ${datos.length === 1 ? 'evento' : 'eventos'}`}</span></div>
      {cargando ? <div className="vacio" role="status">Cargando auditoría…</div>
        : fallo ? <div className="vacio"><p className="mensaje-error" role="alert">{consulta.error}</p><Boton variante="secundario" onClick={actualizar}>Reintentar auditoría</Boton></div>
        : !datos.length ? <div className="vacio">No hay eventos de auditoría para los filtros aplicados.</div>
        : <div className="tabla-wrap"><table className="auditoria-tabla">
          <caption className="auditoria-caption">Acciones de negocio registradas por el sistema.</caption>
          <thead><tr><th scope="col">Fecha y hora (Lima)</th><th scope="col">Usuario actor</th><th scope="col">Acción</th><th scope="col">Registro</th><th scope="col">Resumen</th></tr></thead>
          <tbody>{datos.map(evento => <tr key={evento.id}>
            <td className="auditoria-col-fecha"><time dateTime={evento.fecha}>{mostrarFechaAuditoria(evento.fecha)}</time></td>
            <td className="auditoria-actor"><strong>{evento.userNameActor}</strong><small>{evento.idUsuarioActor === null ? 'Cuenta histórica' : `ID de usuario: ${evento.idUsuarioActor}`}</small></td>
            <td>{etiquetaAccionAuditoria(evento.accion)}</td>
            <td className="auditoria-registro"><strong>{registroAuditoria(evento)}</strong><small>Evento #{evento.id}</small></td>
            <td><ResumenEvento evento={evento}/></td>
          </tr>)}</tbody>
        </table></div>}
    </section>
  </div>;
}
