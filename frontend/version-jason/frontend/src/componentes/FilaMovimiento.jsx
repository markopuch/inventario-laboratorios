import { ArrowRight } from 'lucide-react';
import { fechaValida } from '../utilidades/movimientos';

export default function FilaMovimiento({ movimiento }) {
  const item = movimiento ?? {};
  const fecha = fechaValida(item.fechaMovimiento);
  const actor = [item.actor?.nombre, item.actor?.apellido].filter(Boolean).join(' ') || item.actor?.userName;
  return <tr>
    <td>{fecha ? <time dateTime={fecha.toISOString()}>{fecha.toLocaleString('es-PE')}</time> : '—'}</td>
    <td>{item.tipoMovimiento || '—'}</td>
    <td><strong>{item.equipo?.codigoInterno || '—'}</strong><small>{item.equipo?.nombre || '—'}</small></td>
    <td>{item.laboratorioOrigen?.codigo || item.laboratorioOrigen?.nombre || '—'}</td>
    <td><ArrowRight size={16} aria-hidden="true"/></td>
    <td>{item.laboratorioDestino?.codigo || item.laboratorioDestino?.nombre || '—'}</td>
    <td>{actor || '—'}</td>
    <td>{item.motivo || '—'}</td>
  </tr>;
}
