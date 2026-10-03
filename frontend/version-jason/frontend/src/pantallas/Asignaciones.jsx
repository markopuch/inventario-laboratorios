import { useEffect, useRef, useState } from 'react';
import { UsersRound } from 'lucide-react';
import Boton from '../componentes/Boton';
import { useAuth } from '../contextos/AuthContext';
import { catalogoApi, esCancelacion, mensajeError, usuariosApi } from '../servicios/api';
import { crearControlAsignaciones, prepararGuardadoAsignaciones, validarRespuestaAsignaciones } from '../utilidades/asignaciones.js';

export default function Asignaciones() {
  const { esAdmin } = useAuth();
  const control = useRef(null);
  if (!control.current) control.current = crearControlAsignaciones();
  const [id, setId] = useState('');
  const [idCargado, setIdCargado] = useState(null);
  const [data, setData] = useState(null);
  const [laboratorios, setLaboratorios] = useState([]);
  const [ids, setIds] = useState([]);
  const [ocupacion, setOcupacion] = useState('');
  const [error, setError] = useState('');
  const [exito, setExito] = useState('');

  useEffect(() => {
    if (!esAdmin) {
      setData(null);
      setIdCargado(null);
      setIds([]);
      setLaboratorios([]);
      setOcupacion('');
      setError('');
      setExito('');
    }
    return () => control.current.invalidar();
  }, [esAdmin]);

  function cambiarId(value) {
    control.current.cambiarId(value);
    setId(value);
    setIdCargado(null);
    setData(null);
    setLaboratorios([]);
    setIds([]);
    setOcupacion('');
    setError('');
    setExito('');
  }

  function iniciar() {
    if (!esAdmin) return null;
    try {
      return control.current.iniciar();
    } catch (err) {
      setError(err.message);
      return null;
    }
  }

  function finalizar(solicitud) {
    if (control.current.vigente(solicitud)) {
      control.current.finalizar(solicitud);
      setOcupacion('');
    }
  }

  async function consultar(event) {
    event.preventDefault();
    const solicitud = iniciar();
    if (!solicitud) return;
    setOcupacion('consultando');
    setError('');
    setExito('');
    setData(null);
    setIdCargado(null);
    setLaboratorios([]);
    setIds([]);
    try {
      const [asignaciones, catalogo] = await Promise.all([
        usuariosApi.laboratorios(solicitud.id), catalogoApi.laboratorios()
      ]);
      if (!control.current.vigente(solicitud)) return;
      validarRespuestaAsignaciones(asignaciones.data, solicitud.id);
      if (!Array.isArray(catalogo.data)) throw new Error('Catálogo de laboratorios inválido.');
      setData(asignaciones.data);
      setIdCargado(solicitud.id);
      setLaboratorios(catalogo.data.filter(lab => lab?.activo === true));
      setIds(asignaciones.data.laboratorios.map(lab => lab.id));
    } catch (err) {
      if (control.current.vigente(solicitud) && !esCancelacion(err)) {
        setError(mensajeError(err, 'No se pudieron consultar el usuario y sus laboratorios. Intenta nuevamente.'));
      }
    } finally {
      finalizar(solicitud);
    }
  }

  function seleccionar(idLaboratorio, checked) {
    if (!esAdmin || ocupacion) return;
    setIds(actuales => checked ? [...new Set([...actuales, idLaboratorio])] : actuales.filter(actual => actual !== idLaboratorio));
    setError('');
    setExito('');
  }

  async function guardar() {
    const solicitud = iniciar();
    if (!solicitud) return;
    let guardado;
    try {
      guardado = prepararGuardadoAsignaciones({ idActual: solicitud.id, idCargado, usuario: data?.usuario, ids });
    } catch (err) {
      setError(err.message);
      finalizar(solicitud);
      return;
    }
    setOcupacion('guardando');
    setError('');
    setExito('');
    try {
      const respuesta = await usuariosApi.actualizarLaboratorios(guardado.idUsuario, guardado.payload);
      if (!control.current.vigente(solicitud)) return;
      validarRespuestaAsignaciones(respuesta.data, guardado.idUsuario);
      setData(respuesta.data);
      setIds(respuesta.data.laboratorios.map(lab => lab.id));
      setExito(`Asignaciones del usuario ${guardado.idUsuario} guardadas correctamente.`);
    } catch (err) {
      if (control.current.vigente(solicitud) && !esCancelacion(err)) {
        setError(mensajeError(err, 'No se pudieron actualizar las asignaciones. Intenta nuevamente.'));
      }
    } finally {
      finalizar(solicitud);
    }
  }

  if (!esAdmin) return <div className="pagina"><div className="mensaje-error" role="alert">Solo los administradores pueden gestionar asignaciones.</div></div>;
  const noDisponibles = (data?.laboratorios || []).filter(asignado => !laboratorios.some(lab => lab.id === asignado.id));

  return <div className="pagina">
    <div className="pagina-cabecera">
      <div><span className="eyebrow">Administración</span><h1>Asignaciones</h1><p>Consulta un usuario y selecciona sus laboratorios activos.</p></div>
      <UsersRound size={34} aria-hidden="true" />
    </div>
    <section className="panel asignaciones-panel" aria-busy={Boolean(ocupacion)}>
      <form className="form-inline" onSubmit={consultar}>
        <label>ID de usuario<input type="number" min="1" step="1" required value={id} disabled={ocupacion === 'guardando'} onChange={event => cambiarId(event.target.value)} placeholder="Ej. 1" /></label>
        <Boton tipo="submit" disabled={Boolean(ocupacion)}>{ocupacion === 'consultando' ? 'Consultando...' : 'Consultar'}</Boton>
      </form>
      {ocupacion === 'consultando' && <p role="status">Cargando usuario y laboratorios activos...</p>}
      {data && <div className="asignacion-resumen">
        <h2>{[data.usuario.nombre, data.usuario.apellido].filter(Boolean).join(' ') || data.usuario.userName}</h2>
        <p>{data.usuario.userName} · {data.usuario.rol} · ID {idCargado}</p>
        <p>Laboratorios actuales: {data.laboratorios.map(lab => lab.codigo || lab.nombre).join(', ') || 'ninguno'}</p>
        <fieldset className="asignaciones-selector" disabled={Boolean(ocupacion)}>
          <legend>Laboratorios asignados</legend>
          <div className="asignaciones-lista">
            {laboratorios.map(lab => <label className="asignacion-opcion" key={lab.id}>
              <input type="checkbox" checked={ids.includes(lab.id)} onChange={event => seleccionar(lab.id, event.target.checked)} />
              <span>{lab.codigo} · {lab.nombre}</span>
            </label>)}
            {noDisponibles.map(lab => <label className="asignacion-opcion" key={lab.id}>
              <input type="checkbox" checked={ids.includes(lab.id)} disabled={!ids.includes(lab.id)} onChange={event => seleccionar(lab.id, event.target.checked)} />
              <span>{lab.codigo} · {lab.nombre} (fuera del catálogo activo)</span>
            </label>)}
          </div>
          {!laboratorios.length && <p>No hay laboratorios activos disponibles.</p>}
          {noDisponibles.length > 0 && <p>Puedes retirar los laboratorios fuera del catálogo activo o volver a consultar para actualizar la lista.</p>}
        </fieldset>
        <p>{ids.length} laboratorios seleccionados.{!ids.length && ' Al guardar, el usuario quedará sin asignaciones.'}</p>
        <div className="modal-acciones">
          <Boton onClick={guardar} disabled={Boolean(ocupacion)}>{ocupacion === 'guardando' ? 'Guardando...' : 'Guardar asignaciones'}</Boton>
        </div>
      </div>}
      {ocupacion === 'guardando' && <p role="status">Guardando asignaciones...</p>}
      {error && <div className="mensaje-error" role="alert">{error}</div>}
      {exito && <div className="mensaje-exito" role="status">{exito}</div>}
    </section>
  </div>;
}
