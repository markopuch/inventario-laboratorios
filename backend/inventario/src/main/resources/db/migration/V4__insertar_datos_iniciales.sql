-- Datos ficticios de desarrollo. No se crean usuarios ni credenciales.
INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMIN', 'Administración global del inventario'),
    ('GESTOR', 'Gestión del inventario de los laboratorios asignados'),
    ('LECTOR', 'Consulta del inventario de los laboratorios asignados');

INSERT INTO sede (nombre) VALUES ('Sede de ejemplo');

-- Las FK se resuelven por las claves de catálogo, sin asumir valores de SERIAL.
INSERT INTO area (nombre, descripcion, id_sede)
SELECT datos.nombre, datos.descripcion, s.id_sede
FROM sede AS s
CROSS JOIN (VALUES
    ('Ingeniería', 'Área de ejemplo para ingeniería'),
    ('Ciencias', 'Área de ejemplo para ciencias')
) AS datos(nombre, descripcion)
WHERE s.nombre = 'Sede de ejemplo';

INSERT INTO laboratorio (nombre, codigo, id_area)
SELECT datos.nombre, datos.codigo, a.id_area
FROM (VALUES
    ('Laboratorio de electrónica', 'L201', 'Ingeniería'),
    ('Laboratorio de ciencias', 'L206', 'Ciencias')
) AS datos(nombre, codigo, area_nombre)
JOIN area AS a ON a.nombre = datos.area_nombre
JOIN sede AS s ON s.id_sede = a.id_sede
WHERE s.nombre = 'Sede de ejemplo';

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Electrónica', 'Equipos electrónicos de laboratorio'),
    ('Instrumentación', 'Instrumentos de medición de laboratorio');

INSERT INTO subcategoria (nombre, descripcion, id_categoria)
SELECT datos.nombre, datos.descripcion, c.id_categoria
FROM (VALUES
    ('Fuentes de alimentación', 'Fuentes para equipos y circuitos', 'Electrónica'),
    ('Medición eléctrica', 'Instrumentos para magnitudes eléctricas', 'Electrónica'),
    ('Balanzas', 'Instrumentos para medición de masa', 'Instrumentación'),
    ('Medición dimensional', 'Instrumentos para medición de dimensiones', 'Instrumentación')
) AS datos(nombre, descripcion, categoria_nombre)
JOIN categoria AS c ON c.nombre = datos.categoria_nombre;
