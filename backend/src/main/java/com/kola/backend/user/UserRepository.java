package com.kola.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * `JpaSpecificationExecutor` sert la recherche filtrée de la console admin
 * (AdminUserService).
 *
 * POURQUOI DES SPECIFICATIONS ET NON UNE REQUÊTE JPQL À PARAMÈTRES OPTIONNELS.
 * La forme habituelle — « WHERE (:kycLevel IS NULL OR u.kycLevel = :kycLevel) »
 * — oblige à lier un paramètre à `null`. PostgreSQL ne peut alors pas inférer
 * son type et rejette la requête (« could not determine data type of
 * parameter »), tout particulièrement sur un enum. Une Specification ne
 * construit que les prédicats des filtres réellement fournis : le paramètre
 * absent n'existe pas dans le SQL généré, il n'y a donc plus rien à typer.
 */
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {

    // CORRECTION : Ajout de @Query explicite pour éviter tout bug d'inférence Spring Data
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Le numéro de téléphone est unique sur User : il sert de clé pour savoir
     * si le bénéficiaire d'un transfert est en réalité un compte Kola, auquel
     * cas l'argent doit rester dans le système et créditer son wallet
     * (cf. TransactionService.transfer).
     */
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    List<User> findByRolesRoleName(String roleName);

    long countByEnabledTrueAndAccountLockedFalse();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(u.countryCode, 'UN'), COUNT(u) FROM User u GROUP BY COALESCE(u.countryCode, 'UN') ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByCountry();

    @Query("SELECT u.kycLevel, COUNT(u) FROM User u GROUP BY u.kycLevel ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByKycLevel();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}