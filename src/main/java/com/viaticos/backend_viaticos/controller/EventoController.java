package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.dto.request.EventoRequestDTO;
import com.viaticos.backend_viaticos.dto.request.EventoUpdateRequestDTO;
import com.viaticos.backend_viaticos.dto.response.EventoDTO;
import com.viaticos.backend_viaticos.service.EventoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;




@RestController
@RequestMapping("/eventos")
@CrossOrigin(origins = "http://localhost:5173")
public class EventoController {
    
    @Autowired
    private EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listar() {
        return ResponseEntity.ok(eventoService.listarEventosConTotales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerEventoPorId(id));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<String> finalizar(@PathVariable Long id, @RequestParam Long idUsuario) {
        try{

            eventoService.finalizarEvento(id, idUsuario);
            return ResponseEntity.ok("Evento finalizado con exito");

        }catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar la solicitud: \" + e.getMessage())");
        }
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody EventoRequestDTO request, @RequestParam Long idUsuario) {
        try {

            eventoService.guardarEvento(request, idUsuario);
            return ResponseEntity.ok("Evento creado con exito");

        }catch (Exception e) {
            return ResponseEntity.status(500).body("Error al crear el evento");
        }
    }

    @PatchMapping("/{id}/actualizar")
public ResponseEntity<String> actualizar(
        @PathVariable Long id, 
        @RequestBody EventoUpdateRequestDTO request,
        @RequestParam Long idUsuario) { // Pasamos el usuario por parámetro para la auditoría
    try {
        eventoService.actualizarEvento(id, request, idUsuario);
        return ResponseEntity.ok("Evento actualizado y auditado correctamente");
    } catch (RuntimeException e) {
        return ResponseEntity.status(400).body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error interno del servidor");
    }
}
    
    
}
