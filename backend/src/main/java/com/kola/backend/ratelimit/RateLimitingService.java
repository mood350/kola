package com.kola.backend.ratelimit;

import com.kola.backend.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                RateLimitingService.java                     ║
 * ║   Applique des limites de requêtes par clé (IP, email, ...) ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * IMPLÉMENTATION ACTUELLE : in-memory (ConcurrentHashMap).
 *  → Convient pour une instance unique de l'application.
 *  → Si l'app est déployée sur plusieurs instances (scaling horizontal),
 *    chaque instance aura ses propres compteurs : il faudra alors migrer
 *    vers un store partagé (Redis, déjà présent dans le projet via
 *    spring-boot-starter-data-redis-reactive) en utilisant le module
 *    bucket4j-redis. La structure de cette classe (une méthode resolveBucket
 *    par clé) a été pensée pour rendre cette migration triviale : il suffira
 *    de remplacer le ConcurrentHashMap par un ProxyManager Redis.
 *
 * UTILISATION :
 *   rateLimitingService.consume(RateLimitPolicy.LOGIN, ip + ":" + email);
 *   → lève TooManyRequestsException si le quota est dépassé.
 */
@Service
public class RateLimitingService {

    // clé = "POLICY:identifiant" (ex: "LOGIN:127.0.0.1:user@mail.com")
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Consomme un jeton du bucket correspondant à la politique + clé donnée.
     * Lève TooManyRequestsException si le quota est épuisé.
     *
     * @param policy la politique de rate limiting à appliquer
     * @param key    l'identifiant unique sur lequel limiter (IP, email, IP+email...)
     */
    public void consume(RateLimitPolicy policy, String key) {
        Bucket bucket = resolveBucket(policy, key);
        if (!bucket.tryConsume(1)) {
            throw new TooManyRequestsException(
                    "Trop de tentatives. Veuillez réessayer plus tard."
            );
        }
    }

    private Bucket resolveBucket(RateLimitPolicy policy, String key) {
        String bucketKey = policy.name() + ":" + key;
        return buckets.computeIfAbsent(bucketKey, k ->
                Bucket.builder()
                        .addLimit(policy.toBandwidth())
                        .build()
        );
    }
}
