package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.CreditDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditService {
    private final WebClient webClient;

    public CreditService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    public Mono<CreditDto> findById(String id) {
        return webClient.get()
                .uri("/credits/{id}", id)
                .retrieve()
                .bodyToMono(CreditDto.class);
    }
    public Mono<CreditDto> update(String id, CreditDto creditCardDto) {
        return webClient.put()
                .uri("/credits/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(creditCardDto)
                .retrieve()
                .bodyToMono(CreditDto.class);
    }

    public Flux<CreditDto> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(String idClient) {
        return webClient.get()
                .uri("/credits/findAllByClientWithPayments/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(CreditDto.class);
    }
}
