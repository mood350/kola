package com.dogaa.backend.modules.qr;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.config.QrProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.qr.dto.CreatePaymentRequestRequest;
import com.dogaa.backend.modules.qr.dto.QrCodeResponse;
import com.dogaa.backend.modules.qr.dto.QrPaymentRequest;
import com.dogaa.backend.modules.qr.dto.ScannedQrResponse;
import com.dogaa.backend.modules.qr.entity.QrCode;
import com.dogaa.backend.modules.qr.entity.QrCodeStatus;
import com.dogaa.backend.modules.qr.entity.QrCodeType;
import com.dogaa.backend.modules.qr.mapper.QrCodeMapper;
import com.dogaa.backend.modules.qr.repository.QrCodeRepository;
import com.dogaa.backend.modules.qr.service.QrCodeService;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QrCodeServiceTest {

    @Mock private QrCodeRepository repository;
    @Mock private UserService userService;
    @Mock private TransactionService transactionService;

    private final QrProperties properties = new QrProperties();
    private QrCodeService service;

    private final UUID owner = UUID.randomUUID();
    private final UUID payer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QrCodeService(properties, repository, new QrCodeMapper(properties),
                userService, transactionService);

        User beneficiary = new User();
        beneficiary.setId(owner);
        beneficiary.setFirstName("Ama");
        beneficiary.setLastName("Kossi");
        beneficiary.setPhone("+22890123456");
        when(userService.getById(owner)).thenReturn(beneficiary);

        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.save(any(QrCode.class))).thenAnswer(call -> {
            QrCode saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        Transaction done = new Transaction();
        done.setReference("TRX-1");
        when(transactionService.transfer(any(), any())).thenReturn(done);
    }

    private QrCode staticCode() {
        QrCode qr = new QrCode();
        qr.setId(UUID.randomUUID());
        qr.setCode("STATIC-1");
        qr.setOwnerId(owner);
        qr.setType(QrCodeType.STATIC);
        qr.setStatus(QrCodeStatus.ACTIVE);
        return qr;
    }

    private QrCode paymentRequest(BigDecimal amount) {
        QrCode qr = new QrCode();
        qr.setId(UUID.randomUUID());
        qr.setCode("REQ-1");
        qr.setOwnerId(owner);
        qr.setType(QrCodeType.PAYMENT_REQUEST);
        qr.setStatus(QrCodeStatus.ACTIVE);
        qr.setAmount(amount);
        qr.setCurrency(Currency.XOF);
        qr.setLabel("Table 4");
        qr.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return qr;
    }

    // --- what the code carries --------------------------------------------

    /**
     * The point of the whole design: a QR gets printed and forwarded, so it must not hand out the
     * owner's phone number. A random reference can be revoked; a number cannot be taken back.
     */
    @Test
    void theEncodedPayloadCarriesAReferenceAndNotThePhoneNumber() {
        when(repository.findFirstByOwnerIdAndTypeAndStatus(owner, QrCodeType.STATIC,
                QrCodeStatus.ACTIVE)).thenReturn(Optional.empty());

        QrCodeResponse response = service.myStaticCode(owner);

        assertThat(response.payload()).startsWith(properties.getPayloadBaseUrl());
        assertThat(response.payload()).doesNotContain("22890123456");
        assertThat(response.code()).doesNotContain("22890123456");
    }

    @Test
    void thePermanentCodeIsCreatedOnceAndReused() {
        QrCode existing = staticCode();
        when(repository.findFirstByOwnerIdAndTypeAndStatus(owner, QrCodeType.STATIC,
                QrCodeStatus.ACTIVE)).thenReturn(Optional.of(existing));

        assertThat(service.myStaticCode(owner).code()).isEqualTo("STATIC-1");
        verify(repository, never()).save(any(QrCode.class));
    }

    /** Rotating exists for a code that ended up somewhere unintended: the old one must stop working. */
    @Test
    void rotatingRevokesTheOldCode() {
        QrCode existing = staticCode();
        when(repository.findFirstByOwnerIdAndTypeAndStatus(owner, QrCodeType.STATIC,
                QrCodeStatus.ACTIVE)).thenReturn(Optional.of(existing));

        QrCodeResponse fresh = service.rotateStaticCode(owner);

        assertThat(existing.getStatus()).isEqualTo(QrCodeStatus.REVOKED);
        assertThat(fresh.code()).isNotEqualTo("STATIC-1");
    }

    // --- scanning ---------------------------------------------------------

    /** The payer must recognise who they are paying, without the number being handed over. */
    @Test
    void scanningNamesTheBeneficiaryButMasksTheirNumber() {
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(paymentRequest(new BigDecimal("2500"))));

        ScannedQrResponse scanned = service.scan(payer, "REQ-1");

        assertThat(scanned.payable()).isTrue();
        assertThat(scanned.recipientName()).isEqualTo("Ama Kossi");
        assertThat(scanned.recipientPhoneMasked()).doesNotContain("0123456");
        assertThat(scanned.amountFixed()).isTrue();
        assertThat(scanned.amount()).isEqualByComparingTo("2500");
    }

    /**
     * An unusable code answers 200 with a reason rather than an error: the user is standing in
     * front of a merchant and needs to know which of expired, cancelled or already paid it is.
     */
    @Test
    void anExpiredCodeIsReadableAndSaysWhyItCannotBePaid() {
        QrCode expired = paymentRequest(new BigDecimal("2500"));
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(expired));

        ScannedQrResponse scanned = service.scan(payer, "REQ-1");

        assertThat(scanned.payable()).isFalse();
        assertThat(scanned.reason()).isEqualTo("Ce QR code a expiré");
    }

    @Test
    void anUnknownCodeIsNotFound() {
        when(repository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.scan(payer, "NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- paying -----------------------------------------------------------

    /**
     * A QR is a way to address a payment, not a second kind of payment. Going through
     * {@code TransactionService.transfer} is what keeps the fee, the KYC ceiling and the ledger
     * entry identical to a typed transfer — a separate path here would be a way around them.
     */
    @Test
    void payingGoesThroughTheOrdinaryTransferPath() {
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(paymentRequest(new BigDecimal("2500"))));

        service.pay(payer, "REQ-1", new QrPaymentRequest(null, null, null));

        ArgumentCaptor<TransferRequest> transfer = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transactionService).transfer(eq(payer), transfer.capture());

        assertThat(transfer.getValue().amount()).isEqualByComparingTo("2500");
        assertThat(transfer.getValue().currency()).isEqualTo(Currency.XOF);
        assertThat(transfer.getValue().recipientPhone()).isEqualTo("+22890123456");
        assertThat(transfer.getValue().description()).isEqualTo("Table 4");
    }

    /**
     * A payer who typed one number and was charged another has been lied to, even when the
     * difference is in their favour. So a mismatch is refused, not silently overridden.
     */
    @Test
    void anAmountThatContradictsThePaymentRequestIsRefused() {
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(paymentRequest(new BigDecimal("2500"))));

        assertThatThrownBy(() -> service.pay(payer, "REQ-1",
                new QrPaymentRequest(Currency.XOF, new BigDecimal("100"), null)))
                .isInstanceOf(BadRequestException.class);

        verify(transactionService, never()).transfer(any(), any());
    }

    @Test
    void aStaticCodeNeedsTheAmountAndCurrencyFromThePayer() {
        when(repository.findByCode("STATIC-1")).thenReturn(Optional.of(staticCode()));

        assertThatThrownBy(() -> service.pay(payer, "STATIC-1",
                new QrPaymentRequest(Currency.XOF, null, null)))
                .isInstanceOf(BadRequestException.class);

        service.pay(payer, "STATIC-1", new QrPaymentRequest(Currency.XOF, new BigDecimal("750"), "Merci"));

        ArgumentCaptor<TransferRequest> transfer = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transactionService).transfer(eq(payer), transfer.capture());
        assertThat(transfer.getValue().amount()).isEqualByComparingTo("750");
    }

    /** A receipt someone photographs must not be payable twice. */
    @Test
    void aPaymentRequestIsBurnedOnceItIsPaid() {
        QrCode request = paymentRequest(new BigDecimal("2500"));
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(request));

        service.pay(payer, "REQ-1", new QrPaymentRequest(null, null, null));

        assertThat(request.getStatus()).isEqualTo(QrCodeStatus.USED);
        assertThat(request.getPaidByUserId()).isEqualTo(payer);
        assertThat(request.getTransactionReference()).isEqualTo("TRX-1");

        assertThatThrownBy(() -> service.pay(payer, "REQ-1", new QrPaymentRequest(null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    /** A permanent code is the opposite: it is a business card, and stays payable. */
    @Test
    void aStaticCodeStaysPayableAfterUse() {
        QrCode permanent = staticCode();
        when(repository.findByCode("STATIC-1")).thenReturn(Optional.of(permanent));

        service.pay(payer, "STATIC-1", new QrPaymentRequest(Currency.XOF, new BigDecimal("500"), null));

        assertThat(permanent.getStatus()).isEqualTo(QrCodeStatus.ACTIVE);
    }

    @Test
    void aRevokedCodeCannotBePaid() {
        QrCode revoked = paymentRequest(new BigDecimal("2500"));
        revoked.setStatus(QrCodeStatus.REVOKED);
        when(repository.findByCode("REQ-1")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.pay(payer, "REQ-1", new QrPaymentRequest(null, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(transactionService, never()).transfer(any(), any());
    }

    @Test
    void payingOnesOwnCodeIsRefused() {
        when(repository.findByCode("STATIC-1")).thenReturn(Optional.of(staticCode()));

        assertThatThrownBy(() -> service.pay(owner, "STATIC-1",
                new QrPaymentRequest(Currency.XOF, new BigDecimal("500"), null)))
                .isInstanceOf(ConflictException.class);
    }

    // --- ownership --------------------------------------------------------

    /** Someone else's code is not theirs to cancel, and reads as missing rather than forbidden. */
    @Test
    void anotherUsersCodeCannotBeRevokedOrDownloaded() {
        when(repository.findByCodeAndOwnerId("REQ-1", payer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(payer, "REQ-1"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.requireOwned(payer, "REQ-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- validity window --------------------------------------------------

    @Test
    void aRequestCannotBeMadeToLiveLongerThanTheConfiguredCeiling() {
        int tooLong = (int) properties.getMaxRequestTtl().toMinutes() + 1;

        assertThatThrownBy(() -> service.createPaymentRequest(owner,
                new CreatePaymentRequestRequest(Currency.XOF, new BigDecimal("2500"), "Table 4", tooLong)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aRequestWithoutAnExplicitTtlFallsBackOnTheDefault() {
        QrCodeResponse created = service.createPaymentRequest(owner,
                new CreatePaymentRequestRequest(Currency.XOF, new BigDecimal("2500"), "Table 4", null));

        Instant expected = Instant.now().plus(properties.getDefaultRequestTtl());
        assertThat(created.expiresAt())
                .isBetween(expected.minusSeconds(60), expected.plusSeconds(60));
    }
}
