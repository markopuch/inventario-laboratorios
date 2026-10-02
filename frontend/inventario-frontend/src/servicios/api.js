const API_URL = (import.meta.env.VITE_API_URL ?? '')
  .trim()
  .replace(/\/+$/, '')

function crearError(message, status = 0, errors = {}, tipo = 'HTTP') {
  const error = new Error(message)

  error.name = 'ApiError'
  error.status = status
  error.errors = errors
  error.tipo = tipo

  return error
}

export async function solicitarApi(ruta, opciones = {}) {
  const { method = 'GET', body, token } = opciones

  if (!API_URL) {
    throw crearError(
      'Falta configurar la dirección de la API.',
      0,
      {},
      'CONFIGURACION',
    )
  }

  const headers = {
    Accept: 'application/json',
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const contenido = body === undefined ? undefined : JSON.stringify(body)

  let respuesta
  let texto

  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method,
      headers,
      body: contenido,
      credentials: 'omit',
    })

    texto = respuesta.status === 204 ? '' : await respuesta.text()
  } catch {
    throw crearError(
      'No se pudo completar la conexión con el servidor.',
      0,
      {},
      'RED',
    )
  }

  let datos = null

  if (texto) {
    try {
      datos = JSON.parse(texto)
    } catch {
      if (respuesta.ok) {
        throw crearError(
          'El servidor devolvió una respuesta con formato inesperado.',
          respuesta.status,
          {},
          'RESPUESTA',
        )
      }
    }
  }

  if (!respuesta.ok) {
    const esErrorApi =
      datos?.status === respuesta.status &&
      typeof datos?.message === 'string'

    const mensaje = esErrorApi
      ? datos.message
      : respuesta.status >= 500
        ? 'El servidor no pudo completar la solicitud.'
        : `La solicitud fue rechazada (HTTP ${respuesta.status}).`

    const errores =
      esErrorApi &&
      datos.errors &&
      typeof datos.errors === 'object' &&
      !Array.isArray(datos.errors)
        ? datos.errors
        : {}

    throw crearError(mensaje, respuesta.status, errores)
  }

  return datos
}