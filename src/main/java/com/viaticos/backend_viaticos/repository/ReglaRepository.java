package com.viaticos.backend_viaticos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.Regla;

@Repository
public interface ReglaRepository extends JpaRepository<Regla, Long> {
    
    // Trae la política actual vigente (Donde ESTADO_ACTIVO es 1)
    Optional<Regla> findByEstadoActivo(Integer estadoActivo);
}
