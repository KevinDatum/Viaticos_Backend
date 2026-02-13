package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;
import com.viaticos.backend_viaticos.entity.GastoItem;
import com.viaticos.backend_viaticos.repository.GastoItemRepository;
import com.viaticos.backend_viaticos.service.GastoService;
//import com.viaticos.backend_viaticos.service.OciObjectStorageService;
import com.viaticos.backend_viaticos.service.storage.OciStorageService;
import com.viaticos.backend_viaticos.service.storage.StorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GastoItemRepository gastoItemRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private OciStorageService ociStorageService;
/* 
    @Autowired
    private OciObjectStorageService ociService;
*/
    @GetMapping
    public List<GastoDTO> obtenerTodos() {
        return gastoService.listarTodos();
    }

    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<GastoDTO>> obtenerPorEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(gastoService.listarPorEvento(idEvento));
    }

    @PostMapping
    public Gasto crearGasto(@RequestBody Gasto gasto) {
        return gastoService.guardarGasto(gasto);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<GastoItem>> obtenerItems(@PathVariable Long id) {

        List<GastoItem> items = gastoItemRepository.findByGasto_IdGasto(id);

        return ResponseEntity.ok(items);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        // Extraemos y convertimos los datos del JSON
        // Usamos .toString() y Long.valueOf para evitar errores de casteo
        Long idUsuario = Long.valueOf(body.get("idUsuario").toString());
        String nuevoEstado = body.get("estado").toString();
        String motivo = body.get("motivo").toString();
        String comentario = body.get("comentario").toString();

        // Llamamos al servicio con la nueva firma
        gastoService.actualizarEstado(id, idUsuario, nuevoEstado, motivo, comentario);

        return ResponseEntity.ok().build();
    }
    /* 
    @GetMapping("/{id}/imagen-url")
    public ResponseEntity<?> getImagenUrl(@PathVariable Long id) {

        try {
            String objectName = gastoService.obtenerObjectNameImagen(id);

            if (objectName == null || objectName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Este gasto no tiene imagen asociada"));
            }

            String url = ociService.generateParUrl(objectName, 180);

            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "objectName", objectName));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()));
        }
    }*/

    @PostMapping("/{id}/upload-imagen")
    public ResponseEntity<?> uploadImagen(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        try {
            byte[] webpBytes = storageService.convertToWebpBytes(file);

            if (webpBytes == null || webpBytes.length == 0) {
                throw new RuntimeException("No se generó contenido WEBP (webpBytes vacío).");
            }

            String objectName = storageService.generateObjectName("gastos");

            ociStorageService.uploadObject(webpBytes, objectName, "image/webp");

            // guardar el objectName en DB
            gastoService.actualizarImagen(id, objectName);

            return ResponseEntity.ok(Map.of(
                    "message", "Imagen subida correctamente",
                    "objectName", objectName));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/imagenes/par-urls")
    public ResponseEntity<?> generarParUrlsMasivo(@RequestBody List<String> objectNames) {

        try {
            if (objectNames == null || objectNames.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Lista vacía"
                ));
            }

            Map<String, String> resultado = new HashMap<>();

            for (String objectName : objectNames) {

                if (objectName == null || objectName.isBlank()) {
                    continue;
                }

                String url = ociStorageService.generateParUrl(objectName, 180); // 3 horas
                resultado.put(objectName, url);
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
