package com.viaticos.backend_viaticos.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaticos.backend_viaticos.entity.ReportTemplate;
import com.viaticos.backend_viaticos.repository.ReportTemplateRepository;

@Service
public class ReportTemplateService {

    @Autowired
    private ReportTemplateRepository repository;

    @Autowired
    private ExcelGeneratorService excelGeneratorService;

    public ReportTemplate saveTemplate(String nombre, String mapping, MultipartFile file) throws Exception {

        ReportTemplate template = new ReportTemplate();

        template.setNombre(nombre);
        template.setMappingJson(mapping);
        template.setExcelFile(file.getBytes());
        template.setFechaCreacion(LocalDateTime.now());
        template.setActivo(0);

        return repository.save(template);
    }

    public String getActiveMapping() {

        return repository.findByActivo(1)
                .map(ReportTemplate::getMappingJson)
                .orElseThrow(() -> new RuntimeException("No hay plantilla activa"));
    }

    public void activateTemplate(Long id) {

        List<ReportTemplate> templates = repository.findAll();

        for (ReportTemplate t : templates) {
            t.setActivo(0);
            repository.save(t);
        }

        ReportTemplate template = repository.findById(id)
                .orElseThrow();

        template.setActivo(1);

        repository.save(template);
    }

    public List<ReportTemplate> getAllTemplates() {
        return repository.findAll();
    }

    public byte[] generateReport(Map<String, Object> data) throws Exception {

        System.out.println("LLAMANDO A generateExcel");

        ReportTemplate template = repository.findByActivo(1)
                .orElseThrow(() -> new RuntimeException("No hay plantilla activa"));

        String mappingJson = template.getMappingJson();

        System.out.println("===== MAPPING CARGADO =====");
        System.out.println(mappingJson);
        System.out.println("===========================");

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> mapping = mapper.readValue(
                mappingJson,
                new TypeReference<Map<String, Object>>() {
                });

        // ⚠️ Aquí cargamos la plantilla Excel base
        InputStream templateStream = new ByteArrayInputStream(template.getExcelFile());

        System.out.println("ENTRANDO A generateReport");

        return excelGeneratorService.generateExcel(
                templateStream,
                mapping,
                data);
    }

}
