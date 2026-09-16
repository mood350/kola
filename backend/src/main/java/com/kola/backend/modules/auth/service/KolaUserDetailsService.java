package com.kola.backend.modules.auth.service;

import com.kola.backend.modules.auth.security.KolaUserDetails;
import com.kola.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves an account from its phone number. The number reaching this point is already in E.164:
 * normalisation happens in {@code AuthenticationService} before the manager is called.
 */
@Service
@RequiredArgsConstructor
public class KolaUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        return userService.findByPhone(phone)
                .map(KolaUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + phone));
    }
}
