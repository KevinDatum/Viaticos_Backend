package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.request.LoginRequest;
import com.viaticos.backend_viaticos.dto.response.LoginResponse;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;
import com.viaticos.backend_viaticos.service.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(request.getCorreo());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        Usuario user = userOpt.get();

        // 🛡️ REGLA EXTRA 1: Solo usuarios ACTIVOS pueden ingresar
        if (!"ACTIVO".equalsIgnoreCase(user.getEstado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tu cuenta está deshabilitada. Contacta al administrador.");
        }

        // ✅ Comparar con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        // 🛡️ REGISTRO DE AUDITORÍA: LOGIN EXITOSO
        LogAuditoria log = new LogAuditoria();
        log.setAccion("LOGIN");
        log.setTablaAfectada("USUARIO");
        log.setIdRegistroAfectado(user.getIdUsuario());
        log.setDescripcion("Inicio de sesión exitoso en el sistema.");
        log.setUsuario(user);
        logAuditoriaRepository.save(log);

        String token = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(
                user.getIdUsuario(),
                user.getEmpleado().getIdEmpleado(),
                user.getEmpleado().getNombre() + " " + user.getEmpleado().getApellido(),
                user.getRol().getNombre(),
                user.getEmpleado().getDepartamento().getNombre(),
                user.getEmpleado().getCorreo(),
                user.getDebeCambiarPassword(), // 🛡️ REGLA EXTRA 2: Indicador para el Frontend
                token
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-forced-password")
    public ResponseEntity<?> resetForcedPassword(@RequestBody Map<String, String> payload) {
        try {
            Long idUsuario = Long.parseLong(payload.get("idUsuario"));
            String nuevaPassword = payload.get("nuevaPassword");

            Usuario user = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Encriptamos la nueva contraseña elegida por el usuario
            user.setPasswordHash(passwordEncoder.encode(nuevaPassword));
            
            // ¡IMPORTANTE! Liberamos al usuario del ciclo de reseteo
            user.setDebeCambiarPassword(0); 

            usuarioRepository.save(user);

            // 🛡️ REGISTRO DE AUDITORÍA
            LogAuditoria log = new LogAuditoria();
            log.setAccion("EDICIÓN");
            log.setTablaAfectada("USUARIO");
            log.setIdRegistroAfectado(user.getIdUsuario());
            log.setDescripcion("El usuario estableció su contraseña privada en el primer inicio de sesión.");
            log.setUsuario(user);
            logAuditoriaRepository.save(log);

            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar la contraseña");
        }
    }

}
