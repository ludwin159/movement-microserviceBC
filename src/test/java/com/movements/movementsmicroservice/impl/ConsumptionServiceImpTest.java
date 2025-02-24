package com.movements.movementsmicroservice.impl;

import com.movements.movementsmicroservice.DTO.ClientDto;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.exceptions.ConsumeNotValidException;
import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.repository.ConsumptionRepository;
import com.movements.movementsmicroservice.service.impl.ConsumptionServiceImp;
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


import static com.movements.movementsmicroservice.DTO.ClientDto.TypeClient.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceImpTest {

    @InjectMocks
    private ConsumptionServiceImp consumptionService;

    @Mock
    private CreditCardService creditCardService;
    @Mock
    private ConsumptionRepository consumptionRepository;

    private Consumption consumption1;
    private CreditCardDto creditCard1;
    private ClientDto personalClient;

    @BeforeEach
    void setUp() {
//        when(repositoryFactory.getRepository(any())).thenReturn(consumptionRepository);

        creditCard1 = new CreditCardDto();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        creditCard1.setInterestRate(3.0);

        consumption1 = new Consumption();
        consumption1.setId("CONSUMPTION_1");
        consumption1.setAmount(20.0);
        consumption1.setIdCreditCard("CREDIT_CARD001");
        consumption1.setDescription("Consumo en Plaza Vea");

        personalClient = new ClientDto();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setIdentity("75690210");

    }

    @Test
    @DisplayName("Create a consumption correctly")
    void createCorrectConsumption() {
        String idCreditCard = creditCard1.getId();
        // Given
        when(creditCardService.findById(idCreditCard)).thenReturn(Mono.just(creditCard1));
        when(creditCardService.update(idCreditCard, creditCard1)).thenReturn(Mono.just(creditCard1));
        when(consumptionRepository.save(consumption1)).thenReturn(Mono.just(consumption1));
        // WHen
        Mono<Consumption> consumptionMono = consumptionService.create(consumption1);
        // Then
        StepVerifier.create(consumptionMono)
                .expectNextMatches(consumption -> consumption.getId().equals(consumption1.getId()))
                .verifyComplete();
        verify(creditCardService).findById(idCreditCard);
        verify(creditCardService).update(idCreditCard, creditCard1);
        verify(consumptionRepository).save(consumption1);
    }

    @Test
    @DisplayName("Create a consumption when the consumption is greater than the available balance.")
    void createGraterConsumptionThanBalanceTest() {
        String idCreditCard = creditCard1.getId();
        consumption1.setAmount(501.0);
        // Given
        when(creditCardService.findById(idCreditCard)).thenReturn(Mono.just(creditCard1));
        // WHen
        Mono<Consumption> consumptionMono = consumptionService.create(consumption1);
        // Then
        StepVerifier.create(consumptionMono)
                .expectError(ConsumeNotValidException.class)
                .verify();
        verify(creditCardService).findById(idCreditCard);
    }

    @Test
    @DisplayName("Basic update to consumption")
    void updateBasicTest() {
        String idUpdate = "CONSUMPTION_1";
        Consumption consumption = new Consumption();
        consumption.setId("CONSUMPTION_1");
        consumption.setAmount(22.50);
        consumption.setIdCreditCard("CREDIT_CARD001");
        consumption.setDescription("Bea's consume");
        // Given
        when(consumptionRepository.findById(idUpdate)).thenReturn(Mono.just(consumption1));
        when(consumptionRepository.save(consumption1)).thenReturn(Mono.just(consumption1));
        // When
        Mono<Consumption> consumptionMono = consumptionService.update(idUpdate, consumption);
        // Then
        StepVerifier.create(consumptionMono)
                .expectNextMatches(consumption2 -> consumption2.getId().equals(idUpdate)
                        && consumption2.getAmount() == 22.5)
                .verifyComplete();
    }

    @Test
    @DisplayName("Find all consumptions for id client")
    void findAllConsumptionsByIdCreditCardAndSortByDate() {
        String idClient = "clientN001";
        // Given
        when(consumptionRepository.findAllByIdCreditCard(idClient)).thenReturn(Flux.just(consumption1));
        // When
        Flux<Consumption> consumptionFlux = consumptionService.findAllConsumptionsByIdCreditCardAndSortByDate(idClient);
        // Then
        StepVerifier.create(consumptionFlux)
                .expectNextMatches(consumption -> consumption.getId().equals(consumption1.getId()))
                .verifyComplete();
    }
}