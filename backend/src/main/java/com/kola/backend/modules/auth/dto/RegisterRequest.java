package com.kola.backend.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Step 3: identity and PIN. There is deliberately no phone field — the number comes from
 * {@code verificationToken}, so an account can only ever be created on a verified number.
 */
public record RegisterRequest(
        @NotBlank String verificationToken,

        @NotBlank @Size(min = 2, max = 80) String firstName,
        @NotBlank @Size(min = 2, max = 80) String lastName,

        @Email @Size(max = 160) String email,

        @NotNull @Past LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "[0-9]{4,6}", message = "PIN must be 4 to 6 digits")
        String pin,

        @NotBlank String confirmPin,

        @Size(max = 120) String address,
        @Size(max = 80) String city,
        @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO alpha-2 country code") String country,

        @NotNull(message = "You must accept the privacy policy to create an account")
        @AssertTrue(message = "You must accept the privacy policy to create an account")
        Boolean acceptedPrivacyPolicy) {
}
