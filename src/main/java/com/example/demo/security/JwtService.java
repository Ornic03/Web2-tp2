package com.example.demo.security;

import com.example.demo.model.Usuario;
import com.example.demo.model.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes (HS256)");
        }
        if (expirationMinutes < 1) throw new IllegalArgumentException("Duración JWT inválida");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String crear(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.id().toString())
                .claim("rol", usuario.rol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public UsuarioAutenticado validar(String token) throws JwtException {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        // El parser comprueba la firma y la expiración antes de confiar en los claims.
        Long id = Long.valueOf(claims.getSubject());
        if (id <= 0) throw new IllegalArgumentException("Identificador de usuario inválido");
        String rolClaim = claims.get("rol", String.class);
        if (rolClaim == null) throw new MalformedJwtException("El token no contiene rol");
        Rol rol = Rol.valueOf(rolClaim);
        return new UsuarioAutenticado(id, rol);
    }

    public long expiracionSegundos() { return expirationMinutes * 60; }
}
