package com.movements.movementsmicroservice.impl;

import com.movements.movementsmicroservice.DTO.*;
import com.movements.movementsmicroservice.client.ClientService;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.client.DebitCardService;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.repository.PaymentRepository;
import com.movements.movementsmicroservice.service.MovementService;
import com.movements.movementsmicroservice.service.PaymentMovementService;
import com.movements.movementsmicroservice.service.impl.PaymentServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.*;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class PaymentServiceImpTest {
    @InjectMocks
    private PaymentServiceImp paymentService;
    @Mock
    private CreditService creditService;
    @Mock
    private CreditCardService creditCardService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private Clock clock;
    @Mock
    private ClientService clientService;
    @Mock
    private DebitCardService debitCardService;
    @Mock
    private PaymentMovementService paymentMovementService;
    @Mock
    private MovementService movementService;
    private Payment payment1, payment2;
    private CreditDto credit1;
    private CreditCardDto creditCard1;
    private ClientDto personalClient, businessClient;
    private BankAccountDto bankAccount1, bankAccount2;
    private DebitCardDto debitCard1;

    @BeforeEach
    void setUp() {

        bankAccount1 = new BankAccountDto("clientN001",
                1500.0,
                BankAccountDto.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount1.setId("IDbank001");

        bankAccount2 = new BankAccountDto("clientN002",
                1200.0,
                BankAccountDto.TypeBankAccount.CURRENT_ACCOUNT,
                0,
                0,
                15.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount2.setId("IDbank002");

        debitCard1 = new DebitCardDto();
        debitCard1.setId("DEBITCARD001");
        debitCard1.setIdClient("clientN001");
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());

        payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment1.setMonthCorresponding(3);
        payment1.setYearCorresponding(2025);
        payment1.setIdPayer("");

        payment2 = new Payment();
        payment2.setId("PAYMENT002");
        payment2.setIdProductCredit("CREDIT_CARD001");
        payment2.setAmount(25.0);
        payment2.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);
        payment2.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment2.setIdPayer("");

        credit1 = new CreditDto(
                "CREDIT001",
                "clientN001",
                500.0,
                500.0,
                0.5,
                CreditDto.TypeCredit.PERSONAL_CREDIT,
                LocalDate.of(2025, 2, 23),
                LocalDate.of(2025, 3, 2),
                12,
                45.13
        );
        credit1.setId("CREDIT001");

        creditCard1 = new CreditCardDto();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        creditCard1.setInterestRate(3.0);
        creditCard1.setTotalDebt(500.0);

        personalClient = new ClientDto();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(ClientDto.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setIdentity("75690210");

        businessClient = new ClientDto();
        businessClient.setId("clientN002");
        businessClient.setAddress("Jr. avenida Jose Pérez");
        businessClient.setTypeClient(ClientDto.TypeClient.BUSINESS_CLIENT);
        businessClient.setEmail("business@ejemplo.com");
        businessClient.setBusinessName("Panadería Boluelos");
        businessClient.setTaxId("20756902101");
    }

    @Test
    @DisplayName("Create a payment credit with pending balance is zero")
    void createPaymentWithAmountZeroTest() {
        payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment1.setIdPayer("");
        String creditId = credit1.getId();
        credit1.setPendingBalance(0.0);
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
        verify(creditService).findById(creditId);
    }

    @Test
    @DisplayName("Create a payment credit with monthlyFee is different")
    void createPaymentCreditWithAmountDifferentTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-05-02T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setYearCorresponding(2025);
        payment1.setMonthCorresponding(3);
        payment1.setDatePayment(LocalDateTime.of(2025, 3, 2, 5, 0));
        payment1.setAmount(45.13);
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment1.setIdPayer("");

        payment2 = new Payment();
        payment2.setId("PAYMENT002");
        payment2.setIdProductCredit("CREDIT001");
        payment2.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment2.setYearCorresponding(2025);
        payment2.setMonthCorresponding(4);
        payment1.setDatePayment(LocalDateTime.of(2025, 4, 2, 5, 0));
        payment2.setAmount(45.13);
        payment2.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment2.setIdPayer("");

        Payment paymentNew = new Payment();
        paymentNew.setId("PAYMENT003");
        paymentNew.setIdProductCredit("CREDIT001");
        paymentNew.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        paymentNew.setYearCorresponding(2025);
        paymentNew.setMonthCorresponding(5);
        paymentNew.setAmount(45.19);
        paymentNew.setDatePayment(LocalDateTime.now(clock));
        paymentNew.setTypePayer(Payment.TypePayer.EXTERNAL);
        paymentNew.setIdPayer("");

        String creditId = credit1.getId();
        credit1.setPayments(List.of(payment1, payment2));
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        // When
        Mono<Payment> paymentMono = paymentService.create(paymentNew);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
        verify(creditService).findById(creditId);
    }

    @Test
    @DisplayName("Create a payment credit with existing pay")
    void createPaymentCreditWithExistingPayTest() {
        payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setDatePayment(LocalDateTime.of(2025, 3, 2, 5, 0));
        payment1.setYearCorresponding(2025);
        payment1.setMonthCorresponding(3);
        payment1.setAmount(45.13);
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment1.setIdPayer("");

        payment2 = new Payment();
        payment2.setId("PAYMENT002");
        payment2.setIdProductCredit("CREDIT001");
        payment2.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setDatePayment(LocalDateTime.of(2025, 4, 2, 5, 0));
        payment2.setYearCorresponding(2025);
        payment2.setMonthCorresponding(4);
        payment2.setAmount(45.13);
        payment2.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment2.setIdPayer("");

        Payment paymentNew = new Payment();
        paymentNew.setId("PAYMENT003");
        paymentNew.setIdProductCredit("CREDIT001");
        paymentNew.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        paymentNew.setDatePayment(LocalDateTime.of(2025, 4, 6, 5, 0));
        paymentNew.setYearCorresponding(2025);
        paymentNew.setMonthCorresponding(4);
        paymentNew.setAmount(45.13);
        paymentNew.setTypePayer(Payment.TypePayer.EXTERNAL);
        paymentNew.setIdPayer("");

        String creditId = credit1.getId();
        credit1.setPayments(List.of(payment1, payment2));
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        // When
        Mono<Payment> paymentMono = paymentService.create(paymentNew);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
        verify(creditService).findById(creditId);
    }
    @Test
    @DisplayName("Create a payment credit")
    void createPaymentCreditTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        credit1.setFirstDatePay(LocalDate.of(2025, 2, 21));
        payment1.setAmount(45.13);
        payment1.setDatePayment(LocalDateTime.now(clock));
        String creditId = credit1.getId();
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        when(creditService.update(creditId, credit1)).thenReturn(Mono.just(credit1));
        when(paymentRepository.save(payment1)).thenReturn(Mono.just(payment1));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .expectNextMatches(element -> element.getId().equals("PAYMENT001"))
                .verifyComplete();
        verify(creditService).findById(creditId);
        verify(creditService).update(creditId, credit1);
        verify(paymentRepository).save(payment1);
    }

    @Test
    @DisplayName("Create a payment credit card")
    void createPaymentCreditCardTest() {

        String creditCardId = creditCard1.getId();
        payment2.setAmount(500.0);
        // Given
        when(creditCardService.findById(creditCardId)).thenReturn(Mono.just(creditCard1));
        when(creditCardService.update(creditCardId, creditCard1)).thenReturn(Mono.just(creditCard1));
        when(paymentRepository.save(payment2)).thenReturn(Mono.just(payment2));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment2);
        // Then
        StepVerifier.create(paymentMono)
                .expectNextMatches(element -> element.getId().equals("PAYMENT002"))
                .verifyComplete();
        verify(creditCardService).findById(creditCardId);
        verify(creditCardService).update(creditCardId, creditCard1);
        verify(paymentRepository).save(payment2);
    }
    @Test
    @DisplayName("Create a payment for more than debt CreditCard")
    void createPaymentMoreDebtTest() {
        String creditCardId = creditCard1.getId();
        creditCard1.setAvailableBalance(creditCard1.getLimitCredit());
        // Given
        when(creditCardService.findById(creditCardId)).thenReturn(Mono.just(creditCard1));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment2);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
        verify(creditCardService).findById(creditCardId);
    }
    @Test
    @DisplayName("Create a payment for more than debt Credit")
    void createPaymentMoreDebtCreditTest() {
        String creditId = credit1.getId();
        credit1.setPendingBalance(0.0);
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a Unsupported payment")
    void createUnsupportedPaymentTest() {
        // Given
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.UNSUPPORTED);
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(InvalidPayException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a payment with a credit not found")
    void createPaymentWithoutCredit() {
        // Given
        String idNotExist = "asdf65a6s5df";
        payment1.setIdProductCredit(idNotExist);
        when(creditService.findById(idNotExist)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a payment with a credit card not found")
    void createPaymentWithoutCreditCard() {
        // Given
        String idNotExist = "asdf65a6s5df";
        payment2.setIdProductCredit(idNotExist);
        when(creditCardService.findById(idNotExist)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment2);
        // Then
        StepVerifier.create(paymentMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("update a simple payment")
    void updatePaymentSimpleTest() {

        Payment paymentUpd = new Payment();
        paymentUpd.setId("PAYMENT001");
        paymentUpd.setIdProductCredit("CREDIT001");
        paymentUpd.setAmount(20.0);
        paymentUpd.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        // Given
        when(paymentRepository.findById(payment1.getId())).thenReturn(Mono.just(payment1));
        when(paymentRepository.save(payment1)).thenReturn(Mono.just(payment1));
        // When
        Mono<Payment> paymentMono = paymentService.update(payment1.getId(), paymentUpd);
        // Then
        StepVerifier.create(paymentMono)
                .expectNextMatches(element -> element.getId().equals("PAYMENT001"))
                .verifyComplete();
        verify(paymentRepository).findById(payment1.getId());
        verify(paymentRepository).save(payment1);
    }

    @Test
    @DisplayName("Find resources for report")
    void findAllPaymentByIdProductCreditAndSortByDate() {
        String idProductCredit = "ALLCREDITS_123";
        // Given
        when(paymentRepository.findAllByIdProductCredit(idProductCredit)).thenReturn(Flux.just(payment2, payment1));
        // When
        Flux<Payment> paymentFlux = paymentService.findAllPaymentByIdProductCreditAndSortByDate(idProductCredit);
        // Then
        StepVerifier.create(paymentFlux).expectNextCount(2)
                .verifyComplete();
        verify(paymentRepository).findAllByIdProductCredit(idProductCredit);
    }

    @Test
    @DisplayName("Pay external type and id payer not empty")
    void payExternalAndIdNotEmpty() {
        String idProductCredit = "ALLCREDITS_123";
        // Given
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
        payment1.setIdPayer("asdfasdf21a2s");
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .expectError(InvalidPayException.class)
                .verify();
    }
    @Test
    @DisplayName("Pay credit Card with a client type and id payer not empty")
    void payClientAndIdNotEmpty() {
        // Given
        payment1.setTypePayer(Payment.TypePayer.CLIENT);
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);
        payment1.setIdProductCredit(creditCard1.getId());
        payment1.setIdPayer(personalClient.getId());
        payment1.setAmount(500.0);
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(creditCardService.findById(creditCard1.getId())).thenReturn(Mono.just(creditCard1));
        when(creditCardService.update(creditCard1.getId(), creditCard1)).thenReturn(Mono.just(creditCard1));
        when(paymentRepository.save(payment1)).thenReturn(Mono.just(payment1));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTypeCreditProduct()).isEqualTo(Payment.TypeCreditProduct.CREDIT_CARD);
                    assertThat(response.getAmount()).isEqualTo(500.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Pay credit with a client type and id payer not empty")
    void payCreditOnlyWithClientAndIdNotEmpty() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-05-02T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        // Given
        payment1.setTypePayer(Payment.TypePayer.CLIENT);
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setIdProductCredit(credit1.getId());
        payment1.setIdPayer(personalClient.getId());
        payment1.setAmount(45.13);
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setMonthCorresponding(5);
        payment1.setYearCorresponding(2025);
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(creditService.findById(credit1.getId())).thenReturn(Mono.just(credit1));
        when(creditService.update(credit1.getId(), credit1)).thenReturn(Mono.just(credit1));
        when(paymentRepository.save(payment1)).thenReturn(Mono.just(payment1));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTypeCreditProduct()).isEqualTo(Payment.TypeCreditProduct.CREDIT);
                    assertThat(response.getAmount()).isEqualTo(45.13);
                    assertThat(response.getPenaltyFee()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Pay credit with a debit card type and id payer not empty")
    void payCreditWithDebitCardTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-05-02T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        payment1.setTypePayer(Payment.TypePayer.DEBIT_CARD);
        payment1.setIdPayer(debitCard1.getId());
        payment1.setAmount(credit1.getMonthlyFee());
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setMonthCorresponding(5);
        payment1.setYearCorresponding(2025);

        Movement withdrawal = new Movement();
        withdrawal.setTypeMovement(Movement.TypeMovement.PAY_CREDIT);
        withdrawal.setIdBankAccount(bankAccount1.getId());
        withdrawal.setDescription("Pay with debit card");
        withdrawal.setDate(LocalDateTime.now(clock));
        withdrawal.setAmount(payment1.getAmount());
        withdrawal.setIdTransfer("");
        withdrawal.setIdBankAccountTransfer("");
        // Given
        debitCard1.setBankAccounts(List.of(bankAccount1, bankAccount2));


        when(creditService.findById(credit1.getId())).thenReturn(Mono.just(credit1));
        when(debitCardService.findByIdWithBankAccountsOrderByCreatedAt(debitCard1.getId()))
                .thenReturn(Mono.just(debitCard1));
        when(movementService.create(withdrawal)).thenReturn(Mono.just(withdrawal));
        when(creditService.update(credit1.getId(), credit1)).thenReturn(Mono.just(credit1));
        when(paymentRepository.save((payment1))).thenReturn(Mono.just(payment1));
        when(paymentMovementService.getBankAccountWithBalanceAvailableForPay(any(),eq(45.13)))
                .thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTypeCreditProduct()).isEqualTo(Payment.TypeCreditProduct.CREDIT);
                    assertThat(response.getAmount()).isEqualTo(45.13);
                    assertThat(response.getPenaltyFee()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Pay credit card with a debit card type and id payer not empty")
    void payCreditCardWithDebitCardTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-05-02T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        payment1.setTypePayer(Payment.TypePayer.DEBIT_CARD);
        payment1.setIdPayer(debitCard1.getId());
        payment1.setIdProductCredit(creditCard1.getId());
        payment1.setAmount(creditCard1.getTotalDebt());
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);

        Movement withdrawal = new Movement();
        withdrawal.setTypeMovement(Movement.TypeMovement.PAY_CREDIT);
        withdrawal.setIdBankAccount(bankAccount1.getId());
        withdrawal.setDescription("Pay with debit card");
        withdrawal.setDate(LocalDateTime.now(clock));
        withdrawal.setAmount(payment1.getAmount());
        withdrawal.setIdTransfer("");
        withdrawal.setIdBankAccountTransfer("");
        // Given
        debitCard1.setBankAccounts(List.of(bankAccount1, bankAccount2));


        when(creditCardService.findById(creditCard1.getId())).thenReturn(Mono.just(creditCard1));
        when(debitCardService.findByIdWithBankAccountsOrderByCreatedAt(debitCard1.getId()))
                .thenReturn(Mono.just(debitCard1));
        when(movementService.create(withdrawal)).thenReturn(Mono.just(withdrawal));
        when(creditCardService.update(creditCard1.getId(), creditCard1)).thenReturn(Mono.just(creditCard1));
        when(paymentRepository.save((payment1))).thenReturn(Mono.just(payment1));
        when(paymentMovementService.getBankAccountWithBalanceAvailableForPay(any(),eq(500.0)))
                .thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTypeCreditProduct()).isEqualTo(Payment.TypeCreditProduct.CREDIT_CARD);
                    assertThat(response.getAmount()).isEqualTo(500.0);
                    assertThat(response.getPenaltyFee()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Pay credit card with a debit card type and principal not enough balance")
    void payCreditCardWithDebitCardWithoutBalanceTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-05-02T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        payment1.setTypePayer(Payment.TypePayer.DEBIT_CARD);
        payment1.setIdPayer(debitCard1.getId());
        payment1.setIdProductCredit(creditCard1.getId());
        payment1.setAmount(creditCard1.getTotalDebt());
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);

        Movement withdrawal = new Movement();
        withdrawal.setTypeMovement(Movement.TypeMovement.PAY_CREDIT);
        withdrawal.setIdBankAccount(bankAccount2.getId());
        withdrawal.setDescription("Pay with debit card");
        withdrawal.setDate(LocalDateTime.now(clock));
        withdrawal.setAmount(payment1.getAmount());
        withdrawal.setIdTransfer("");
        withdrawal.setIdBankAccountTransfer("");
        bankAccount1.setBalance(5.0);
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());

        // Given
        debitCard1.setBankAccounts(List.of(bankAccount1, bankAccount2));


        when(creditCardService.findById(creditCard1.getId())).thenReturn(Mono.just(creditCard1));
        when(debitCardService.findByIdWithBankAccountsOrderByCreatedAt(debitCard1.getId()))
                .thenReturn(Mono.just(debitCard1));
        when(movementService.create(withdrawal)).thenReturn(Mono.just(withdrawal));
        when(creditCardService.update(creditCard1.getId(), creditCard1)).thenReturn(Mono.just(creditCard1));
        when(paymentRepository.save((payment1))).thenReturn(Mono.just(payment1));
        when(paymentMovementService.getBankAccountWithBalanceAvailableForPay(any(),eq(500.0)))
                .thenReturn(Mono.just(bankAccount2));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTypeCreditProduct()).isEqualTo(Payment.TypeCreditProduct.CREDIT_CARD);
                    assertThat(response.getAmount()).isEqualTo(500.0);
                    assertThat(response.getPenaltyFee()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Pay a credit card when no account has a balance")
    void payCreditCardWhenNoAccountHasBalanceTest() {
        payment1.setTypePayer(Payment.TypePayer.DEBIT_CARD);
        payment1.setIdPayer(debitCard1.getId());
        payment1.setIdProductCredit(creditCard1.getId());
        payment1.setAmount(creditCard1.getTotalDebt());
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);

        bankAccount1.setBalance(5.0);
        bankAccount2.setBalance(5.0);
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());

        // Given
        debitCard1.setBankAccounts(List.of(bankAccount1, bankAccount2));

        when(creditCardService.findById(creditCard1.getId())).thenReturn(Mono.just(creditCard1));
        when(debitCardService.findByIdWithBankAccountsOrderByCreatedAt(debitCard1.getId()))
                .thenReturn(Mono.just(debitCard1));
        when(paymentMovementService.getBankAccountWithBalanceAvailableForPay(any(),eq(500.0)))
                .thenReturn(Mono.error(new InvalidPayException("")));
        // When
        Mono<Payment> payment = paymentService.create(payment1);
        // Then
        StepVerifier.create(payment)
                .expectError(InvalidPayException.class)
                .verify();
    }

    @Test
    @DisplayName("Credit payment with due date")
    void creditPaymentWithDueDateTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        payment1.setAmount(48.14); // Pay with interest rate
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setMonthCorresponding(2);
        payment1.setYearCorresponding(2025);
        credit1.setFirstDatePay(LocalDate.now(clock).withDayOfMonth(16));
        String creditId = credit1.getId();
        // Given
        when(creditService.findById(creditId)).thenReturn(Mono.just(credit1));
        when(creditService.update(creditId, credit1)).thenReturn(Mono.just(credit1));
        when(paymentRepository.save(payment1)).thenReturn(Mono.just(payment1));
        // When
        Mono<Payment> paymentMono = paymentService.create(payment1);
        // Then
        StepVerifier.create(paymentMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo("PAYMENT001");
                    assertThat(element.getAmount()).isEqualTo(45.13);
                    assertThat(element.getPenaltyFee()).isEqualTo(3.01);
                })
                .verifyComplete();
        verify(creditService).findById(creditId);
        verify(creditService).update(creditId, credit1);
        verify(paymentRepository).save(payment1);
    }

}