package com.viaticos.backend_viaticos.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateAnalysisService {

    private final ExcelTemplateParserService parserService;
    private final OciGenAiService aiMappingService;

    public String analyzeTemplate(MultipartFile file) throws Exception {

        log.info("Analizando plantilla Excel...");

        // 1️⃣ Convertir Excel → JSON estructura
        String excelStructure =
                parserService.extractTemplateStructure(file.getInputStream());

        // 2️⃣ Enviar estructura al LLM
        String mapping =
                aiMappingService.generateTemplateMapping(excelStructure);

        return mapping;
    }
}
