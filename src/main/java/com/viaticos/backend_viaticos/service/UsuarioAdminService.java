package com.viaticos.backend_viaticos.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.dto.request.CreateUserRequestDTO;
import com.viaticos.backend_viaticos.dto.request.UpdateUserRequestDTO;
import com.viaticos.backend_viaticos.dto.response.UsuarioAdminDTO;
import com.viaticos.backend_viaticos.dto.response.UsuarioCreadoResponseDTO;
import com.viaticos.backend_viaticos.entity.Cargo;
import com.viaticos.backend_viaticos.entity.Departamento;
import com.viaticos.backend_viaticos.entity.Empleado;
import com.viaticos.backend_viaticos.entity.Empresa;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.entity.Rol;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.CargoRepository;
import com.viaticos.backend_viaticos.repository.DepartamentoRepository;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;
import com.viaticos.backend_viaticos.repository.EmpresaRepository;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
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

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
    private static final SecureRandom random = new SecureRandom();

    private String generarPasswordTemporal(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // 🛠️ MÉTODO PRIVADO PARA REUTILIZAR LA LÓGICA DE AUDITORÍA
    private void registrarAuditoria(String accion, String tabla, Long idRegistro, String valorAnterior,
            String valorNuevo, String descripcion, Usuario usuarioAfectado) {
        LogAuditoria log = new LogAuditoria();
        log.setAccion(accion);
        log.setTablaAfectada(tabla);
        log.setIdRegistroAfectado(idRegistro);
        log.setValorAnterior(valorAnterior);
        log.setValorNuevo(valorNuevo);
        log.setDescripcion(descripcion);
        log.setUsuarioAfectado(usuarioAfectado);

        // Extraer el usuario que está realizando la acción (El Administrador logueado)
        try {
            String correoEjecutor = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            usuarioRepository.findByCorreo(correoEjecutor).ifPresent(log::setUsuario);
        } catch (Exception e) {
            System.out.println("No se pudo obtener el usuario del contexto de seguridad.");
        }

        logAuditoriaRepository.save(log);
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
        usuario.setEstado("ACTIVO");
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeCambiarPassword(1); // 🔥 nuevo campo en tabla

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        registrarAuditoria(
                "CREACIÓN",
                "USUARIO",
                usuarioGuardado.getIdUsuario(),
                null,
                null,
                "Creó nueva cuenta para: " + empleadoGuardado.getNombre() + " " + empleadoGuardado.getApellido(),
                usuarioGuardado);

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
                passwordTemporal);
    }

    public List<UsuarioAdminDTO> listarUsuariosAdmin() {
        return usuarioRepository.findAllUsuariosAdmin();
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstado(Long idUsuario) {
        Usuario user = usuarioRepository.findById(idUsuario).orElseThrow();
        String estadoAnterior = user.getEstado();

        if (estadoAnterior.equalsIgnoreCase("ACTIVO")) {
            user.setEstado("INACTIVO");
        } else {
            user.setEstado("ACTIVO");
        }
        usuarioRepository.save(user);

        // ✨ REGISTRO DE AUDITORÍA
        registrarAuditoria(
                user.getEstado().equals("ACTIVO") ? "EDICIÓN" : "ELIMINACIÓN", // Si lo inactiva, lo marcamos como
                                                                               // eliminación lógica
                "USUARIO",
                user.getIdUsuario(),
                estadoAnterior,
                user.getEstado(),
                "Cambió el estado de acceso del usuario",
                user);

        return usuarioRepository.findAllUsuariosAdmin().stream().filter(u -> u.getIdUsuario().equals(idUsuario))
                .findFirst().orElseThrow();
    }

    @Transactional
    public java.util.Map<String, String> resetPassword(Long idUsuario) {
        Usuario user = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar nueva contraseña temporal
        String passwordTemporal = generarPasswordTemporal(10);

        // Actualizar el usuario
        user.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        user.setDebeCambiarPassword(1);

        usuarioRepository.save(user);

        // Devolver los datos para el frontend
        java.util.Map<String, String> response = new java.util.HashMap<>();
        // Asumiendo que getEmpleado() funciona; si no, user.getCorreo() o similar según
        // tu entidad
        response.put("correo", user.getEmpleado().getCorreo());
        response.put("passwordTemporal", passwordTemporal);

        registrarAuditoria(
                "EDICIÓN",
                "USUARIO",
                user.getIdUsuario(),
                "HASH_ANTERIOR",
                "HASH_NUEVO",
                "Generó una nueva contraseña temporal",
                user);

        return response;
    }

    @Transactional
    public UsuarioAdminDTO actualizarUsuario(Long idUsuario, UpdateUserRequestDTO dto) {
        Usuario user = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Empleado empleado = user.getEmpleado();

        // Buscar las nuevas entidades
        Departamento dep = departamentoRepository.findById(dto.getIdDepartamento()).orElseThrow();
        Cargo cargo = cargoRepository.findById(dto.getIdCargo()).orElseThrow();
        Empresa empresa = empresaRepository.findById(dto.getIdEmpresa()).orElseThrow();
        Pais pais = paisRepository.findById(dto.getIdPais()).orElseThrow();
        Rol rol = rolRepository.findById(dto.getIdRol()).orElseThrow();

        Empleado jefe = null;
        if (dto.getIdJefe() != null) {
            jefe = empleadoRepository.findById(dto.getIdJefe()).orElseThrow();
        }

        // Actualizar datos del empleado
        empleado.setDepartamento(dep);
        empleado.setCargo(cargo);
        empleado.setEmpresa(empresa);
        empleado.setPais(pais);
        empleado.setJefe(jefe);
        empleadoRepository.save(empleado);

        // Actualizar rol del usuario
        user.setRol(rol);
        usuarioRepository.save(user);

        registrarAuditoria(
                "EDICIÓN",
                "EMPLEADO",
                empleado.getIdEmpleado(),
                null,
                null,
                "Actualizó el perfil del usuario (Rol, Depto, Cargo o País)",
                user);

        // Devolver la información actualizada
        return usuarioRepository.findAllUsuariosAdmin().stream()
                .filter(u -> u.getIdUsuario().equals(idUsuario))
                .findFirst()
                .orElseThrow();
    }

}
