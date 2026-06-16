package com.kola.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;


    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                // Claims supplémentaires (rôles, id, etc.)
                .claims(extraClaims)
                // Subject = identifiant principal = email de l'user
                .subject(userDetails.getUsername())
                // Date d'émission du token
                .issuedAt(new Date(System.currentTimeMillis()))
                // Date d'expiration
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                // Signature avec notre clé secrète (algorithme HS256)
                .signWith(getSigningKey())
                // Construction finale → génère la chaîne JWT
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait la date d'expiration du token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Méthode générique pour extraire n'importe quel claim du token.

     * Utilise une Function pour spécifier quel claim extraire.
     * Exemple : extractClaim(token, Claims::getSubject)
     *           extractClaim(token, Claims::getExpiration)
     *
     * @param token          Le JWT
     * @param claimsResolver La fonction d'extraction du claim voulu
     * @return La valeur du claim extrait
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Décode et retourne tous les claims du token.
     * C'est ici que la signature est vérifiée :
     * Si le token a été falsifié → JwtException est levée automatiquement.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                // On fournit notre clé pour vérifier la signature
                .verifyWith(getSigningKey())
                .build()
                // Décode le token et vérifie la signature
                .parseSignedClaims(token)
                // Retourne le payload (les claims)
                .getPayload();
    }
    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshExpiration;

    // Génère un refresh token (même logique, durée plus longue)
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
