CREATE TABLE auditoria (
    id_auditoria SERIAL PRIMARY KEY,
    id_usuario_actor INTEGER NULL,
    username_actor VARCHAR(50) NOT NULL,
    accion VARCHAR(40) NOT NULL,
    entidad VARCHAR(40) NOT NULL,
    id_entidad INTEGER NOT NULL,
    cambios VARCHAR(2000) NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_actor FOREIGN KEY (id_usuario_actor)
        REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT ck_auditoria_accion CHECK (accion IN (
        'CREAR','EDITAR','ACTIVAR','DESACTIVAR','CAMBIAR_ROL','RESTABLECER_CONTRASENA',
        'ASIGNAR_LABORATORIOS','TRASLADAR','INICIAR_MANTENIMIENTO',
        'COMPLETAR_MANTENIMIENTO','CANCELAR_MANTENIMIENTO')),
    CONSTRAINT ck_auditoria_entidad CHECK (entidad IN (
        'categoria','subcategoria','sede','area','laboratorio','usuario',
        'usuario_laboratorio','equipo','mantenimiento')),
    CONSTRAINT ck_auditoria_id_entidad CHECK (id_entidad > 0),
    CONSTRAINT ck_auditoria_username CHECK (BTRIM(username_actor) <> ''),
    CONSTRAINT ck_auditoria_cambios CHECK (BTRIM(cambios) <> '')
);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha DESC, id_auditoria DESC);
CREATE INDEX idx_auditoria_entidad ON auditoria (entidad, id_entidad);
CREATE INDEX idx_auditoria_actor ON auditoria (id_usuario_actor);

