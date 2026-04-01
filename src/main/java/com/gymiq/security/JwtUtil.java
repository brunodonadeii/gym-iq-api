package com.gymiq.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;


@Slf4j
@Component
public class JwtUtil {

    @Value("${gymiq.jwt.secret}")
    private String jwtSecret;

    @Value("${gymiq.jwt.expiration-ms}")
    private long jwtExpirationMs;


    public String gerarToken(String email, String perfil, Integer idUsuario) {
        return Jwts.builder()
                .subject(email)
                .claim("perfil", perfil)
                .claim("idUsuario", idUsuario)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getChave())
                .compact();
    }


    public String extrairEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    public String extrairPerfil(String token) {
        return (String) parsearClaims(token).get("perfil");
    }

    public Integer extrairIdUsuario(String token) {
        return ((Number) parsearClaims(token).get("idUsuario")).intValue();
    }


    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT não suportado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformado: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Assinatura JWT inválida: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT vazio ou nulo: {}", e.getMessage());
        }
        return false;
    }


    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getChave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getChave() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
