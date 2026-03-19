package com.viaticos.backend_viaticos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.dto.response.ReporteTarjetaDTO;
import com.viaticos.backend_viaticos.dto.response.TarjetaDTO;
import com.viaticos.backend_viaticos.service.TarjetaService;

@RestController
@RequestMapping("/tarjetas")
public class TarjetaController {

    @Autowired
    private TarjetaService tarjetaService;

    // ==========================================
    // 1. GET: VER TODAS LAS TARJETAS (Para el Admin)
    // Ruta: GET /api/tarjetas
    // ==========================================
    @GetMapping
    public ResponseEntity<List<TarjetaDTO>> obtenerTodas() {
        return ResponseEntity.ok(tarjetaService.obtenerTodas());
    }

    // ==========================================
    // 2. GET: VER TARJETAS DE UN EMPLEADO (Para "Mis Tarjetas")
    // Ruta: GET /api/tarjetas/empleado/5
    // ==========================================
    @GetMapping("/empleado/{idEmpleado}")
    public ResponseEntity<List<TarjetaDTO>> obtenerPorEmpleado(@PathVariable Long idEmpleado) {
        return ResponseEntity.ok(tarjetaService.obtenerPorEmpleado(idEmpleado));
    }

    // ==========================================
    // 3. POST: CREAR NUEVA TARJETA EN INVENTARIO
    // Ruta: POST /api/tarjetas?idAdmin=1
    // ==========================================
    @PostMapping
    public ResponseEntity<TarjetaDTO> crearTarjeta(
            @RequestBody TarjetaDTO dto, 
            @RequestParam Long idAdmin) {
        return ResponseEntity.ok(tarjetaService.crearTarjeta(dto, idAdmin));
    }

    // ==========================================
    // 4. PUT: ASIGNAR TARJETA A EMPLEADO
    // Ruta: PUT /api/tarjetas/10/asignar/5?idAdmin=1
    // ==========================================
    @PutMapping("/{idTarjeta}/asignar/{idEmpleado}")
    public ResponseEntity<TarjetaDTO> asignarTarjeta(
            @PathVariable Long idTarjeta, 
            @PathVariable Long idEmpleado, 
            @RequestParam Long idAdmin) {
        return ResponseEntity.ok(tarjetaService.asignarTarjeta(idTarjeta, idEmpleado, idAdmin));
    }

    // ==========================================
    // 5. PUT: REVOCAR TARJETA (Regresarla a inventario)
    // Ruta: PUT /api/tarjetas/10/revocar?idAdmin=1
    // ==========================================
    @PutMapping("/{idTarjeta}/revocar")
    public ResponseEntity<TarjetaDTO> revocarTarjeta(
            @PathVariable Long idTarjeta, 
            @RequestParam Long idAdmin) {
        return ResponseEntity.ok(tarjetaService.revocarTarjeta(idTarjeta, idAdmin));
    }

    // ==========================================
    // 6. POST: REPORTAR INCIDENCIA (Empleado)
    // Ruta: POST /api/tarjetas/10/reportar
    // ==========================================
    @PostMapping("/{idTarjeta}/reportar")
    public ResponseEntity<TarjetaDTO> reportarIncidencia(
            @PathVariable Long idTarjeta,
            @RequestBody ReporteTarjetaDTO reporte) {
        return ResponseEntity.ok(tarjetaService.reportarTarjeta(idTarjeta, reporte));
    }

    // ==========================================
    // 7. PUT: RESOLVER INCIDENCIA (Admin)
    // Ruta: PUT /api/tarjetas/10/resolver?nuevoEstado=BLOQUEADA&idAdmin=1
    // ==========================================
    @PutMapping("/{idTarjeta}/resolver")
    public ResponseEntity<TarjetaDTO> resolverIncidencia(
            @PathVariable Long idTarjeta,
            @RequestParam String nuevoEstado,
            @RequestBody Map<String, String> body,
            @RequestParam Long idAdmin) {
        
        String resolucion = body.getOrDefault("resolucion", "");
        return ResponseEntity.ok(tarjetaService.resolverIncidencia(idTarjeta, nuevoEstado, resolucion, idAdmin));
    }
}
