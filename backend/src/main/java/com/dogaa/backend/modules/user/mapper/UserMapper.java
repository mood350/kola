package com.dogaa.backend.modules.user.mapper;

import com.dogaa.backend.modules.user.dto.UserResponse;
import com.dogaa.backend.modules.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.isPhoneVerified(),
                user.isEmailVerified(),
                user.getKycTier(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
