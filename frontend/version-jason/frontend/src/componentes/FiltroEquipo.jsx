export default function FiltroEquipo({ filtros, setFiltros, laboratorios = [], subcategorias = [], onApply, disabled = false }) {
  const update=(e)=>setFiltros({...filtros,[e.target.name]:e.target.value});
  return <div className="filtros">
    <select aria-label="Filtrar equipos por estado" name="estado" value={filtros.estado} onChange={update}><option value="">Todos los estados</option><option>OPERATIVO</option><option>MANTENIMIENTO</option><option>INOPERATIVO</option><option>BAJA</option></select>
    <select aria-label="Filtrar equipos por laboratorio" name="idLaboratorio" value={filtros.idLaboratorio} onChange={update}><option value="">Todos los laboratorios</option>{laboratorios.map(x=><option key={x.id} value={x.id}>{x.codigo} · {x.nombre}</option>)}</select>
    <select aria-label="Filtrar equipos por subcategoría" name="idSubcategoria" value={filtros.idSubcategoria} onChange={update}><option value="">Todas las subcategorías</option>{subcategorias.map(x=><option key={x.id} value={x.id}>{x.nombre}</option>)}</select>
    <select aria-label="Filtrar equipos por necesidad de mantenimiento" name="requiereMantenimiento" value={filtros.requiereMantenimiento} onChange={update}><option value="">Mantenimiento: todos</option><option value="true">Requiere mantenimiento</option><option value="false">No requiere</option></select>
    <button type="button" disabled={disabled} className="btn btn-secundario" onClick={onApply}>Aplicar</button>
  </div>;
}
