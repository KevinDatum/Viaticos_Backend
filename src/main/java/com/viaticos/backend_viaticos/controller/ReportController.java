package com.viaticos.backend_viaticos.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.service.ReportTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportTemplateService templateService;

    @GetMapping("/template")
    public ResponseEntity<String> getActiveTemplate() {

        String mapping = templateService.getActiveMapping();

        return ResponseEntity.ok(mapping);
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateReport(@RequestBody Map<String, Object> data) {

        System.out.println("DATA RECIBIDA:");
        System.out.println(data);

        try {

            byte[] excel = templateService.generateReport(data);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=reporte.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excel);

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();

        }

    }

}
