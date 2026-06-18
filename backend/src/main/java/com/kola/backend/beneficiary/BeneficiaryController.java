package com.kola.backend.beneficiary;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getMyBeneficiaries(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(beneficiaryService.getMyBeneficiaries(currentUser));
    }

    @GetMapping("/{beneficiaryId}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long beneficiaryId
    ) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(currentUser, beneficiaryId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse createBeneficiary(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid CreateBeneficiaryRequest request
    ) {
        return beneficiaryService.createBeneficiary(currentUser, request);
    }

    @DeleteMapping("/{beneficiaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long beneficiaryId
    ) {
        beneficiaryService.deleteBeneficiary(currentUser, beneficiaryId);
    }
}
