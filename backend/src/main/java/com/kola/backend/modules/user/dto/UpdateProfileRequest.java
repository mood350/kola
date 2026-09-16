package com.kola.backend.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Every field is optional: only the non-null ones are applied. */
public record UpdateProfileRequest(@Size(min = 2, max = 80) String firstName,
                                   @Size(min = 2, max = 80) String lastName,
                                   @Email @Size(max = 160) String email,
                                   @Past LocalDate dateOfBirth,
                                   @Size(max = 120) String address,
                                   @Size(max = 80) String city,
                                   @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO alpha-2 country code")
                                   String country) {
}
