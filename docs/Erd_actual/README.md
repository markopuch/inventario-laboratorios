# Diagramas actuales — Inventario de Laboratorios

Modelo vigente revisado el **11 de octubre de 2026**, basado en Flyway V1–V13 y
las Entities JPA: **12 entidades, 98 columnas y 16 relaciones FK**.

| Documento | Uso | Imagen ampliable | Imagen para compartir | Fuente editable |
|---|---|---|---|---|
| [ERD lógico v3](erd-logico-v3.md) | Entidades, atributos principales y cardinalidades. | [SVG](erd-logico-v3.svg) | [PNG](erd-logico-v3.png) | [DOT](erd-logico-v3.dot) + Mermaid en el MD |
| [ERD físico v3](erd-fisico-v3.md) | Todas las columnas, tipos PostgreSQL, PK/FK, nullability y restricciones. | [SVG](erd-fisico-v3.svg) | [PNG](erd-fisico-v3.png) | [DOT](erd-fisico-v3.dot) + Mermaid en el MD |
| [Modelo vigente V13](modelo-vigente-v13.md) | Contexto de ampliaciones y evidencia previa. | — | — | Markdown |

Para estudiar o exponer empieza por el lógico. Para revisar SQL utiliza el físico
y amplía el SVG; su diccionario Markdown permite leer los campos por tabla.

Auditoría tiene una FK al actor; entidad/id_entidad es una referencia lógica.
Mantenimiento obtiene el laboratorio mediante Equipo. No hay tabla Reporte.

## Documentos históricos

- [ERD lógico v2](erd-logico-v2.md) y [ERD físico v2](erd-fisico-v2.md): corte V1–V9,
  diez entidades y trece FK. Se mantienen para conservar la evolución del proyecto.
- [Cambios de v1 a v2](erd-v2-cambios.md).
- [PDF lógico original](../erd-logico.pdf) y [PDF físico original](../erd-fisico.pdf):
  diseños anteriores; no representan el modelo actual.

Los diagramas v3 documentan el esquema existente y no introducen cambios de
código, Flyway, datos o frontend.
