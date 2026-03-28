package com.viaticos.backend_viaticos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.entity.CategoriaGasto;
import com.viaticos.backend_viaticos.service.CategoriaGastoService;

@RestController
@RequestMapping("/categorias") 
public class CategoriaGastoController {

    @Autowired
    private CategoriaGastoService categoriaGastoService;

    // 1. GET: Listar todas
    @GetMapping
    public ResponseEntity<List<CategoriaGasto>> obtenerTodas() {
        return ResponseEntity.ok(categoriaGastoService.obtenerTodas());
    }

    // 2. POST: Crear nueva
    @PostMapping
    public ResponseEntity<?> crearCategoria(@RequestBody CategoriaGasto categoria) {
        try {
            CategoriaGasto nueva = categoriaGastoService.crearCategoria(categoria);
            return ResponseEntity.ok(nueva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. DELETE: Eliminar por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaGastoService.eliminarCategoria(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Este catch atrapará el error si intentas borrar una categoría que ya tiene gastos asociados
            return ResponseEntity.badRequest().body(
                Map.of("error", "No se pudo eliminar la categoría. Es probable que ya esté en uso por algunos gastos.")
            );
        }
    }
}
