package com.viaticos.backend_viaticos.repository;

import com.viaticos.backend_viaticos.dto.response.UsuarioAdminDTO;
import com.viaticos.backend_viaticos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE u.empleado.correo = :correo")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    Optional<Usuario> findByEmpleado_IdEmpleado(Long idEmpleado);

    @Query(value = """
                SELECT
                    u.id_usuario AS "idUsuario",
                    e.id_empleado AS "idEmpleado",
                    (e.nombre || ' ' || e.apellido) AS "nombreCompleto",
                    e.correo AS "correo",
                    d.nombre AS "departamento",
                    c.nombre AS "cargo",
                    em.nombre AS "empresa",
                    p.nombre AS "pais",
                    r.nombre AS "rol",
                    u.estado AS "estado"
                FROM USUARIO u
                JOIN EMPLEADO e ON u.id_empleado = e.id_empleado
                JOIN DEPARTAMENTO d ON e.id_departamento = d.id_departamento
                JOIN CARGO c ON e.id_cargo = c.id_cargo
                JOIN EMPRESA em ON e.id_empresa = em.id_empresa
                JOIN PAIS p ON e.id_pais = p.id_pais
                JOIN ROL r ON u.id_rol = r.id_rol
            """, nativeQuery = true)
    List<UsuarioAdminDTO> findAllUsuariosAdmin();
}
