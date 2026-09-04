package com.dogaa.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 80) String firstName,
        @NotBlank @Size(min = 2, max = 80) String lastName,

        /** Any local or international format; normalised to E.164 before storage. */
        @NotBlank @Size(max = 20) String phone,

        @Email @Size(max = 160) String email,

        @NotNull @Past LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "[0-9]{4,6}", message = "PIN must be 4 to 6 digits")
        String pin,

        @NotBlank String confirmPin,

        @Size(max = 120) String address,
        @Size(max = 80) String city,
        @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO alpha-2 country code") String country) {
}
