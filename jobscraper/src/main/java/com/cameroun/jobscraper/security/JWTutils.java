package com.cameroun.jobscraper.security;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Component
@Getter
@RequiredArgsConstructor
public class JWTutils {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}") 
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, Object> redisTemplate;
    
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String username, long expirationTime) {

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date (System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey())
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }   

    public boolean validateToken(String token, UserDetails userDetails) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // --- LOGIQUE DE BLACKLISTING AVEC REDIS ---
    public Boolean isTokenInvalidated(String token) {
        if (token == null || token.trim().isEmpty()) {
             return true; 
        }
        return redisTemplate.hasKey(token);
    }
    /**
     * Invalide un token en le stockant dans Redis avec sa durée de vie restante.
     * Cela permet de s'assurer que le token ne sera plus valide avant son expiration naturelle.
     */
    public void invalidateToken(String token) {
        Date expiration = extractExpiration(token);
        long timeLeft = expiration.getTime() - System.currentTimeMillis(); 

        if (timeLeft > 0) {
            redisTemplate.opsForValue().set(token, true, timeLeft, TimeUnit.MILLISECONDS);
        }
    }
    // ------------------------------------------

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    private <T> T extractClaim(String token, java.util.function.Function<io.jsonwebtoken.Claims, T> claimsResolver) {
       try {
            final io.jsonwebtoken.Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("Token expiré");
        } catch (io.jsonwebtoken.JwtException e) {
            throw new RuntimeException("Token invalide");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction des claims : " + e.getMessage());
        }
    }

    private Claims extractAllClaims(String token) {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        };
}