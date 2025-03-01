package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    void update() {
        Payment payment1 = new Payment();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
        payment1.setTypeCreditProduct(Payment.TypeCreditProduct.CREDIT);
        payment1.setIdPayer("");
        payment1.setIdPayer("");
        payment1.setTypePayer(Payment.TypePayer.EXTERNAL);

        when(paymentService.update("PAYMENT001", payment1)).thenReturn(Mono.just(payment1));

        webTestClient.put().uri("/payments/PAYMENT001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payment1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.idProductCredit").isEqualTo("CREDIT001");

    }
}