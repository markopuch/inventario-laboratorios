export const ROLES_USUARIO = Object.freeze(['ADMIN', 'GESTOR', 'LECTOR']);

function invalido(message) {
  const error = new Error(message);
  error.code = 'VALIDACION_LOCAL';
  throw error;
}

function texto(value, nombre, maximo, requerido = true) {
  if (value != null && typeof value !== 'string') invalido(`${nombre} no es válido.`);
  const resultado = (value ?? '').trim();
  if (requerido && !resultado) invalido(`${nombre} es obligatorio.`);
  if (resultado.length > maximo) invalido(`${nombre} no puede superar los ${maximo} caracteres.`);
  return resultado;
}

export function puedeAdministrarUsuarios(rol) {
  return rol === 'ADMIN';
}

export function validarPasswordUsuario(password, confirmacion) {
  if (typeof password !== 'string' || !password.trim()) invalido('La contraseña es obligatoria.');
  // String.length coincide con las unidades UTF-16 de String.length en Java.
  if (password.length < 4 || password.length > 72) invalido('La contraseña debe tener entre 4 y 72 caracteres.');
  if (new TextEncoder().encode(password).length > 72) invalido('La contraseña no puede superar los 72 bytes UTF-8.');
  if (confirmacion !== undefined && password !== confirmacion) invalido('Las contraseñas no coinciden.');
  return password;
}

export function payloadEditarUsuario(form = {}) {
  const email = texto(form.email, 'El email', 150);
  if (!/^[^\s@]+@[^\s@]+$/.test(email)) invalido('El email debe tener un formato válido.');
  return {
    nombre: texto(form.nombre, 'El nombre', 100),
    apellido: texto(form.apellido, 'El apellido', 100),
    email,
    cargo: texto(form.cargo, 'El cargo', 100, false) || null
  };
}

export function payloadRolUsuario(rol) {
  if (!ROLES_USUARIO.includes(rol)) invalido('Selecciona un rol válido.');
  return { rol };
}

export function payloadEstadoUsuario(activo) {
  if (typeof activo !== 'boolean') invalido('Selecciona un estado válido.');
  return { activo };
}

export function payloadCrearUsuario(form = {}) {
  if (typeof form.confirmacion !== 'string') invalido('Confirma la contraseña.');
  return {
    userName: texto(form.userName, 'El usuario', 50),
    ...payloadEditarUsuario(form),
    password: validarPasswordUsuario(form.password, form.confirmacion),
    ...payloadRolUsuario(form.rol),
    ...payloadEstadoUsuario(form.activo)
  };
}

export function payloadPasswordUsuario(password, confirmacion) {
  if (typeof confirmacion !== 'string') invalido('Confirma la contraseña.');
  return { password: validarPasswordUsuario(password, confirmacion) };
}

export function usuarioPublico(data) {
  if (!Number.isSafeInteger(data?.id) || data.id <= 0 || typeof data.userName !== 'string' ||
      !ROLES_USUARIO.includes(data.rol) || typeof data.activo !== 'boolean') {
    invalido('La API devolvió un usuario inválido. Vuelve a consultar.');
  }
  // No se conservan campos internos o sensibles aunque una respuesta los añadiera.
  return Object.fromEntries(['id', 'userName', 'nombre', 'apellido', 'email', 'cargo', 'rol', 'activo', 'fechaCreacion']
    .map(campo => [campo, data[campo]]));
}

export function listaUsuarios(data) {
  if (!Array.isArray(data)) invalido('La API devolvió un listado de usuarios inválido.');
  const usuarios = data.map(usuarioPublico);
  if (new Set(usuarios.map(usuario => usuario.id)).size !== usuarios.length) invalido('El listado de usuarios contiene IDs repetidos.');
  return usuarios;
}
