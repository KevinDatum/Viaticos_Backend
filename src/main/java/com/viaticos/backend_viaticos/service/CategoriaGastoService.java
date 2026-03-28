package com.viaticos.backend_viaticos.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.entity.CategoriaGasto;
import com.viaticos.backend_viaticos.repository.CategoriaGastoRepository;

@Service
public class CategoriaGastoService {

    @Autowired
    private CategoriaGastoRepository categoriaGastoRepository;

    // Obtener todas las categorías
    public List<CategoriaGasto> obtenerTodas() {
        return categoriaGastoRepository.findAll();
    }

    // Crear una nueva categoría
    public CategoriaGasto crearCategoria(CategoriaGasto categoria) {
        // Aseguramos que siempre esté en mayúsculas
        String nombreLimpio = categoria.getNombre().trim().toUpperCase();
        
        CategoriaGasto nuevaCategoria = new CategoriaGasto();
        nuevaCategoria.setNombre(nombreLimpio);
        
        return categoriaGastoRepository.save(nuevaCategoria);
    }

    // Eliminar una categoría
    public void eliminarCategoria(Long idCategoria) {
        // Nota: Si la categoría ya está asociada a un Gasto en la base de datos, 
        // Spring Data lanzará una DataIntegrityViolationException. 
        // Esto es excelente porque protege la integridad de los datos históricos.
        categoriaGastoRepository.deleteById(idCategoria);
    }
}
