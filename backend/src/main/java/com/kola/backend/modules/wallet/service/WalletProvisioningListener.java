package com.kola.backend.modules.wallet.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.modules.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives every new account its current and savings wallets, so the two-account model holds from
 * the first second rather than from the first time someone remembers to call an endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletProvisioningListener {

    private final WalletService walletService;

    @Value("${app.wallet.default-currency:XOF}")
    private Currency defaultCurrency;

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        walletService.provision(event.userId(), defaultCurrency, WalletType.CURRENT);
        walletService.provision(event.userId(), defaultCurrency, WalletType.SAVINGS);
        log.info("Provisioned current and savings {} wallets for user {}",
                defaultCurrency, event.userId());
    }
}
