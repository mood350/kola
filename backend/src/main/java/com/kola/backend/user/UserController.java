package com.kola.backend.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User currentUser) {
        return UserResponse.fromEntity(currentUser);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(currentUser, request));
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid UpdateAvatarRequest request
    ) {
        return ResponseEntity.ok(userService.updateAvatar(currentUser, request.avatar()));
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser, request);
    }

    public record UpdateAvatarRequest(
            @NotBlank(message = "L'avatar est obligatoire")
            String avatar
    ) {
    }
}
