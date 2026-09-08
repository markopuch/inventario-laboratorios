CREATE TABLE equipo (
    id_equipo SERIAL PRIMARY KEY,
    codigo_interno VARCHAR(50) NOT NULL,
    serie_utec VARCHAR(100),
    numero_serie VARCHAR(100),
    nombre VARCHAR(150) NOT NULL,
    marca VARCHAR(100),
    modelo VARCHAR(100),
    estado VARCHAR(30) NOT NULL DEFAULT 'OPERATIVO',
    anio INTEGER,
    orden_compra VARCHAR(50),
    ubicacion_interna VARCHAR(200),
    comentario TEXT,
    requiere_mantenimiento BOOLEAN NOT NULL DEFAULT FALSE,
    id_subcategoria INTEGER NOT NULL,
    id_laboratorio INTEGER NOT NULL,
    id_responsable INTEGER NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_equipo_codigo_interno UNIQUE (codigo_interno),
    CONSTRAINT uq_equipo_serie_utec UNIQUE (serie_utec),
    CONSTRAINT uq_equipo_numero_serie UNIQUE (numero_serie),
    CONSTRAINT ck_equipo_estado
        CHECK (estado IN ('OPERATIVO', 'MANTENIMIENTO', 'INOPERATIVO', 'BAJA')),
    CONSTRAINT ck_equipo_anio CHECK (anio IS NULL OR anio BETWEEN 1900 AND 2100),
    CONSTRAINT fk_equipo_subcategoria FOREIGN KEY (id_subcategoria)
        REFERENCES subcategoria (id_subcategoria) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_equipo_laboratorio FOREIGN KEY (id_laboratorio)
        REFERENCES laboratorio (id_laboratorio) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_equipo_responsable FOREIGN KEY (id_responsable)
        REFERENCES usuario (id_usuario) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE TABLE movimiento_equipo (
    id_movimiento SERIAL PRIMARY KEY,
    id_equipo INTEGER NOT NULL,
    id_laboratorio_origen INTEGER NULL,
    id_laboratorio_destino INTEGER NOT NULL,
    id_usuario_actor INTEGER NOT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movimiento_equipo_tipo_no_vacio CHECK (BTRIM(tipo_movimiento) <> ''),
    CONSTRAINT ck_movimiento_equipo_motivo_no_vacio CHECK (BTRIM(motivo) <> ''),
    CONSTRAINT fk_movimiento_equipo_equipo FOREIGN KEY (id_equipo)
        REFERENCES equipo (id_equipo) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_equipo_origen FOREIGN KEY (id_laboratorio_origen)
        REFERENCES laboratorio (id_laboratorio) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_equipo_destino FOREIGN KEY (id_laboratorio_destino)
        REFERENCES laboratorio (id_laboratorio) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_equipo_actor FOREIGN KEY (id_usuario_actor)
        REFERENCES usuario (id_usuario) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX idx_equipo_id_subcategoria ON equipo (id_subcategoria);
CREATE INDEX idx_equipo_id_laboratorio ON equipo (id_laboratorio);
CREATE INDEX idx_equipo_id_responsable ON equipo (id_responsable);
CREATE INDEX idx_movimiento_equipo_id_equipo ON movimiento_equipo (id_equipo);
CREATE INDEX idx_movimiento_equipo_id_laboratorio_origen
    ON movimiento_equipo (id_laboratorio_origen);
CREATE INDEX idx_movimiento_equipo_id_laboratorio_destino
    ON movimiento_equipo (id_laboratorio_destino);
CREATE INDEX idx_movimiento_equipo_id_usuario_actor ON movimiento_equipo (id_usuario_actor);
