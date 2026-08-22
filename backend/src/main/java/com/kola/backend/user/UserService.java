package com.kola.backend.user;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Le principal fourni par Spring Security est détaché du contexte de
     * persistance : on recharge l'entité pour que les modifications soient
     * bien suivies et flushées.
     */
    private User reload(User currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public UserResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = reload(currentUser);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhoneNumber(request.phoneNumber().trim());

        // Null = l'appelant ne touche pas à l'avatar (formulaire d'infos vs.
        // sélecteur d'avatar sont deux écrans distincts côté mobile).
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateAvatar(User currentUser, String avatar) {
        User user = reload(currentUser);
        user.setAvatar(avatar);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        User user = reload(currentUser);

        // Volontairement IllegalArgumentException (→ 400) et non
        // BadCredentialsException : cette dernière est mappée en 401 par
        // GlobalExceptionHandler, ce que le mobile interpréterait comme une
        // session expirée et provoquerait une déconnexion intempestive.
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
