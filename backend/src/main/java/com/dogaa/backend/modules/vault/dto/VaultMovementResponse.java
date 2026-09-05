package com.dogaa.backend.modules.vault.dto;

import com.dogaa.backend.modules.wallet.dto.WalletResponse;

/**
 * Ce que renvoie un mouvement d'argent sur un coffre : les deux soldes qui viennent de changer.
 *
 * <p>Alimenter un coffre ne fait pas sortir d'argent du portefeuille, cela déplace une somme du
 * côté dépensable vers le côté bloqué. Le client doit donc rafraîchir <b>les deux</b> comptes après
 * l'opération. Ne lui rendre que le coffre l'obligeait à un second appel {@code GET /wallets} :
 * tant qu'il ne le faisait pas, il continuait d'afficher un solde courant que l'utilisateur venait
 * de dépenser.
 *
 * <p>{@code wallet} est le portefeuille qui finance le coffre — le compte courant de la devise du
 * coffre. Son {@code availableBalance} a baissé du montant versé et son {@code lockedBalance} a
 * augmenté d'autant ; {@code totalBalance} est inchangé, et c'est voulu.
 */
public record VaultMovementResponse(VaultResponse vault, WalletResponse wallet) {
}
