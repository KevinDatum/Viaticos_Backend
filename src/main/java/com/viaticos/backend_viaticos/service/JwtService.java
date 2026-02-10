package com.viaticos.backend_viaticos.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String SECRET_KEY = "8H2nP9rX3sKqT7mV2dF4gB1cY0zW5uJ9pL6aR3eN8xQ7sT2v";

    // 3 horas
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 3;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(Usuario usuario) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("idEmpleado", usuario.getEmpleado().getIdEmpleado());
        claims.put("rol", usuario.getRol().getNombre());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getEmpleado().getCorreo()) // o username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 1 hora
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractIdEmpleado(String token) {
        return extractAllClaims(token).get("idEmpleado", Long.class);
    }

    public String extractRol(String token) {
        return extractAllClaims(token).get("rol", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
