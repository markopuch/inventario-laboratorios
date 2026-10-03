package com.utec.inventario.mapper;
import java.util.List;
import org.mapstruct.Mapper;
import com.utec.inventario.domain.*;
import com.utec.inventario.dto.response.*;

@Mapper(componentModel = "spring")
public interface ReporteMapper {
    FiltroReporte convert(com.utec.inventario.dto.request.FiltroReporteRequest request);
    ConteoReporteResponse toResponse(ConteoReporte domain);
    List<ConteoReporteResponse> toResponseConteos(List<ConteoReporte> domains);
    EquiposLaboratorioReporteResponse toResponse(EquiposLaboratorioReporte domain);
    List<EquiposLaboratorioReporteResponse> toResponseLaboratorios(List<EquiposLaboratorioReporte> domains);
    ResumenReporteResponse toResponse(ResumenReporte domain);
    MovimientosReporteResponse toResponse(MovimientosReporte domain);
    MantenimientosReporteResponse toResponse(MantenimientosReporte domain);
}
