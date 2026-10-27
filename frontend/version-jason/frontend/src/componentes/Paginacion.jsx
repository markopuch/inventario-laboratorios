import Boton from './Boton';
import { rangoPagina } from '../utilidades/paginacion.js';

export default function Paginacion({ pagina, numero, tamano, cargando, error, disabled = false,
  nombre, onPagina, onTamano }) {
  const { desde, hasta } = rangoPagina(pagina);
  return <nav className="paginacion" aria-label={`Paginación de ${nombre}`} aria-busy={cargando}>
    <label>Por página<select aria-label={`${nombre} por página`} value={tamano} disabled={disabled}
      onChange={event => onTamano(Number(event.target.value))}>
      {[20, 50, 100].map(valor => <option key={valor} value={valor}>{valor}</option>)}
    </select></label>
    <p role="status" aria-live="polite">{cargando ? 'Consultando resultados…' : error || !pagina
      ? 'Paginación no disponible' : pagina.totalElements === 0 ? '0 resultados · 0 páginas'
        : `${desde}–${hasta} de ${pagina.totalElements} · Página ${pagina.number + 1} de ${pagina.totalPages}`}</p>
    <div className="paginacion-acciones">
      <Boton variante="secundario" disabled={disabled || cargando || !pagina || pagina.first}
        onClick={() => onPagina(numero - 1)}>Anterior</Boton>
      <Boton variante="secundario" disabled={disabled || cargando || !pagina || pagina.last}
        onClick={() => onPagina(numero + 1)}>Siguiente</Boton>
    </div>
  </nav>;
}
