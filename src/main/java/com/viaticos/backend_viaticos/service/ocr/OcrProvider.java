package com.viaticos.backend_viaticos.service.ocr;

public interface OcrProvider {
    /**
     * Procesa una imagen accesible por URL (PAR URL)
     * y devuelve el resultado en formato JSON string.
     */
    String process(String imageUrl) throws Exception ;
}
