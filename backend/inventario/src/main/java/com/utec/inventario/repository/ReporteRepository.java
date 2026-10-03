package com.utec.inventario.repository;

import java.sql.Date;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.utec.inventario.domain.*;

@Repository
public class ReporteRepository {
    private final NamedParameterJdbcTemplate jdbc;
    @Autowired
    public ReporteRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Agregaciones en PostgreSQL: no descargan todas las entidades ni sus usuarios.
    public List<ConteoReporte> equiposPorEstado(FiltroReporte filtro, List<Integer> alcance, boolean global) {
        Consulta c = equipos(filtro, alcance, global);
        return conteos("SELECT e.estado AS valor, count(*) AS total FROM equipo e WHERE " + c.where()
                + " GROUP BY e.estado ORDER BY e.estado", c.params());
    }

    public List<EquiposLaboratorioReporte> equiposPorLaboratorio(FiltroReporte filtro,
            List<Integer> alcance, boolean global) {
        Consulta c = equipos(filtro, alcance, global);
        return this.jdbc.query("SELECT l.id_laboratorio,l.codigo,l.nombre,count(*) AS total FROM equipo e "
                + "JOIN laboratorio l ON l.id_laboratorio=e.id_laboratorio WHERE " + c.where()
                + " GROUP BY l.id_laboratorio,l.codigo,l.nombre ORDER BY l.id_laboratorio", c.params(),
                (rs, row) -> new EquiposLaboratorioReporte(rs.getInt("id_laboratorio"),
                        rs.getString("codigo"), rs.getString("nombre"), rs.getLong("total")));
    }

    public List<ConteoReporte> movimientosPorTipo(FiltroReporte filtro, List<Integer> alcance, boolean global) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(laboratorios(
                "(rl.id_laboratorio=m.id_laboratorio_origen OR rl.id_laboratorio=m.id_laboratorio_destino)",
                filtro, alcance, global, params));
        estadoEquipo(where, params, filtro);
        fechas(where, params, filtro, "m.fecha_movimiento", false);
        return conteos("SELECT m.tipo_movimiento AS valor,count(*) AS total FROM movimiento_equipo m "
                + "JOIN equipo e ON e.id_equipo=m.id_equipo WHERE " + where
                + " GROUP BY m.tipo_movimiento ORDER BY m.tipo_movimiento", params);
    }

    public List<ConteoReporte> mantenimientosPorEstado(FiltroReporte filtro, List<Integer> alcance, boolean global) {
        return mantenimientos(filtro, alcance, global, "estado");
    }

    public List<ConteoReporte> mantenimientosPorTipo(FiltroReporte filtro, List<Integer> alcance, boolean global) {
        return mantenimientos(filtro, alcance, global, "tipo");
    }

    private List<ConteoReporte> mantenimientos(FiltroReporte filtro, List<Integer> alcance,
            boolean global, String agrupacion) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(laboratorios("rl.id_laboratorio=e.id_laboratorio",
                filtro, alcance, global, params));
        estadoEquipo(where, params, filtro);
        if (filtro.getEstadoMantenimiento() != null) {
            where.append(" AND m.estado=:estadoMantenimiento");
            params.addValue("estadoMantenimiento", filtro.getEstadoMantenimiento().name());
        }
        if (filtro.getTipoMantenimiento() != null) {
            where.append(" AND m.tipo=:tipoMantenimiento");
            params.addValue("tipoMantenimiento", filtro.getTipoMantenimiento().name());
        }
        fechas(where, params, filtro, "m.fecha_programada", true);
        // agrupacion solo procede de los dos métodos anteriores, nunca del cliente.
        return conteos("SELECT m." + agrupacion + " AS valor,count(*) AS total FROM mantenimiento m "
                + "JOIN equipo e ON e.id_equipo=m.id_equipo WHERE " + where
                + " GROUP BY m." + agrupacion + " ORDER BY m." + agrupacion, params);
    }

    private Consulta equipos(FiltroReporte filtro, List<Integer> alcance, boolean global) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(laboratorios("rl.id_laboratorio=e.id_laboratorio",
                filtro, alcance, global, params));
        estadoEquipo(where, params, filtro);
        fechas(where, params, filtro, "e.fecha_creacion", false);
        return new Consulta(where.toString(), params);
    }

    private String laboratorios(String relacion, FiltroReporte filtro, List<Integer> alcance,
            boolean global, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder("EXISTS (SELECT 1 FROM laboratorio rl "
                + "JOIN area ra ON ra.id_area=rl.id_area WHERE " + relacion);
        if (!global) {
            if (alcance.isEmpty()) sql.append(" AND FALSE");
            else { sql.append(" AND rl.id_laboratorio IN (:alcance)"); params.addValue("alcance", alcance); }
        }
        if (filtro.getIdSede() != null) {
            sql.append(" AND ra.id_sede=:idSede"); params.addValue("idSede", filtro.getIdSede());
        }
        if (filtro.getIdArea() != null) {
            sql.append(" AND ra.id_area=:idArea"); params.addValue("idArea", filtro.getIdArea());
        }
        if (filtro.getIdLaboratorio() != null) {
            sql.append(" AND rl.id_laboratorio=:idLaboratorio"); params.addValue("idLaboratorio", filtro.getIdLaboratorio());
        }
        return sql.append(")").toString();
    }

    private void estadoEquipo(StringBuilder sql, MapSqlParameterSource params, FiltroReporte filtro) {
        if (filtro.getEstado() != null) {
            sql.append(" AND e.estado=:estadoEquipo"); params.addValue("estadoEquipo", filtro.getEstado().name());
        }
    }

    private void fechas(StringBuilder sql, MapSqlParameterSource params, FiltroReporte filtro,
            String columna, boolean fechaSinHora) {
        if (filtro.getFechaDesde() != null) {
            sql.append(" AND ").append(columna).append(">=:desde");
            params.addValue("desde", fechaSinHora ? Date.valueOf(filtro.getFechaDesde())
                    : filtro.getFechaDesde().atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        if (filtro.getFechaHasta() != null) {
            sql.append(" AND ").append(columna).append(fechaSinHora ? "<=:hasta" : "<:hasta");
            params.addValue("hasta", fechaSinHora ? Date.valueOf(filtro.getFechaHasta())
                    : filtro.getFechaHasta().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }
    }

    private List<ConteoReporte> conteos(String sql, MapSqlParameterSource params) {
        return this.jdbc.query(sql, params, (rs, row) ->
                new ConteoReporte(rs.getString("valor"), rs.getLong("total")));
    }
    private record Consulta(String where, MapSqlParameterSource params) {}
}

