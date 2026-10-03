import { LayoutDashboard, Boxes, ArrowRightLeft, BookOpen, MapPinned, UsersRound, Wrench, BarChart3, Settings, X, FlaskConical } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../contextos/AuthContext';

export default function BarraLateral({ abierta, onClose }) {
  const { esAdmin, alcance } = useAuth();
  const links = [
    { to:'/', label:'Dashboard', icon:LayoutDashboard },
    { to:'/equipos', label:'Equipos', icon:Boxes },
    { to:'/categorias', label:'Categorías', icon:BookOpen },
    { to:'/laboratorios', label:'Laboratorios', icon:MapPinned },
    { to:'/asignaciones', label:'Asignaciones', icon:UsersRound, admin:true },
    { to:'/movimientos', label:'Movimientos', icon:ArrowRightLeft },
    { to:'/mantenimientos', label:'Mantenimientos', icon:Wrench, pendiente:true },
    { to:'/reportes', label:'Reportes', icon:BarChart3, pendiente:true },
    { to:'/configuracion', label:'Configuración', icon:Settings, admin:true, pendiente:true },
  ];
  return <aside className={`barra-lateral ${abierta ? 'abierta':''}`}>
    <div className="marca"><div className="marca-icono"><FlaskConical size={22} strokeWidth={2}/></div><div><strong>Inventario de</strong><span>Laboratorios</span></div><button className="icono-btn mobile-only" aria-label="Cerrar menú lateral" onClick={onClose}><X size={19}/></button></div>
    <nav aria-label="Navegación principal">{links.filter(x=>!x.admin || esAdmin).map(({to,label,icon:Icon,pendiente}) => pendiente
      ? <div key={to} className="nav-link nav-pendiente" aria-disabled="true"><Icon size={19}/><span>{label}<small>Próximamente</small></span></div>
      : <NavLink key={to} to={to} end={to==='/'} onClick={onClose} className={({isActive}) => isActive ? 'nav-link activo':'nav-link'}><Icon size={19}/><span>{label}</span></NavLink>)}</nav>
    <div className="barra-pie"><div>{esAdmin ? 'Alcance global' : `${alcance?.laboratorios?.length ?? 0} laboratorios asignados`}</div><small>Gestión de inventario académico</small></div>
  </aside>;
}
