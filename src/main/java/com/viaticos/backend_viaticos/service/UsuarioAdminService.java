package com.viaticos.backend_viaticos.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.dto.request.CreateUserRequestDTO;
import com.viaticos.backend_viaticos.dto.response.UsuarioAdminDTO;
import com.viaticos.backend_viaticos.dto.response.UsuarioCreadoResponseDTO;
import com.viaticos.backend_viaticos.entity.Cargo;
import com.viaticos.backend_viaticos.entity.Departamento;
import com.viaticos.backend_viaticos.entity.Empleado;
import com.viaticos.backend_viaticos.entity.Empresa;
import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.entity.Rol;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.CargoRepository;
import com.viaticos.backend_viaticos.repository.DepartamentoRepository;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;
import com.viaticos.backend_viaticos.repository.EmpresaRepository;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.repository.RolRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class UsuarioAdminService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
    private static final SecureRandom random = new SecureRandom();

    private String generarPasswordTemporal(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional
    public UsuarioCreadoResponseDTO crearUsuarioConEmpleado(CreateUserRequestDTO dto) {

        Optional<Usuario> userExistente = usuarioRepository.findByCorreo(dto.getCorreo());
        if (userExistente.isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese correo");
        }

        Departamento dep = departamentoRepository.findById(dto.getIdDepartamento())
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));

        Empresa empresa = empresaRepository.findById(dto.getIdEmpresa())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Pais pais = paisRepository.findById(dto.getIdPais())
                .orElseThrow(() -> new RuntimeException("País no encontrado"));

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        Empleado jefe = null;
        if (dto.getIdJefe() != null) {
            jefe = empleadoRepository.findById(dto.getIdJefe())
                    .orElseThrow(() -> new RuntimeException("Jefe no encontrado"));
        }

        // Crear empleado
        Empleado empleado = new Empleado();
        empleado.setNombre(dto.getNombre());
        empleado.setApellido(dto.getApellido());
        empleado.setCorreo(dto.getCorreo());
        empleado.setDepartamento(dep);
        empleado.setCargo(cargo);
        empleado.setEmpresa(empresa);
        empleado.setPais(pais);
        empleado.setJefe(jefe);

        Empleado empleadoGuardado = empleadoRepository.save(empleado);

        // Generar password temporal
        String passwordTemporal = generarPasswordTemporal(10);

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setEmpleado(empleadoGuardado);
        usuario.setRol(rol);
        usuario.setEstado("activo");
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeCambiarPassword(1); // 🔥 nuevo campo en tabla

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return new UsuarioCreadoResponseDTO(
                usuarioGuardado.getIdUsuario(),
                empleadoGuardado.getIdEmpleado(),
                empleadoGuardado.getNombre() + " " + empleadoGuardado.getApellido(),
                empleadoGuardado.getCorreo(),
                dep.getNombre(),
                cargo.getNombre(),
                empresa.getNombre(),
                pais.getNombre(),
                rol.getNombre(),
                usuarioGuardado.getEstado(),
                passwordTemporal
        );
    }

    public List<UsuarioAdminDTO> listarUsuariosAdmin() {
        return usuarioRepository.findAllUsuariosAdmin();
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstado(Long idUsuario) {

        Usuario user = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getEstado().equalsIgnoreCase("activo")) {
            user.setEstado("inactivo");
        } else {
            user.setEstado("activo");
        }

        usuarioRepository.save(user);

        // retornamos la lista actualizada (opción simple)
        // o devolvemos un DTO armado manualmente (mejor pero más código)
        return usuarioRepository.findAllUsuariosAdmin()
                .stream()
                .filter(u -> u.getIdUsuario().equals(idUsuario))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se pudo devolver el usuario actualizado"));
    }

}
