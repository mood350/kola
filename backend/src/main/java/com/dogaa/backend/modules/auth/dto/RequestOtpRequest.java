package com.dogaa.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Step 1 of registration: the user gives a phone number and nothing else. */
public record RequestOtpRequest(@NotBlank @Size(max = 20) String phone) {
}
