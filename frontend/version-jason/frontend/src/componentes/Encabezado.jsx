import { FlaskConical, LogOut, Menu } from 'lucide-react';
import { useAuth } from '../contextos/AuthContext';

export default function Encabezado({ onMenu }) {
  const { usuario, logout } = useAuth();
  return <header className="encabezado">
    <button className="icono-btn mobile-only" onClick={onMenu} aria-label="Abrir menú"><Menu size={21}/></button>
    <div className="encabezado-contexto"><FlaskConical size={18}/><span>Inventario de Laboratorios</span></div>
    <div className="encabezado-acciones">
      <div className="usuario-mini"><div className="avatar">{usuario?.nombre?.[0] || usuario?.userName?.[0] || 'U'}</div><div><strong>{usuario?.nombre || usuario?.userName}</strong><small>{usuario?.rol}</small></div></div>
      <button className="icono-btn" onClick={logout} title="Cerrar sesión" aria-label="Cerrar sesión"><LogOut size={18}/></button>
    </div>
  </header>;
}
