# Memoria de Inventario de Laboratorios

Lee [memory.md](memory.md) antes de trabajar. Contiene la arquitectura vigente,
versiones, mapa del repositorio, estado de cada componente, decisiones y pendientes.
Sus anexos están en [docs/memory](docs/memory/).

Usa el código/configuración actuales como autoridad y distingue configuración,
evidencia histórica y verificación nueva. El frontend integrado es Jason;
Marko es una variante de tutoría pausada, conservada solo en la computadora local.
`frontend/version-marko/` y `docs/sprints_realizados-frontend-Marko/` están
excluidas de Git: no borrarlas ni volver a agregarlas con `git add -f`.
Los clones nuevos no incluyen esas carpetas. No mezcles sus dependencias o sprints.
No alteres migraciones Flyway ya aplicadas ni datos para actualizar documentación.
No incluyas contraseñas, hashes de contraseñas, JWT, secretos o `.env` privados.

Tras un cambio material autorizado, actualiza la memoria afectada con fecha,
fuentes y resultado de las comprobaciones. No presentes suites antiguas como
recién ejecutadas. Las preferencias y restricciones históricas no sustituyen
la instrucción actual del usuario.
