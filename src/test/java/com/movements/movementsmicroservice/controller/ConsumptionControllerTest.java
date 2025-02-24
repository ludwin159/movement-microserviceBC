package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.service.ConsumptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ConsumptionController.class)
class ConsumptionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsumptionService consumptionService;

    @Test
    @DisplayName("Update Consumption")
    void update() {
        Consumption consumption1 = new Consumption();
        consumption1.setId("CONSUMPTION_1");
        consumption1.setAmount(20.0);
        consumption1.setIdCreditCard("CREDIT_CARD001");
        consumption1.setDescription("Consumo en Plaza Vea");


        when(consumptionService.update(eq("CONSUMPTION_1"),  any())).thenReturn(Mono.just(consumption1));

        webTestClient.put().uri("/consumptions/CONSUMPTION_1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consumption1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.amount").isEqualTo(20.0);

    }
}