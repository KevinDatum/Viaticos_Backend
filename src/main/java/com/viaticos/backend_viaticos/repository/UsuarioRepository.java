package com.viaticos.backend_viaticos.repository;

import com.viaticos.backend_viaticos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    @Query("SELECT u FROM Usuario u WHERE u.empleado.correo = :correo")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    Optional<Usuario> findByEmpleado_IdEmpleado(Long idEmpleado);
}
