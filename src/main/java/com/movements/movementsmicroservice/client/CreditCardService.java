package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.CreditCardDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditCardService {
    private final WebClient webClient;

    public CreditCardService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    public Mono<CreditCardDto> findById(String id) {
        return webClient.get()
                .uri("/credit-cards/{id}", id)
                .retrieve()
                .bodyToMono(CreditCardDto.class);
    }

    public Mono<CreditCardDto> update(String id, CreditCardDto creditCardDto) {
        return webClient.put()
                .uri("/credit-cards/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(creditCardDto), CreditCardDto.class)
                .retrieve()
                .bodyToMono(CreditCardDto.class);
    }
    public Flux<CreditCardDto> allCreditCardsByIdClientWithPaymentAndConsumption(String idClient) {
        return webClient.get()
                .uri("/credit-cards/findAllByIdClientWithMovements/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(CreditCardDto.class);
    }
}
