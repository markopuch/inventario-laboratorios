CREATE TABLE mantenimiento (
    id_mantenimiento SERIAL PRIMARY KEY,
    id_equipo INTEGER NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    fecha_programada DATE NOT NULL,
    fecha_inicio TIMESTAMPTZ,
    fecha_fin TIMESTAMPTZ,
    id_responsable INTEGER,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADO',
    observaciones TEXT,
    estado_equipo_anterior VARCHAR(30),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mantenimiento_equipo FOREIGN KEY (id_equipo)
        REFERENCES equipo (id_equipo) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_mantenimiento_responsable FOREIGN KEY (id_responsable)
        REFERENCES usuario (id_usuario) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_mantenimiento_tipo CHECK (tipo IN ('PREVENTIVO', 'CORRECTIVO', 'CALIBRACION', 'OTRO')),
    CONSTRAINT ck_mantenimiento_estado CHECK (estado IN ('PROGRAMADO', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')),
    CONSTRAINT ck_mantenimiento_descripcion CHECK (BTRIM(descripcion) <> ''),
    CONSTRAINT ck_mantenimiento_observaciones CHECK (observaciones IS NULL OR CHAR_LENGTH(observaciones) <= 4000),
    CONSTRAINT ck_mantenimiento_estado_equipo_anterior
        CHECK (estado_equipo_anterior IS NULL OR estado_equipo_anterior IN ('OPERATIVO', 'INOPERATIVO', 'MANTENIMIENTO')),
    CONSTRAINT ck_mantenimiento_orden_fechas CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_mantenimiento_ciclo CHECK (
        (estado = 'PROGRAMADO' AND fecha_inicio IS NULL AND fecha_fin IS NULL AND estado_equipo_anterior IS NULL)
        OR (estado = 'EN_PROCESO' AND fecha_inicio IS NOT NULL AND fecha_fin IS NULL AND estado_equipo_anterior IS NOT NULL)
        OR (estado = 'COMPLETADO' AND fecha_inicio IS NOT NULL AND fecha_fin IS NOT NULL AND estado_equipo_anterior IS NOT NULL)
        OR (estado = 'CANCELADO' AND fecha_fin IS NOT NULL AND
            ((fecha_inicio IS NULL AND estado_equipo_anterior IS NULL) OR (fecha_inicio IS NOT NULL AND estado_equipo_anterior IS NOT NULL)))
    )
);

CREATE INDEX idx_mantenimiento_equipo ON mantenimiento (id_equipo);
CREATE INDEX idx_mantenimiento_responsable ON mantenimiento (id_responsable);
CREATE INDEX idx_mantenimiento_fecha_programada ON mantenimiento (fecha_programada);
CREATE INDEX idx_mantenimiento_estado_tipo ON mantenimiento (estado, tipo);
CREATE UNIQUE INDEX uq_mantenimiento_equipo_en_proceso
    ON mantenimiento (id_equipo) WHERE estado = 'EN_PROCESO';
