-- La unicidad incluye categorías inactivas y evita duplicados por concurrencia.
-- UPPER coincide con la comparación IgnoreCase de Spring Data JPA.
-- Si existen duplicados previos, deben revisarse antes de aplicar esta migración.
CREATE UNIQUE INDEX uq_categoria_nombre_ignore_case ON categoria (UPPER(nombre));
