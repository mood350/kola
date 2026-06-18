package com.kola.backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  RateLimitPolicy.java                       ║
 * ║   Définit les règles de rate limiting par type d'action     ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Chaque politique définit :
 *  - capacity   : nombre de requêtes autorisées dans la fenêtre
 *  - refillRate : nombre de requêtes ré-autorisées par unité de temps
 *  - duration   : taille de la fenêtre de temps
 *
 * Les valeurs sont volontairement strictes pour les endpoints sensibles
 * (login, register, forgot-password) car ce sont les cibles classiques
 * de brute-force, credential stuffing et spam d'emails.
 */
public enum RateLimitPolicy {

    // 5 tentatives de connexion / 15 minutes / clé = email + IP
    LOGIN(5, Duration.ofMinutes(15)),

    // 3 inscriptions / heure / clé = IP (évite la création massive de comptes)
    REGISTER(3, Duration.ofHours(1)),

    // 3 demandes de reset / heure / clé = email (évite le spam d'emails)
    FORGOT_PASSWORD(3, Duration.ofHours(1)),

    // 5 tentatives de reset / heure / clé = IP (évite le bruteforce de l'OTP)
    RESET_PASSWORD(5, Duration.ofHours(1)),

    // 10 confirmations de compte / heure / clé = IP (évite le bruteforce de l'OTP d'activation)
    CONFIRM_ACCOUNT(10, Duration.ofHours(1)),

    // 20 rafraîchissements de token / heure / clé = IP
    REFRESH_TOKEN(20, Duration.ofHours(1));

    private final int capacity;
    private final Duration duration;

    RateLimitPolicy(int capacity, Duration duration) {
        this.capacity = capacity;
        this.duration = duration;
    }

    /**
     * Construit la "bandwidth" Bucket4j correspondante : un bucket qui se
     * remplit intégralement (greedy refill) toutes les `duration`.
     */
    public Bandwidth toBandwidth() {
        return Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
    }
}
