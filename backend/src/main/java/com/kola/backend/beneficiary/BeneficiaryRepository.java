package com.kola.backend.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    // Récupérer tous les bénéficiaires d'un utilisateur
    List<Beneficiary> findByOwnerId(Long ownerId);

    // Vérifier si un bénéficiaire existe déjà pour cet user + numéro + réseau
    boolean existsByOwnerIdAndPhoneNumberAndNetwork(Long ownerId, String phoneNumber, MobileNetwork network);
}
