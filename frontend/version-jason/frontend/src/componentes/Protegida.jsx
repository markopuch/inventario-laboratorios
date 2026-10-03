import { Navigate } from 'react-router-dom';
import { useAuth } from '../contextos/AuthContext';
export default function Protegida({ children, roles }) {
  const { token, usuario, alcance, cargando } = useAuth();
  if (cargando) return <div className="pantalla-carga" role="status">Cargando sesión...</div>;
  const coherente = token && usuario?.activo === true &&
    ['ADMIN', 'GESTOR', 'LECTOR'].includes(usuario.rol) && Array.isArray(alcance?.laboratorios) &&
    alcance.alcanceGlobal === (usuario.rol === 'ADMIN');
  if (!coherente) return <Navigate to="/login" replace/>;
  if (roles && !roles.includes(usuario.rol)) return <Navigate to="/" replace/>;
  return children;
}
