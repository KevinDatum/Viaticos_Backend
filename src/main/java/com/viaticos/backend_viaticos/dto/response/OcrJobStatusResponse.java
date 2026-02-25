package com.viaticos.backend_viaticos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OcrJobStatusResponse {
    
    private Long jobId;
    private String status;
    private String errorMessage;
    private String resultJson;
    private Long idGasto;
}
