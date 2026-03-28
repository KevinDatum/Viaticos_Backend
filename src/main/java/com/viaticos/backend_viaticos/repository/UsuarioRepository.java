package com.viaticos.backend_viaticos.repository;

import com.viaticos.backend_viaticos.dto.response.UsuarioAdminDTO;
import com.viaticos.backend_viaticos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Se quedan igual (Eficientes por naturaleza)
    @Query("SELECT u FROM Usuario u WHERE u.empleado.correo = :correo")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    Optional<Usuario> findByEmpleado_IdEmpleado(Long idEmpleado);

    // ✨ Optimizado mediante Vista
    @Query(value = "SELECT * FROM VW_USUARIOS_ADMIN", nativeQuery = true)
    List<UsuarioAdminDTO> findAllUsuariosAdmin();
}
