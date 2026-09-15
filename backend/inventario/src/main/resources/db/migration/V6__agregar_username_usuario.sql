-- Conserva los usuarios existentes y asigna un identificador de acceso único.
ALTER TABLE usuario ADD COLUMN username VARCHAR(50);

UPDATE usuario
SET username = 'usuario_' || id_usuario;

ALTER TABLE usuario ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX uq_usuario_username_ignore_case ON usuario (UPPER(username));
