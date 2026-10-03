import test from 'node:test';
import assert from 'node:assert/strict';
import {
  listaUsuarios, puedeAdministrarUsuarios, payloadCrearUsuario, payloadEditarUsuario,
  payloadEstadoUsuario, payloadPasswordUsuario, payloadRolUsuario, usuarioPublico, validarPasswordUsuario
} from './usuarios.js';

const formulario = { userName: ' nuevo ', nombre: ' Ana ', apellido: ' Pérez ', email: ' ana@example.test ',
  cargo: ' Técnica ', password: 'abcd', confirmacion: 'abcd', rol: 'GESTOR', activo: true };

test('crear usuario envía solo los campos reales del DTO y activo explícito', () => {
  assert.deepEqual(payloadCrearUsuario({ ...formulario, passwordHash: 'no-enviar', id: 999 }), {
    userName: 'nuevo', nombre: 'Ana', apellido: 'Pérez', email: 'ana@example.test', cargo: 'Técnica',
    password: 'abcd', rol: 'GESTOR', activo: true
  });
  assert.equal(payloadCrearUsuario({ ...formulario, activo: false }).activo, false);
});

test('PUT usuario no envía username, rol, activo, contraseña ni IDs', () => {
  assert.deepEqual(payloadEditarUsuario({ ...formulario, id: 3, passwordHash: 'no-enviar' }), {
    nombre: 'Ana', apellido: 'Pérez', email: 'ana@example.test', cargo: 'Técnica'
  });
  assert.equal(payloadEditarUsuario({ ...formulario, cargo: '   ' }).cargo, null);
});

test('rol, estado y restablecimiento tienen payloads independientes', () => {
  assert.deepEqual(payloadRolUsuario('LECTOR'), { rol: 'LECTOR' });
  assert.deepEqual(payloadEstadoUsuario(false), { activo: false });
  assert.deepEqual(payloadPasswordUsuario(' nueva ', ' nueva '), { password: ' nueva ' });
  assert.throws(() => payloadRolUsuario('SUPERADMIN'), /rol válido/);
  assert.throws(() => payloadEstadoUsuario('false'), /estado válido/);
  assert.throws(() => payloadPasswordUsuario('abcd'), /Confirma/);
  assert.throws(() => payloadCrearUsuario({ ...formulario, confirmacion: undefined }), /Confirma/);
});

test('contraseña valida longitud Java, bytes UTF-8 y confirmación sin recortarla', () => {
  assert.equal(validarPasswordUsuario(' abcd ', ' abcd '), ' abcd ');
  assert.doesNotThrow(() => validarPasswordUsuario('x'.repeat(72)));
  assert.doesNotThrow(() => validarPasswordUsuario('á'.repeat(36)));
  for (const password of ['', '    ', 'abc', 'x'.repeat(73), 'á'.repeat(37), '😀'.repeat(19)]) {
    assert.throws(() => validarPasswordUsuario(password));
  }
  assert.throws(() => validarPasswordUsuario('abcd', 'abce'), /no coinciden/);
});

test('campos públicos respetan tamaños y obligatorios del backend', () => {
  assert.throws(() => payloadCrearUsuario({ ...formulario, userName: 'x'.repeat(51) }), /50/);
  assert.throws(() => payloadEditarUsuario({ ...formulario, nombre: ' ' }), /obligatorio/);
  assert.throws(() => payloadEditarUsuario({ ...formulario, apellido: 'x'.repeat(101) }), /100/);
  assert.throws(() => payloadEditarUsuario({ ...formulario, email: 'sin-arroba' }), /formato/);
  assert.throws(() => payloadEditarUsuario({ ...formulario, cargo: 'x'.repeat(101) }), /100/);
});

test('el listado administrativo conserva activos e inactivos sin campos sensibles', () => {
  const base = { id: 1, userName: 'ana', nombre: 'Ana', rol: 'ADMIN', activo: true, passwordHash: 'no-mostrar', password: 'no-mostrar' };
  const lista = listaUsuarios([base, { ...base, id: 2, userName: 'aldo', activo: false }]);
  assert.equal(lista.length, 2);
  assert.equal(lista[1].activo, false);
  assert.equal(Object.hasOwn(usuarioPublico(base), 'passwordHash'), false);
  assert.equal(Object.hasOwn(usuarioPublico(base), 'password'), false);
  assert.throws(() => listaUsuarios([base, base]), /IDs repetidos/);
  assert.throws(() => listaUsuarios({ content: [base] }), /listado/);
});

test('acciones de administración se habilitan exclusivamente para ADMIN', () => {
  assert.equal(puedeAdministrarUsuarios('ADMIN'), true);
  for (const rol of ['GESTOR', 'LECTOR', '', undefined]) assert.equal(puedeAdministrarUsuarios(rol), false);
});
