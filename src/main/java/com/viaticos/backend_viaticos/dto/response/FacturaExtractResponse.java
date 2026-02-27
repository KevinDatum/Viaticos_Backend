package com.viaticos.backend_viaticos.dto.response;

import java.util.List;

import com.viaticos.backend_viaticos.dto.request.GastoItemOcrRequestDTO;
import com.viaticos.backend_viaticos.dto.request.GastoOcrRequestDTO;

import lombok.Data;

@Data
public class FacturaExtractResponse {

    private GastoOcrRequestDTO gasto;
    private List<GastoItemOcrRequestDTO> items;

    private AuditoriaExtract auditoria; 

    @Data
    public static class AuditoriaExtract {
        private String estado_ia;
        private String motivo_ia;
    }
}
