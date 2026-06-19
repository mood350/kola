package com.kola.backend.beneficiary;

import com.kola.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    private Beneficiary beneficiary(Long id, String alias, User owner) {
        return Beneficiary.builder()
                .id(id)
                .alias(alias)
                .phoneNumber("+22890000000")
                .countryCode("TG")
                .network(MobileNetwork.MTN_MOMO)
                .owner(owner)
                .build();
    }

    @Test
    void getMyBeneficiaries_shouldReturnOwnedBeneficiaries() {
        User user = user(1L);
        when(beneficiaryRepository.findByOwnerId(1L)).thenReturn(
                List.of(beneficiary(1L, "Alice", user), beneficiary(2L, "Bob", user))
        );

        List<BeneficiaryResponse> result = beneficiaryService.getMyBeneficiaries(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).alias()).isEqualTo("Alice");
    }

    @Test
    void getBeneficiaryById_shouldReturn_whenOwned() {
        User user = user(1L);
        Beneficiary ben = beneficiary(1L, "Charlie", user);
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(ben));

        BeneficiaryResponse result = beneficiaryService.getBeneficiaryById(user, 1L);

        assertThat(result.alias()).isEqualTo("Charlie");
    }

    @Test
    void getBeneficiaryById_shouldThrow_whenNotOwned() {
        User owner = user(2L);
        User requester = user(1L);
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(beneficiary(1L, "Alice", owner)));

        assertThatThrownBy(() -> beneficiaryService.getBeneficiaryById(requester, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createBeneficiary_shouldSave_whenNotDuplicate() {
        User user = user(1L);
        when(beneficiaryRepository.existsByOwnerIdAndPhoneNumberAndNetwork(1L, "+22890000000", MobileNetwork.MTN_MOMO))
                .thenReturn(false);
        when(beneficiaryRepository.save(any())).thenAnswer(i -> i.<Beneficiary>getArgument(0));

        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest(
                "Alice", "+22890000000", "TG", MobileNetwork.MTN_MOMO
        );
        BeneficiaryResponse result = beneficiaryService.createBeneficiary(user, request);

        assertThat(result.alias()).isEqualTo("Alice");
        assertThat(result.phoneNumber()).isEqualTo("+22890000000");
    }

    @Test
    void createBeneficiary_shouldThrow_whenDuplicate() {
        User user = user(1L);
        when(beneficiaryRepository.existsByOwnerIdAndPhoneNumberAndNetwork(1L, "+22890000000", MobileNetwork.MTN_MOMO))
                .thenReturn(true);

        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest(
                "Alice", "+22890000000", "TG", MobileNetwork.MTN_MOMO
        );

        assertThatThrownBy(() -> beneficiaryService.createBeneficiary(user, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteBeneficiary_shouldDelete_whenOwned() {
        User user = user(1L);
        Beneficiary ben = beneficiary(1L, "ToDelete", user);
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(ben));

        beneficiaryService.deleteBeneficiary(user, 1L);

        verify(beneficiaryRepository).delete(ben);
    }

    @Test
    void deleteBeneficiary_shouldThrow_whenNotOwned() {
        User owner = user(2L);
        User requester = user(1L);
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(beneficiary(1L, "Alice", owner)));

        assertThatThrownBy(() -> beneficiaryService.deleteBeneficiary(requester, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteBeneficiary_shouldThrow_whenNotFound() {
        when(beneficiaryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.deleteBeneficiary(user(1L), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
