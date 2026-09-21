-- Las áreas inactivas también reservan su nombre dentro de la sede.
-- Los duplicados se revisan manualmente; nunca se borran ni fusionan aquí.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM area
        GROUP BY id_sede, UPPER(nombre)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen áreas con nombres duplicados dentro de una sede.'
            USING HINT = 'Revisar los datos antes de aplicar V8. No borrar, fusionar ni ejecutar Flyway repair automáticamente.';
    END IF;
END;
$$;

-- Se conserva la restricción sensible a mayúsculas de V1.
CREATE UNIQUE INDEX uq_area_sede_nombre_ignore_case
    ON area (id_sede, UPPER(nombre));
