package com.viaticos.backend_viaticos.security;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) throws Exception {

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario u : usuarios) {

            String passwordActual = u.getPasswordHash();

            // Si ya está encriptada, no hacer nada
            if (passwordActual != null && passwordActual.startsWith("$2")) {
                continue;
            }

            // Si está en texto plano, se encripta
            String passwordEncriptada = passwordEncoder.encode(passwordActual);

            u.setPasswordHash(passwordEncriptada);
            usuarioRepository.save(u);

            System.out.println("Contraseña migrada para usuario ID: " + u.getIdUsuario());
        }

        System.out.println("✅ Migración finalizada.");
    }
}
