package com.viaticos.backend_viaticos.controller;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.viaticos.backend_viaticos.entity.Regla;
import com.viaticos.backend_viaticos.service.OciGenAiService;
import com.viaticos.backend_viaticos.service.ReglaService;

import java.util.List;

@RestController
@RequestMapping("/admin/reglas")
@CrossOrigin(origins = "http://localhost:5173")
public class ReglaAdminController {

    @Autowired
    private ReglaService reglaService;

    @Autowired
    private OciGenAiService ociGenAiService;

    @GetMapping
    public ResponseEntity<List<Regla>> listarReglas() {
        return ResponseEntity.ok(reglaService.listarTodas());
    }

    // ✨ EL ENDPOINT MÁGICO PARA PDF/WORD
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzePolicyDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Extraer el texto crudo del archivo (PDF, Word, TXT)
            Tika tika = new Tika();
            String documentText = tika.parseToString(file.getInputStream());

            // 2. Pasar el texto a Cohere para convertirlo en el JSON de Lujo
            String jsonInteligente = ociGenAiService.extractRulesFromDocument(documentText);

            return ResponseEntity.ok(jsonInteligente);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error analizando documento con IA: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Regla> guardarRegla(@RequestBody Regla regla) {
        return ResponseEntity.ok(reglaService.guardarRegla(regla));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activarRegla(@PathVariable Long id) {
        reglaService.activarRegla(id);
        return ResponseEntity.ok("Regla activada");
    }
}
