package com.viaticos.backend_viaticos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.ReportTemplate;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long>{

    Optional<ReportTemplate> findByActivo(Integer activo);
    
}
