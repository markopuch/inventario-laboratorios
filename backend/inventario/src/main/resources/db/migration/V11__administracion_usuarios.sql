-- La columna cargo ya existe desde V2. No se alteran identidades ni credenciales existentes.
-- Fallar antes de crear índices si los datos históricos requieren revisión manual.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario GROUP BY UPPER(username) HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Existen usernames duplicados sin distinguir mayúsculas/minúsculas; revise los datos antes de migrar';
    END IF;
    IF EXISTS (SELECT 1 FROM usuario GROUP BY UPPER(email) HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Existen emails duplicados sin distinguir mayúsculas/minúsculas; revise los datos antes de migrar';
    END IF;
END $$;

-- UPPER coincide con las consultas IgnoreCase de Spring Data; incluye usuarios inactivos.
-- uq_usuario_username_ignore_case ya protege username desde V6 y se conserva intacto.
CREATE UNIQUE INDEX uq_usuario_email_ignore_case ON usuario (UPPER(email));
