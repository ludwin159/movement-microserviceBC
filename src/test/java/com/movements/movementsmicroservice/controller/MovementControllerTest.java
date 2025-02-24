package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.service.MovementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@WebFluxTest(MovementController.class)
class MovementControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MovementService movementService;

    @Test
    @DisplayName("Update controller test")
    void updateTest() {
        Movement movement1 = new Movement();
        movement1.setId("IDMOVEMENT01");
        movement1.setIdBankAccount("IDbank001");
        movement1.setAmount(20.0);
        movement1.setTypeMovement(Movement.TypeMovement.DEPOSIT);
        movement1.setDescription("Deposit Movement");
        movement1.setIdBankAccountTransfer("AAAAAAA111111");

        when(movementService.update(movement1.getId(), movement1)).thenReturn(Mono.just(movement1));
        webTestClient.put().uri("/movements/IDMOVEMENT01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movement1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("IDMOVEMENT01")
                .jsonPath("$.amount").isEqualTo(20.0);
    }

    @Test
    @DisplayName("Get all report for id client")
    void getBankProductsByIdClientTest() {
        String idClient = "Client001";
        Map<String, List<?>> mockResponse = Map.of(
                "bankAccounts", Collections.emptyList(),
                "credits", Collections.emptyList(),
                "creditCards", Collections.emptyList());

        when(movementService.getBankProductsByIdClient(idClient)).thenReturn(Mono.just(mockResponse));
        webTestClient.get().uri("/movements/bank-products-by-client/{idClient}", idClient)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bankAccounts").isArray();

    }
}