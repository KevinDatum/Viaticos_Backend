package com.viaticos.backend_viaticos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.viaticos.backend_viaticos.entity.ReportTemplate;
import com.viaticos.backend_viaticos.service.ReportTemplateService;
import com.viaticos.backend_viaticos.service.TemplateAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/template")
@RequiredArgsConstructor
public class TemplateAdminController {

    private final TemplateAnalysisService templateAnalysisService;

    @Autowired
    private ReportTemplateService templateService;

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeExcelTemplate(@RequestParam("file") MultipartFile file) {

        try {

            String jsonMapping = templateAnalysisService.analyzeTemplate(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonMapping);

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Error al generar el mapeo: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveTemplate(
            @RequestParam("nombre") String nombre,
            @RequestParam("mapping") String mapping,
            @RequestParam("file") MultipartFile file) {

        try {

            templateService.saveTemplate(nombre, mapping, file);

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(e.getMessage());

        }
    }

    // Activar plantilla
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateTemplate(@PathVariable Long id) {

        templateService.activateTemplate(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReportTemplate>> getTemplates() {

        List<ReportTemplate> templates = templateService.getAllTemplates();

        return ResponseEntity.ok(templates);
    }
}