package com.dogaa.backend.modules.auth.security;

import com.dogaa.backend.common.enums.UserStatus;
import com.dogaa.backend.modules.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts a Dogaa {@link User} to Spring Security. The "username" is the phone number and the
 * "password" is the PIN hash, so {@code DaoAuthenticationProvider} does the PIN comparison for us.
 *
 * <p>Mapping the lockout and the account status onto {@code isAccountNonLocked} / {@code isEnabled}
 * means the provider enforces them before it even looks at the PIN.
 */
@Getter
@RequiredArgsConstructor
public class DogaaUserDetails implements UserDetails {

    private final transient User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public String getPassword() {
        return user.getPinHash();
    }

    @Override
    public String getUsername() {
        return user.getPhone();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    public CurrentUser toCurrentUser() {
        return new CurrentUser(user.getId(), user.getPhone(), user.getRole());
    }
}
