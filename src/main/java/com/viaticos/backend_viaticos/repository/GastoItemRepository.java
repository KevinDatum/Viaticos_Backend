package com.viaticos.backend_viaticos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.GastoItem;
import java.util.List;


@Repository
public interface GastoItemRepository extends JpaRepository<GastoItem, Long>{

    List<GastoItem> findByGasto_IdGasto(Long idGasto);

    @Modifying
    @Query("DELETE FROM GastoItem i WHERE i.gasto.idGasto = :idGasto")
    void deleteByGastoId(@Param("idGasto") Long idGasto);
    
}
