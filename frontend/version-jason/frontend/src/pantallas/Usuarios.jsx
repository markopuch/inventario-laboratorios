import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Plus, Search, UsersRound } from 'lucide-react';
import Boton from '../componentes/Boton';
import { useAuth } from '../contextos/AuthContext';
import { esCancelacion, mensajeError, usuariosApi } from '../servicios/api';
import { validarRespuestaAsignaciones } from '../utilidades/asignaciones.js';
import {
  ROLES_USUARIO, listaUsuarios, puedeAdministrarUsuarios, payloadCrearUsuario, payloadEditarUsuario,
  payloadEstadoUsuario, payloadPasswordUsuario, payloadRolUsuario, usuarioPublico
} from '../utilidades/usuarios.js';
import Asignaciones from './Asignaciones';

const titulos = { crear: 'Nuevo usuario', editar: 'Editar usuario', rol: 'Cambiar rol',
  estado: 'Cambiar estado', password: 'Restablecer contraseña', laboratorios: 'Gestionar laboratorios' };

export default function Usuarios() {
  const { esAdmin, usuario: actor, logout, refrescarSesion } = useAuth();
  const autorizado = esAdmin && puedeAdministrarUsuarios(actor?.rol);
  const montado = useRef(false), revision = useRef(0), bloqueo = useRef(false), permiso = useRef(autorizado);
  permiso.current = autorizado;
  const [usuarios, setUsuarios] = useState([]), [asignaciones, setAsignaciones] = useState({});
  const [cargando, setCargando] = useState(true), [ocupado, setOcupado] = useState(false);
  const [errorCarga, setErrorCarga] = useState(''), [error, setError] = useState(''), [exito, setExito] = useState('');
  const [busqueda, setBusqueda] = useState(''), [estado, setEstado] = useState('todos');
  const [modal, setModal] = useState(null), [form, setForm] = useState({});

  useEffect(() => {
    montado.current = true;
    return () => { montado.current = false; revision.current += 1; };
  }, []);

  const cargar = useCallback(async () => {
    if (!permiso.current) return false;
    const actual = ++revision.current;
    const vigente = () => montado.current && permiso.current && actual === revision.current;
    setCargando(true); setErrorCarga('');
    try {
      const respuesta = await usuariosApi.listar();
      if (!vigente()) return false;
      const lista = listaUsuarios(respuesta.data);
      // AdminUsuarioResponse no incluye laboratorios: se consultan sus endpoints reales.
      const resultados = await Promise.allSettled(lista.map(usuario => usuariosApi.laboratorios(usuario.id)));
      if (!vigente()) return false;
      const laboratorios = Object.fromEntries(lista.map((usuario, indice) => {
        const resultado = resultados[indice];
        if (resultado.status === 'fulfilled') {
          try {
            validarRespuestaAsignaciones(resultado.value.data, usuario.id);
            return [usuario.id, { laboratorios: resultado.value.data.laboratorios, error: '' }];
          } catch (err) { return [usuario.id, { laboratorios: null, error: err.message }]; }
        }
        return [usuario.id, { laboratorios: null, error: mensajeError(resultado.reason, 'No se pudieron consultar las asignaciones.') }];
      }));
      setUsuarios(lista); setAsignaciones(laboratorios);
      return true;
    } catch (err) {
      if (vigente() && !esCancelacion(err)) setErrorCarga(err.code === 'VALIDACION_LOCAL' ? err.message : mensajeError(err, 'No se pudieron cargar los usuarios.'));
      return false;
    } finally { if (vigente()) setCargando(false); }
  }, []);

  useEffect(() => {
    if (autorizado) cargar();
    else { revision.current += 1; setUsuarios([]); setAsignaciones({}); setModal(null); setForm({}); setCargando(false); }
    return () => { revision.current += 1; };
  }, [autorizado, cargar]);

  function cerrar() {
    if (bloqueo.current) return;
    setModal(null); setForm({}); setError('');
  }

  useEffect(() => {
    if (!modal) return undefined;
    const escape = event => { if (event.key === 'Escape' && !bloqueo.current) { setModal(null); setForm({}); setError(''); } };
    window.addEventListener('keydown', escape);
    return () => window.removeEventListener('keydown', escape);
  }, [modal]);

  async function abrir(tipo, usuario) {
    if (!permiso.current || bloqueo.current || cargando || errorCarga) return;
    setError(''); setExito('');
    if (tipo === 'crear') {
      setForm({ userName: '', nombre: '', apellido: '', email: '', cargo: '', password: '', confirmacion: '', rol: 'LECTOR', activo: true });
      setModal({ tipo, usuario: null });
      return;
    }
    bloqueo.current = true; setOcupado(true);
    const actual = revision.current;
    try {
      const respuesta = await usuariosApi.detalle(usuario.id);
      if (!montado.current || !permiso.current || actual !== revision.current) return;
      const seleccionado = usuarioPublico(respuesta.data);
      if (seleccionado.id !== usuario.id) throw new Error('La respuesta no corresponde al usuario seleccionado.');
      setForm({ nombre: seleccionado.nombre ?? '', apellido: seleccionado.apellido ?? '', email: seleccionado.email ?? '',
        cargo: seleccionado.cargo ?? '', rol: seleccionado.rol, activo: tipo === 'estado' ? !seleccionado.activo : seleccionado.activo,
        password: '', confirmacion: '' });
      setModal({ tipo, usuario: seleccionado });
    } catch (err) {
      if (montado.current && permiso.current && !esCancelacion(err)) setError(mensajeError(err, err.code === 'VALIDACION_LOCAL' ? err.message : 'No se pudo consultar al usuario.'));
    } finally { bloqueo.current = false; if (montado.current) setOcupado(false); }
  }

  function campo(nombre, value) { setForm(actual => ({ ...actual, [nombre]: value })); setError(''); }

  async function guardar(event) {
    event.preventDefault();
    if (!permiso.current || bloqueo.current || !modal || modal.tipo === 'laboratorios') return;
    let payload;
    try {
      if (modal.tipo === 'crear') payload = payloadCrearUsuario(form);
      else if (modal.tipo === 'editar') payload = payloadEditarUsuario(form);
      else if (modal.tipo === 'rol') payload = payloadRolUsuario(form.rol);
      else if (modal.tipo === 'estado') payload = payloadEstadoUsuario(form.activo);
      else payload = payloadPasswordUsuario(form.password, form.confirmacion);
    } catch (err) { setError(err.message); return; }

    const operacion = modal.tipo, seleccionado = modal.usuario;
    bloqueo.current = true; setOcupado(true); setError(''); setExito('');
    // El secreto solo permanece en el cuerpo de esta petición, nunca en estado tras enviarlo.
    setForm(actual => ({ ...actual, password: '', confirmacion: '' }));
    try {
      if (operacion === 'crear') await usuariosApi.crear(payload);
      else if (operacion === 'editar') await usuariosApi.actualizar(seleccionado.id, payload);
      else if (operacion === 'rol') await usuariosApi.cambiarRol(seleccionado.id, payload.rol);
      else if (operacion === 'estado') await usuariosApi.cambiarEstado(seleccionado.id, payload.activo);
      else await usuariosApi.cambiarPassword(seleccionado.id, payload.password);
      if (!montado.current) return;
      setModal(null); setForm({});
      const propio = seleccionado?.id === actor?.id;
      if (propio && operacion === 'estado' && payload.activo === false) {
        logout('Tu cuenta fue desactivada.');
        return;
      }
      if (propio && ['rol', 'editar'].includes(operacion)) {
        await refrescarSesion();
        // Si cambia su rol, el servidor decide el perfil y las guardias retiran esta pantalla.
        if (!montado.current || (operacion === 'rol' && payload.rol !== 'ADMIN')) return;
      }
      const actualizado = await cargar();
      if (montado.current && permiso.current) {
        if (actualizado) setExito(operacion === 'password' ? 'Contraseña restablecida. El listado fue actualizado.' : 'Operación guardada y listado actualizado desde el servidor.');
        else setError('El servidor confirmó el cambio, pero no se pudo recargar el listado. Reintenta la consulta antes de continuar.');
      }
    } catch (err) {
      if (montado.current && permiso.current && !esCancelacion(err)) setError(mensajeError(err, 'No se pudo completar la operación.'));
    } finally {
      payload = null;
      bloqueo.current = false;
      if (montado.current) { setOcupado(false); setForm(actual => ({ ...actual, password: '', confirmacion: '' })); }
    }
  }

  async function asignacionesGuardadas() {
    if (!await cargar()) throw new Error('Las asignaciones se guardaron, pero no se pudo recargar el listado. Reintenta la consulta.');
  }

  const ocupacionAsignaciones = useCallback(valor => {
    bloqueo.current = valor;
    setOcupado(valor);
  }, []);

  const visibles = useMemo(() => usuarios.filter(usuario => {
    const coincideEstado = estado === 'todos' || usuario.activo === (estado === 'activos');
    const texto = [usuario.userName, usuario.nombre, usuario.apellido, usuario.email, usuario.cargo, usuario.rol].join(' ').toLowerCase();
    return coincideEstado && texto.includes(busqueda.trim().toLowerCase());
  }), [usuarios, estado, busqueda]);

  if (!autorizado) return <div className="pagina"><div className="mensaje-error" role="alert">Solo ADMIN puede administrar usuarios.</div></div>;

  const cambiandoPropio = modal?.usuario?.id === actor?.id;
  const datosPersonales = modal && ['crear', 'editar'].includes(modal.tipo);
  const pidePassword = modal && ['crear', 'password'].includes(modal.tipo);

  return <div className="pagina">
    <div className="pagina-cabecera"><div><span className="eyebrow">Administración</span><h1>Usuarios</h1><p>Administra cuentas, roles, estado y laboratorios asignados.</p></div>
      <Boton disabled={cargando || ocupado || Boolean(errorCarga)} onClick={() => abrir('crear')}><Plus size={17} aria-hidden="true" /> Nuevo usuario</Boton>
    </div>
    <div className="barra-herramientas">
      <div className="busqueda"><Search size={16} aria-hidden="true" /><input aria-label="Buscar usuarios" placeholder="Buscar usuario, nombre, email o rol…" value={busqueda} onChange={event => setBusqueda(event.target.value)} disabled={ocupado} /></div>
      <select aria-label="Filtrar usuarios por estado" value={estado} onChange={event => setEstado(event.target.value)} disabled={ocupado}><option value="todos">Activos e inactivos</option><option value="activos">Activos</option><option value="inactivos">Inactivos</option></select>
      <Boton variante="secundario" disabled={cargando || ocupado} onClick={cargar}>Actualizar</Boton>
    </div>
    {error && !modal && <div className="mensaje-error" role="alert">{error}</div>}
    {exito && <div className="mensaje-exito" role="status">{exito}</div>}
    <section className="panel" aria-busy={cargando || ocupado}>
      <div className="panel-titulo"><div><h2>Listado administrativo</h2><small>{cargando || errorCarga ? 'Consulta en curso' : `${visibles.length} usuarios visibles`}</small></div><UsersRound size={28} aria-hidden="true" /></div>
      {cargando ? <div className="vacio" role="status">Cargando usuarios y asignaciones…</div> : errorCarga ? <div className="mensaje-error" role="alert"><span>{errorCarga}</span><Boton variante="secundario" disabled={ocupado} onClick={cargar}>Reintentar</Boton></div> : <>
        <div className="tabla-wrap"><table><thead><tr><th>Usuario</th><th>Nombre</th><th>Email</th><th>Cargo</th><th>Rol</th><th>Estado</th><th>Laboratorios</th><th>Acciones</th></tr></thead>
          <tbody>{visibles.map(usuario => <tr key={usuario.id}>
            <td><strong>{usuario.userName}</strong><small>ID {usuario.id}{usuario.id === actor?.id ? ' · Tu cuenta' : ''}</small></td>
            <td>{[usuario.nombre, usuario.apellido].filter(Boolean).join(' ') || '—'}</td><td>{usuario.email || '—'}</td><td>{usuario.cargo || '—'}</td><td>{usuario.rol}</td>
            <td><span className={`estado estado-${usuario.activo ? 'activo' : 'inactivo'}`}>{usuario.activo ? 'Activo' : 'Inactivo'}</span></td>
            <td>{asignaciones[usuario.id]?.error ? <span title={asignaciones[usuario.id].error}>No disponibles</span> : asignaciones[usuario.id]?.laboratorios?.map(lab => lab.codigo || lab.nombre).join(', ') || 'Sin asignaciones'}{usuario.rol === 'ADMIN' && <small>Acceso global por rol</small>}</td>
            <td><div className="acciones-tabla"><button disabled={ocupado} onClick={() => abrir('editar', usuario)}>Editar</button><button disabled={ocupado} onClick={() => abrir('rol', usuario)}>Rol</button><button disabled={ocupado} onClick={() => abrir('estado', usuario)}>{usuario.activo ? 'Desactivar' : 'Activar'}</button><button disabled={ocupado} onClick={() => abrir('password', usuario)}>Contraseña</button><button disabled={ocupado} onClick={() => abrir('laboratorios', usuario)}>Laboratorios</button></div></td>
          </tr>)}</tbody></table></div>{!visibles.length && <div className="vacio">No se encontraron usuarios para estos filtros.</div>}
      </>}
    </section>

    {modal && <div className="modal-fondo" onMouseDown={cerrar}><div className={`modal ${modal.tipo === 'laboratorios' ? '' : 'modal-pequeno'}`} role="dialog" aria-modal="true" aria-labelledby="titulo-usuario" onMouseDown={event => event.stopPropagation()}>
      <div className="modal-cabecera"><div><span className="eyebrow">{modal.usuario?.userName || 'Administración'}</span><h2 id="titulo-usuario">{titulos[modal.tipo]}</h2></div><button type="button" className="icono-btn" disabled={ocupado} aria-label="Cerrar usuario" onClick={cerrar}>×</button></div>
      {modal.tipo === 'laboratorios' ? <Asignaciones usuarioId={modal.usuario.id} onGuardar={asignacionesGuardadas} onOcupacion={ocupacionAsignaciones} /> : <form onSubmit={guardar}>
        {datosPersonales && <>
          <label>Usuario<input required={modal.tipo === 'crear'} maxLength={50} autoFocus value={modal.tipo === 'crear' ? form.userName : modal.usuario.userName} disabled={ocupado || modal.tipo === 'editar'} autoComplete="off" onChange={event => campo('userName', event.target.value)} /></label>
          {modal.tipo === 'editar' && <small>El nombre de usuario es inmutable. Rol, estado y contraseña se gestionan por separado.</small>}
          <div className="form-grid"><label>Nombre<input required maxLength={100} value={form.nombre} disabled={ocupado} onChange={event => campo('nombre', event.target.value)} /></label><label>Apellido<input required maxLength={100} value={form.apellido} disabled={ocupado} onChange={event => campo('apellido', event.target.value)} /></label></div>
          <label>Email<input type="email" required maxLength={150} value={form.email} disabled={ocupado} onChange={event => campo('email', event.target.value)} /></label>
          <label>Cargo<input maxLength={100} value={form.cargo} disabled={ocupado} onChange={event => campo('cargo', event.target.value)} /></label>
        </>}
        {['crear', 'rol'].includes(modal.tipo) && <label>Rol<select required value={form.rol} disabled={ocupado} onChange={event => campo('rol', event.target.value)}>{ROLES_USUARIO.map(rol => <option key={rol} value={rol}>{rol}</option>)}</select></label>}
        {['crear', 'estado'].includes(modal.tipo) && <label>Estado<select required value={String(form.activo)} disabled={ocupado} onChange={event => campo('activo', event.target.value === 'true')}><option value="true">Activo</option><option value="false">Inactivo</option></select></label>}
        {cambiandoPropio && modal.tipo === 'rol' && form.rol !== 'ADMIN' && <p>Al guardar, tu sesión actualizará el rol y dejarás de acceder a Administración.</p>}
        {cambiandoPropio && modal.tipo === 'estado' && !form.activo && <p>Al desactivar tu cuenta se cerrará tu sesión.</p>}
        {modal.tipo === 'estado' && <p>Las asignaciones se conservan al desactivar una cuenta. El backend protege al último ADMIN activo.</p>}
        {pidePassword && <><label>{modal.tipo === 'crear' ? 'Contraseña' : 'Nueva contraseña'}<input type="password" required minLength={4} maxLength={72} autoComplete="new-password" value={form.password} disabled={ocupado} onChange={event => campo('password', event.target.value)} /></label><label>Confirmar contraseña<input type="password" required minLength={4} maxLength={72} autoComplete="new-password" value={form.confirmacion} disabled={ocupado} onChange={event => campo('confirmacion', event.target.value)} /></label><small>Entre 4 y 72 caracteres y hasta 72 bytes UTF-8. No se muestra ni conserva después de enviar.</small></>}
        {error && <div className="mensaje-error" role="alert">{error}</div>}
        <div className="modal-acciones"><Boton variante="secundario" disabled={ocupado} onClick={cerrar}>Cancelar</Boton><Boton tipo="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : modal.tipo === 'crear' ? 'Crear usuario' : 'Guardar cambios'}</Boton></div>
      </form>}
    </div></div>}
  </div>;
}
