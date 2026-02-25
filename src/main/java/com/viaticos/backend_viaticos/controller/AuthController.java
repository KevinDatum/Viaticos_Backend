package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.request.LoginRequest;
import com.viaticos.backend_viaticos.dto.response.LoginResponse;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;
import com.viaticos.backend_viaticos.service.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(request.getCorreo());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        Usuario user = userOpt.get();

        // ✅ Comparar con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        String token = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(
                user.getIdUsuario(),
                user.getEmpleado().getIdEmpleado(),
                user.getEmpleado().getNombre() + " " + user.getEmpleado().getApellido(),
                user.getRol().getNombre(),
                user.getEmpleado().getDepartamento().getNombre(),
                user.getEmpleado().getCorreo(),
                user.getDebeCambiarPassword(),
                token
        );

        return ResponseEntity.ok(response);
    }

}
