export function fechaValida(valor) {
  if (typeof valor !== 'string' || !valor.trim()) return null;
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? null : fecha;
}

export function ordenarMovimientos(items) {
  return [...items].sort((a, b) => {
    const fechaA = fechaValida(a?.fechaMovimiento)?.getTime() ?? -Infinity;
    const fechaB = fechaValida(b?.fechaMovimiento)?.getTime() ?? -Infinity;
    return fechaA === fechaB ? (b?.id ?? 0) - (a?.id ?? 0) : fechaB - fechaA;
  });
}

export function esMovimientoDeHoy(movimiento, ahora = new Date()) {
  const fecha = fechaValida(movimiento?.fechaMovimiento);
  return Boolean(fecha && fecha.getFullYear() === ahora.getFullYear() &&
    fecha.getMonth() === ahora.getMonth() && fecha.getDate() === ahora.getDate());
}

const normalizar = (valor) => String(valor ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export function filtrarMovimientos(items, busqueda) {
  const texto = normalizar(busqueda).trim();
  if (!texto) return items;
  return items.filter(movimiento => normalizar([
    movimiento?.tipoMovimiento, movimiento?.motivo,
    movimiento?.equipo?.codigoInterno, movimiento?.equipo?.nombre,
    movimiento?.laboratorioOrigen?.codigo, movimiento?.laboratorioOrigen?.nombre,
    movimiento?.laboratorioDestino?.codigo, movimiento?.laboratorioDestino?.nombre,
    movimiento?.actor?.nombre, movimiento?.actor?.apellido, movimiento?.actor?.userName
  ].filter(Boolean).join(' ')).includes(texto));
}

export function validarListaMovimientos(data) {
  if (!Array.isArray(data) || data.some(item => !item || !Number.isInteger(item.id) || item.id <= 0)) {
    throw new Error('La respuesta de movimientos no es válida.');
  }
  return ordenarMovimientos(data);
}
