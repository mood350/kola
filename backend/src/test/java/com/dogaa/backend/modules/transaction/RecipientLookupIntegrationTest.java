package com.dogaa.backend.modules.transaction;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.TooManyRequestsException;
import com.dogaa.backend.modules.transaction.dto.RecipientLookupResponse;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.RecipientDirectory;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirming who a number belongs to before sending money.
 *
 * <p>A P2P transfer cannot be undone without opening a dispute, so this lookup is the last chance
 * to catch a mistyped digit — and, being a phone-to-name endpoint, an enumeration surface that has
 * to stay bounded.
 */
@SpringBootTest
@ActiveProfiles("test")
class RecipientLookupIntegrationTest {

    @Autowired private RecipientDirectory recipientDirectory;
    @Autowired private TransactionService transactionService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private UUID senderId;
    private String recipientPhone;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    private User newUser(String first, String last, String phone) {
        return userRepository.save(User.builder()
                .firstName(first).lastName(last).phone(phone)
                .pinHash("x").dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true).kycTier(KycTier.TIER_3)
                .privacyPolicyAcceptedAt(Instant.now()).build());
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User sender = newUser("Kossi", "Adjo", "+22890111222");
        senderId = sender.getId();
        Wallet wallet = walletService.provision(senderId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(wallet.getId(), xof("500000"));

        User recipient = newUser("Ama", "Kossi", "+22890333444");
        recipientPhone = recipient.getPhone();
        walletService.provision(recipient.getId(), Currency.XOF, WalletType.CURRENT);
    }

    // --- the confirmation screen -------------------------------------------

    @Test
    void aKnownNumberComesBackWithItsHoldersName() {
        RecipientLookupResponse found = recipientDirectory.lookup(senderId, recipientPhone);

        assertThat(found.registered()).isTrue();
        assertThat(found.name()).isEqualTo("Ama Kossi");
        assertThat(found.self()).isFalse();
    }

    /**
     * The number the user typed and the number that gets paid must be the same account. Returning
     * the normalised form is what guarantees the confirmation screen and the transfer agree.
     */
    @Test
    void theNumberComesBackNormalisedWhateverWasTyped() {
        assertThat(recipientDirectory.lookup(senderId, "90 33 34 44").phone())
                .isEqualTo(recipientPhone);
        assertThat(recipientDirectory.lookup(senderId, "0022890333444").phone())
                .isEqualTo(recipientPhone);
    }

    /**
     * Not a 404: an unknown number is still payable, it leaves through Mobile Money. The client
     * needs to know there is no name to confirm, which differs from the lookup having failed.
     */
    @Test
    void anUnknownNumberIsAnAnswerAndNotAnError() {
        RecipientLookupResponse unknown = recipientDirectory.lookup(senderId, "+22899999999");

        assertThat(unknown.registered()).isFalse();
        assertThat(unknown.name()).isNull();
        assertThat(unknown.phone()).isEqualTo("+22899999999");
    }

    /** Saying so here beats letting the transfer fail after the user has confirmed it. */
    @Test
    void lookingUpOnesOwnNumberSaysSo() {
        assertThat(recipientDirectory.lookup(senderId, "+22890111222").self()).isTrue();
    }

    /**
     * A screen must be able to say "ce compte n'affiche pas de nom" rather than show a stand-in
     * for a check that did not happen.
     *
     * <p>Registration makes both names mandatory and the columns are NOT NULL, so this is
     * defensive rather than reachable through the API — blank is the only shape a name can take
     * that the database still accepts.
     */
    @Test
    void anAccountWithoutANameReturnsNullRatherThanAPlaceholder() {
        User nameless = newUser(" ", " ", "+22890555666");
        walletService.provision(nameless.getId(), Currency.XOF, WalletType.CURRENT);

        RecipientLookupResponse found = recipientDirectory.lookup(senderId, "+22890555666");

        assertThat(found.registered()).isTrue();
        assertThat(found.name()).isNull();
    }

    @Test
    void anEmptyNumberIsRejected() {
        assertThatThrownBy(() -> recipientDirectory.lookup(senderId, "  "))
                .isInstanceOf(BadRequestException.class);
    }

    // --- the name reaches the history --------------------------------------

    /**
     * Stamped at transfer time rather than resolved when the history is read: a line must keep
     * naming who was paid even after that person renames their account.
     */
    @Test
    void theTransferRecordsTheRecipientsName() {
        Transaction tx = transactionService.transfer(senderId,
                new TransferRequest(Currency.XOF, xof("10000"), recipientPhone, "loyer"));

        assertThat(tx.getCounterpartyName()).isEqualTo("Ama Kossi");
        assertThat(tx.getCounterparty()).isEqualTo(recipientPhone);
    }

    @Test
    void aRenameDoesNotRewriteAnOlderTransaction() {
        Transaction tx = transactionService.transfer(senderId,
                new TransferRequest(Currency.XOF, xof("10000"), recipientPhone, "loyer"));

        User recipient = userRepository.findByPhone(recipientPhone).orElseThrow();
        recipient.setFirstName("Amavi");
        userRepository.save(recipient);

        assertThat(tx.getCounterpartyName()).isEqualTo("Ama Kossi");
    }

    /** An external payout has no Dogaa account behind it, so there is no name to record. */
    @Test
    void anExternalPayoutHasNoName() {
        Transaction tx = transactionService.transfer(senderId,
                new TransferRequest(Currency.XOF, xof("10000"), "+22899999999", "hors Dogaa"));

        assertThat(tx.getCounterpartyName()).isNull();
    }

    // --- the enumeration surface -------------------------------------------

    /**
     * Walked in a loop, this endpoint is a way to harvest the names behind a numbering plan. The
     * budget is generous for a person choosing a recipient and useless for a crawler.
     */
    @Test
    void thelookupBudgetRunsOutBeforeANumberingPlanCanBeWalked() {
        UUID crawler = newUser("Crawler", "Bot", "+22890777888").getId();

        assertThatThrownBy(() -> {
            for (int i = 0; i < 500; i++) {
                recipientDirectory.lookup(crawler, "+2289000" + String.format("%04d", i));
            }
        }).isInstanceOf(TooManyRequestsException.class);
    }
}
