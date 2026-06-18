package com.kola.backend.beneficiary;

import com.kola.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getMyBeneficiaries(User currentUser) {
        return beneficiaryRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(BeneficiaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(User currentUser, Long beneficiaryId) {
        return BeneficiaryResponse.fromEntity(findOwnedBeneficiaryOrThrow(currentUser, beneficiaryId));
    }

    @Transactional
    public BeneficiaryResponse createBeneficiary(User currentUser, CreateBeneficiaryRequest request) {
        if (beneficiaryRepository.existsByOwnerIdAndPhoneNumberAndNetwork(
                currentUser.getId(), request.phoneNumber(), request.network())) {
            throw new IllegalStateException(
                    "Ce bénéficiaire existe déjà (même numéro et même réseau)"
            );
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .alias(request.alias())
                .phoneNumber(request.phoneNumber())
                .countryCode(request.countryCode())
                .network(request.network())
                .owner(currentUser)
                .build();

        beneficiary = beneficiaryRepository.save(beneficiary);
        return BeneficiaryResponse.fromEntity(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(User currentUser, Long beneficiaryId) {
        Beneficiary beneficiary = findOwnedBeneficiaryOrThrow(currentUser, beneficiaryId);
        beneficiaryRepository.delete(beneficiary);
    }

    private Beneficiary findOwnedBeneficiaryOrThrow(User currentUser, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new EntityNotFoundException("Bénéficiaire introuvable"));

        if (!beneficiary.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce bénéficiaire ne vous appartient pas");
        }
        return beneficiary;
    }
}
