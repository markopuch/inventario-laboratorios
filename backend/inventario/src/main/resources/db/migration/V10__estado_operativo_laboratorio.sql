-- El estado operativo no sustituye la baja lógica ni altera el alcance.
ALTER TABLE laboratorio
    ADD COLUMN estado_operativo VARCHAR(20) NOT NULL DEFAULT 'OPERATIVO';

ALTER TABLE laboratorio
    ADD CONSTRAINT ck_laboratorio_estado_operativo
        CHECK (estado_operativo IN ('OPERATIVO', 'MANTENIMIENTO'));
