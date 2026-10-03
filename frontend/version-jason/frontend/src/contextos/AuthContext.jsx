import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { crearControlSesion } from '../servicios/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState({ token: null, usuario: null, alcance: null, cargando: false, mensajeSesion: '' });
  const control = useMemo(() => crearControlSesion(setSesion), []);

  useEffect(() => {
    control.activar();
    return () => control.desactivar();
  }, [control]);

  const value = useMemo(() => ({
    ...sesion,
    login: control.login,
    logout: () => control.logout(),
    cancelarLogin: control.cancelarLogin,
    esAdmin: Boolean(sesion.token && sesion.usuario?.rol === 'ADMIN' && sesion.alcance?.alcanceGlobal),
    puedeGestionar: Boolean(sesion.token && sesion.alcance && ['ADMIN', 'GESTOR'].includes(sesion.usuario?.rol))
  }), [sesion, control]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
