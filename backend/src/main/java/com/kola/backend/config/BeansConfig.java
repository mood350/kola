package com.kola.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Authentication beans, kept apart from the filter-chain wiring in {@link SecurityConfig}.
 */
@Configuration
@RequiredArgsConstructor
public class BeansConfig {

    private final UserDetailsService userDetailsService;

    /**
     * BCrypt over the PIN. Strength 12 is deliberate: the PIN space is small, so the hash has
     * to be slow enough that an offline dictionary run over 10 000 candidates is not free.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Let the "no such phone number" case surface as bad credentials, so the API cannot be
        // used to find out which numbers are registered.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * A transaction that always starts fresh and never joins the caller's.
     *
     * <p>Needed wherever a failure has to survive the caller's rollback, or — as in
     * {@code TransactionService.executeIdempotent} — wherever the code must get back <em>outside</em>
     * a transaction that a constraint violation has marked rollback-only before it can read
     * anything again. An annotation cannot do this when the call comes from the same class, since
     * Spring's proxy is bypassed for calls to {@code this}.
     */
    @Bean
    public TransactionTemplate requiresNewTransaction(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
