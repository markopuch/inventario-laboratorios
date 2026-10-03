import { ArrowRightLeft, Pencil, Trash2, Wrench } from 'lucide-react';
export default function TarjetaEquipo({ equipo, puedeGestionar = false, disabled = false, onEdit, onMove, onDelete }) {
  return <article className="tarjeta-equipo">
    <div className="equipo-cabecera"><div className="equipo-icono"><Wrench size={20}/></div><span className={`estado estado-${equipo.estado?.toLowerCase()}`}>{equipo.estado}</span></div>
    <div><h3>{equipo.nombre}</h3><p className="codigo">{equipo.codigoInterno}</p></div>
    <dl className="equipo-datos"><div><dt>Laboratorio</dt><dd>{equipo.laboratorio?.codigo || '—'}</dd></div><div><dt>Subcategoría</dt><dd>{equipo.subcategoria?.nombre || '—'}</dd></div><div><dt>Marca / modelo</dt><dd>{[equipo.marca,equipo.modelo].filter(Boolean).join(' ') || '—'}</dd></div></dl>
    {puedeGestionar && equipo.estado !== 'BAJA' && <div className="equipo-acciones"><button disabled={disabled} onClick={()=>onEdit(equipo)}><Pencil size={15}/> Editar</button><button disabled={disabled} onClick={()=>onMove(equipo)}><ArrowRightLeft size={15}/> Trasladar</button><button disabled={disabled} className="peligro" aria-label={`Dar de baja ${equipo.codigoInterno}`} onClick={()=>onDelete(equipo)}><Trash2 size={15}/></button></div>}
  </article>;
}
