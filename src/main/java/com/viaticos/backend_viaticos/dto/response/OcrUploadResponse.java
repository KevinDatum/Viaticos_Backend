package com.viaticos.backend_viaticos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OcrUploadResponse {

    private Long jobId;
    private String objectNameTemp;
    private String objectNameWebp;
    private String parUrlWebp;
    private String status;
    private String message;
}
