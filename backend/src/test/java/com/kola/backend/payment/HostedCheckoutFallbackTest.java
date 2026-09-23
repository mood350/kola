package com.kola.backend.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le choix entre prélèvement direct et page hébergée.
 *
 * ═══ CE QUE CES TESTS PROTÈGENT ═══
 *
 * Un compte marchand sans autorisation de prélèvement sans redirection reçoit
 * un 400 « Opération non autorisée » sur tous les opérateurs. Le repli — la
 * page de paiement de FedaPay — est donc le chemin par défaut, et le seul qui
 * fonctionne aujourd'hui. Ces tests fixent que le drapeau commande bien les
 * deux chemins, et surtout qu'aucun des deux ne rend une écriture impayable :
 * un dépôt en attente sans moyen de le régler est un client qui a perdu son
 * argent de vue.
 */
class HostedCheckoutFallbackTest {

    private static final String PAGE = "https://sandbox-process.fedapay.com/jeton";

    private final FedaPayClient client = mock(FedaPayClient.class);
    private final FedaPayProperties properties = new FedaPayProperties();
    private final FedaPayPaymentProvider provider = new FedaPayPaymentProvider(client, properties);

    private static PaymentProvider.CollectCommand commande() {
        return new PaymentProvider.CollectCommand(
                new BigDecimal("1000"), "XOF", MobileMoneyMode.MTN_BENIN,
                "+22961000001", "BJ", "KLA-2026-000001", "Rechargement Kola",
                "client@example.com", "Ada", "Lovelace");
    }

    @Test
    @DisplayName("Par défaut le client est envoyé sur la page hébergée, sans rien pousser sur son téléphone")
    void replisurLaPageHebergee() {
        when(client.createTransaction(any()))
                .thenReturn(new FedaPayClient.CreatedTransaction("494450", PAGE));

        PaymentProvider.CollectInitiation initiation = provider.initiateCollect(commande());

        assertThat(initiation.providerTransactionId()).isEqualTo("494450");
        assertThat(initiation.paymentUrl()).isEqualTo(PAGE);

        /* L'appel qui échoue en 400 sur un compte non autorisé ne doit pas
           même être tenté : c'est tout l'objet du repli. */
        verify(client, never()).sendNow(anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Une création sans payment_url va chercher le lien auprès du jeton")
    void repliDeSecoursParLeJeton() {
        when(client.createTransaction(any()))
                .thenReturn(new FedaPayClient.CreatedTransaction("494450", null));
        when(client.generateToken("494450"))
                .thenReturn(new FedaPayClient.PaymentToken("jwt", PAGE));

        assertThat(provider.initiateCollect(commande()).paymentUrl()).isEqualTo(PAGE);
    }

    @Test
    @DisplayName("Sans aucune page de paiement, on refuse plutôt que de rendre une écriture impayable")
    void aucuneUrlExploitable() {
        when(client.createTransaction(any()))
                .thenReturn(new FedaPayClient.CreatedTransaction("494450", null));
        when(client.generateToken("494450"))
                .thenReturn(new FedaPayClient.PaymentToken("jwt", null));

        assertThatThrownBy(() -> provider.initiateCollect(commande()))
                .isInstanceOf(PaymentProviderException.class);
    }

    @Test
    @DisplayName("Drapeau levé : la demande repart sur le téléphone, et sans URL à ouvrir")
    void prelevementDirectQuandIlEstAutorise() {
        properties.setDirectCharge(true);
        when(client.createTransaction(any()))
                .thenReturn(new FedaPayClient.CreatedTransaction("494450", PAGE));
        when(client.generateToken("494450"))
                .thenReturn(new FedaPayClient.PaymentToken("jwt", PAGE));
        when(client.sendNow("jwt", MobileMoneyMode.MTN_BENIN, "+22961000001", "BJ"))
                .thenReturn("pending");

        PaymentProvider.CollectInitiation initiation = provider.initiateCollect(commande());

        assertThat(initiation.status()).isEqualTo("pending");
        /* Nulle, et pas « celle qu'on avait sous la main » : ouvrir une page
           alors que la demande est déjà sur le téléphone ferait payer deux fois. */
        assertThat(initiation.paymentUrl()).isNull();
        verify(client).sendNow("jwt", MobileMoneyMode.MTN_BENIN, "+22961000001", "BJ");
    }
}
