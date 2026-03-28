package com.viaticos.backend_viaticos.repository;

import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.CategoriaGasto;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Long>{
    
}
