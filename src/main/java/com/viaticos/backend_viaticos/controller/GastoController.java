package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;
import com.viaticos.backend_viaticos.entity.GastoItem;
import com.viaticos.backend_viaticos.repository.GastoItemRepository;
import com.viaticos.backend_viaticos.service.GastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GastoItemRepository gastoItemRepository;

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
}
