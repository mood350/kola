package com.kola.backend.merchant;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;

    /** Consultation avant paiement : confirme au payeur qui il va payer. */
    @GetMapping("/{code}")
    public MerchantResponse getByCode(@PathVariable String code) {
        Merchant merchant = merchantRepository.findByMerchantCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Marchand introuvable"));
        return MerchantResponse.fromEntity(merchant);
    }
}
