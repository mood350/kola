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

    /**
     * Claim distinguant un token d'accès d'un token de rafraîchissement.
     *
     * Les deux étaient jusqu'ici signés avec la même clé, avec le même sujet
     * et sans rien qui permette de les différencier : ils étaient donc
     * strictement interchangeables. Concrètement, un refresh token volé
     * s'utilisait directement en `Authorization: Bearer` comme un token
     * d'accès — avec sa durée de vie de 7 jours au lieu de 24 h — et un
     * access token était accepté par /api/auth/refresh-token pour battre
     * monnaie indéfiniment. Le type est désormais vérifié à chaque usage.
     */
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                // Claims supplémentaires (rôles, id, etc.)
                .claims(extraClaims)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
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
        return isValidForType(token, userDetails, TYPE_ACCESS);
    }

    /**
     * Un token sans claim `type` est refusé : ce sont les jetons émis avant
     * l'introduction du claim, et rien ne permet de savoir s'ils étaient des
     * tokens d'accès ou de rafraîchissement. Les accepter par défaut
     * reviendrait à garder la faille ouverte le temps de leur expiration.
     * Effet de bord assumé : les sessions en cours doivent se reconnecter.
     */
    private boolean isValidForType(String token, UserDetails userDetails, String expectedType) {
        final Claims claims = extractAllClaims(token);
        return expectedType.equals(claims.get(CLAIM_TYPE, String.class))
                && userDetails.getUsername().equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
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
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isValidForType(token, userDetails, TYPE_REFRESH);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
