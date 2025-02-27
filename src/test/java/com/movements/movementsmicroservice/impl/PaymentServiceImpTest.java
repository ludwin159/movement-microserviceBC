package com.movements.movementsmicroservice.impl;

import com.movements.movementsmicroservice.DTO.ClientDto;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.repository.PaymentRepository;
import com.movements.movementsmicroservice.service.impl.PaymentServiceImp;
import org.joda.time.DateTime;
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
import java.util.List;

import static org.mockito.Mockito.*;
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

    private Payment payment1, payment2;
    private CreditDto credit1;
    private CreditCardDto creditCard1;
    private ClientDto personalClient, businessClient;

    @BeforeEach
    void setUp() {

        payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);
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

        payment1.setAmount(45.13);
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
}