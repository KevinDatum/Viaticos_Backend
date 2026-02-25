package com.viaticos.backend_viaticos.dto.response;

import java.util.List;

import com.viaticos.backend_viaticos.dto.request.GastoItemOcrRequestDTO;
import com.viaticos.backend_viaticos.dto.request.GastoOcrRequestDTO;

import lombok.Data;

@Data
public class FacturaExtractResponse {

    private GastoOcrRequestDTO gasto;
    private List<GastoItemOcrRequestDTO> items;

    private String metodoPago;
    private String ultimos4Tarjeta;
}
