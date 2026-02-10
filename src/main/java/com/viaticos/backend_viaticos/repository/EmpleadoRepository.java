package com.viaticos.backend_viaticos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.dto.response.EmpleadoDTO;
import com.viaticos.backend_viaticos.entity.Empleado;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Query("""
                SELECT
                    e.idEmpleado as idEmpleado,
                    e.nombre as nombre,
                    e.apellido as apellido,
                    e.correo as correo,
                    d.idDepartamento as idDepartamento,
                    d.nombre as departamento,
                    c.idCargo as idCargo,
                    c.nombre as cargo
                FROM Empleado e
                JOIN e.departamento d
                JOIN e.cargo c
                WHERE e.jefe.idEmpleado = :idGerente
            """)
    List<EmpleadoDTO> obtenerSubordinados(@Param("idGerente") Long idGerente);
}
