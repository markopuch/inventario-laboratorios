CREATE TABLE rol (
    id_rol SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    cargo VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_rol INTEGER NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- El alcance se registra aquí; usuario no contiene id_area ni id_laboratorio.
CREATE TABLE usuario_laboratorio (
    id_usuario INTEGER NOT NULL,
    id_laboratorio INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_laboratorio PRIMARY KEY (id_usuario, id_laboratorio),
    CONSTRAINT fk_usuario_laboratorio_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_laboratorio_laboratorio FOREIGN KEY (id_laboratorio)
        REFERENCES laboratorio (id_laboratorio) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_usuario_id_rol ON usuario (id_rol);
-- La PK compuesta ya cubre las búsquedas por su primera columna, id_usuario.
CREATE INDEX idx_usuario_laboratorio_id_laboratorio
    ON usuario_laboratorio (id_laboratorio);
