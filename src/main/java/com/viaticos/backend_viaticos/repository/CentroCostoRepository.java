package com.viaticos.backend_viaticos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.CentroCosto;

@Repository
public interface CentroCostoRepository extends JpaRepository<CentroCosto, Long>{
    
}
