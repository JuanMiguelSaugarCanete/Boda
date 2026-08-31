package com.boda.bousers.services;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // Clave secreta fija cargada de configuración (JWT_SECRET en .env):
    // los tokens siguen siendo válidos aunque se reinicie el servidor
    private final Key secretKey;

    // Duración del token en milisegundos (por defecto 24 horas)
    private final long expirationTime;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:86400000}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    public String generateToken(String email, String familyId) {
        return Jwts.builder()
                .setSubject(email)                          // Identificador principal (email)
                .claim("familyId", familyId)                // Guardamos el ID de la familia dentro del token
                .setIssuedAt(new Date())                    // Fecha de emisión
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Expiración
                .signWith(secretKey)                        // Firma digital
                .compact();
    }

    // Valida la firma y la expiración del token y devuelve sus datos.
    // Lanza io.jsonwebtoken.JwtException si el token es inválido o ha expirado.
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
