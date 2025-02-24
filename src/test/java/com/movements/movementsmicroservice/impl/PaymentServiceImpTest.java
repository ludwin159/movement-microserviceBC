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

        payment2 = new Payment();
        payment2.setId("PAYMENT002");
        payment2.setIdProductCredit("CREDIT_CARD001");
        payment2.setAmount(25.0);
        payment2.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT_CARD);

        credit1 = new CreditDto(
                "CREDIT001",
                "clientN001",
                500.0,
                200.0,
                15.0,
                CreditDto.TypeCredit.PERSONAL_CREDIT
        );
        credit1.setId("CREDIT001");

        creditCard1 = new CreditCardDto();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        creditCard1.setInterestRate(3.0);

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
    @DisplayName("Create a payment credit")
    void createPaymentCreditTest() {

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
}