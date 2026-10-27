const entero = valor => Number.isSafeInteger(valor) && valor >= 0;

export function validarPagina(data, validarContenido = contenido => contenido) {
  if (!data || Array.isArray(data) || !Array.isArray(data.content) ||
      !entero(data.number) || !entero(data.size) || data.size < 1 || data.size > 100 ||
      !entero(data.totalElements) || !entero(data.totalPages) ||
      data.totalPages !== Math.ceil(data.totalElements / data.size) ||
      data.first !== (data.number === 0) ||
      data.last !== (data.totalPages === 0 || data.number >= data.totalPages - 1) ||
      data.content.length !== Math.max(0, Math.min(data.size, data.totalElements - data.number * data.size))) {
    throw Object.assign(new Error('El servidor devolvió una página inválida. Intenta actualizar los resultados.'),
      { code: 'RESPUESTA_PAGINA_INVALIDA' });
  }
  const content = validarContenido(data.content);
  return { content, number: data.number, size: data.size, totalElements: data.totalElements,
    totalPages: data.totalPages, first: data.first, last: data.last };
}

export function rangoPagina(pagina) {
  if (!pagina?.content.length) return { desde: 0, hasta: 0 };
  const desde = pagina.number * pagina.size + 1;
  return { desde, hasta: desde + pagina.content.length - 1 };
}

export function paginaDisponible(pagina) {
  return Math.max(0, Math.min(pagina.number, pagina.totalPages - 1));
}
