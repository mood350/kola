package com.kola.backend.payment;

import com.kola.backend.transaction.TransactionService;
import com.kola.backend.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * L'ordre des deux gestes : demander d'abord, débiter ensuite.
 *
 * ═══ POURQUOI CE TEST EXISTE ═══
 *
 * Chez FedaPay, les versements sont une autorisation accordée compte par
 * compte ; sans elle, {@code POST /v1/payouts} répond 403 quoi qu'on envoie.
 * Si la question n'est posée qu'au moment de l'appel, chaque tentative de
 * retrait ouvre une écriture, débite le portefeuille, se fait refuser, puis
 * recrédite. Le résultat est correct au franc près, mais l'utilisateur voit son
 * solde descendre et remonter, et le journal se remplit d'erreurs pour une
 * fonction simplement pas encore ouverte.
 *
 * Ce test fixe donc l'ordre : quand le canal est fermé, RIEN n'est écrit au
 * grand livre. Il pince aussi l'invariant inverse — un refus survenu APRÈS le
 * débit doit toujours recréditer.
 */
class WithdrawalRefusedBeforeDebitTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final PaymentProvider paymentProvider = mock(PaymentProvider.class);
    private final MobileMoneyWithdrawalService service =
            new MobileMoneyWithdrawalService(transactionService, paymentProvider);

    private static MobileMoneyWithdrawalRequest demande() {
        return new MobileMoneyWithdrawalRequest(
                1L, new BigDecimal("10000"), MobileMoneyMode.MOOV_TOGO,
                "+22890000002", "cle-idempotence");
    }

    @Test
    @DisplayName("Versements fermés : le portefeuille n'est pas même touché")
    void aucuneEcritureQuandLesVersementsSontFermes() {
        when(paymentProvider.payoutsAvailable()).thenReturn(false);

        assertThatThrownBy(() -> service.withdraw(mock(User.class), demande()))
                .isInstanceOf(PaymentMethodUnavailableException.class);

        /* Aucun appel au grand livre : pas d'écriture ouverte, donc pas de
           débit, donc rien à recréditer. C'est la propriété du test. */
        verifyNoInteractions(transactionService);
        verify(paymentProvider, never()).initiatePayout(any());
    }

    @Test
    @DisplayName("Versements ouverts mais refus du prestataire : le débit est rendu")
    void unRefusApresLeDebitRecrediteToujours() {
        when(paymentProvider.payoutsAvailable()).thenReturn(true);

        com.kola.backend.transaction.Transaction pending =
                com.kola.backend.transaction.Transaction.builder()
                        .id(42L)
                        .reference("KLA-2026-000042")
                        .status(com.kola.backend.transaction.TransactionStatus.PENDING)
                        .amount(new BigDecimal("10000"))
                        .currency("XOF")
                        .build();

        when(transactionService.openMobileMoneyWithdrawal(any(), any())).thenReturn(pending);
        when(paymentProvider.initiatePayout(any()))
                .thenThrow(new PaymentProviderException("refus du prestataire"));

        User user = mock(User.class);
        when(user.getEmail()).thenReturn("client@kola.test");

        assertThatThrownBy(() -> service.withdraw(user, demande()))
                .isInstanceOf(PaymentProviderException.class);

        /* Une fois l'écriture ouverte, le portefeuille EST débité : le seul
           dénouement acceptable d'un échec est le remboursement. */
        verify(transactionService).abandonPendingWithdrawal(42L);
        verify(transactionService, never())
                .attachProviderTransaction(anyLong(), anyString(), anyString(), anyString());
    }
}
