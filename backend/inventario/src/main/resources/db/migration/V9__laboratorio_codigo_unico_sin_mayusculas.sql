-- El código se reserva globalmente, incluso si el laboratorio está inactivo.
-- Los duplicados se revisan manualmente; nunca se borran ni fusionan aquí.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM laboratorio
        GROUP BY UPPER(codigo)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen laboratorios con códigos duplicados sin distinguir mayúsculas.'
            USING HINT = 'Revisar los datos antes de aplicar V9. No borrar, fusionar ni ejecutar Flyway repair automáticamente.';
    END IF;
END;
$$;

-- Se conserva la restricción global sensible a mayúsculas de V1.
CREATE UNIQUE INDEX uq_laboratorio_codigo_ignore_case
    ON laboratorio (UPPER(codigo));
