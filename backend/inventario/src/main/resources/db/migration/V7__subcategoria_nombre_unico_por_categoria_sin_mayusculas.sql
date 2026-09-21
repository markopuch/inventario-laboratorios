-- Los nombres de las subcategorías inactivas también siguen reservados en su categoría.
-- No corregir ni borrar duplicados automáticamente: detener la migración para revisarlos.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM subcategoria
        GROUP BY id_categoria, UPPER(nombre)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen subcategorías con nombres duplicados dentro de una categoría.'
            USING HINT = 'Revisar los datos antes de aplicar V7. No borrar, fusionar ni ejecutar Flyway repair automáticamente.';
    END IF;
END;
$$;

-- UPPER coincide con las consultas IgnoreCase de Spring Data JPA.
CREATE UNIQUE INDEX uq_subcategoria_categoria_nombre_ignore_case
    ON subcategoria (id_categoria, UPPER(nombre));
