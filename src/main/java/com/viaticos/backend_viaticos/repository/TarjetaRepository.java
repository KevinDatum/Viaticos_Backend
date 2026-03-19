package com.viaticos.backend_viaticos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.entity.Tarjeta;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Long>{

    // 1. Para el Empleado/Gerente: Trae solo las tarjetas que tiene asignadas
    // La sintaxis 'Empleado_IdEmpleado' le dice a Spring que busque el ID dentro del objeto Empleado
    List<Tarjeta> findByEmpleado_IdEmpleadoAndEstado(Long idEmpleado, String estado);

    // 2. Para el Admin: Trae TODAS las tarjetas que NO tienen dueño (Disponibles en inventario)
    List<Tarjeta> findByEmpleadoIsNull();
    
    // 3. Para el Admin: Filtrar tarjetas por país (opcional, para futuras vistas)
    List<Tarjeta> findByPais_IdPais(Long idPais);
    
}
