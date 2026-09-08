-- Organización y catálogos. Las referencias se conservan mediante bajas lógicas.
CREATE TABLE sede (
    id_sede SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200),
    distrito VARCHAR(100),
    departamento VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE area (
    id_area SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    id_sede INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_area_nombre_sede UNIQUE (nombre, id_sede),
    CONSTRAINT fk_area_sede FOREIGN KEY (id_sede)
        REFERENCES sede (id_sede) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE TABLE laboratorio (
    id_laboratorio SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    ubicacion VARCHAR(200),
    id_area INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_laboratorio_codigo UNIQUE (codigo),
    CONSTRAINT fk_laboratorio_area FOREIGN KEY (id_area)
        REFERENCES area (id_area) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE TABLE categoria (
    id_categoria SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
);

CREATE TABLE subcategoria (
    id_subcategoria SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    id_categoria INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subcategoria_nombre_categoria UNIQUE (nombre, id_categoria),
    CONSTRAINT fk_subcategoria_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_area_id_sede ON area (id_sede);
CREATE INDEX idx_laboratorio_id_area ON laboratorio (id_area);
CREATE INDEX idx_subcategoria_id_categoria ON subcategoria (id_categoria);
