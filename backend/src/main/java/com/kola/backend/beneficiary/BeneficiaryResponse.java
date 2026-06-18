package com.kola.backend.beneficiary;

public record BeneficiaryResponse(
        Long id,
        String alias,
        String phoneNumber,
        String countryCode,
        MobileNetwork network
) {
    public static BeneficiaryResponse fromEntity(Beneficiary b) {
        return new BeneficiaryResponse(
                b.getId(),
                b.getAlias(),
                b.getPhoneNumber(),
                b.getCountryCode(),
                b.getNetwork()
        );
    }
}
