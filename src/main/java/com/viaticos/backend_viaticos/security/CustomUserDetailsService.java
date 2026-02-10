package com.viaticos.backend_viaticos.security;


import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        // Validar estado del usuario
        if (usuario.getEstado() != null && usuario.getEstado().equalsIgnoreCase("INACTIVO")) {
            throw new UsernameNotFoundException("Usuario inactivo");
        }

        String rol = usuario.getRol().getNombre(); // EJ: ADMIN, EMPLEADO, GERENTE

        return new org.springframework.security.core.userdetails.User(
                usuario.getEmpleado().getCorreo(),
                usuario.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }
}
